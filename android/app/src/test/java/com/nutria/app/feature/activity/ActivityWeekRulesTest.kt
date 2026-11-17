package com.nutria.app.feature.activity

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityWeekRulesTest {
    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun weekStartsOnMondayAndContainsSevenDates() {
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 15),
            ),
            ActivityWeekRules.datesInWeek(today),
        )
    }

    @Test
    fun historyIncludesTodayAndExactlyThePreviousTwentyNineDays() {
        assertTrue(ActivityWeekRules.isSelectable(today, today))
        assertTrue(ActivityWeekRules.isSelectable(LocalDate.of(2026, 10, 17), today))
        assertFalse(ActivityWeekRules.isSelectable(LocalDate.of(2026, 10, 16), today))
        assertFalse(ActivityWeekRules.isSelectable(today.plusDays(1), today))
    }

    @Test
    fun weekNavigationStopsAtHistoryBoundaryAndClampsPartialWeek() {
        var selectedDate = today
        repeat(4) {
            selectedDate = ActivityWeekRules.adjacentWeekSelection(selectedDate, -1, today)
                ?: error("Expected an earlier week within the history window")
        }
        assertEquals(LocalDate.of(2026, 10, 18), selectedDate)
        assertNull(ActivityWeekRules.adjacentWeekSelection(selectedDate, -1, today))
        assertNull(ActivityWeekRules.adjacentWeekSelection(today, 1, today))

        val monday = LocalDate.of(2026, 11, 16)
        var mondaySelection = monday
        repeat(5) {
            mondaySelection = ActivityWeekRules.adjacentWeekSelection(mondaySelection, -1, monday)
                ?: error("Expected the partial oldest week")
        }
        assertEquals(LocalDate.of(2026, 10, 18), mondaySelection)
    }
}
