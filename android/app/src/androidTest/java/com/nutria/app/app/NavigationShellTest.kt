package com.nutria.app.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun drawerShowsAllSpanishDestinationsAndSettingsIsAvailable() {
        composeRule.onNodeWithContentDescription("Abrir navegación").performClick()

        listOf("Inicio", "Comida", "Recetas", "Foto", "Ciclo", "Plan", "Actividad")
            .forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }

        composeRule.onNodeWithText("Comida").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir navegación").assertIsDisplayed()
    }

    @Test
    fun selectingAnExistingDestinationDoesNotCreateAnotherBackEntry() {
        openDrawerAndSelect("Comida")
        openDrawerAndSelect("Recetas")
        openDrawerAndSelect("Comida")

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Inicio").assertIsDisplayed()
    }

    private fun openDrawerAndSelect(destination: String) {
        composeRule.onNodeWithContentDescription("Abrir navegación").performClick()
        composeRule.onNodeWithText(destination).performClick()
        composeRule.waitForIdle()
    }
}
