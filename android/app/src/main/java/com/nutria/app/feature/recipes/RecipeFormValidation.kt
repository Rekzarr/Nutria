package com.nutria.app.feature.recipes

import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.SavedRecipe
import java.util.UUID

data class RecipeIngredientDraft(
    val name: String = "",
    val edibleGrams: String = "100",
    val caloriesPer100Grams: String = "",
    val proteinPer100Grams: String = "",
    val carbohydratesPer100Grams: String = "",
    val fatPer100Grams: String = "",
)

data class RecipeFormDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: RecipeCategory = RecipeCategory.MEAL,
    val servings: String = "1",
    val prepMinutes: String = "",
    val complexity: RecipeComplexity? = null,
    val ingredients: List<RecipeIngredientDraft> = listOf(RecipeIngredientDraft()),
    val instructions: String = "",
)

data class RecipeFormValidation(
    val recipe: SavedRecipe?,
    val errors: List<String>,
) {
    val isValid: Boolean get() = recipe != null && errors.isEmpty()
}

object RecipeFormValidator {
    const val MAX_RECIPE_NAME_LENGTH = 60

    fun validate(draft: RecipeFormDraft): RecipeFormValidation {
        val errors = mutableListOf<String>()
        val name = draft.name.trim()
        if (name.isEmpty()) {
            errors += "Escribe el nombre de la receta."
        } else if (name.length > MAX_RECIPE_NAME_LENGTH) {
            errors += "El nombre admite hasta $MAX_RECIPE_NAME_LENGTH caracteres."
        }

        val servings = draft.servings.trim().toIntOrNull()
        if (servings == null || servings <= 0) {
            errors += "Indica un número de raciones mayor que cero."
        }

        val prepMinutes = if (draft.prepMinutes.isBlank()) {
            null
        } else {
            draft.prepMinutes.trim().toIntOrNull().also {
                if (it == null || it <= 0) errors += "El tiempo debe ser un número de minutos mayor que cero."
            }
        }

        if (draft.ingredients.isEmpty()) {
            errors += "Añade al menos un ingrediente."
        }
        val ingredients = draft.ingredients.mapIndexedNotNull { index, ingredient ->
            val ingredientName = ingredient.name.trim()
            val grams = ingredient.edibleGrams.toNonNegativeNumber()
            val calories = ingredient.caloriesPer100Grams.toNonNegativeNumber()
            val protein = ingredient.proteinPer100Grams.toNonNegativeNumber()
            val carbohydrates = ingredient.carbohydratesPer100Grams.toNonNegativeNumber()
            val fat = ingredient.fatPer100Grams.toNonNegativeNumber()
            var valid = true
            if (ingredientName.isEmpty()) {
                errors += "Completa el nombre del ingrediente ${index + 1}."
                valid = false
            }
            if (grams == null || grams <= 0.0) {
                errors += "El peso del ingrediente ${index + 1} debe ser mayor que cero."
                valid = false
            }
            if (calories == null || protein == null || carbohydrates == null || fat == null) {
                errors += "Completa los cuatro valores nutricionales por 100 g del ingrediente ${index + 1}."
                valid = false
            }
            if (!valid) {
                null
            } else {
                RecipeIngredient(
                    name = ingredientName,
                    edibleGrams = requireNotNull(grams),
                    nutritionPer100Grams = Nutrients(
                        caloriesKcal = requireNotNull(calories),
                        proteinGrams = requireNotNull(protein),
                        carbohydratesGrams = requireNotNull(carbohydrates),
                        fatGrams = requireNotNull(fat),
                    ),
                )
            }
        }

        if (errors.isNotEmpty()) return RecipeFormValidation(null, errors)
        return RecipeFormValidation(
            recipe = SavedRecipe(
                id = draft.id,
                name = name,
                category = draft.category,
                ingredients = ingredients,
                servings = requireNotNull(servings),
                prepMinutes = prepMinutes,
                complexity = draft.complexity,
                instructions = draft.instructions.lines().map(String::trim).filter(String::isNotEmpty),
            ),
            errors = emptyList(),
        )
    }

    private fun String.toNonNegativeNumber(): Double? =
        trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
}

fun SavedRecipe.toRecipeFormDraft() = RecipeFormDraft(
    id = id,
    name = name,
    category = category,
    servings = servings.toString(),
    prepMinutes = prepMinutes?.toString().orEmpty(),
    complexity = complexity,
    ingredients = ingredients.map { ingredient ->
        RecipeIngredientDraft(
            name = ingredient.name,
            edibleGrams = ingredient.edibleGrams.toString(),
            caloriesPer100Grams = ingredient.nutritionPer100Grams.caloriesKcal.toString(),
            proteinPer100Grams = ingredient.nutritionPer100Grams.proteinGrams.toString(),
            carbohydratesPer100Grams = ingredient.nutritionPer100Grams.carbohydratesGrams.toString(),
            fatPer100Grams = ingredient.nutritionPer100Grams.fatGrams.toString(),
        )
    }.ifEmpty { listOf(RecipeIngredientDraft()) },
    instructions = instructions.joinToString("\n"),
)
