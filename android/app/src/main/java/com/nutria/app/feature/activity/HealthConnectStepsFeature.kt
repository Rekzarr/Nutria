package com.nutria.app.feature.activity

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.HealthConnectStepsRepository
import com.nutria.app.core.model.HealthConnectStepsState
import com.nutria.app.data.health.AndroidHealthConnectStepsDataSource
import com.nutria.app.data.health.HealthConnectStepsRepositoryImpl
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Shared truthful steps card for Inicio and Actividad. Reads happen on entry,
 * on foreground return, and on an explicit refresh only.
 */
@Composable
fun HealthConnectStepsFeature(
    lifecycleOwner: LifecycleOwner,
    compact: Boolean,
    onOpenActivity: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) {
        HealthConnectStepsRepositoryImpl(AndroidHealthConnectStepsDataSource(context))
    }
    val coroutineScope = rememberCoroutineScope()
    var stepsState by remember { mutableStateOf<HealthConnectStepsState?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val refresh: suspend () -> Unit = {
        isLoading = true
        try {
            val result = repository.readDailySteps(LocalDate.now())
            stepsState = when (result) {
                is HealthConnectStepsState.PermissionRequired ->
                    HealthConnectPermissionState.missingGrant(
                        localDate = result.localDate,
                        grantedInCurrentProcess = HealthConnectPermissionHistory.grantedInThisProcess,
                        deniedInCurrentProcess = HealthConnectPermissionHistory.deniedInThisProcess,
                    )
                is HealthConnectStepsState.Available,
                is HealthConnectStepsState.NoData -> {
                    HealthConnectPermissionHistory.grantedInThisProcess = true
                    HealthConnectPermissionHistory.deniedInThisProcess = false
                    result
                }
                else -> result
            }
        } finally {
            isLoading = false
        }
    }
    val latestRefresh by rememberUpdatedState(refresh)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch { latestRefresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestedPermission = remember {
        HealthPermission.getReadPermission(StepsRecord::class)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (requestedPermission in grantedPermissions) {
            HealthConnectPermissionHistory.grantedInThisProcess = true
            HealthConnectPermissionHistory.deniedInThisProcess = false
            coroutineScope.launch { refresh() }
        } else {
            HealthConnectPermissionHistory.deniedInThisProcess = true
            stepsState = HealthConnectStepsState.PermissionDenied(LocalDate.now())
        }
    }

    HealthConnectStepsContent(
        state = stepsState,
        isLoading = isLoading,
        compact = compact,
        onRefresh = { coroutineScope.launch { refresh() } },
        onAllow = { permissionLauncher.launch(setOf(requestedPermission)) },
        onOpenProvider = { openHealthConnectProvider(context) },
        onOpenActivity = onOpenActivity,
    )
}

private object HealthConnectPermissionHistory {
    // Process-only history distinguishes a prior grant that has since been revoked
    // without persisting any Health Connect permission or step data.
    var grantedInThisProcess: Boolean = false
    var deniedInThisProcess: Boolean = false
}

@Composable
internal fun HealthConnectStepsContent(
    state: HealthConnectStepsState?,
    isLoading: Boolean,
    compact: Boolean,
    onRefresh: () -> Unit,
    onAllow: () -> Unit,
    onOpenProvider: () -> Unit,
    onOpenActivity: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.health_steps_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.health_steps_disclosure_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.health_steps_disclosure),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isLoading && state != null) {
                Text(
                    text = stringResource(R.string.health_steps_refreshing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (state) {
                null -> Text(
                    text = stringResource(R.string.health_steps_checking),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = liveHealthStatusModifier,
                )
                is HealthConnectStepsState.Available -> {
                    Text(
                        text = stringResource(
                            R.string.health_steps_total,
                            SpanishFormats.formatInteger(state.totalSteps)
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = liveHealthStatusModifier,
                    )
                    Text(
                        text = stringResource(R.string.health_steps_source),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.health_steps_updated,
                            SpanishFormats.formatTime(state.refreshedAt)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is HealthConnectStepsState.NoData -> {
                    Text(
                        text = stringResource(R.string.health_steps_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                    Text(
                        text = stringResource(R.string.health_steps_no_data_explanation),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is HealthConnectStepsState.PermissionRequired ->
                    Text(
                        text = stringResource(R.string.health_steps_permission_required),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                is HealthConnectStepsState.PermissionDenied ->
                    Text(
                        text = stringResource(R.string.health_steps_permission_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                is HealthConnectStepsState.PermissionRevoked ->
                    Text(
                        text = stringResource(R.string.health_steps_permission_revoked),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                is HealthConnectStepsState.ProviderUnavailable ->
                    Text(
                        text = stringResource(R.string.health_steps_provider_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                is HealthConnectStepsState.ProviderUpdateRequired ->
                    Text(
                        text = stringResource(R.string.health_steps_provider_update),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
                is HealthConnectStepsState.ReadError ->
                    Text(
                        text = stringResource(R.string.health_steps_read_error),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = liveHealthStatusModifier,
                    )
            }

            if (!compact) {
                when (state) {
                    null -> Unit
                    is HealthConnectStepsState.PermissionRequired ->
                        Button(onClick = onAllow) {
                            Text(stringResource(R.string.health_steps_allow))
                        }
                    is HealthConnectStepsState.PermissionDenied,
                    is HealthConnectStepsState.PermissionRevoked ->
                        Button(onClick = onAllow) {
                            Text(stringResource(R.string.health_steps_allow_again))
                        }
                    is HealthConnectStepsState.ProviderUnavailable,
                    is HealthConnectStepsState.ProviderUpdateRequired ->
                        Button(onClick = onOpenProvider) {
                            Text(stringResource(R.string.health_steps_open_provider))
                        }
                    is HealthConnectStepsState.Available,
                    is HealthConnectStepsState.NoData,
                    is HealthConnectStepsState.ReadError ->
                        OutlinedButton(onClick = onRefresh, enabled = !isLoading) {
                            Text(stringResource(R.string.health_steps_refresh))
                        }
                }
            } else {
                Button(onClick = onOpenActivity) {
                    Text(stringResource(R.string.health_steps_open_activity))
                }
            }
        }
    }
}

private val liveHealthStatusModifier = Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
}

private fun openHealthConnectProvider(context: Context) {
    val packageName = AndroidHealthConnectStepsDataSource.HEALTH_CONNECT_PROVIDER_PACKAGE
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        "market://details?id=$packageName&url=healthconnect%3A%2F%2Fonboarding".toUri()
    ).setPackage("com.android.vending")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra("overlay", true)
        .putExtra("callerId", context.packageName)

    runCatching { context.startActivity(marketIntent) }.onFailure {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(webIntent) }
    }
}
