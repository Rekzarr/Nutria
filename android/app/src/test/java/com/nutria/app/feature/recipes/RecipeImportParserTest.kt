package com.nutria.app.feature.recipes

import com.nutria.app.core.model.RecipeCategory
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecipeImportParserTest {
    @Test
    fun parsesRecipeFieldsAndKeepsInstructionOrder() {
        val recipe = RecipeImportParser.parse(ByteArrayInputStream(validJson.toByteArray()))

        assertEquals("Ensalada de pasta con feta y atún", recipe.name)
        assertEquals(RecipeCategory.DINNER, recipe.category)
        assertEquals(1, recipe.servings)
        assertEquals(listOf("Cuece la pasta.", "Añade el aliño."), recipe.instructions)
        assertEquals(5, recipe.ingredients.size)
        assertEquals(821.0, recipe.ingredients.first().nutritionPer100Grams.caloriesKcal, 0.0)
    }

    @Test
    fun rejectsUnsupportedFormatVersion() {
        val invalidJson = validJson.replace(RecipeImportParser.FORMAT, "nutria.recipe.v2")

        assertThrows(IllegalArgumentException::class.java) {
            RecipeImportParser.parse(invalidJson)
        }
    }

    @Test
    fun rejectsInvalidIngredientNutrients() {
        val invalidJson = validJson.replace("\"caloriesKcal\":821", "\"caloriesKcal\":-1")

        assertThrows(IllegalArgumentException::class.java) {
            RecipeImportParser.parse(invalidJson)
        }
    }

    @Test
    fun rejectsFilesOverSizeLimitWhileReading() {
        val oversized = ByteArray(RecipeImportParser.MAX_FILE_SIZE_BYTES + 1)

        assertThrows(IllegalArgumentException::class.java) {
            RecipeImportParser.parse(ByteArrayInputStream(oversized))
        }
    }

    private companion object {
        val validJson = """
            {
              "format": "nutria.recipe.v1",
              "recipe": {
                "name": "Ensalada de pasta con feta y atún",
                "category": "DINNER",
                "servings": 1,
                "prepMinutes": null,
                "complexity": null,
                "ingredients": [
                  {"name":"Aceite de oliva","edibleGrams":10,"nutritionPer100g":{"caloriesKcal":821,"proteinGrams":0,"carbohydratesGrams":0,"fatGrams":91.2}},
                  {"name":"Queso feta","edibleGrams":35,"nutritionPer100g":{"caloriesKcal":267,"proteinGrams":16.8,"carbohydratesGrams":0.2,"fatGrams":22}},
                  {"name":"Pasta seca","edibleGrams":65,"nutritionPer100g":{"caloriesKcal":336,"proteinGrams":14,"carbohydratesGrams":69,"fatGrams":1.5}},
                  {"name":"Tomate","edibleGrams":105,"nutritionPer100g":{"caloriesKcal":18,"proteinGrams":0.9,"carbohydratesGrams":3.9,"fatGrams":0.2}},
                  {"name":"Atún al natural escurrido","edibleGrams":105,"nutritionPer100g":{"caloriesKcal":106,"proteinGrams":25,"carbohydratesGrams":0,"fatGrams":0.7}}
                ],
                "instructions": ["Cuece la pasta.", "Añade el aliño."]
              }
            }
        """.trimIndent()
    }
}
