package com.nutria.app.core.model

import java.time.LocalDate

enum class PeriodValidationError {
    START_IN_FUTURE,
    END_BEFORE_START,
    END_IN_FUTURE,
    OVERLAPS_EXISTING_PERIOD,
}

enum class CyclePhase { MENSTRUAL, FOLLICULAR, OVULATORY_WINDOW, LUTEAL, POSSIBLY_LATE }

sealed interface CycleEstimate {
    data object Unknown : CycleEstimate

    data class Known(
        val phase: CyclePhase,
        val cycleDay: Int,
        val cycleLengthDays: Int,
        val expectedNextStart: LocalDate,
        val daysLate: Long,
        val isOngoing: Boolean,
    ) : CycleEstimate
}

object CycleRules {
    fun validateSettings(cycleLengthDays: Int, periodLengthDays: Int): Boolean =
        cycleLengthDays in CycleSettings.MIN_CYCLE_LENGTH_DAYS..CycleSettings.MAX_CYCLE_LENGTH_DAYS &&
            periodLengthDays in CycleSettings.MIN_PERIOD_LENGTH_DAYS..CycleSettings.MAX_PERIOD_LENGTH_DAYS &&
            periodLengthDays < cycleLengthDays

    /**
     * Validates against the legacy inclusive overlap rule. A period without an end
     * is treated as ending today for overlap checking; an older open record is
     * bounded by the configured average period length.
     */
    fun validateNewPeriod(
        startDate: LocalDate,
        endDate: LocalDate?,
        today: LocalDate,
        settings: CycleSettings,
        existingPeriods: List<PeriodRecord>,
    ): PeriodValidationError? {
        if (startDate > today) return PeriodValidationError.START_IN_FUTURE
        if (endDate != null && endDate < startDate) return PeriodValidationError.END_BEFORE_START
        if (endDate != null && endDate > today) return PeriodValidationError.END_IN_FUTURE

        val newEnd = endDate ?: today
        val overlaps = existingPeriods.any { period ->
            if (period.startDate > today) return@any false
            val oldEnd = period.endDate
                ?.takeIf { it >= period.startDate }
                ?: period.startDate.plusDays(settings.periodLengthDays - 1L)
            startDate <= oldEnd && newEnd >= period.startDate
        }
        return if (overlaps) PeriodValidationError.OVERLAPS_EXISTING_PERIOD else null
    }

    /** Mirrors the PWA's calendar estimate, without implying a medical prediction. */
    fun estimate(
        periods: List<PeriodRecord>,
        settings: CycleSettings,
        asOfDate: LocalDate,
    ): CycleEstimate {
        val latest = periods
            .asSequence()
            .filter { it.startDate <= asOfDate }
            .maxByOrNull { it.startDate }
            ?: return CycleEstimate.Unknown

        val cycleDay = (latest.startDate.daysUntil(asOfDate) + 1L).toInt()
        val isOngoing = if (latest.endDate == null) {
            cycleDay <= settings.periodLengthDays
        } else {
            latest.endDate >= asOfDate
        }
        val expectedStart = latest.startDate.plusDays(settings.cycleLengthDays.toLong())
        val daysLate = maxOf(0L, expectedStart.daysUntil(asOfDate))
        val ovulationDay = settings.cycleLengthDays - 14
        val ovulationStart = maxOf(settings.periodLengthDays + 1, ovulationDay - 1)
        val ovulationEnd = maxOf(ovulationStart, ovulationDay + 1)

        val phase = when {
            isOngoing || cycleDay <= settings.periodLengthDays -> CyclePhase.MENSTRUAL
            cycleDay > settings.cycleLengthDays -> CyclePhase.POSSIBLY_LATE
            cycleDay in ovulationStart..ovulationEnd -> CyclePhase.OVULATORY_WINDOW
            cycleDay < ovulationStart -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
        return CycleEstimate.Known(
            phase = phase,
            cycleDay = cycleDay,
            cycleLengthDays = settings.cycleLengthDays,
            expectedNextStart = expectedStart,
            daysLate = daysLate,
            isOngoing = isOngoing,
        )
    }

    fun normalizedNote(note: String): String {
        val normalized = note.trim()
        require(normalized.length <= CycleDayEntry.MAX_NOTE_LENGTH)
        return normalized
    }

    private fun LocalDate.daysUntil(other: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(this, other)
}
