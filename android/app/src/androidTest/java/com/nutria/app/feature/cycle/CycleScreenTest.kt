package com.nutria.app.feature.cycle

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.CycleRepository
import com.nutria.app.core.model.CycleDayEntry
import com.nutria.app.core.model.CycleSettings
import com.nutria.app.core.model.PeriodRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CycleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHistoryShowsUnknownEstimateAndSafetyLanguage() {
        composeRule.setContent {
            MaterialTheme {
                CycleScreen(repository = FakeCycleRepository())
            }
        }

        composeRule.onNodeWithText("Sin datos suficientes para una estimación").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Estas fechas y fases son estimaciones de calendario. No confirman ovulación, fertilidad ni embarazo y no sustituyen el consejo de un profesional sanitario. No sirven para diagnosticar.",
        ).assertIsDisplayed()
    }

    @Test
    fun validPeriodStartIsSavedAndShownInHistory() {
        val today = LocalDate.now()
        val repository = FakeCycleRepository()
        composeRule.setContent {
            MaterialTheme {
                CycleScreen(repository = repository, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }

        composeRule.onNodeWithText("Inicio (dd/MM/aaaa)")
            .performScrollTo()
            .performTextInput(SpanishFormats.formatDate(today))
        composeRule.onNodeWithText("Guardar período").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.periods.value.any { it.startDate == today }
        }

        assertEquals(today, repository.periods.value.single().startDate)
        composeRule.onNodeWithText(
            "Inicio: ${SpanishFormats.formatDate(today)} · Fin: Sin finalizar",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun todayJournalRestoresSavedValuesBeforeEditing() {
        val today = LocalDate.now()
        val savedEntry = CycleDayEntry(
            date = today,
            flow = com.nutria.app.core.model.CycleFlow.LIGHT,
            pain = 3,
            mood = com.nutria.app.core.model.CycleMood.TIRED,
            note = "nota privada",
        )
        val repository = FakeCycleRepository(initialEntries = listOf(savedEntry))
        composeRule.setContent {
            MaterialTheme {
                CycleScreen(repository = repository, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Guardar registro de hoy").assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithText("Nota opcional")
            .performScrollTo()
            .performClick()
            .performTextInput(" actualizada")
        composeRule.onNodeWithText("nota privada actualizada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Guardar registro de hoy").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.entries.value.singleOrNull()?.note == "nota privada actualizada"
        }

        assertEquals(
            savedEntry.copy(note = "nota privada actualizada"),
            repository.entries.value.single(),
        )
    }

    @Test
    fun ongoingPeriodKeepsExpectedStartInsteadOfShowingLateDays() {
        val today = LocalDate.now()
        val period = PeriodRecord("ongoing", today.minusDays(29), today)
        val repository = FakeCycleRepository(initialPeriods = listOf(period))
        composeRule.setContent {
            MaterialTheme {
                CycleScreen(repository = repository, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }

        composeRule.onNodeWithText(
            "Próximo inicio estimado: ${SpanishFormats.formatDate(today.minusDays(1))}",
        ).assertIsDisplayed()
    }
}

private class FakeCycleRepository(
    initialEntries: List<CycleDayEntry> = emptyList(),
    initialPeriods: List<PeriodRecord> = emptyList(),
) : CycleRepository {
    val settings = MutableStateFlow(CycleSettings())
    val periods = MutableStateFlow(initialPeriods)
    val entries = MutableStateFlow(initialEntries)

    override fun observeSettings(): Flow<CycleSettings> = settings
    override fun observePeriods(): Flow<List<PeriodRecord>> = periods

    override fun observeEntries(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
    ): Flow<List<CycleDayEntry>> = entries.map { records ->
        records.filter { it.date in fromInclusive..throughInclusive }.sortedByDescending { it.date }
    }

    override fun observeRecentEntries(
        throughInclusive: LocalDate,
        limit: Int,
    ): Flow<List<CycleDayEntry>> = entries.map { records ->
        records.filter { it.date <= throughInclusive }.sortedByDescending { it.date }.take(limit)
    }

    override suspend fun saveSettings(settings: CycleSettings) {
        this.settings.value = settings
    }

    override suspend fun savePeriod(period: PeriodRecord) {
        periods.value = (periods.value.filterNot { it.id == period.id } + period)
            .sortedByDescending { it.startDate }
    }

    override suspend fun deletePeriod(periodId: String) {
        periods.value = periods.value.filterNot { it.id == periodId }
    }

    override suspend fun saveDayEntry(entry: CycleDayEntry) {
        entries.value = (entries.value.filterNot { it.date == entry.date } + entry)
            .sortedByDescending { it.date }
    }
}
