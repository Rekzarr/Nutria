package com.nutria.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidationTest {
    @Test
    fun acceptsPositiveGoalsAndSpanishDecimalSeparator() {
        val result = GoalDraftValidator.validate(
            NutritionGoalDraft(
                calories = "2100",
                waterMillilitres = "2050",
                proteinGrams = "125,5",
                carbohydratesGrams = "240",
                fatGrams = "70.25",
            ),
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(2_100, result.goals?.caloriesKcal)
        assertEquals(2_050, result.goals?.waterMillilitres)
        assertEquals(125.5, result.goals?.proteinGrams ?: 0.0, 0.0)
    }

    @Test
    fun rejectsNonPositiveAndInvalidWaterGoalsWithoutReturningPartialValues() {
        val result = GoalDraftValidator.validate(
            NutritionGoalDraft(
                calories = "0",
                waterMillilitres = "75",
                proteinGrams = "-1",
                carbohydratesGrams = "1e100",
                fatGrams = "",
            ),
        )

        assertNull(result.goals)
        assertEquals(
            setOf(
                GoalDraftValidator.CALORIES,
                GoalDraftValidator.WATER,
                GoalDraftValidator.PROTEIN,
                GoalDraftValidator.CARBOHYDRATES,
                GoalDraftValidator.FAT,
            ),
            result.errors.keys,
        )
    }

    @Test
    fun serviceUrlRequiresHttpsExceptExplicitLocalDebugAndRejectsCredentials() {
        assertEquals(
            "https://nutria.example/",
            (ServiceUrlValidator.validate(" https://nutria.example ", false) as ServiceUrlValidation.Valid)
                .normalizedUrl,
        )
        assertEquals(
            "http://localhost:5000/",
            (ServiceUrlValidator.validate("http://localhost:5000", true) as ServiceUrlValidation.Valid)
                .normalizedUrl,
        )
        assertTrue(ServiceUrlValidator.validate("http://nutria.example", true) is ServiceUrlValidation.Invalid)
        assertTrue(ServiceUrlValidator.validate("https://user:pass@nutria.example", false) is ServiceUrlValidation.Invalid)
        assertTrue(ServiceUrlValidator.validate("https://nutria.example/?key=value", false) is ServiceUrlValidation.Invalid)
        assertTrue(
            (ServiceUrlValidator.validate("   ", false) as ServiceUrlValidation.Valid)
                .normalizedUrl == null,
        )
    }
}
