package com.nutria.app.data.remote

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenFoodFactsClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OpenFoodFactsClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpenFoodFactsClient(
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
    fun searchUsesOpenFoodFactsAndParsesSpanishNameAndNutrients() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"hits":[{"code":"123","product_name_es":"Té verde","product_name":"Green tea","brands":["Marca"],"nutriments":{"energy-kcal_100g":42,"proteins_100g":3.1,"carbohydrates_100g":6.2,"fat_100g":1.4}}]}""",
            ),
        )

        val result = client.searchIngredients("  ñame y té  ")
        val request = server.takeRequest()

        assertTrue(result is NutriaApiResult.Success)
        val product = (result as NutriaApiResult.Success).value.single()
        assertEquals("Té verde", product.name)
        assertEquals("Marca", product.brand)
        assertEquals(42.0, product.caloriesPer100g, 0.0)
        assertEquals(3.1, product.proteinPer100g, 0.0)
        assertEquals("/search", request.requestUrl?.encodedPath)
        assertEquals("POST", request.method)
        val requestBody = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("ñame y té", requestBody.get("q").asString)
        assertEquals(8, requestBody.get("page_size").asInt)
        assertEquals("es", requestBody.getAsJsonArray("langs")[0].asString)
        assertEquals("nutriments", requestBody.getAsJsonArray("fields").last().asString)
        assertEquals("Nutria/1.0", request.getHeader("User-Agent"))
    }

    @Test
    fun searchFallsBackToEnglishNameAndConvertsKilojoules() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"hits":[{"code":"456","product_name":"English name","nutriments":{"energy-kj_100g":418.4,"proteins_100g":4,"carbohydrates_100g":5,"fat_100g":6}}]}""",
            ),
        )

        val result = client.searchIngredients("arroz")
        val product = (result as NutriaApiResult.Success).value.single()

        assertEquals("English name", product.name)
        assertEquals(100.0, product.caloriesPer100g, 0.0001)
    }

    @Test
    fun incompleteProductsAreFilteredAndEmptySearchIsSuccessful() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"hits":[{"code":"missing","product_name":"Missing nutrients"},{"code":"complete","product_name":"Completo","nutriments":{"energy-kcal_100g":100,"proteins_100g":2,"carbohydrates_100g":3,"fat_100g":4}}]}""",
            ),
        )

        val result = client.searchIngredients("lentejas")

        assertEquals("complete", (result as NutriaApiResult.Success).value.single().code)
    }

    @Test
    fun searchNeverReturnsMoreThanEightProducts() = runBlocking {
        val product = """{"code":"123","product_name":"Producto","nutriments":{"energy-kcal_100g":100,"proteins_100g":2,"carbohydrates_100g":3,"fat_100g":4}}"""
        val hits = List(10) { product }.joinToString(",")
        server.enqueue(jsonResponse("""{"hits":[$hits]}"""))

        val result = client.searchIngredients("arroz")

        assertEquals(8, (result as NutriaApiResult.Success).value.size)
    }

    @Test
    fun invalidQueryDoesNotSendARequest() = runBlocking {
        val result = client.searchIngredients("   ")

        assertEquals(NutriaApiErrorKind.INVALID_INPUT, (result as NutriaApiResult.Failure).error.kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun overlongQueryDoesNotSendARequest() = runBlocking {
        val result = client.searchIngredients("a".repeat(OpenFoodFactsClient.MAX_QUERY_LENGTH + 1))

        assertEquals(NutriaApiErrorKind.INVALID_INPUT, (result as NutriaApiResult.Failure).error.kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun upstreamRateLimitIsReportedExplicitly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))

        val result = client.searchIngredients("arroz")
        val error = (result as NutriaApiResult.Failure).error

        assertEquals(NutriaApiErrorKind.RATE_LIMITED, error.kind)
        assertEquals(429, error.httpStatus)
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
