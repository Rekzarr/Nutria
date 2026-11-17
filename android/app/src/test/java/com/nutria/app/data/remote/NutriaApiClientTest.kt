package com.nutria.app.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NutriaApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: NutriaApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = NutriaApiClient(
            // MockWebServer uses the socket's reverse-DNS canonical name for url(), which is
            // not guaranteed to be loopback in this host environment.
            baseUrl = HttpUrl.Builder()
                .scheme("http")
                .host("127.0.0.1")
                .port(server.port)
                .build()
                .toString(),
            allowCleartextForLocalDebug = true,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun analysisMapsFrozenServerErrorStatusesToActionableKinds() = runBlocking {
        val expected = listOf(
            400 to NutriaApiErrorKind.INVALID_INPUT,
            413 to NutriaApiErrorKind.PAYLOAD_TOO_LARGE,
            429 to NutriaApiErrorKind.RATE_LIMITED,
            502 to NutriaApiErrorKind.UPSTREAM_FAILURE,
            503 to NutriaApiErrorKind.SERVICE_UNAVAILABLE,
            504 to NutriaApiErrorKind.TIMEOUT,
        )

        expected.forEach { (status, kind) ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(status)
                    .setHeader("Content-Type", "application/problem+json")
                    .setBody("""{"title":"Error del servidor"}"""),
            )

            val result = client.analyzeMeal(
                MealPhoto("meal.jpg", "image/jpeg", byteArrayOf(1, 2, 3)),
            )
            val error = (result as NutriaApiResult.Failure).error

            assertEquals(status, error.httpStatus)
            assertEquals(kind, error.kind)
            assertEquals("Error del servidor", error.message)
        }
    }

    @Test
    fun analysisSendsOnlyExpectedMultipartFieldsAndMapsCompleteResponse() = runBlocking {
        server.enqueue(jsonResponse("""{"name":"Ensalada","calories":220,"protein":12,"carbs":18,"fat":9}"""))
        val bytes = byteArrayOf(0x10, 0x20, 0x30)

        val result = client.analyzeMeal(
            photo = MealPhoto("""C:\privado\plato.webp""", "image/webp", bytes),
            name = "  Ensalada de casa  ",
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        val analysis = (result as NutriaApiResult.Success).value

        assertEquals("Ensalada", analysis.name)
        assertEquals(220.0, analysis.calories, 0.0)
        assertEquals("POST", request.method)
        assertEquals("/api/analyze-meal", request.requestUrl?.encodedPath)
        assertTrue(request.getHeader("Content-Type").orEmpty().startsWith("multipart/form-data;"))
        assertTrue(body.contains("""name="photo"; filename="plato.webp""""))
        assertFalse(body.contains("privado"))
        assertTrue(body.contains("Content-Type: image/webp"))
        assertTrue(body.contains("""name="name""""))
        assertTrue(body.contains("Ensalada de casa"))
        assertFalse(body.contains("OPENAI_API_KEY"))
        assertFalse(body.contains("cycle"))
        assertFalse(body.contains("steps"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun invalidOrOversizedPhotoIsRejectedBeforeUpload() = runBlocking {
        val unsupported = client.analyzeMeal(MealPhoto("meal.gif", "image/gif", byteArrayOf(1)))
        val oversized = client.analyzeMeal(
            MealPhoto(
                "meal.jpg",
                "image/jpeg",
                ByteArray(NutriaApiClient.MAX_PHOTO_BYTES + 1),
            ),
        )

        assertEquals(NutriaApiErrorKind.INVALID_INPUT, (unsupported as NutriaApiResult.Failure).error.kind)
        assertEquals(NutriaApiErrorKind.PAYLOAD_TOO_LARGE, (oversized as NutriaApiResult.Failure).error.kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun photoRequestIsNotAutomaticallyRetriedAfterConnectionDrop() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = client.analyzeMeal(MealPhoto("meal.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))

        assertTrue(result is NutriaApiResult.Failure)
        server.takeRequest()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun incompleteOrNegativeAnalysisResponseIsRejected() = runBlocking {
        server.enqueue(jsonResponse("""{"name":"Comida","calories":100,"protein":5,"carbs":4}"""))

        val incomplete = client.analyzeMeal(MealPhoto("meal.jpg", "image/jpeg", byteArrayOf(1)))
        assertEquals(
            NutriaApiErrorKind.INVALID_RESPONSE,
            (incomplete as NutriaApiResult.Failure).error.kind,
        )

        server.enqueue(
            jsonResponse(
                """{"name":"Comida","calories":-1,"protein":5,"carbs":4,"fat":3}""",
            ),
        )
        val negative = client.analyzeMeal(MealPhoto("meal.jpg", "image/jpeg", byteArrayOf(1)))
        assertEquals(NutriaApiErrorKind.INVALID_RESPONSE, (negative as NutriaApiResult.Failure).error.kind)
    }

    @Test
    fun cleartextIsRejectedUnlessExplicitLocalDebugModeIsEnabled() {
        val exception = runCatching {
            NutriaApiClient("http://example.test/")
        }.exceptionOrNull()

        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun localDebugOptInStillRejectsRemoteCleartextHosts() {
        val exception = runCatching {
            NutriaApiClient(
                baseUrl = "http://example.test/",
                allowCleartextForLocalDebug = true,
            )
        }.exceptionOrNull()

        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
