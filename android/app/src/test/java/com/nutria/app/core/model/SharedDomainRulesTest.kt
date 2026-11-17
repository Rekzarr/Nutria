package com.nutria.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

class SharedDomainRulesTest {
    private val per100 = Nutrients(200.0, 10.0, 30.0, 5.0)

    @Test
    fun defaultsAndFoodConversionsPreservePwaValues() {
        assertEquals(2_000, NutritionGoals().caloriesKcal)
        assertEquals(2_000, NutritionGoals().waterMillilitres)
        assertEquals(120.0, NutritionGoals().proteinGrams, 0.0)
        assertEquals(250.0, NutritionGoals().carbohydratesGrams, 0.0)
        assertEquals(65.0, NutritionGoals().fatGrams, 0.0)
        assertEquals(28.3495, NutritionRules.edibleGrams(FoodAmount(1.0, FoodUnit.OUNCES)), 0.0)
        assertEquals(250.0, NutritionRules.edibleGrams(FoodAmount(2.0, FoodUnit.PORTIONS, 125.0)), 0.0)
        assertEquals(100.0, NutritionRules.edibleGrams(FoodAmount(100.0, FoodUnit.GRAMS)), 0.0)
        assertEquals(Nutrients(56.699, 2.83495, 8.50485, 1.417475),
            NutritionRules.calculateFood(per100, FoodAmount(1.0, FoodUnit.OUNCES)))
    }

    @Test
    fun nutritionValidationAndRecipeTotalsAreConsistent() {
        assertFalse(runCatching { NutritionGoals(caloriesKcal = 0) }.isSuccess)
        assertFalse(runCatching { Nutrients(Double.NaN, 0.0, 0.0, 0.0) }.isSuccess)
        assertFalse(runCatching { FoodAmount(0.0, FoodUnit.GRAMS) }.isSuccess)
        assertFalse(runCatching { FoodAmount(1.0, FoodUnit.PORTIONS) }.isSuccess)

        val ingredients = listOf(
            RecipeIngredient("A", 50.0, per100),
            RecipeIngredient("B", 150.0, Nutrients(100.0, 4.0, 20.0, 2.0)),
        )
        val total = NutritionRules.calculateRecipeTotal(ingredients)
        assertEquals(Nutrients(250.0, 11.0, 45.0, 5.5), total)
        assertEquals(Nutrients(125.0, 5.5, 22.5, 2.75), NutritionRules.calculatePerServing(total, 2))
        assertEquals(250.0, total.roundedToTwoDecimals().caloriesKcal, 0.0)
        assertEquals(11.0, total.roundedForDisplay().proteinGrams, 0.0)
        assertEquals(3.0, Nutrients(2.5, 0.0, 0.0, 0.0).roundedForDisplay().caloriesKcal, 0.0)
        assertEquals(1.0, Nutrients(1.005, 0.0, 0.0, 0.0).roundedToTwoDecimals().caloriesKcal, 0.0)
    }

