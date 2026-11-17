package com.nutria.app.feature.activity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["local_date", "started_at_epoch_ms"])],
)
internal data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at_epoch_ms") val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at_epoch_ms") val endedAtEpochMs: Long?,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    val status: String,
    @ColumnInfo(name = "end_reason") val endReason: String?,
)

@Entity(
    tableName = "workout_events",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "occurred_at_epoch_ms"])],
)
internal data class WorkoutEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val kind: String,
    @ColumnInfo(name = "occurred_at_epoch_ms") val occurredAtEpochMs: Long,
)

@Entity(
    tableName = "heart_rate_samples",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "received_at_epoch_ms"])],
)
internal data class HeartRateSampleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "received_at_epoch_ms") val receivedAtEpochMs: Long,
    @ColumnInfo(name = "beats_per_minute") val beatsPerMinute: Int,
)

internal data class WorkoutSessionWithSampleCount(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "sample_count") val sampleCount: Int,
    @ColumnInfo(name = "had_interruption") val hadInterruption: Boolean,
    @ColumnInfo(name = "recovery_detected") val recoveryDetected: Boolean,
    @ColumnInfo(name = "interruption_observed_at_epoch_ms") val interruptionObservedAtEpochMs: Long?,
)

@Dao
internal interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: WorkoutEventEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSample(sample: HeartRateSampleEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun findSession(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE'")
    suspend fun findActiveSessions(): List<WorkoutSessionEntity>

    @Query(
        "UPDATE workout_sessions SET status = :status, ended_at_epoch_ms = :endedAtEpochMs, " +
            "end_reason = :endReason WHERE id = :sessionId",
    )
    suspend fun updateSessionState(
        sessionId: String,
        status: String,
        endedAtEpochMs: Long?,
        endReason: String?,
    )

    @Query(
        "SELECT * FROM workout_sessions WHERE local_date = :localDate " +
            "ORDER BY started_at_epoch_ms DESC",
    )
    fun observeSessions(localDate: String): Flow<List<WorkoutSessionEntity>>

    @Query(
        "SELECT s.*, COUNT(h.id) AS sample_count, " +
            "EXISTS(SELECT 1 FROM workout_events AS e WHERE e.session_id = s.id " +
            "AND e.kind IN ('CONNECTION_LOST', 'USER_DISCONNECTED', 'BLUETOOTH_DISABLED', " +
            "'PERMISSION_LOST', 'PROCESS_RECOVERY')) AS had_interruption " +
            ", EXISTS(SELECT 1 FROM workout_events AS r WHERE r.session_id = s.id " +
            "AND r.kind = 'PROCESS_RECOVERY') AS recovery_detected " +
            ", (SELECT MIN(i.occurred_at_epoch_ms) FROM workout_events AS i WHERE i.session_id = s.id " +
            "AND i.kind IN ('CONNECTION_LOST', 'USER_DISCONNECTED', 'BLUETOOTH_DISABLED', " +
            "'PERMISSION_LOST', 'PROCESS_RECOVERY')) AS interruption_observed_at_epoch_ms " +
            "FROM workout_sessions AS s " +
            "LEFT JOIN heart_rate_samples AS h ON h.session_id = s.id " +
            "GROUP BY s.id ORDER BY s.started_at_epoch_ms DESC LIMIT :limit",
    )
    fun observeRecentSessions(limit: Int): Flow<List<WorkoutSessionWithSampleCount>>

    @Query(
        "SELECT s.*, COUNT(h.id) AS sample_count, " +
            "EXISTS(SELECT 1 FROM workout_events AS e WHERE e.session_id = s.id " +
            "AND e.kind IN ('CONNECTION_LOST', 'USER_DISCONNECTED', 'BLUETOOTH_DISABLED', " +
            "'PERMISSION_LOST', 'PROCESS_RECOVERY')) AS had_interruption " +
            ", EXISTS(SELECT 1 FROM workout_events AS r WHERE r.session_id = s.id " +
            "AND r.kind = 'PROCESS_RECOVERY') AS recovery_detected " +
            ", (SELECT MIN(i.occurred_at_epoch_ms) FROM workout_events AS i WHERE i.session_id = s.id " +
            "AND i.kind IN ('CONNECTION_LOST', 'USER_DISCONNECTED', 'BLUETOOTH_DISABLED', " +
            "'PERMISSION_LOST', 'PROCESS_RECOVERY')) AS interruption_observed_at_epoch_ms " +
            "FROM workout_sessions AS s " +
            "LEFT JOIN heart_rate_samples AS h ON h.session_id = s.id " +
            "WHERE s.local_date = :localDate " +
            "GROUP BY s.id ORDER BY s.started_at_epoch_ms DESC",
    )
    fun observeSessionSummaries(localDate: String): Flow<List<WorkoutSessionWithSampleCount>>

    @Query(
        "SELECT * FROM workout_events WHERE session_id = :sessionId " +
            "ORDER BY occurred_at_epoch_ms, id",
    )
    fun observeEvents(sessionId: String): Flow<List<WorkoutEventEntity>>

    @Query(
        "SELECT id, session_id, received_at_epoch_ms, beats_per_minute " +
            "FROM heart_rate_samples WHERE session_id = :sessionId " +
            "ORDER BY received_at_epoch_ms, id",
    )
    fun observeSamples(sessionId: String): Flow<List<HeartRateSampleEntity>>

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
