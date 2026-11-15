package com.nutria.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration

sealed interface HealthConnectStepsState {
    val localDate: LocalDate

    data class Available(
        override val localDate: LocalDate,
        val totalSteps: Long,
        val refreshedAt: Instant,
    ) : HealthConnectStepsState {
        init {
            // A zero aggregate is represented by NoData, never a numeric reading.
            require(totalSteps > 0)
        }
    }

    data class NoData(
        override val localDate: LocalDate,
        val refreshedAt: Instant,
    ) : HealthConnectStepsState

    data class PermissionRequired(override val localDate: LocalDate) : HealthConnectStepsState
    data class PermissionDenied(override val localDate: LocalDate) : HealthConnectStepsState
    data class PermissionRevoked(override val localDate: LocalDate) : HealthConnectStepsState
    data class ProviderUnavailable(override val localDate: LocalDate) : HealthConnectStepsState
    data class ProviderUpdateRequired(override val localDate: LocalDate) : HealthConnectStepsState
    data class ReadError(override val localDate: LocalDate) : HealthConnectStepsState
}

sealed interface HeartRateBandState {
    data object Unsupported : HeartRateBandState
    data object BluetoothDisabled : HeartRateBandState
    data object PermissionRequired : HeartRateBandState
    data object PermissionDenied : HeartRateBandState
    data object Disconnected : HeartRateBandState
    data object Scanning : HeartRateBandState
    data object Connecting : HeartRateBandState
    data class Connected(val displayName: String?) : HeartRateBandState
    data class Incompatible(val reason: ConnectionFailure) : HeartRateBandState
    data class ConnectionFailed(val reason: ConnectionFailure) : HeartRateBandState
    data class Lost(val reason: ConnectionFailure) : HeartRateBandState
}

enum class ConnectionFailure {
    SCAN_FAILED,
    CONNECTION_FAILED,
    MISSING_HEART_RATE_SERVICE,
    MISSING_MEASUREMENT_CHARACTERISTIC,
    LINK_LOST,
    BLUETOOTH_DISABLED,
    PERMISSION_REVOKED,
    SERVICE_STOPPED,
}

data class HeartRateReading(
    val beatsPerMinute: Int,
    val receivedAt: Instant,
) {
    init {
        require(beatsPerMinute in 1..MAX_BPM)
    }

    companion object {
        const val MAX_BPM = 65_535
    }
}

sealed interface HeartRateFreshness {
    data object NoReading : HeartRateFreshness
    data class Fresh(val reading: HeartRateReading) : HeartRateFreshness
    data class Stale(val reading: HeartRateReading) : HeartRateFreshness
}

object HeartRateRules {
    val STALE_AFTER: Duration = Duration.ofSeconds(15)

    fun freshness(reading: HeartRateReading?, now: Instant): HeartRateFreshness {
        if (reading == null) return HeartRateFreshness.NoReading
        val age = Duration.between(reading.receivedAt, now)
        return if (!age.isNegative && age <= STALE_AFTER) {
            HeartRateFreshness.Fresh(reading)
        } else {
            // Future timestamps are also not represented as a live measurement.
            HeartRateFreshness.Stale(reading)
        }
    }

    /** Parses the standard Heart Rate Measurement (0x2A37) flags and BPM field. */
    fun parseMeasurement(value: ByteArray, receivedAt: Instant): HeartRateReading? {
        if (value.size < 2) return null
        val flags = value[0].toInt() and 0xFF
        if (flags and RESERVED_FLAGS_MASK != 0) return null
        val usesUInt16 = flags and UINT16_VALUE_FLAG != 0
        val bpm = if (usesUInt16) {
            if (value.size < 3) return null
            (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
        } else {
            value[1].toInt() and 0xFF
        }
        return runCatching { HeartRateReading(bpm, receivedAt) }.getOrNull()
    }

    private const val UINT16_VALUE_FLAG = 0x01
    private const val RESERVED_FLAGS_MASK = 0xE0
}

enum class WorkoutSessionStatus { ACTIVE, INTERRUPTED, COMPLETED }

enum class WorkoutEndStatus { INTERRUPTED, COMPLETED }

data class WorkoutSession(
    val id: String,
    val startedAt: Instant,
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val status: WorkoutSessionStatus,
    val endedAt: Instant? = null,
) {
    init {
        require(id.isNotBlank())
        require(endedAt == null || endedAt >= startedAt)
        require(status != WorkoutSessionStatus.COMPLETED || endedAt != null)
        require(status != WorkoutSessionStatus.ACTIVE || endedAt == null)
    }
}

enum class WorkoutEventKind {
    USER_STARTED,
    USER_STOPPED,
    USER_DISCONNECTED,
    CONNECTION_LOST,
    RECONNECTED,
    BLUETOOTH_DISABLED,
    PERMISSION_LOST,
    PROCESS_RECOVERY,
}

data class WorkoutEvent(
    val sessionId: String,
    val kind: WorkoutEventKind,
    val observedAt: Instant,
) {
    init {
        require(sessionId.isNotBlank())
    }
}

data class WorkoutGap(
    val startedAt: Instant,
    val endedAt: Instant?,
) {
    init {
        require(endedAt == null || endedAt >= startedAt)
    }
}

object WorkoutGapRules {
    /**
     * Gaps start only at an observed interruption marker and close only at an
     * observed reconnect or user stop. Missing samples are never interpolated.
     */
    fun gaps(events: List<WorkoutEvent>, sessionId: String): List<WorkoutGap> {
        val ordered = events
            .filter { it.sessionId == sessionId }
            .withIndex()
            .sortedWith(compareBy<IndexedValue<WorkoutEvent>> { it.value.observedAt }.thenBy { it.index })
            .map { it.value }
        val result = mutableListOf<WorkoutGap>()
        var openStart: Instant? = null

        for (event in ordered) {
            when (event.kind) {
                WorkoutEventKind.CONNECTION_LOST,
                WorkoutEventKind.USER_DISCONNECTED,
                WorkoutEventKind.BLUETOOTH_DISABLED,
                WorkoutEventKind.PERMISSION_LOST,
                WorkoutEventKind.PROCESS_RECOVERY,
                -> if (openStart == null) openStart = event.observedAt

                WorkoutEventKind.RECONNECTED,
                WorkoutEventKind.USER_STOPPED,
                -> openStart?.let { start ->
                    if (event.observedAt >= start) {
                        result += WorkoutGap(start, event.observedAt)
                        openStart = null
                    }
                }

                WorkoutEventKind.USER_STARTED -> Unit
            }
        }
        openStart?.let { result += WorkoutGap(it, null) }
        return result
    }
}
