package com.nutria.app.feature.diary

import com.nutria.app.core.model.FoodUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodEntryValidatorTest {
    @Test
    fun ounceEntryUsesExactConversionAndScalesEachNutrient() {
        val result = FoodEntryValidator.validate(
            FoodEntryDraft(
                name = "  Avena  ",
                quantity = "1",
                unit = FoodUnit.OUNCES,
                caloriesPer100Grams = "200",
                proteinPer100Grams = "10",
                carbohydratesPer100Grams = "30",
                fatPer100Grams = "5",
            ),
        )

        assertTrue(result.isValid)
        assertEquals("Avena", result.entry?.name)
        assertEquals(28.3495, result.entry?.edibleGrams ?: 0.0, 0.0000001)
        assertEquals(56.699, result.entry?.nutrients?.caloriesKcal ?: 0.0, 0.0000001)
        assertEquals(2.83495, result.entry?.nutrients?.proteinGrams ?: 0.0, 0.0000001)
    }

    @Test
    fun portionEntryMultipliesPortionsByGramsPerPortionAndAcceptsSpanishDecimals() {
        val result = FoodEntryValidator.validate(
            FoodEntryDraft(
                name = "Yogur",
                quantity = "2",
                unit = FoodUnit.PORTIONS,
                gramsPerPortion = "125",
                caloriesPer100Grams = "60,5",
                proteinPer100Grams = "4,2",
                carbohydratesPer100Grams = "5",
                fatPer100Grams = "0",
            ),
        )

        assertTrue(result.isValid)
        assertEquals(250.0, result.entry?.edibleGrams ?: 0.0, 0.0)
        assertEquals(151.25, result.entry?.nutrients?.caloriesKcal ?: 0.0, 0.0000001)
        assertEquals(10.5, result.entry?.nutrients?.proteinGrams ?: 0.0, 0.0000001)
        assertEquals(0.0, result.entry?.nutrients?.fatGrams ?: -1.0, 0.0)
    }

    @Test
    fun blankNameInvalidQuantityIncompleteAndNegativeNutrientsAreRejectedInline() {
        val result = FoodEntryValidator.validate(
            FoodEntryDraft(
                name = "   ",
                quantity = "0",
                caloriesPer100Grams = "",
                proteinPer100Grams = "-1",
                carbohydratesPer100Grams = "abc",
                fatPer100Grams = "1",
            ),
        )

        assertFalse(result.isValid)
        assertTrue(result.entry == null)
        assertTrue(result.errors.containsKey("name"))
        assertTrue(result.errors.containsKey("quantity"))
        assertTrue(result.errors.containsKey("calories"))
        assertTrue(result.errors.containsKey("protein"))
        assertTrue(result.errors.containsKey("carbohydrates"))
    }

    @Test
    fun portionsRequirePositiveWeightAndFoodNameKeepsLegacyLengthLimit() {
        val missingWeight = FoodEntryValidator.validate(
            FoodEntryDraft(
                name = "A",
                quantity = "1",
                unit = FoodUnit.PORTIONS,
                gramsPerPortion = "0",
                caloriesPer100Grams = "1",
                proteinPer100Grams = "0",
                carbohydratesPer100Grams = "0",
                fatPer100Grams = "0",
            ),
        )
        val tooLongName = FoodEntryValidator.validate(
            FoodEntryDraft(
                name = "A".repeat(61),
                quantity = "1",
                caloriesPer100Grams = "1",
                proteinPer100Grams = "0",
                carbohydratesPer100Grams = "0",
                fatPer100Grams = "0",
            ),
        )

        assertTrue(missingWeight.errors.containsKey("gramsPerPortion"))
        assertTrue(tooLongName.errors.containsKey("name"))
    }
}
