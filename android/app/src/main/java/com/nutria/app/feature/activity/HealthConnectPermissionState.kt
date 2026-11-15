package com.nutria.app.feature.activity

import com.nutria.app.core.model.HealthConnectStepsState
import java.time.LocalDate

/** Classifies a missing read grant without persisting permission or health data. */
internal object HealthConnectPermissionState {
    fun missingGrant(
        localDate: LocalDate,
        grantedInCurrentProcess: Boolean,
        deniedInCurrentProcess: Boolean,
    ): HealthConnectStepsState = when {
        deniedInCurrentProcess -> HealthConnectStepsState.PermissionDenied(localDate)
        grantedInCurrentProcess -> HealthConnectStepsState.PermissionRevoked(localDate)
        else -> HealthConnectStepsState.PermissionRequired(localDate)
    }
}
