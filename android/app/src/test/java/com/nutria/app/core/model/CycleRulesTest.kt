package com.nutria.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleRulesTest {
    private val today = LocalDate.of(2026, 11, 14)
    private val settings = CycleSettings(cycleLengthDays = 28, periodLengthDays = 5)

    @Test
    fun settingsEnforceLegacyRangesAndPeriodMustBeShorterThanCycle() {
        assertTrue(CycleRules.validateSettings(21, 2))
        assertTrue(CycleRules.validateSettings(45, 10))
        assertFalse(CycleRules.validateSettings(20, 5))
        assertFalse(CycleRules.validateSettings(46, 5))
        assertFalse(CycleRules.validateSettings(28, 1))
        assertFalse(CycleRules.validateSettings(28, 11))
        assertFalse(CycleRules.validateSettings(21, 21))
    }

    @Test
    fun periodValidationRejectsFutureReversedAndInclusiveOverlap() {
        val existing = listOf(PeriodRecord("old", today.minusDays(10), today.minusDays(6)))
        assertEquals(
            PeriodValidationError.START_IN_FUTURE,
            CycleRules.validateNewPeriod(today.plusDays(1), null, today, settings, existing),
        )
        assertEquals(
            PeriodValidationError.END_BEFORE_START,
            CycleRules.validateNewPeriod(today.minusDays(2), today.minusDays(3), today, settings, emptyList()),
        )
        assertEquals(
            PeriodValidationError.END_IN_FUTURE,
            CycleRules.validateNewPeriod(today, today.plusDays(1), today, settings, emptyList()),
        )
        assertEquals(
            PeriodValidationError.OVERLAPS_EXISTING_PERIOD,
            CycleRules.validateNewPeriod(today.minusDays(7), today.minusDays(5), today, settings, existing),
        )
        assertEquals(
            PeriodValidationError.OVERLAPS_EXISTING_PERIOD,
            CycleRules.validateNewPeriod(today.minusDays(10), null, today, settings, existing),
        )
        assertEquals(
            null,
            CycleRules.validateNewPeriod(today.minusDays(4), today.minusDays(2), today, settings, existing),
        )
    }

    @Test
    fun estimatesUseLatestPastPeriodAndKeepCalendarPhasesExplicitlyApproximate() {
        val period = PeriodRecord("latest", today.minusDays(14))
        val estimate = CycleRules.estimate(
            listOf(period, PeriodRecord("future", today.plusDays(4))),
            settings,
            today,
        ) as CycleEstimate.Known
        assertEquals(15, estimate.cycleDay)
        assertEquals(CyclePhase.OVULATORY_WINDOW, estimate.phase)
        assertEquals(today.minusDays(14).plusDays(28), estimate.expectedNextStart)
        assertFalse(estimate.isOngoing)

        assertEquals(
            CyclePhase.MENSTRUAL,
            (CycleRules.estimate(listOf(PeriodRecord("new", today.minusDays(2))), settings, today) as CycleEstimate.Known).phase,
        )
        assertEquals(
            CyclePhase.POSSIBLY_LATE,
            (CycleRules.estimate(listOf(PeriodRecord("late", today.minusDays(29))), settings, today) as CycleEstimate.Known).phase,
        )
        assertEquals(CycleEstimate.Unknown, CycleRules.estimate(emptyList(), settings, today))
    }

    @Test
    fun notesAreTrimmedAndBoundedAt160Characters() {
        assertEquals("nota", CycleRules.normalizedNote("  nota  "))
        assertTrue(runCatching { CycleRules.normalizedNote("x".repeat(161)) }.isFailure)
        assertEquals(160, CycleRules.normalizedNote("x".repeat(160)).length)
    }

    @Test
    fun spanishCycleDateInputIsStrictAndUsesDayMonthYear() {
        assertEquals(LocalDate.of(2026, 11, 14), CycleDateInput.parse("14/11/2026"))
        assertEquals(null, CycleDateInput.parse("31/02/2026"))
        assertEquals(null, CycleDateInput.parse("2026-11-14"))
        assertEquals(null, CycleDateInput.parse(""))
    }
}
