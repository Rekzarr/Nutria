package com.nutria.app.feature.activity

import com.nutria.app.core.model.HealthConnectStepsState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectPermissionStateTest {
    private val date = LocalDate.of(2026, 11, 14)

    @Test
    fun unknownMissingGrantStaysPermissionRequired() {
        assertEquals(
            HealthConnectStepsState.PermissionRequired(date),
            HealthConnectPermissionState.missingGrant(
                localDate = date,
                grantedInCurrentProcess = false,
                deniedInCurrentProcess = false,
            ),
        )
    }

    @Test
    fun explicitDenialIsReportedAsDenied() {
        assertEquals(
            HealthConnectStepsState.PermissionDenied(date),
            HealthConnectPermissionState.missingGrant(
                localDate = date,
                grantedInCurrentProcess = false,
                deniedInCurrentProcess = true,
            ),
        )
    }

    @Test
    fun previouslyGrantedPermissionIsReportedAsRevoked() {
        assertEquals(
            HealthConnectStepsState.PermissionRevoked(date),
            HealthConnectPermissionState.missingGrant(
                localDate = date,
                grantedInCurrentProcess = true,
                deniedInCurrentProcess = false,
            ),
        )
    }
}
