package com.nutria.app.feature.diary

import com.nutria.app.data.remote.IngredientProduct
import com.nutria.app.core.model.FoodAmount
import com.nutria.app.core.model.FoodUnit
import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.NutritionRules

data class FoodEntryDraft(
    val name: String = "",
    val quantity: String = "100",
    val unit: FoodUnit = FoodUnit.GRAMS,
    val gramsPerPortion: String = "150",
    val caloriesPer100Grams: String = "",
    val proteinPer100Grams: String = "",
    val carbohydratesPer100Grams: String = "",
    val fatPer100Grams: String = "",
)

fun FoodEntryDraft.withIngredientProduct(product: IngredientProduct): FoodEntryDraft = copy(
    name = product.name.take(MAX_FOOD_NAME_LENGTH),
    caloriesPer100Grams = product.caloriesPer100g.toString(),
    proteinPer100Grams = product.proteinPer100g.toString(),
    carbohydratesPer100Grams = product.carbsPer100g.toString(),
    fatPer100Grams = product.fatPer100g.toString(),
)

data class ValidatedFoodEntry(
    val name: String,
    val edibleGrams: Double,
    val nutrients: Nutrients,
)

data class FoodEntryValidation(
    val entry: ValidatedFoodEntry?,
    val errors: Map<String, String>,
) {
    val isValid: Boolean get() = entry != null && errors.isEmpty()
}

object FoodEntryValidator {
    fun validate(draft: FoodEntryDraft): FoodEntryValidation {
        val errors = linkedMapOf<String, String>()
        val name = draft.name.trim()
        if (name.isEmpty()) {
            errors["name"] = "Escribe el nombre del alimento."
        } else if (name.length > MAX_FOOD_NAME_LENGTH) {
            errors["name"] = "El nombre admite hasta $MAX_FOOD_NAME_LENGTH caracteres."
        }

        val quantity = parseNonNegative(draft.quantity)
        if (quantity == null || quantity <= 0.0) {
            errors["quantity"] = "La cantidad debe ser mayor que cero."
        }

        val gramsPerPortion = if (draft.unit == FoodUnit.PORTIONS) {
            parseNonNegative(draft.gramsPerPortion).also {
                if (it == null || it <= 0.0) {
                    errors["gramsPerPortion"] = "Indica un peso por porción mayor que cero."
                }
            }
        } else {
            null
        }

        val calories = parseNutrient(draft.caloriesPer100Grams, "calories", errors)
        val protein = parseNutrient(draft.proteinPer100Grams, "protein", errors)
        val carbohydrates = parseNutrient(draft.carbohydratesPer100Grams, "carbohydrates", errors)
        val fat = parseNutrient(draft.fatPer100Grams, "fat", errors)

        if (errors.isNotEmpty()) return FoodEntryValidation(null, errors)

        return runCatching {
            val amount = FoodAmount(
                quantity = requireNotNull(quantity),
                unit = draft.unit,
                gramsPerPortion = gramsPerPortion,
            )
            val nutritionPer100Grams = Nutrients(
                caloriesKcal = requireNotNull(calories),
                proteinGrams = requireNotNull(protein),
                carbohydratesGrams = requireNotNull(carbohydrates),
                fatGrams = requireNotNull(fat),
            )
            ValidatedFoodEntry(
                name = name,
                edibleGrams = NutritionRules.edibleGrams(amount),
                nutrients = NutritionRules.calculateFood(nutritionPer100Grams, amount),
            )
        }.fold(
            onSuccess = { FoodEntryValidation(it, emptyMap()) },
            onFailure = {
                FoodEntryValidation(null, mapOf("form" to "Revisa las cantidades y los valores nutricionales."))
            },
        )
    }

    private fun parseNutrient(
        text: String,
        field: String,
        errors: MutableMap<String, String>,
    ): Double? {
        if (text.isBlank()) {
            errors[field] = "Completa este valor nutricional."
            return null
        }
        val value = parseNonNegative(text)
        if (value == null) {
            errors[field] = "Usa un número igual o mayor que cero."
        }
        return value
    }

    private fun parseNonNegative(text: String): Double? =
        text.trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }

}

private const val MAX_FOOD_NAME_LENGTH = 60
