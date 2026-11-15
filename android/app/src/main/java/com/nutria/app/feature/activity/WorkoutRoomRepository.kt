package com.nutria.app.feature.activity

import androidx.room.withTransaction
import com.nutria.app.core.data.WorkoutRepository
import com.nutria.app.core.data.WorkoutSessionSummary
import com.nutria.app.core.model.HeartRateReading
import com.nutria.app.core.model.WorkoutEndStatus
import com.nutria.app.core.model.WorkoutEvent
import com.nutria.app.core.model.WorkoutEventKind
import com.nutria.app.core.model.WorkoutSession
import com.nutria.app.core.model.WorkoutSessionStatus
import com.nutria.app.data.local.NutriaDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * The only persistence adapter for band workout data. Callers must pass readings
 * emitted by the connected Heart Rate Measurement notification stream.
 */
internal class WorkoutRoomRepository(
    private val openDatabase: suspend () -> NutriaDatabase,
) : WorkoutRepository {
    override fun observeSessions(localDate: LocalDate): Flow<List<WorkoutSession>> = flow {
        val dao = openDatabase().workoutDao()
        emitAll(dao.observeSessions(localDate.toString()).map { rows -> rows.map(::toDomain) })
    }

    override fun observeRecentSessions(limit: Int): Flow<List<WorkoutSessionSummary>> = flow {
        val dao = openDatabase().workoutDao()
        emitAll(
            dao.observeRecentSessions(limit.coerceIn(1, MAX_RECENT_SESSIONS))
                .map { rows ->
                    rows.map { row ->
                        WorkoutSessionSummary(
                            session = toDomain(row.session),
                            sampleCount = row.sampleCount,
                            hadInterruption = row.hadInterruption,
                            recoveryDetected = row.recoveryDetected,
                            interruptionObservedAt = row.interruptionObservedAtEpochMs
                                ?.let(Instant::ofEpochMilli),
                        )
                    }
                },
        )
    }

    override fun observeEvents(sessionId: String): Flow<List<WorkoutEvent>> = flow {
        emitAll(
            openDatabase().workoutDao().observeEvents(sessionId).map { rows ->
                rows.map { row ->
                    WorkoutEvent(
                        sessionId = row.sessionId,
                        kind = WorkoutEventKind.valueOf(row.kind),
                        observedAt = Instant.ofEpochMilli(row.occurredAtEpochMs),
                    )
                }
            },
        )
    }

    override fun observeReadings(sessionId: String): Flow<List<HeartRateReading>> = flow {
        emitAll(
            openDatabase().workoutDao().observeSamples(sessionId).map { rows ->
                rows.map { row ->
                    HeartRateReading(
                        beatsPerMinute = row.beatsPerMinute,
                        receivedAt = Instant.ofEpochMilli(row.receivedAtEpochMs),
                    )
                }
            },
        )
    }

    override suspend fun startWorkout(
        localDate: LocalDate,
        zoneId: ZoneId,
        startedAt: Instant,
    ): WorkoutSession {
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            startedAt = startedAt,
            localDate = localDate,
            zoneId = zoneId,
            status = WorkoutSessionStatus.ACTIVE,
        )
        val db = openDatabase()
        db.withTransaction {
            val dao = db.workoutDao()
            dao.insertSession(session.toEntity())
            dao.insertEvent(
                WorkoutEventEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    kind = WorkoutEventKind.USER_STARTED.name,
                    occurredAtEpochMs = startedAt.toEpochMilli(),
                ),
            )
        }
        return session
    }

    override suspend fun appendReceivedReading(sessionId: String, reading: HeartRateReading) {
        val db = openDatabase()
        db.withTransaction {
            val session = db.workoutDao().findSession(sessionId)
                ?: error("No existe la sesión local del entrenamiento.")
            check(session.status == WorkoutSessionStatus.ACTIVE.name) {
                "No se admiten lecturas en una sesión interrumpida o finalizada."
            }
            db.workoutDao().insertSample(
                HeartRateSampleEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    receivedAtEpochMs = reading.receivedAt.toEpochMilli(),
                    beatsPerMinute = reading.beatsPerMinute,
                ),
            )
        }
    }

    override suspend fun recordInterruption(sessionId: String, event: WorkoutEvent) {
        require(event.sessionId == sessionId)
        val db = openDatabase()
        db.withTransaction {
            val dao = db.workoutDao()
            val session = dao.findSession(sessionId) ?: return@withTransaction
            if (session.status != WorkoutSessionStatus.ACTIVE.name) return@withTransaction
            dao.insertEvent(event.toEntity())
            dao.updateSessionState(
                sessionId = sessionId,
                status = WorkoutSessionStatus.INTERRUPTED.name,
                endedAtEpochMs = event.observedAt.toEpochMilli(),
                endReason = event.kind.name,
            )
        }
    }

    override suspend fun recordExplicitReconnection(sessionId: String, event: WorkoutEvent) {
        require(event.sessionId == sessionId)
        require(event.kind == WorkoutEventKind.RECONNECTED)
        val db = openDatabase()
        db.withTransaction {
            val dao = db.workoutDao()
            val session = dao.findSession(sessionId) ?: return@withTransaction
            check(session.status == WorkoutSessionStatus.INTERRUPTED.name) {
                "Solo se puede reanudar explícitamente una sesión interrumpida."
            }
            dao.insertEvent(event.toEntity())
            dao.updateSessionState(
                sessionId = sessionId,
                status = WorkoutSessionStatus.ACTIVE.name,
                endedAtEpochMs = null,
                endReason = null,
            )
        }
    }

    override suspend fun finishWorkout(
        sessionId: String,
        endedAt: Instant,
        status: WorkoutEndStatus,
    ) {
        val db = openDatabase()
        db.withTransaction {
            val dao = db.workoutDao()
            val session = dao.findSession(sessionId) ?: return@withTransaction
            if (session.status == WorkoutSessionStatus.COMPLETED.name) return@withTransaction
            val eventKind = if (status == WorkoutEndStatus.COMPLETED) {
                WorkoutEventKind.USER_STOPPED
            } else {
                WorkoutEventKind.USER_DISCONNECTED
            }
            dao.insertEvent(
                WorkoutEventEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    kind = eventKind.name,
                    occurredAtEpochMs = endedAt.toEpochMilli(),
                ),
            )
            dao.updateSessionState(
                sessionId = sessionId,
                status = status.toSessionStatus().name,
                endedAtEpochMs = endedAt.toEpochMilli(),
                endReason = eventKind.name,
            )
        }
    }

    override suspend fun deleteWorkout(sessionId: String) {
        openDatabase().workoutDao().deleteSession(sessionId)
    }

    /** Marks any session left open by a process death with an observed-on-recovery gap. */
    suspend fun recoverOpenSessions(observedAt: Instant) {
        val db = openDatabase()
        db.withTransaction {
            val dao = db.workoutDao()
            dao.findActiveSessions().forEach { session ->
                val event = WorkoutEvent(
                    sessionId = session.id,
                    kind = WorkoutEventKind.PROCESS_RECOVERY,
                    observedAt = observedAt,
                )
                dao.insertEvent(event.toEntity())
                dao.updateSessionState(
                    sessionId = session.id,
                    status = WorkoutSessionStatus.INTERRUPTED.name,
                    endedAtEpochMs = observedAt.toEpochMilli(),
                    endReason = event.kind.name,
                )
            }
        }
    }

    private fun WorkoutSession.toEntity() = WorkoutSessionEntity(
        id = id,
        startedAtEpochMs = startedAt.toEpochMilli(),
        endedAtEpochMs = endedAt?.toEpochMilli(),
        localDate = localDate.toString(),
        zoneId = zoneId.id,
        status = status.name,
        endReason = null,
    )

    private fun WorkoutEvent.toEntity() = WorkoutEventEntity(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        kind = kind.name,
        occurredAtEpochMs = observedAt.toEpochMilli(),
    )

    private fun toDomain(entity: WorkoutSessionEntity) = WorkoutSession(
        id = entity.id,
        startedAt = Instant.ofEpochMilli(entity.startedAtEpochMs),
        localDate = LocalDate.parse(entity.localDate),
        zoneId = ZoneId.of(entity.zoneId),
        status = WorkoutSessionStatus.valueOf(entity.status),
        endedAt = entity.endedAtEpochMs?.let(Instant::ofEpochMilli),
    )

    private fun WorkoutEndStatus.toSessionStatus() = when (this) {
        WorkoutEndStatus.INTERRUPTED -> WorkoutSessionStatus.INTERRUPTED
        WorkoutEndStatus.COMPLETED -> WorkoutSessionStatus.COMPLETED
    }

    companion object {
        private const val MAX_RECENT_SESSIONS = 100
    }
}
