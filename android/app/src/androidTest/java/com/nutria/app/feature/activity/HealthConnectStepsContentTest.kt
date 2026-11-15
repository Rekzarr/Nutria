package com.nutria.app.feature.activity

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutria.app.core.model.HealthConnectStepsState
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectStepsContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 11, 14)
    private val time = Instant.parse("2026-11-14T12:00:00Z")

    @Test
    fun emptyDayExplainsMissingRecordsWithoutShowingZeroSteps() {
        composeRule.setContent {
            MaterialTheme {
                HealthConnectStepsContent(
                    state = HealthConnectStepsState.NoData(date, time),
                    isLoading = false,
                    compact = false,
                    onRefresh = {},
                    onAllow = {},
                    onOpenProvider = {},
                )
            }
        }

        composeRule.onNodeWithText("No hay datos de pasos para hoy en Health Connect")
            .assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("0 pasos").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Fuente: Health Connect").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun permissionIsExplainedBeforeTheExplicitRequestAction() {
        var requested = false
        composeRule.setContent {
            MaterialTheme {
                HealthConnectStepsContent(
                    state = HealthConnectStepsState.PermissionRequired(date),
                    isLoading = false,
                    compact = false,
                    onRefresh = {},
                    onAllow = { requested = true },
                    onOpenProvider = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "Para consultar tus pasos, permite la lectura de pasos en Health Connect. Nutria no solicita otros datos de salud."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Permitir acceso a pasos").performClick()
        assertTrue(requested)
    }

    @Test
    fun actualAggregateShowsItsSourceAndNeverAddsEstimatedMetrics() {
        composeRule.setContent {
            MaterialTheme {
                HealthConnectStepsContent(
                    state = HealthConnectStepsState.Available(date, 8_321L, time),
                    isLoading = false,
                    compact = false,
                    onRefresh = {},
                    onAllow = {},
                    onOpenProvider = {},
                )
            }
        }

        composeRule.onNodeWithText("8.321 pasos").assertIsDisplayed()
        composeRule.onNodeWithText("Fuente: Health Connect").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("calorías", substring = true).fetchSemanticsNodes().isEmpty())
    }
}
