package com.nutria.app.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.random.Random
import kotlin.math.roundToInt

object RecommendationRules {
    const val MAX_RECOMMENDATION_CALORIES = 550
    const val MAX_VISIBLE_RECOMMENDATIONS = 3

    fun eligible(
        catalog: List<RecipeRecommendation>,
        savedRecipes: List<SavedRecipe>,
        filter: RecommendationFilter,
    ): List<RecipeRecommendation> {
        val savedRecommendationIds = savedRecipes.mapNotNull { it.recommendationId }.toSet()
        val savedNames = savedRecipes.map { it.name.lowercase(SPANISH_LOCALE) }.toSet()

        return catalog.filter { candidate ->
            val alreadySaved = candidate.id in savedRecommendationIds ||
                candidate.name.lowercase(SPANISH_LOCALE) in savedNames
            val categoryMatches = filter.category == RecommendationCategory.ALL ||
                (filter.category == RecommendationCategory.MEAL && candidate.category == RecipeCategory.MEAL) ||
                (filter.category == RecommendationCategory.DINNER && candidate.category == RecipeCategory.DINNER)
            val complexityMatches = filter.complexity == ComplexityFilter.ALL ||
                filter.complexity.name == candidate.complexity.name
            val calories = NutritionRules.calculateRecipeTotal(candidate.ingredients).caloriesKcal

            !alreadySaved &&
                candidate.prepMinutes <= filter.maxPrepMinutes &&
                complexityMatches &&
                categoryMatches &&
                calories.roundToInt() <= MAX_RECOMMENDATION_CALORIES
        }
    }

    /** Selection remains shuffle-based like the PWA, with Random injectable for reproducible tests. */
    fun select(
        eligible: List<RecipeRecommendation>,
        random: Random = Random.Default,
        limit: Int = MAX_VISIBLE_RECOMMENDATIONS,
    ): List<RecipeRecommendation> {
        require(limit > 0)
        return eligible.shuffled(random).take(limit)
    }

    private val SPANISH_LOCALE = Locale.forLanguageTag("es-ES")
}

sealed interface MenuGenerationResult {
    data object NoRecipes : MenuGenerationResult
    data class MissingRequiredCategory(val slot: CalendarSlot) : MenuGenerationResult
    data class ReplacementConfirmationRequired(val dates: List<LocalDate>) : MenuGenerationResult
    data class Generated(val assignments: List<CalendarAssignment>) : MenuGenerationResult
}

object CalendarRules {
    fun monthDates(month: YearMonth): List<LocalDate> =
        (1..month.lengthOfMonth()).map(month::atDay)

    fun weekDates(containingDate: LocalDate): List<LocalDate> {
        val monday = containingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L..6L).map(monday::plusDays)
    }

    /**
     * Builds a replacement plan without mutating persistence. The caller must confirm
     * replacement explicitly, then save the returned assignments transactionally.
     */
    fun generateMenu(
        dates: List<LocalDate>,
        recipes: List<SavedRecipe>,
        thirdMealEnabled: Boolean,
        existingAssignments: List<CalendarAssignment>,
        replacementConfirmed: Boolean,
        random: Random = Random.Default,
    ): MenuGenerationResult {
        if (recipes.isEmpty()) return MenuGenerationResult.NoRecipes
        require(dates.isNotEmpty())
        require(dates.distinct().size == dates.size)
        require(recipes.map { it.id }.distinct().size == recipes.size)

        val slots = buildList {
            add(CalendarSlot.MEAL)
            add(CalendarSlot.DINNER)
            if (thirdMealEnabled) add(CalendarSlot.BREAKFAST)
        }
        val candidates = slots.associateWith { slot ->
            when (slot) {
                CalendarSlot.MEAL -> recipes.filter { it.category != RecipeCategory.DINNER }
                CalendarSlot.DINNER -> recipes.filter { it.category == RecipeCategory.DINNER }
                CalendarSlot.BREAKFAST -> recipes
            }
        }
        slots.firstOrNull { candidates.getValue(it).isEmpty() }?.let {
            return MenuGenerationResult.MissingRequiredCategory(it)
        }

        val dateSet = dates.toSet()
        val hasAssignmentsToReplace = existingAssignments.any { it.localDate in dateSet }
        if (hasAssignmentsToReplace && !replacementConfirmed) {
            return MenuGenerationResult.ReplacementConfirmationRequired(dates)
        }

        val generated = buildList {
            for (date in dates) {
                val usedToday = mutableSetOf<String>()
                for (slot in slots) {
                    val eligible = candidates.getValue(slot)
                    val unused = eligible.filterNot { it.id in usedToday }
                    // Reuse is allowed only when a category has no unused alternative.
                    val chosen = (unused.ifEmpty { eligible }).random(random)
                    usedToday += chosen.id
                    add(CalendarAssignment(date, slot, chosen.id))
                }
            }
        }
        return MenuGenerationResult.Generated(generated)
    }
}
