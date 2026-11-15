package com.nutria.app.data.health

import com.nutria.app.core.model.HealthConnectStepsState
import java.util.concurrent.CancellationException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectStepsRepositoryTest {
    private val date = LocalDate.of(2026, 11, 14)
    private val refreshedAt = Instant.parse("2026-11-14T12:00:00Z")
    private val grantedSteps = setOf(HealthConnectStepsRepositoryImpl.READ_STEPS_PERMISSION)

    @Test
    fun integrationRequestsOnlyTheHealthConnectReadStepsPermission() {
        assertEquals(
            "android.permission.health.READ_STEPS",
            HealthConnectStepsRepositoryImpl.READ_STEPS_PERMISSION,
        )
    }

    @Test
    fun providerUnavailableOrUpdateRequiredReturnsDistinctStateWithoutQuerying() = runBlockingTest {
        val unavailable = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.UNAVAILABLE,
            permissions = grantedSteps,
            total = 123L,
        )
        assertEquals(
            HealthConnectStepsState.ProviderUnavailable(date),
            repository(unavailable).readDailySteps(date),
        )
        assertFalse(unavailable.aggregateCalled)

        val updateRequired = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.UPDATE_REQUIRED,
            permissions = grantedSteps,
            total = 123L,
        )
        assertEquals(
            HealthConnectStepsState.ProviderUpdateRequired(date),
            repository(updateRequired).readDailySteps(date),
        )
        assertFalse(updateRequired.aggregateCalled)
    }

    @Test
    fun missingPermissionNeverQueriesHealthRecords() = runBlockingTest {
        val source = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = emptySet(),
            total = 123L,
        )

        assertEquals(
            HealthConnectStepsState.PermissionRequired(date),
            repository(source).readDailySteps(date),
        )
        assertFalse(source.aggregateCalled)
    }

    @Test
    fun onlyPositiveActualAggregateIsReportedWithRefreshTimestamp() = runBlockingTest {
        val source = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            total = 8_321L,
        )

        assertEquals(
            HealthConnectStepsState.Available(date, 8_321L, refreshedAt),
            repository(source).readDailySteps(date),
        )
        assertTrue(source.aggregateCalled)
    }

    @Test
    fun nullAndZeroAggregatesAreNoDataNotZeroReadings() = runBlockingTest {
        listOf(null, 0L).forEach { total ->
            val source = FakeStepsDataSource(
                availability = HealthConnectProviderAvailability.AVAILABLE,
                permissions = grantedSteps,
                total = total,
            )
            assertEquals(
                HealthConnectStepsState.NoData(date, refreshedAt),
                repository(source).readDailySteps(date),
            )
        }
    }

    @Test
    fun localDayRangeUsesZoneBoundariesAndHandlesDaylightSavingTransitions() = runBlockingTest {
        val springForwardDay = LocalDate.of(2026, 3, 8)
        val source = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            total = 1L,
        )
        repository(source, ZoneId.of("America/Los_Angeles"))
            .readDailySteps(springForwardDay)

        assertEquals(Instant.parse("2026-03-08T08:00:00Z"), source.startInclusive)
        assertEquals(Instant.parse("2026-03-09T07:00:00Z"), source.endExclusive)
        assertEquals(
            Duration.ofHours(23),
            Duration.between(source.startInclusive!!, source.endExclusive!!),
        )

        val fallBackDay = LocalDate.of(2026, 11, 1)
        val fallBackSource = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            total = 1L,
        )
        repository(fallBackSource, ZoneId.of("America/Los_Angeles"))
            .readDailySteps(fallBackDay)

        assertEquals(Instant.parse("2026-11-01T07:00:00Z"), fallBackSource.startInclusive)
        assertEquals(Instant.parse("2026-11-02T08:00:00Z"), fallBackSource.endExclusive)
        assertEquals(
            Duration.ofHours(25),
            Duration.between(fallBackSource.startInclusive!!, fallBackSource.endExclusive!!),
        )
    }

    @Test
    fun permissionRevocationAndReadErrorsRemainDistinct() = runBlockingTest {
        val revoked = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            error = SecurityException("permission revoked"),
        )
        assertEquals(
            HealthConnectStepsState.PermissionRevoked(date),
            repository(revoked).readDailySteps(date),
        )

        val failed = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            error = IllegalStateException("provider error"),
        )
        assertEquals(
            HealthConnectStepsState.ReadError(date),
            repository(failed).readDailySteps(date),
        )
    }

    @Test
    fun coroutineCancellationIsNotConvertedIntoAReadError() {
        val source = FakeStepsDataSource(
            availability = HealthConnectProviderAvailability.AVAILABLE,
            permissions = grantedSteps,
            error = CancellationException("screen closed"),
        )

        val outcome = runCatching { runBlockingTest { repository(source).readDailySteps(date) } }
        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }

    private fun repository(
        source: FakeStepsDataSource,
        zoneId: ZoneId = ZoneOffset.UTC,
    ) = HealthConnectStepsRepositoryImpl(
        dataSource = source,
        zoneIdProvider = { zoneId },
        clock = Clock.fixed(refreshedAt, ZoneOffset.UTC),
    )

    private class FakeStepsDataSource(
        private val availability: HealthConnectProviderAvailability,
        private val permissions: Set<String>,
        private val total: Long? = null,
        private val error: Exception? = null,
    ) : HealthConnectStepsDataSource {
        var aggregateCalled = false
            private set
        var startInclusive: Instant? = null
            private set
        var endExclusive: Instant? = null
            private set

        override fun providerAvailability() = availability

        override suspend fun grantedPermissions() = permissions

        override suspend fun aggregateSteps(startInclusive: Instant, endExclusive: Instant): Long? {
            aggregateCalled = true
            this.startInclusive = startInclusive
            this.endExclusive = endExclusive
            error?.let { throw it }
            return total
        }
    }
}

private fun runBlockingTest(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking { block() }
}
