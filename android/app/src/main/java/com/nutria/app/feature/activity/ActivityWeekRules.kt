package com.nutria.app.feature.activity

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal object ActivityWeekRules {
    const val HISTORY_DAYS = 30L

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun datesInWeek(date: LocalDate): List<LocalDate> =
        (0L..6L).map { weekStart(date).plusDays(it) }

    fun earliestDate(today: LocalDate): LocalDate =
        today.minusDays(HISTORY_DAYS - 1)

    fun isSelectable(date: LocalDate, today: LocalDate): Boolean =
        date >= earliestDate(today) && date <= today

    fun adjacentWeekSelection(
        selectedDate: LocalDate,
        weekOffset: Long,
        today: LocalDate,
    ): LocalDate? {
        require(weekOffset == -1L || weekOffset == 1L)
        val destinationWeekStart = weekStart(selectedDate).plusWeeks(weekOffset)
        if (destinationWeekStart < weekStart(earliestDate(today)) ||
            destinationWeekStart > weekStart(today)
        ) {
            return null
        }

        val firstSelectableDate = maxOf(destinationWeekStart, earliestDate(today))
        val lastSelectableDate = minOf(destinationWeekStart.plusDays(6), today)
        return selectedDate.plusWeeks(weekOffset).coerceIn(firstSelectableDate, lastSelectableDate)
    }
}
