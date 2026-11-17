package com.nutria.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Performs explicit, read-only ingredient searches against the public Open Food Facts API. */
class OpenFoodFactsClient(
    baseUrl: String = SEARCH_A_LICIOUS_BASE_URL,
    allowCleartextForLocalDebug: Boolean = false,
) : IngredientSearchRepository {
    private val baseUrl = parseBaseUrl(baseUrl, allowCleartextForLocalDebug)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    override suspend fun searchIngredients(query: String): NutriaApiResult<List<IngredientProduct>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length !in 1..MAX_QUERY_LENGTH) {
            return failure(
                NutriaApiErrorKind.INVALID_INPUT,
                "Escribe un ingrediente de entre 1 y $MAX_QUERY_LENGTH caracteres.",
            )
        }

        val requestBody = JsonObject().apply {
            addProperty("q", trimmedQuery)
            add("langs", JsonArray().apply {
                add("es")
                add("en")
            })
            addProperty("page_size", MAX_PRODUCTS)
            add("fields", JsonArray().apply {
                listOf("code", "product_name_es", "product_name", "brands", "nutriments")
                    .forEach(::add)
            })
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegment("search").build())
            .header("User-Agent", "Nutria/1.0")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return execute(request) { body ->
            val root = JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject
                ?: return@execute null
            val hits = root.get("hits")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return@execute null
            hits.mapNotNull { element ->
                element.takeIf { it.isJsonObject }?.asJsonObject?.toIngredientProduct()
            }.take(MAX_PRODUCTS)
        }
    }

    private fun JsonObject.toIngredientProduct(): IngredientProduct? {
        val code = readString("code")?.takeIf(String::isNotBlank) ?: return null
        val name = readString("product_name_es")
            ?.takeIf(String::isNotBlank)
            ?: readString("product_name")?.takeIf(String::isNotBlank)
            ?: return null
        val nutriments = get("nutriments")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val calories = nutriments.readNutrient("energy-kcal_100g")
            ?: nutriments.readNutrient("energy-kj_100g")?.div(KILOJOULES_PER_KILOCALORIE)
            ?: return null
        val protein = nutriments.readNutrient("proteins_100g") ?: return null
        val carbs = nutriments.readNutrient("carbohydrates_100g") ?: return null
        val fat = nutriments.readNutrient("fat_100g") ?: return null
        if (calories > MAX_CALORIES_PER_100G ||
            protein > MAX_OTHER_NUTRIENT_PER_100G ||
            carbs > MAX_OTHER_NUTRIENT_PER_100G ||
            fat > MAX_OTHER_NUTRIENT_PER_100G
        ) {
            return null
        }

        return IngredientProduct(
            code = code,
            name = name,
            brand = readString("brands").orEmpty(),
            caloriesPer100g = calories,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
        )
    }

    private fun JsonObject.readString(name: String): String? {
        val value = get(name) ?: return null
        return when {
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString
            value.isJsonArray -> value.asJsonArray.mapNotNull { item ->
                if (item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                    item.asString.trim().takeIf(String::isNotEmpty)
                } else {
                    null
                }
            }.joinToString(", ").takeIf(String::isNotEmpty)
            else -> null
        }
    }

    private fun JsonObject.readNutrient(name: String): Double? {
        val primitive = get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        val value = when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.toDoubleOrNull() ?: return null
            else -> return null
        }
        return value.takeIf { it.isFinite() && it >= 0.0 }
    }

    private suspend fun <T : Any> execute(
        request: Request,
        decodeSuccess: (String) -> T?,
    ): NutriaApiResult<T> = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)
        continuation.cancelCallWhenCancelled(call)
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isActive) return
                val error = if (e is InterruptedIOException) {
                    NutriaApiError(
                        kind = NutriaApiErrorKind.TIMEOUT,
                        message = "La búsqueda en Open Food Facts tardó demasiado. Inténtalo de nuevo.",
                    )
                } else {
                    NutriaApiError(
                        kind = NutriaApiErrorKind.NETWORK,
                        message = "No se pudo conectar con Open Food Facts. Comprueba tu conexión.",
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
                        when {
                            received.code == 429 -> failure(
                                NutriaApiErrorKind.RATE_LIMITED,
                                "Open Food Facts ha limitado las búsquedas. Espera un momento e inténtalo de nuevo.",
                                received.code,
                            )
                            received.code >= 500 -> failure(
                                NutriaApiErrorKind.UPSTREAM_FAILURE,
                                "Open Food Facts no está disponible en este momento.",
                                received.code,
                            )
                            !received.isSuccessful -> failure(
                                NutriaApiErrorKind.HTTP,
                                "Open Food Facts no pudo completar la búsqueda (HTTP ${received.code}).",
                                received.code,
                            )
                            body == null -> invalidResponse()
                            else -> {
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
                    }
                } catch (_: IOException) {
                    NutriaApiResult.Failure(
                        NutriaApiError(
                            NutriaApiErrorKind.NETWORK,
                            "No se pudo leer la respuesta de Open Food Facts.",
                        ),
                    )
                }
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }

    private fun failure(
        kind: NutriaApiErrorKind,
        message: String,
        status: Int? = null,
    ) = NutriaApiResult.Failure(
        NutriaApiError(kind = kind, message = message, httpStatus = status),
    )

    private fun invalidResponse() = failure(
        NutriaApiErrorKind.INVALID_RESPONSE,
        "Open Food Facts devolvió una respuesta no válida.",
    )

    private fun parseBaseUrl(rawUrl: String, allowCleartextForLocalDebug: Boolean): HttpUrl {
        val url = rawUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("La URL de Open Food Facts no es válida.")
        val host = url.host.trim('[', ']').lowercase()
        val isLoopbackHost = host == "localhost" || host.startsWith("127.") || host == "::1"
        require(url.username.isEmpty() && url.password.isEmpty()) {
            "La URL de Open Food Facts no debe incluir credenciales."
        }
        require(url.encodedQuery == null && url.encodedFragment == null) {
            "La URL base de Open Food Facts no debe incluir query ni fragmento."
        }
        require(url.isHttps || (allowCleartextForLocalDebug && isLoopbackHost && url.scheme == "http")) {
            "Open Food Facts requiere HTTPS; HTTP solo se permite para pruebas locales."
        }
        return url
    }

    private fun CancellableContinuation<*>.cancelCallWhenCancelled(call: Call) {
        invokeOnCancellation { call.cancel() }
    }

    companion object {
        const val MAX_QUERY_LENGTH = 80
        const val MAX_PRODUCTS = 8
        const val SEARCH_A_LICIOUS_BASE_URL = "https://search.openfoodfacts.org"
        private const val KILOJOULES_PER_KILOCALORIE = 4.184
        private const val MAX_CALORIES_PER_100G = 10_000.0
        private const val MAX_OTHER_NUTRIENT_PER_100G = 1_000.0
    }
}
