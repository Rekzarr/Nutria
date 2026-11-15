package com.nutria.app.core.model

data class DailyNutritionSummary(
    val consumed: Nutrients,
    val remainingCaloriesKcal: Double,
)

fun summarizeDailyNutrition(
    meals: List<MealEntry>,
    goals: NutritionGoals,
): DailyNutritionSummary {
    val consumed = meals.fold(Nutrients.ZERO) { total, meal -> total + meal.nutrients }
    return DailyNutritionSummary(
        consumed = consumed,
        remainingCaloriesKcal = (goals.caloriesKcal - consumed.caloriesKcal).coerceAtLeast(0.0),
    )
}
