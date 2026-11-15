package com.nutria.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ActivityRulesTest {
    private val now = Instant.parse("2026-11-14T12:00:00Z")
    private val sessionId = "session-1"

    @Test
    fun stepsDistinguishActualPositiveReadingsFromNoDataAndUnavailableStates() {
        val date = LocalDate.of(2026, 11, 14)
        assertEquals(HealthConnectStepsState.NoData(date, now), HealthConnectStepsState.NoData(date, now))
        assertEquals(8_321L, HealthConnectStepsState.Available(date, 8_321, now).totalSteps)
        assertFalse(runCatching { HealthConnectStepsState.Available(date, 0, now) }.isSuccess)
        assertEquals(date, HealthConnectStepsState.PermissionRequired(date).localDate)
        assertEquals(date, HealthConnectStepsState.PermissionDenied(date).localDate)
        assertEquals(date, HealthConnectStepsState.PermissionRevoked(date).localDate)
        assertEquals(date, HealthConnectStepsState.ProviderUnavailable(date).localDate)
        assertEquals(date, HealthConnectStepsState.ProviderUpdateRequired(date).localDate)
        assertEquals(date, HealthConnectStepsState.ReadError(date).localDate)
    }

    @Test
    fun readingFreshnessNeverPresentsMissingOldOrFutureValuesAsLive() {
        val recent = HeartRateReading(72, now.minusSeconds(15))
        val old = HeartRateReading(72, now.minusSeconds(16))
        val future = HeartRateReading(72, now.plusSeconds(1))
        assertEquals(HeartRateFreshness.NoReading, HeartRateRules.freshness(null, now))
        assertEquals(HeartRateFreshness.Fresh(recent), HeartRateRules.freshness(recent, now))
        assertEquals(HeartRateFreshness.Stale(old), HeartRateRules.freshness(old, now))
        assertEquals(HeartRateFreshness.Stale(future), HeartRateRules.freshness(future, now))
        assertFalse(runCatching { HeartRateReading(0, now) }.isSuccess)
    }

    @Test
    fun parsesStandardEightAndSixteenBitMeasurementsAndRejectsMalformedValues() {
        assertEquals(HeartRateReading(72, now), HeartRateRules.parseMeasurement(byteArrayOf(0, 72), now))
        assertEquals(HeartRateReading(300, now), HeartRateRules.parseMeasurement(byteArrayOf(1, 44, 1), now))
        assertEquals(null, HeartRateRules.parseMeasurement(byteArrayOf(1, 44), now))
        assertEquals(null, HeartRateRules.parseMeasurement(byteArrayOf(0), now))
        assertEquals(null, HeartRateRules.parseMeasurement(byteArrayOf(0, 0), now))
        assertEquals(null, HeartRateRules.parseMeasurement(byteArrayOf(0x20, 72), now))
    }

    @Test
    fun gapsBeginOnlyAtObservedInterruptionsAndCloseOnExplicitRecoveryOrStop() {
        val events = listOf(
            event(WorkoutEventKind.USER_STARTED, now),
            event(WorkoutEventKind.CONNECTION_LOST, now.plusSeconds(10)),
            event(WorkoutEventKind.BLUETOOTH_DISABLED, now.plusSeconds(12)),
            event(WorkoutEventKind.RECONNECTED, now.plusSeconds(20)),
            event(WorkoutEventKind.PERMISSION_LOST, now.plusSeconds(30)),
            event(WorkoutEventKind.USER_STOPPED, now.plusSeconds(35)),
            WorkoutEvent("other", WorkoutEventKind.CONNECTION_LOST, now.plusSeconds(40)),
        )
        assertEquals(
            listOf(
                WorkoutGap(now.plusSeconds(10), now.plusSeconds(20)),
                WorkoutGap(now.plusSeconds(30), now.plusSeconds(35)),
            ),
            WorkoutGapRules.gaps(events, sessionId),
        )
        assertTrue(
            WorkoutGapRules.gaps(listOf(event(WorkoutEventKind.PROCESS_RECOVERY, now)), sessionId)
                .single().endedAt == null,
        )
    }

    private fun event(kind: WorkoutEventKind, at: Instant) = WorkoutEvent(sessionId, kind, at)
}
