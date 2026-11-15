package com.nutria.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.nutria.app.core.data.HealthConnectStepsRepository
import com.nutria.app.core.model.HealthConnectStepsState
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class HealthConnectProviderAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UPDATE_REQUIRED,
}

internal interface HealthConnectStepsDataSource {
    fun providerAvailability(): HealthConnectProviderAvailability
    suspend fun grantedPermissions(): Set<String>
    suspend fun aggregateSteps(startInclusive: Instant, endExclusive: Instant): Long?
}

/**
 * Android platform boundary. It reads a daily aggregate only; it has no write,
 * persistence, upload, or background-read operation.
 */
internal class AndroidHealthConnectStepsDataSource(context: Context) : HealthConnectStepsDataSource {
    private val appContext = context.applicationContext
    private val client by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HealthConnectClient.getOrCreate(appContext)
    }

    override fun providerAvailability(): HealthConnectProviderAvailability =
        when (
            HealthConnectClient.getSdkStatus(
                appContext,
                HEALTH_CONNECT_PROVIDER_PACKAGE
            )
        ) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectProviderAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectProviderAvailability.UPDATE_REQUIRED
            else -> HealthConnectProviderAvailability.UNAVAILABLE
        }

    override suspend fun grantedPermissions(): Set<String> =
        client.permissionController.getGrantedPermissions()

    override suspend fun aggregateSteps(startInclusive: Instant, endExclusive: Instant): Long? {
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startInclusive, endExclusive)
            )
        )
        return response[StepsRecord.COUNT_TOTAL]
    }

    companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}

/**
 * Converts Health Connect's runtime state to the shared domain contract.
 * The current local-day bounds are DST-aware and the returned total exists
 * only in transient caller/UI state.
 */
internal class HealthConnectStepsRepositoryImpl(
    private val dataSource: HealthConnectStepsDataSource,
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
) : HealthConnectStepsRepository {
    override suspend fun readDailySteps(localDate: LocalDate): HealthConnectStepsState {
        try {
            when (dataSource.providerAvailability()) {
                HealthConnectProviderAvailability.UNAVAILABLE ->
                    return HealthConnectStepsState.ProviderUnavailable(localDate)
                HealthConnectProviderAvailability.UPDATE_REQUIRED ->
                    return HealthConnectStepsState.ProviderUpdateRequired(localDate)
                HealthConnectProviderAvailability.AVAILABLE -> Unit
            }

            if (READ_STEPS_PERMISSION !in dataSource.grantedPermissions()) {
                return HealthConnectStepsState.PermissionRequired(localDate)
            }

            val zoneId = zoneIdProvider()
            val start = localDate.atStartOfDay(zoneId).toInstant()
            val end = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
            val totalSteps = dataSource.aggregateSteps(start, end)
            val refreshedAt = clock.instant()
            return if (totalSteps == null || totalSteps <= 0L) {
                HealthConnectStepsState.NoData(localDate, refreshedAt)
            } else {
                HealthConnectStepsState.Available(localDate, totalSteps, refreshedAt)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            return HealthConnectStepsState.PermissionRevoked(localDate)
        } catch (_: Exception) {
            return HealthConnectStepsState.ReadError(localDate)
        }
    }

    companion object {
        val READ_STEPS_PERMISSION: String =
            HealthPermission.getReadPermission(StepsRecord::class)
    }
}
