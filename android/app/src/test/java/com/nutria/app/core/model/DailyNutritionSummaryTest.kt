package com.nutria.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailyNutritionSummaryTest {
    @Test
    fun aggregatesMealsAndCalculatesRemainingCalories() {
        val meals = listOf(
            meal("breakfast", Nutrients(320.5, 18.0, 40.0, 8.0)),
            meal("lunch", Nutrients(710.0, 42.0, 75.0, 22.0)),
        )

        val result = summarizeDailyNutrition(meals, NutritionGoals(caloriesKcal = 2_000))

        assertEquals(1_030.5, result.consumed.caloriesKcal, 0.0)
        assertEquals(60.0, result.consumed.proteinGrams, 0.0)
        assertEquals(115.0, result.consumed.carbohydratesGrams, 0.0)
        assertEquals(969.5, result.remainingCaloriesKcal, 0.0)
    }

    @Test
    fun remainingCaloriesDoNotBecomeNegative() {
        val result = summarizeDailyNutrition(
            meals = listOf(meal("large", Nutrients(2_500.0, 0.0, 0.0, 0.0))),
            goals = NutritionGoals(caloriesKcal = 2_000),
        )

        assertEquals(0.0, result.remainingCaloriesKcal, 0.0)
    }

    private fun meal(id: String, nutrients: Nutrients) = MealEntry(
        id = id,
        localDate = LocalDate.of(2026, 11, 14),
        name = id,
        nutrients = nutrients,
        source = MealSource.FOOD,
        recordedAt = Instant.parse("2026-11-14T12:00:00Z"),
    )

}
