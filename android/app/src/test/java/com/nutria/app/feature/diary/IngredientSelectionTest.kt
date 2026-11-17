package com.nutria.app.feature.diary

import com.nutria.app.core.model.FoodUnit
import com.nutria.app.data.remote.IngredientProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientSelectionTest {
    @Test
    fun selectedProductFillsPer100GramNutritionWithoutChangingAmount() {
        val original = FoodEntryDraft(
            name = "Otro alimento",
            quantity = "2",
            unit = FoodUnit.OUNCES,
            caloriesPer100Grams = "1",
        )

        val selected = original.withIngredientProduct(
            IngredientProduct(
                code = "123",
                name = "Yogur natural",
                brand = "Marca",
                caloriesPer100g = 62.5,
                proteinPer100g = 3.8,
                carbsPer100g = 4.7,
                fatPer100g = 3.1,
            ),
        )

        assertEquals("Yogur natural", selected.name)
        assertEquals("62.5", selected.caloriesPer100Grams)
        assertEquals("3.8", selected.proteinPer100Grams)
        assertEquals("4.7", selected.carbohydratesPer100Grams)
        assertEquals("3.1", selected.fatPer100Grams)
        assertEquals("2", selected.quantity)
        assertEquals(FoodUnit.OUNCES, selected.unit)
    }

    @Test
    fun selectedProductNameIsBoundedToFoodEntryLimit() {
        val selected = FoodEntryDraft().withIngredientProduct(
            IngredientProduct(
                code = "123",
                name = "a".repeat(80),
                brand = "",
                caloriesPer100g = 1.0,
                proteinPer100g = 1.0,
                carbsPer100g = 1.0,
                fatPer100g = 1.0,
            ),
        )

        assertEquals(60, selected.name.length)
    }
}
