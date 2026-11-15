package com.nutria.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Unrounded nutrition values. Rounding belongs at the same display/persistence
 * boundary as the PWA; keeping these values precise avoids cumulative drift.
 */
data class Nutrients(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydratesGrams: Double,
    val fatGrams: Double,
) {
    init {
        require(listOf(caloriesKcal, proteinGrams, carbohydratesGrams, fatGrams)
            .all { it.isFinite() && it >= 0.0 })
    }

    operator fun plus(other: Nutrients) = Nutrients(
        caloriesKcal + other.caloriesKcal,
        proteinGrams + other.proteinGrams,
        carbohydratesGrams + other.carbohydratesGrams,
        fatGrams + other.fatGrams,
    )

    fun scaledBy(factor: Double): Nutrients {
        require(factor.isFinite() && factor >= 0.0)
        return Nutrients(
            caloriesKcal * factor,
            proteinGrams * factor,
            carbohydratesGrams * factor,
            fatGrams * factor,
        )
    }

    /** Matches the PWA's whole-number display rounding (not storage rounding). */
    fun roundedForDisplay() = Nutrients(
        caloriesKcal = pwaRound(caloriesKcal),
        proteinGrams = pwaRound(proteinGrams),
        carbohydratesGrams = pwaRound(carbohydratesGrams),
        fatGrams = pwaRound(fatGrams),
    )

    /** Matches the PWA recipe form's two-decimal calculation boundary. */
    fun roundedToTwoDecimals() = Nutrients(
        caloriesKcal = roundTwo(caloriesKcal),
        proteinGrams = roundTwo(proteinGrams),
        carbohydratesGrams = roundTwo(carbohydratesGrams),
        fatGrams = roundTwo(fatGrams),
    )

    private fun roundTwo(value: Double) =
        BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun pwaRound(value: Double) = kotlin.math.floor(value + 0.5)

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0)
    }
}

data class NutritionGoals(
    val caloriesKcal: Int = DEFAULT_CALORIES_KCAL,
    val waterMillilitres: Int = DEFAULT_WATER_MILLILITRES,
    val proteinGrams: Double = DEFAULT_PROTEIN_GRAMS,
    val carbohydratesGrams: Double = DEFAULT_CARBOHYDRATES_GRAMS,
    val fatGrams: Double = DEFAULT_FAT_GRAMS,
) {
    init {
        require(caloriesKcal > 0 && waterMillilitres > 0)
        require(listOf(proteinGrams, carbohydratesGrams, fatGrams)
            .all { it.isFinite() && it > 0.0 })
    }

    companion object {
        const val DEFAULT_CALORIES_KCAL = 2_000
        const val DEFAULT_WATER_MILLILITRES = 2_000
        const val DEFAULT_PROTEIN_GRAMS = 120.0
        const val DEFAULT_CARBOHYDRATES_GRAMS = 250.0
        const val DEFAULT_FAT_GRAMS = 65.0
    }
}

enum class FoodUnit { GRAMS, OUNCES, PORTIONS }

data class FoodAmount(
    val quantity: Double,
    val unit: FoodUnit,
    val gramsPerPortion: Double? = null,
) {
    init {
        require(quantity.isFinite() && quantity > 0.0)
        if (unit == FoodUnit.PORTIONS) {
            require(gramsPerPortion != null && gramsPerPortion.isFinite() && gramsPerPortion > 0.0)
        } else {
            require(gramsPerPortion == null)
        }
    }
}

data class MealEntry(
    val id: String,
    val localDate: LocalDate,
    val name: String,
    val nutrients: Nutrients,
    val source: MealSource,
    val recordedAt: Instant,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
    }
}

enum class MealSource { FOOD, RECIPE, PHOTO }

data class WaterEvent(
    val id: String,
    val localDate: LocalDate,
    val amountMillilitres: Int,
    val sequence: Long,
    val recordedAt: Instant,
) {
    init {
        require(id.isNotBlank() && amountMillilitres > 0 && sequence > 0)
    }
}

data class DailyIntakeActivity(
    val mealCount: Int,
    val waterMillilitres: Int,
) {
    init {
        require(mealCount >= 0 && waterMillilitres >= 0)
    }

    val hasIntake: Boolean get() = mealCount > 0 || waterMillilitres > 0
}

enum class RecipeCategory { MEAL, DINNER }

data class RecipeIngredient(
    val name: String,
    val edibleGrams: Double,
    val nutritionPer100Grams: Nutrients,
) {
    init {
        require(name.isNotBlank())
        require(edibleGrams.isFinite() && edibleGrams > 0.0)
    }
}

data class SavedRecipe(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val recommendationId: String? = null,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
    }
}

enum class RecipeComplexity { EASY, MEDIUM, ADVANCED }

enum class RecommendationCategory { ALL, MEAL, DINNER }

enum class ComplexityFilter { ALL, EASY, MEDIUM, ADVANCED }

data class RecommendationFilter(
    val maxPrepMinutes: Int = 30,
    val complexity: ComplexityFilter = ComplexityFilter.ALL,
    val category: RecommendationCategory = RecommendationCategory.ALL,
) {
    init {
        require(maxPrepMinutes in SUPPORTED_PREP_TIMES)
    }

    companion object {
        val SUPPORTED_PREP_TIMES = setOf(15, 30, 45, 60)
    }
}

data class RecipeRecommendation(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val prepMinutes: Int,
    val complexity: RecipeComplexity,
    val ingredients: List<RecipeIngredient>,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(prepMinutes > 0)
        require(ingredients.isNotEmpty())
    }
}

enum class CalendarSlot { MEAL, DINNER, BREAKFAST }

data class CalendarAssignment(
    val localDate: LocalDate,
    val slot: CalendarSlot,
    val recipeId: String,
) {
    init {
        require(recipeId.isNotBlank())
    }
}

enum class CycleFlow { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }

enum class CycleMood { GOOD, STABLE, TIRED, SENSITIVE, IRRITABLE }

data class CycleSettings(
    val cycleLengthDays: Int = DEFAULT_CYCLE_LENGTH_DAYS,
    val periodLengthDays: Int = DEFAULT_PERIOD_LENGTH_DAYS,
) {
    init {
        require(cycleLengthDays in MIN_CYCLE_LENGTH_DAYS..MAX_CYCLE_LENGTH_DAYS)
        require(periodLengthDays in MIN_PERIOD_LENGTH_DAYS..MAX_PERIOD_LENGTH_DAYS)
        require(periodLengthDays < cycleLengthDays)
    }

    companion object {
        const val DEFAULT_CYCLE_LENGTH_DAYS = 28
        const val DEFAULT_PERIOD_LENGTH_DAYS = 5
        const val MIN_CYCLE_LENGTH_DAYS = 21
        const val MAX_CYCLE_LENGTH_DAYS = 45
        const val MIN_PERIOD_LENGTH_DAYS = 2
        const val MAX_PERIOD_LENGTH_DAYS = 10
    }
}

data class PeriodRecord(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
) {
    init {
        require(id.isNotBlank())
        require(endDate == null || endDate >= startDate)
    }
}

data class CycleDayEntry(
    val date: LocalDate,
    val flow: CycleFlow,
    val pain: Int,
    val mood: CycleMood,
    val note: String,
) {
    init {
        require(pain in 0..10)
        require(note.trim().length <= MAX_NOTE_LENGTH)
    }

    companion object {
        const val MAX_NOTE_LENGTH = 160
    }
}