    @Test
    fun localDateKeysAndDayRangesUseCalendarBoundaries() {
        assertEquals(LocalDate.of(2026, 2, 28), LocalDateRules.parseDateKey("2026-02-28"))
        assertNull(LocalDateRules.parseDateKey("2026-02-30"))
        assertNull(LocalDateRules.parseDateKey("2026-2-03"))
        assertEquals("2026-03-01", LocalDateRules.dateKey(LocalDate.of(2026, 3, 1)))

        val zone = ZoneId.of("Europe/Madrid")
        val springDay = LocalDateRules.dayRange(LocalDate.of(2026, 3, 29), zone)
        val autumnDay = LocalDateRules.dayRange(LocalDate.of(2026, 10, 25), zone)
        assertEquals(23L, java.time.Duration.between(springDay.startInclusive, springDay.endExclusive).toHours())
        assertEquals(25L, java.time.Duration.between(autumnDay.startInclusive, autumnDay.endExclusive).toHours())
        assertEquals(1L, LocalDateRules.daysBetween(LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 29)))
    }

    @Test
    fun careStreakStopsAtFirstEmptyDateAndCountsToday() {
        val today = LocalDate.of(2026, 11, 14)
        val activity = mapOf(
            today to DailyIntakeActivity(mealCount = 0, waterMillilitres = 250),
            today.minusDays(1) to DailyIntakeActivity(mealCount = 1, waterMillilitres = 0),
            today.minusDays(2) to DailyIntakeActivity(mealCount = 0, waterMillilitres = 0),
            today.minusDays(3) to DailyIntakeActivity(mealCount = 4, waterMillilitres = 1_000),
        )
        assertEquals(2, StreakRules.careStreak(today, activity))
        assertEquals(0, StreakRules.careStreak(today.plusDays(1), activity))
    }

    @Test
    fun recommendationRulesApplyFiltersSavingsAndThreeItemLimitDeterministically() {
        val catalog = (1..5).map { index ->
            recommendation(
                id = "r$index",
                category = if (index == 5) RecipeCategory.DINNER else RecipeCategory.MEAL,
                prepMinutes = if (index == 4) 45 else 30,
                complexity = if (index == 3) RecipeComplexity.ADVANCED else RecipeComplexity.EASY,
                calories = when (index) {
                    2 -> 551.0
                    6 -> 550.5
                    else -> 400.0
                },
            )
        }
        val saved = listOf(
            SavedRecipe("saved-id", "r1", RecipeCategory.MEAL, recommendationId = "r1"),
        )
        val eligible = RecommendationRules.eligible(
            catalog,
            saved,
            RecommendationFilter(maxPrepMinutes = 30, complexity = ComplexityFilter.EASY),
        )
        assertEquals(listOf("r5"), eligible.map { it.id })
        assertEquals(eligible, RecommendationRules.select(eligible, Random(42)))
        assertEquals(3, RecommendationRules.select(catalog, Random(1)).size)
    }

    @Test
    fun calendarRulesGenerateMondayFirstAndRequireReplacementConfirmation() {
        val monday = LocalDate.of(2026, 11, 9)
        assertEquals((0L..6L).map(monday::plusDays), CalendarRules.weekDates(LocalDate.of(2026, 11, 11)))
        assertEquals(28, CalendarRules.monthDates(YearMonth.of(2026, 2)).size)

        val recipes = listOf(
            SavedRecipe("meal-a", "Comida A", RecipeCategory.MEAL),
            SavedRecipe("meal-b", "Comida B", RecipeCategory.MEAL),
            SavedRecipe("dinner", "Cena", RecipeCategory.DINNER),
        )
        val dates = CalendarRules.weekDates(monday)
        val existing = listOf(CalendarAssignment(dates.first(), CalendarSlot.MEAL, "meal-a"))
        assertTrue(CalendarRules.generateMenu(
            dates, recipes, thirdMealEnabled = false, existing, replacementConfirmed = false,
            random = Random(3),
        ) is MenuGenerationResult.ReplacementConfirmationRequired)

        val generated = CalendarRules.generateMenu(
            dates, recipes, thirdMealEnabled = false, existing, replacementConfirmed = true,
            random = Random(3),
        ) as MenuGenerationResult.Generated
        assertEquals(14, generated.assignments.size)
        assertTrue(generated.assignments.groupBy { it.localDate }.values.all { it.map { a -> a.slot }.toSet().size == 2 })
        assertTrue(generated.assignments.groupBy { it.localDate }.values.all { day ->
            day.map { it.recipeId }.distinct().size == day.size
        })
    }

    @Test
    fun calendarRulesReportMissingCategoryAndAllowReuseOnlyWhenNecessary() {
        val dates = listOf(LocalDate.of(2026, 11, 14))
        val onlyMeal = listOf(SavedRecipe("meal", "Comida", RecipeCategory.MEAL))
        assertEquals(
            MenuGenerationResult.MissingRequiredCategory(CalendarSlot.DINNER),
            CalendarRules.generateMenu(dates, onlyMeal, false, emptyList(), true, Random(1)),
        )

        val oneEach = listOf(
            SavedRecipe("meal", "Comida", RecipeCategory.MEAL),
            SavedRecipe("dinner", "Cena", RecipeCategory.DINNER),
        )
        val withBreakfast = CalendarRules.generateMenu(
            dates, oneEach, true, emptyList(), true, Random(1),
        ) as MenuGenerationResult.Generated
        assertEquals(3, withBreakfast.assignments.size)
        assertEquals(2, withBreakfast.assignments.map { it.recipeId }.distinct().size)
    }

    private fun recommendation(
        id: String,
        category: RecipeCategory,
        prepMinutes: Int,
        complexity: RecipeComplexity,
        calories: Double,
    ) = RecipeRecommendation(
        id = id,
        name = "Receta $id",
        category = category,
        prepMinutes = prepMinutes,
        complexity = complexity,
        ingredients = listOf(RecipeIngredient("Base", 100.0, Nutrients(calories, 10.0, 10.0, 5.0))),
    )
}
