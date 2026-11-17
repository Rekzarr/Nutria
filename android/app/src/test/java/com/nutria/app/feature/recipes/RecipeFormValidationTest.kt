package com.nutria.app.feature.recipes

import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.NutritionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeFormValidationTest {
    @Test
    fun validRecipeCalculatesAndKeepsDetails() {
        val result = RecipeFormValidator.validate(
            RecipeFormDraft(
                id = "recipe-1",
                name = "  Bol de avena  ",
                category = RecipeCategory.DINNER,
                servings = "2",
                prepMinutes = "25",
                complexity = RecipeComplexity.MEDIUM,
                ingredients = listOf(
                    RecipeIngredientDraft(
                        name = "Avena",
                        edibleGrams = "50",
                        caloriesPer100Grams = "380",
                        proteinPer100Grams = "13",
                        carbohydratesPer100Grams = "68",
                        fatPer100Grams = "7",
                    ),
                    RecipeIngredientDraft(
                        name = "Leche",
                        edibleGrams = "200",
                        caloriesPer100Grams = "50",
                        proteinPer100Grams = "3.5",
                        carbohydratesPer100Grams = "5",
                        fatPer100Grams = "2",
                    ),
                ),
                instructions = "  Mezclar\n\nNota: reservar un poco de leche\n\nCalentar  ",
            ),
        )

        assertTrue(result.isValid)
        val recipe = requireNotNull(result.recipe)
        assertEquals("Bol de avena", recipe.name)
        assertEquals(
            listOf("Mezclar", "Nota: reservar un poco de leche", "Calentar"),
            recipe.instructions,
        )
        assertEquals(2, recipe.ingredients.size)
        val total = NutritionRules.calculateRecipeTotal(recipe.ingredients)
        assertEquals(290.0, total.caloriesKcal, 0.001)
        assertEquals(13.5, total.proteinGrams, 0.001)
        assertEquals(145.0, NutritionRules.calculatePerServing(total, recipe.servings).caloriesKcal, 0.001)
    }

    @Test
    fun incompleteIngredientsOrInvalidServingsCannotBeSaved() {
        val result = RecipeFormValidator.validate(
            RecipeFormDraft(
                name = "Receta",
                servings = "0",
                ingredients = listOf(RecipeIngredientDraft(name = "Arroz", edibleGrams = "0")),
            ),
        )

        assertFalse(result.isValid)
        assertNull(result.recipe)
        assertNotNull(result.errors.find { it.contains("raciones") })
        assertNotNull(result.errors.find { it.contains("peso") })
        assertNotNull(result.errors.find { it.contains("nutricionales") })
    }
}
