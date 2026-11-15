package com.nutria.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class InstantRange(val startInclusive: Instant, val endExclusive: Instant) {
    init {
        require(endExclusive > startInclusive)
    }
}

object LocalDateRules {
    /** A local day is not assumed to be 24 hours; DST days can be shorter or longer. */
    fun dayRange(date: LocalDate, zoneId: ZoneId): InstantRange {
        val start = date.atStartOfDay(zoneId).toInstant()
        val nextStart = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        return InstantRange(start, nextStart)
    }

    fun dateKey(date: LocalDate): String = date.toString()

    fun parseDateKey(value: String): LocalDate? {
        if (!DATE_KEY.matches(value)) return null
        return runCatching { LocalDate.parse(value) }
            .getOrNull()
            ?.takeIf { it.toString() == value }
    }

    fun daysBetween(fromInclusive: LocalDate, toExclusive: LocalDate): Long =
        ChronoUnit.DAYS.between(fromInclusive, toExclusive)

    private val DATE_KEY = Regex("""\d{4}-\d{2}-\d{2}""")
}

object NutritionRules {
    const val GRAMS_PER_OUNCE = 28.3495

    fun edibleGrams(amount: FoodAmount): Double {
        val grams = when (amount.unit) {
            FoodUnit.GRAMS -> amount.quantity
            FoodUnit.OUNCES -> amount.quantity * GRAMS_PER_OUNCE
            FoodUnit.PORTIONS -> amount.quantity * requireNotNull(amount.gramsPerPortion)
        }
        require(grams.isFinite() && grams > 0.0)
        return grams
    }

    fun calculateFood(nutritionPer100Grams: Nutrients, amount: FoodAmount): Nutrients =
        nutritionPer100Grams.scaledBy(edibleGrams(amount) / 100.0)

    fun calculateRecipeTotal(ingredients: List<RecipeIngredient>): Nutrients {
        require(ingredients.isNotEmpty())
        return ingredients.fold(Nutrients.ZERO) { total, ingredient ->
            total + ingredient.nutritionPer100Grams.scaledBy(ingredient.edibleGrams / 100.0)
        }
    }

    fun calculatePerServing(total: Nutrients, servings: Int): Nutrients {
        require(servings > 0)
        return total.scaledBy(1.0 / servings)
    }
}

object StreakRules {
    /** Counts backward from today, stopping at the first day with no meal and no water. */
    fun careStreak(
        today: LocalDate,
        activityByDate: Map<LocalDate, DailyIntakeActivity>,
    ): Int {
        var date = today
        var streak = 0
        while (activityByDate[date]?.hasIntake == true) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }
}
