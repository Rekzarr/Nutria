package com.nutria.app.feature.activity

import com.nutria.app.core.model.ConnectionFailure
import com.nutria.app.core.model.HeartRateBandState
import com.nutria.app.core.model.HeartRateFreshness
import com.nutria.app.core.model.HeartRateReading
import com.nutria.app.core.model.HeartRateRules
import com.nutria.app.core.model.WorkoutEvent
import com.nutria.app.core.model.WorkoutEventKind
import com.nutria.app.core.model.WorkoutGap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WorkoutRulesTest {
    @Test
    fun unsupportedPermissionOffIncompatibleAndLostStatesRemainDistinct() {
        val states = listOf(
            HeartRateBandState.Unsupported,
            HeartRateBandState.PermissionRequired,
            HeartRateBandState.PermissionDenied,
            HeartRateBandState.BluetoothDisabled,
            HeartRateBandState.Disconnected,
            HeartRateBandState.Scanning,
            HeartRateBandState.Connecting,
            HeartRateBandState.Connected("Banda"),
            HeartRateBandState.Incompatible(ConnectionFailure.MISSING_HEART_RATE_SERVICE),
            HeartRateBandState.ConnectionFailed(ConnectionFailure.CONNECTION_FAILED),
            HeartRateBandState.Lost(ConnectionFailure.LINK_LOST),
        )
        assertEquals(states.size, states.distinct().size)
    }

    @Test
    fun parsesActualEightAndSixteenBitMeasurementsAndRejectsMalformedValues() {
        val receivedAt = Instant.parse("2026-11-14T10:00:00Z")
        assertEquals(
            81,
            HeartRateRules.parseMeasurement(byteArrayOf(0, 81), receivedAt)?.beatsPerMinute,
        )
        assertEquals(
            513,
            HeartRateRules.parseMeasurement(byteArrayOf(1, 1, 2), receivedAt)?.beatsPerMinute,
        )
        assertNull(HeartRateRules.parseMeasurement(byteArrayOf(1, 1), receivedAt))
        assertNull(HeartRateRules.parseMeasurement(byteArrayOf(0x20, 81), receivedAt))
        assertNull(HeartRateRules.parseMeasurement(byteArrayOf(0, 0), receivedAt))
    }

    @Test
    fun staleAndFutureMeasurementsAreNeverPresentedAsFresh() {
        val now = Instant.parse("2026-11-14T10:00:15Z")
        val exactlyFresh = HeartRateReading(72, now.minusSeconds(15))
        val stale = HeartRateReading(73, now.minusSeconds(16))
        val future = HeartRateReading(74, now.plusSeconds(1))

        assertTrue(HeartRateRules.freshness(null, now) is HeartRateFreshness.NoReading)
        assertEquals(HeartRateFreshness.Fresh(exactlyFresh), HeartRateRules.freshness(exactlyFresh, now))
        assertTrue(HeartRateRules.freshness(stale, now) is HeartRateFreshness.Stale)
        assertTrue(HeartRateRules.freshness(future, now) is HeartRateFreshness.Stale)
    }

    @Test
    fun onlyObservedReadingsAndInterruptionEventsDefineWorkoutGaps() {
        val start = Instant.parse("2026-11-14T10:00:00Z")
        val sessionId = "synthetic-session"
        val parsed = HeartRateRules.parseMeasurement(byteArrayOf(0, 81), start.plusSeconds(5))
        assertEquals(81, parsed?.beatsPerMinute)

        val events = listOf(
            WorkoutEvent(sessionId, WorkoutEventKind.USER_STARTED, start),
            WorkoutEvent(sessionId, WorkoutEventKind.CONNECTION_LOST, start.plusSeconds(20)),
            WorkoutEvent(sessionId, WorkoutEventKind.RECONNECTED, start.plusSeconds(40)),
            WorkoutEvent(sessionId, WorkoutEventKind.USER_DISCONNECTED, start.plusSeconds(55)),
        )
        assertEquals(
            listOf(
                WorkoutGap(start.plusSeconds(20), start.plusSeconds(40)),
                WorkoutGap(start.plusSeconds(55), null),
            ),
            com.nutria.app.core.model.WorkoutGapRules.gaps(events, sessionId),
        )
    }
}
