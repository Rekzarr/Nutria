package com.nutria.app.feature.activity

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityWeekSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectorChangesWeeksAndDisablesDatesOutsideTheThirtyDayWindow() {
        val today = LocalDate.of(2026, 11, 15)
        val selectedDate = mutableStateOf(today)
        composeRule.setContent {
            MaterialTheme {
                ActivityWeekSelector(
                    selectedDate = selectedDate.value,
                    today = today,
                    onSelectDate = { selectedDate.value = it },
                )
            }
        }

        composeRule.onNodeWithText("Semana del 09/11/2026 al 15/11/2026").assertIsDisplayed()
        repeat(4) {
            composeRule.onNodeWithContentDescription("Semana anterior").performClick()
        }
        composeRule.onNodeWithText("Semana del 12/10/2026 al 18/10/2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("viernes, 16 de octubre")
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("sábado, 17 de octubre")
            .assertIsEnabled()

        composeRule.onNodeWithContentDescription("sábado, 17 de octubre").performClick()
        composeRule.runOnIdle {
            assertEquals(LocalDate.of(2026, 10, 17), selectedDate.value)
        }
    }
}
