package com.nutria.app.feature.recipes

import com.nutria.app.core.model.NutritionRules
import com.nutria.app.core.model.RecommendationFilter
import com.nutria.app.core.model.RecommendationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class RecommendationCatalogTest {
    @Test
    fun catalogOffersUniqueRecipesUnderTheDocumentedServingLimit() {
        val catalog = RecommendationCatalog.recipes
        val eligible = RecommendationRules.eligible(
            catalog = catalog,
            savedRecipes = emptyList(),
            filter = RecommendationFilter(maxPrepMinutes = 60),
        )

        assertEquals(11, catalog.map { it.id }.distinct().size)
        assertEquals(catalog.size, eligible.size)
        assertTrue(catalog.all { it.ingredients.isNotEmpty() && it.instructions.isNotEmpty() })
        assertTrue(catalog.all {
            NutritionRules.calculateRecipeTotal(it.ingredients).caloriesKcal.roundToInt() <=
                RecommendationRules.MAX_RECOMMENDATION_CALORIES
        })
    }

    @Test
    fun savedRecommendationIsExcludedFromTheCatalog() {
        val recommendation = RecommendationCatalog.recipes.first()
        val saved = recommendation.toSavedRecipe()

        val remaining = RecommendationRules.eligible(
            catalog = RecommendationCatalog.recipes,
            savedRecipes = listOf(saved),
            filter = RecommendationFilter(maxPrepMinutes = 60),
        )

        assertTrue(remaining.none { it.id == recommendation.id })
    }

    private fun com.nutria.app.core.model.RecipeRecommendation.toSavedRecipe() =
        com.nutria.app.core.model.SavedRecipe(
            id = "saved-$id",
            name = name,
            category = category,
            recommendationId = id,
            ingredients = ingredients,
            prepMinutes = prepMinutes,
            complexity = complexity,
            instructions = instructions,
        )
}
