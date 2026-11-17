package com.nutria.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Typed access to the Nutria photo-analysis endpoint.
 *
 * This client deliberately has no OpenAI/Open Food Facts credentials or provider URLs. It does
 * not persist request or response data; callers should invoke it only after the user action that
 * authorizes the corresponding request.
 */
class NutriaApiClient(
    baseUrl: String,
    allowCleartextForLocalDebug: Boolean = false,
) : MealAnalysisApiRepository {
    private val serviceUrl = parseServiceUrl(baseUrl, allowCleartextForLocalDebug)
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // The server's upstream photo-analysis timeout is 90 seconds.
        .callTimeout(110, TimeUnit.SECONDS)
        .readTimeout(110, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // In particular, do not replay a cost-bearing photo-analysis POST automatically.
        .retryOnConnectionFailure(false)
        .build()

    override suspend fun analyzeMeal(
        photo: MealPhoto,
        name: String?,
    ): NutriaApiResult<MealAnalysis> {
        val mimeType = photo.mediaType.substringBefore(';').trim().lowercase()
        if (photo.bytes.isEmpty() || mimeType !in SUPPORTED_IMAGE_TYPES) {
            return failure(
                NutriaApiErrorKind.INVALID_INPUT,
                "Selecciona una imagen JPG, PNG o WebP válida.",
            )
        }
        if (photo.bytes.size > MAX_PHOTO_BYTES) {
            return failure(
                NutriaApiErrorKind.PAYLOAD_TOO_LARGE,
                "La imagen supera el límite de 10 MiB.",
            )
        }

        val trimmedName = name?.trim()?.takeIf(String::isNotEmpty)
        if (trimmedName != null && trimmedName.length > MAX_MEAL_NAME_LENGTH) {
            return failure(
                NutriaApiErrorKind.INVALID_INPUT,
                "El nombre del plato no puede superar $MAX_MEAL_NAME_LENGTH caracteres.",
            )
        }

        val mediaType = mimeType.toMediaTypeOrNull()
            ?: return failure(NutriaApiErrorKind.INVALID_INPUT, "El formato de imagen no es válido.")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "photo",
                filename = safeFileName(photo.fileName),
                body = photo.bytes.toRequestBody(mediaType),
            )
            .apply {
                if (trimmedName != null) addFormDataPart("name", trimmedName)
            }
            .build()
        val request = Request.Builder()
            .url(endpoint("api", "analyze-meal"))
            .post(requestBody)
            .build()

        return execute(request) { body ->
            val dto = gson.fromJson(body, MealAnalysisResponseDto::class.java)
                ?: return@execute null
            val resultName = dto.name?.takeIf(String::isNotBlank) ?: return@execute null
            val calories = dto.calories?.takeIf { it.isValidNutrient() } ?: return@execute null
            val protein = dto.protein?.takeIf { it.isValidNutrient() } ?: return@execute null
            val carbs = dto.carbs?.takeIf { it.isValidNutrient() } ?: return@execute null
            val fat = dto.fat?.takeIf { it.isValidNutrient() } ?: return@execute null
            MealAnalysis(resultName, calories, protein, carbs, fat)
        }
    }

    private fun endpoint(vararg pathSegments: String): HttpUrl {
        val builder = serviceUrl.newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private suspend fun <T : Any> execute(
        request: Request,
        decodeSuccess: (String) -> T?,
    ): NutriaApiResult<T> = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)
        continuation.cancelCallWhenCancelled(call)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isActive) return
                val error = if (e is InterruptedIOException) {
                    NutriaApiError(
                        kind = NutriaApiErrorKind.TIMEOUT,
                        message = "La solicitud tardó demasiado. Comprueba tu conexión e inténtalo de nuevo.",
                    )
                } else {
                    NutriaApiError(
                        kind = NutriaApiErrorKind.NETWORK,
                        message = "No se pudo conectar con Nutria. Comprueba la URL y tu conexión.",
                    )
                }
                continuation.resume(NutriaApiResult.Failure(error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }

                val result = try {
                    response.use { received ->
                        val body = received.body?.string()
                        if (!received.isSuccessful) {
                            httpFailure(received.code, body)
                        } else if (body == null) {
                            invalidResponse()
                        } else {
                            val decoded = try {
                                decodeSuccess(body)
                            } catch (_: JsonParseException) {
                                null
                            } catch (_: IllegalStateException) {
                                null
                            }
                            decoded?.let { NutriaApiResult.Success(it) } ?: invalidResponse()
                        }
                    }
                } catch (_: IOException) {
                    networkFailure()
                }
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }

    private fun httpFailure(status: Int, body: String?): NutriaApiResult.Failure {
        val serverTitle = body?.let(::parseProblemTitle)
        val (kind, fallback) = when (status) {
            400 -> NutriaApiErrorKind.INVALID_INPUT to
                "Revisa los datos enviados y vuelve a intentarlo."
            413 -> NutriaApiErrorKind.PAYLOAD_TOO_LARGE to
                "La imagen supera el límite de 10 MiB."
            429 -> NutriaApiErrorKind.RATE_LIMITED to
                "Has alcanzado el límite temporal de solicitudes. Inténtalo más tarde."
            502 -> NutriaApiErrorKind.UPSTREAM_FAILURE to
                "El servicio de nutrición no pudo completar la solicitud."
            503 -> NutriaApiErrorKind.SERVICE_UNAVAILABLE to
                "El servicio de Nutria no está disponible en este momento."
            504 -> NutriaApiErrorKind.TIMEOUT to
                "La solicitud tardó demasiado. Comprueba tu conexión e inténtalo de nuevo."
            else -> NutriaApiErrorKind.HTTP to "Nutria no pudo completar la solicitud (HTTP $status)."
        }
        return NutriaApiResult.Failure(
            NutriaApiError(
                kind = kind,
                message = serverTitle ?: fallback,
                httpStatus = status,
                serverTitle = serverTitle,
            ),
        )
    }

    private fun parseProblemTitle(body: String): String? = try {
        gson.fromJson(body, ProblemDetailsDto::class.java)
            ?.title
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    } catch (_: JsonParseException) {
        null
    } catch (_: IllegalStateException) {
        null
    }

    private fun networkFailure() = NutriaApiResult.Failure(
        NutriaApiError(
            kind = NutriaApiErrorKind.NETWORK,
            message = "No se pudo conectar con Nutria. Comprueba la URL y tu conexión.",
        ),
    )

    private fun invalidResponse() = NutriaApiResult.Failure(
        NutriaApiError(
            kind = NutriaApiErrorKind.INVALID_RESPONSE,
            message = "La respuesta del servicio Nutria no es válida.",
        ),
    )

    private fun failure(kind: NutriaApiErrorKind, message: String) =
        NutriaApiResult.Failure(NutriaApiError(kind = kind, message = message))

    private fun parseServiceUrl(
        rawUrl: String,
        allowCleartextForLocalDebug: Boolean,
    ): HttpUrl {
        val url = rawUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("La URL del servicio Nutria no es válida.")
        require(url.username.isEmpty() && url.password.isEmpty()) {
            "La URL del servicio Nutria no debe incluir credenciales."
        }
        require(url.encodedQuery == null && url.encodedFragment == null) {
            "La URL base del servicio Nutria no debe incluir query ni fragmento."
        }
        val host = url.host.trim('[', ']').lowercase()
        val isLoopbackHost = host == "localhost" ||
            host.startsWith("127.") ||
            host == "::1"
        require(
            url.isHttps || (allowCleartextForLocalDebug && isLoopbackHost && url.scheme == "http"),
        ) {
            "La conexión con Nutria debe usar HTTPS; HTTP solo se permite para depuración local " +
                "(host=${url.host}, scheme=${url.scheme})."
        }
        return url
    }

    private fun safeFileName(fileName: String): String {
        val safeName = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it >= ' ' && it != '\u007f' }
            .replace("\"", "_")
            .take(120)
        return safeName.ifBlank { "meal-image" }
    }

    private fun CancellableContinuation<*>.cancelCallWhenCancelled(call: Call) {
        invokeOnCancellation { call.cancel() }
    }

    private data class MealAnalysisResponseDto(
        val name: String? = null,
        val calories: Double? = null,
        val protein: Double? = null,
        val carbs: Double? = null,
        val fat: Double? = null,
    )

    private data class ProblemDetailsDto(
        val title: String? = null,
    )

    companion object {
        const val MAX_MEAL_NAME_LENGTH = 80
        const val MAX_PHOTO_BYTES = 10 * 1024 * 1024
        private val SUPPORTED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")

        private fun Double.isValidNutrient(): Boolean = isFinite() && this >= 0.0
    }
}
