package com.nutria.app.data.remote

data class MealPhoto(
    val fileName: String,
    val mediaType: String,
    val bytes: ByteArray,
)

data class MealAnalysis(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

data class IngredientProduct(
    val code: String,
    val name: String,
    val brand: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
)

sealed interface NutriaApiResult<out T> {
    data class Success<T>(val value: T) : NutriaApiResult<T>
    data class Failure(val error: NutriaApiError) : NutriaApiResult<Nothing>
}

enum class NutriaApiErrorKind {
    INVALID_INPUT,
    PAYLOAD_TOO_LARGE,
    RATE_LIMITED,
    UPSTREAM_FAILURE,
    SERVICE_UNAVAILABLE,
    TIMEOUT,
    NETWORK,
    INVALID_RESPONSE,
    HTTP,
}

data class NutriaApiError(
    val kind: NutriaApiErrorKind,
    val message: String,
    val httpStatus: Int? = null,
    val serverTitle: String? = null,
)

interface NutriaApiRepository {
    suspend fun searchIngredients(query: String): NutriaApiResult<List<IngredientProduct>>

    suspend fun analyzeMeal(
        photo: MealPhoto,
        name: String? = null,
    ): NutriaApiResult<MealAnalysis>
}
