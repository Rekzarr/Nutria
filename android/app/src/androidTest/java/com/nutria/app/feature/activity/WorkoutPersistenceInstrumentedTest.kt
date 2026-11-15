package com.nutria.app.feature.activity

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutria.app.core.model.HeartRateReading
import com.nutria.app.core.model.WorkoutEvent
import com.nutria.app.core.model.WorkoutEventKind
import com.nutria.app.core.model.WorkoutSessionStatus
import com.nutria.app.data.local.NutriaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class WorkoutPersistenceInstrumentedTest {
    private lateinit var database: NutriaDatabase
    private lateinit var repository: WorkoutRoomRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NutriaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRoomRepository { database }
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun persistsOnlyReceivedSamplesAndObservedGapThenCascadesExplicitDeletion() = runBlocking {
        val startedAt = Instant.parse("2026-11-14T10:00:00Z")
        val session = repository.startWorkout(
            localDate = LocalDate.of(2026, 11, 14),
            zoneId = ZoneId.of("Europe/Madrid"),
            startedAt = startedAt,
        )
        repository.appendReceivedReading(
            session.id,
            HeartRateReading(83, startedAt.plusSeconds(4)),
        )
        repository.recordInterruption(
            session.id,
            WorkoutEvent(
                session.id,
                WorkoutEventKind.CONNECTION_LOST,
                startedAt.plusSeconds(12),
            ),
        )

        assertEquals(
            WorkoutSessionStatus.INTERRUPTED,
            repository.observeSessions(session.localDate).first().single().status,
        )
        assertEquals(
            listOf(83),
            repository.observeReadings(session.id).first().map { it.beatsPerMinute },
        )
        assertEquals(
            listOf(WorkoutEventKind.USER_STARTED, WorkoutEventKind.CONNECTION_LOST),
            repository.observeEvents(session.id).first().map { it.kind },
        )

        repository.deleteWorkout(session.id)
        assertTrue(repository.observeSessions(session.localDate).first().isEmpty())
        assertTrue(repository.observeReadings(session.id).first().isEmpty())
        assertTrue(repository.observeEvents(session.id).first().isEmpty())
    }
}
