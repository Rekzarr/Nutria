package com.nutria.app.feature.settings

import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nutria.app.app.NutriaMaterialTheme
import com.nutria.app.core.data.AppTheme
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.data.SettingsPreferences
import com.nutria.app.core.data.SettingsPreferencesRepository
import com.nutria.app.core.model.NutritionGoals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exposesSpanishPrivacyAndThemeOptionsAndCancelRestoresSavedTheme() {
        val goalsRepository = FakeGoalsRepository()
        val preferencesRepository = FakeSettingsPreferencesRepository()
        val preview = mutableStateOf(AppTheme.SAGE)

        composeRule.setContent {
            NutriaMaterialTheme(theme = preview.value) {
                SettingsScreen(
                    goalsRepository = goalsRepository,
                    preferencesRepository = preferencesRepository,
                    onPreviewTheme = { preview.value = it },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }

        composeRule.onNodeWithText("Verde natural").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lavanda").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Coral").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Azul océano").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(AppTheme.OCEAN, preview.value) }
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(AppTheme.SAGE, preview.value) }
        composeRule.onNodeWithText("Tus registros en Nutria").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pasos de Health Connect").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pulsera Bluetooth").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Análisis de fotos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Búsqueda de ingredientes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("URL del servicio Nutria").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Guardar cambios").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun savingThemeUpdatesThePreferencesRepository() {
        val goalsRepository = FakeGoalsRepository()
        val preferencesRepository = FakeSettingsPreferencesRepository()
        val preview = mutableStateOf(AppTheme.SAGE)

        composeRule.setContent {
            NutriaMaterialTheme(theme = preview.value) {
                SettingsScreen(
                    goalsRepository = goalsRepository,
                    preferencesRepository = preferencesRepository,
                    onPreviewTheme = { preview.value = it },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }

        composeRule.onNodeWithText("Coral").performScrollTo().performClick()
        composeRule.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            preferencesRepository.current().theme == AppTheme.CORAL
        }
        composeRule.runOnIdle { assertEquals(AppTheme.CORAL, preview.value) }
    }

    private class FakeGoalsRepository : GoalsRepository {
        private val state = MutableStateFlow(NutritionGoals())
        override fun observeGoals(): Flow<NutritionGoals> = state
        override suspend fun saveGoals(goals: NutritionGoals) {
            state.value = goals
        }
    }

    private class FakeSettingsPreferencesRepository : SettingsPreferencesRepository {
        private val state = MutableStateFlow(SettingsPreferences())
        override fun observeSettings(): Flow<SettingsPreferences> = state
        override suspend fun saveSettings(preferences: SettingsPreferences) {
            state.value = preferences
        }

        fun current(): SettingsPreferences = state.value
    }
}
