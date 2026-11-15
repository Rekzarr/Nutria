package com.nutria.app.feature.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nutria.app.R
import com.nutria.app.core.model.ConnectionFailure
import com.nutria.app.core.model.HeartRateBandState
import com.nutria.app.core.model.HeartRateFreshness
import com.nutria.app.core.model.HeartRateRules
import com.nutria.app.core.model.WorkoutSessionStatus
import com.nutria.app.app.SpanishFormats
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutFeatureScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var service by remember { mutableStateOf<WorkoutBleService?>(null) }
    val serviceConnection = remember(context) {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as? WorkoutBleService.LocalBinder)?.service()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    DisposableEffect(context, serviceConnection) {
        val bound = context.bindService(
            Intent(context, WorkoutBleService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
        onDispose {
            if (bound) runCatching { context.unbindService(serviceConnection) }
        }
    }

    val ui = service?.uiState?.collectAsState()?.value ?: WorkoutBleService.WorkoutFeatureUiState()
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(ui.latestReading, ui.session?.startedAt) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            startBluetoothFlow(context, service)
        } else {
            service?.reportPermissionsDenied()
        }
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) service?.startScan()
        else service?.reportBluetoothDisabled()
    }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionDenied = !granted
        if (granted) service?.requestStartWorkout()
    }

    fun requestWorkoutStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationPermissionDenied = false
            service?.requestStartWorkout()
        }
    }

    DisposableEffect(lifecycle, service) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                service?.reconcileOnForeground(hasBluetoothPermissions(context))
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.workout_heading),
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.workout_disclosure_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.workout_disclosure),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.workout_band_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = bandStateMessage(ui.bandState),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (ui.storageError) {
                    Text(
                        text = stringResource(R.string.workout_storage_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                when (ui.bandState) {
                    HeartRateBandState.Unsupported -> Text(
                        stringResource(R.string.workout_unsupported_help),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HeartRateBandState.BluetoothDisabled -> Text(
                        stringResource(R.string.workout_bluetooth_help),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HeartRateBandState.PermissionRequired -> {
                        Text(
                            stringResource(R.string.workout_permission_help),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                permissionLauncher.launch(requiredBluetoothPermissions())
                            }) {
                                Text(stringResource(R.string.workout_request_permission))
                            }
                            TextButton(onClick = { openAppSettings(context) }) {
                                Text(stringResource(R.string.workout_open_settings))
                            }
                        }
                    }
                    HeartRateBandState.PermissionDenied -> {
                        Text(
                            stringResource(R.string.workout_permission_denied_help),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                permissionLauncher.launch(requiredBluetoothPermissions())
                            }) {
                                Text(stringResource(R.string.workout_request_permission))
                            }
                            TextButton(onClick = { openAppSettings(context) }) {
                                Text(stringResource(R.string.workout_open_settings))
                            }
                        }
                    }
                    is HeartRateBandState.ConnectionFailed -> TextButton(
                        onClick = { connectButtonAction(context, service, permissionLauncher, enableBluetoothLauncher) },
                    ) {
                        Text(stringResource(R.string.workout_retry))
                    }
                    is HeartRateBandState.Lost -> {
                        Text(
                            stringResource(R.string.workout_gap_disclosure),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                connectButtonAction(context, service, permissionLauncher, enableBluetoothLauncher)
                            }) {
                                Text(stringResource(R.string.workout_reconnect))
                            }
                            if (ui.session != null) {
                                OutlinedButton(onClick = { service?.requestStopWorkout() }) {
                                    Text(stringResource(R.string.workout_stop))
                                }
                            }
                        }
                    }
                    HeartRateBandState.Scanning -> {
                        Text(stringResource(R.string.workout_scan_help))
                        OutlinedButton(onClick = { service?.cancelScan() }) {
                            Text(stringResource(R.string.workout_cancel_scan))
                        }
                    }
                    else -> Unit
                }

                if (ui.devices.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.workout_select_device),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    ui.devices.forEach { device ->
                        OutlinedButton(
                            onClick = { service?.connectToBand(device.token) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.workout_select_named_device, device.displayName))
                        }
                    }
                }

                if (ui.bandState == HeartRateBandState.Disconnected ||
                    ui.bandState is HeartRateBandState.Incompatible
                ) {
                    Button(
                        onClick = {
                            connectButtonAction(context, service, permissionLauncher, enableBluetoothLauncher)
                        },
                    ) {
                        Text(stringResource(R.string.workout_connect))
                    }
                }
                if (ui.bandState is HeartRateBandState.Connected) {
                    OutlinedButton(onClick = { service?.disconnectBand() }) {
                        Text(stringResource(R.string.workout_disconnect))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.workout_session_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                val session = ui.session
                if (session == null) {
                    Text(stringResource(R.string.workout_no_active_session))
                    if (ui.bandState is HeartRateBandState.Connected) {
                        Button(onClick = { requestWorkoutStart() }) {
                            Text(stringResource(R.string.workout_start))
                        }
                    } else {
                        Text(
                            stringResource(R.string.workout_connect_before_start),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (notificationPermissionDenied) {
                        Text(
                            stringResource(R.string.workout_notification_permission_denied),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { openAppSettings(context) }) {
                            Text(stringResource(R.string.workout_open_settings))
                        }
                    }
                } else {
                    Text(stringResource(R.string.workout_started_at, formatTime(session.startedAt)))
                    Text(
                        stringResource(
                            R.string.workout_elapsed,
                            formatDuration(Duration.between(session.startedAt, now).coerceAtLeast(Duration.ZERO)),
                        ),
                    )
                    Text(
                        stringResource(R.string.workout_elapsed_caveat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (session.status) {
                        WorkoutSessionStatus.ACTIVE -> {
                            when (val freshness = HeartRateRules.freshness(ui.latestReading, now)) {
                                HeartRateFreshness.NoReading -> Text(
                                    stringResource(R.string.workout_waiting_reading),
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                is HeartRateFreshness.Fresh -> {
                                    val accessibilityDescription = stringResource(
                                        R.string.workout_heart_rate_accessibility,
                                        freshness.reading.beatsPerMinute,
                                        formatTime(freshness.reading.receivedAt),
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.workout_heart_rate,
                                            freshness.reading.beatsPerMinute,
                                            formatTime(freshness.reading.receivedAt),
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.semantics {
                                            contentDescription = accessibilityDescription
                                        },
                                    )
                                }
                                is HeartRateFreshness.Stale -> Text(
                                    stringResource(
                                        R.string.workout_stale_reading,
                                        freshness.reading.beatsPerMinute,
                                        formatTime(freshness.reading.receivedAt),
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(stringResource(R.string.workout_actual_sample_count, sessionSampleCount(ui, session.id)))
                            OutlinedButton(onClick = { service?.requestStopWorkout() }) {
                                Text(stringResource(R.string.workout_stop))
                            }
                        }
                        WorkoutSessionStatus.INTERRUPTED -> {
                            Text(
                                stringResource(
                                    R.string.workout_interrupted_at,
                                    session.endedAt?.let(::formatTime) ?: stringResource(R.string.workout_time_unknown),
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(stringResource(R.string.workout_gap_disclosure))
                            Text(stringResource(R.string.workout_actual_sample_count, sessionSampleCount(ui, session.id)))
                            OutlinedButton(onClick = { service?.requestStopWorkout() }) {
                                Text(stringResource(R.string.workout_finish_interrupted))
                            }
                        }
                        WorkoutSessionStatus.COMPLETED -> {
                            Text(stringResource(R.string.workout_completed))
                            Text(stringResource(R.string.workout_actual_sample_count, sessionSampleCount(ui, session.id)))
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.workout_history_title),
            style = MaterialTheme.typography.titleLarge,
        )
        if (ui.recentSessions.isEmpty()) {
            Text(stringResource(R.string.workout_history_empty))
        } else {
            ui.recentSessions.forEach { summary ->
                val savedSession = summary.session
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.workout_history_date,
                                SpanishFormats.formatDate(savedSession.localDate),
                            ),
                        )
                        Text(stringResource(R.string.workout_started_at, formatTime(savedSession.startedAt)))
                        savedSession.endedAt?.let {
                            val label = if (savedSession.status == WorkoutSessionStatus.COMPLETED) {
                                R.string.workout_history_finished
                            } else {
                                R.string.workout_history_interrupted
                            }
                            Text(stringResource(label, formatTime(it)))
                        }
                        Text(stringResource(R.string.workout_actual_sample_count, summary.sampleCount))
                        if (summary.hadInterruption) {
                            summary.interruptionObservedAt?.let { interruptedAt ->
                                Text(
                                    stringResource(
                                        R.string.workout_history_interruption_time,
                                        formatTime(interruptedAt),
                                    ),
                                )
                            }
                            Text(
                                stringResource(
                                    if (summary.recoveryDetected) {
                                        R.string.workout_recovery_gap_disclosure
                                    } else {
                                        R.string.workout_gap_disclosure
                                    },
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(onClick = { service?.deleteWorkout(savedSession.id) }) {
                            Text(stringResource(R.string.workout_delete))
                        }
                    }
                }
            }
        }
    }

    if (ui.bandState == HeartRateBandState.BluetoothDisabled) {
        TextButton(onClick = {
            if (hasBluetoothPermissions(context)) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                permissionLauncher.launch(requiredBluetoothPermissions())
            }
        }) {
            Text(stringResource(R.string.workout_enable_bluetooth))
        }
    }
}

private fun connectButtonAction(
    context: Context,
    service: WorkoutBleService?,
    permissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    enableBluetoothLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
) {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        service?.startScan()
        return
    }
    if (!hasBluetoothPermissions(context)) {
        permissionLauncher.launch(requiredBluetoothPermissions())
        return
    }
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    if (adapter?.isEnabled != true) {
        service?.reportBluetoothDisabled()
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        return
    }
    service?.startScan()
}

private fun startBluetoothFlow(context: Context, service: WorkoutBleService?) {
    if (hasBluetoothPermissions(context)) service?.startScan()
    else service?.reportPermissionsMissing()
}

private fun requiredBluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothPermissions(context: Context): Boolean =
    requiredBluetoothPermissions().all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
private fun bandStateMessage(state: HeartRateBandState): String =
    when (state) {
        HeartRateBandState.Unsupported -> stringResource(R.string.workout_state_unsupported)
        HeartRateBandState.BluetoothDisabled -> stringResource(R.string.workout_state_bluetooth_off)
        HeartRateBandState.PermissionRequired -> stringResource(R.string.workout_state_permission)
        HeartRateBandState.PermissionDenied -> stringResource(R.string.workout_state_permission_denied)
        HeartRateBandState.Disconnected -> stringResource(R.string.workout_state_disconnected)
        HeartRateBandState.Scanning -> stringResource(R.string.workout_state_scanning)
        HeartRateBandState.Connecting -> stringResource(R.string.workout_state_connecting)
        is HeartRateBandState.Connected -> stringResource(
            R.string.workout_state_connected,
            state.displayName ?: stringResource(R.string.workout_unknown_band),
        )
        is HeartRateBandState.Incompatible -> connectionFailureMessage(state.reason)
        is HeartRateBandState.ConnectionFailed -> connectionFailureMessage(state.reason)
        is HeartRateBandState.Lost -> stringResource(
            R.string.workout_state_lost,
            connectionFailureMessage(state.reason),
        )
    }

@Composable
private fun connectionFailureMessage(failure: ConnectionFailure): String =
    when (failure) {
        ConnectionFailure.SCAN_FAILED -> stringResource(R.string.workout_error_scan)
        ConnectionFailure.CONNECTION_FAILED -> stringResource(R.string.workout_error_connect)
        ConnectionFailure.MISSING_HEART_RATE_SERVICE,
        ConnectionFailure.MISSING_MEASUREMENT_CHARACTERISTIC -> stringResource(R.string.workout_state_incompatible)
        ConnectionFailure.LINK_LOST -> stringResource(R.string.workout_error_lost)
        ConnectionFailure.BLUETOOTH_DISABLED -> stringResource(R.string.workout_state_bluetooth_off)
        ConnectionFailure.PERMISSION_REVOKED -> stringResource(R.string.workout_state_permission)
        ConnectionFailure.SERVICE_STOPPED -> stringResource(R.string.workout_error_service)
    }

private fun sessionSampleCount(
    ui: WorkoutBleService.WorkoutFeatureUiState,
    sessionId: String,
): Int = ui.recentSessions.firstOrNull { it.session.id == sessionId }?.sampleCount ?: 0

private fun formatTime(instant: Instant): String =
    TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(instant)

private fun formatDuration(duration: Duration): String {
    val seconds = duration.seconds
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        String.format(Locale.forLanguageTag("es-ES"), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format(Locale.forLanguageTag("es-ES"), "%02d:%02d", minutes, remainingSeconds)
    }
}

private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.forLanguageTag("es-ES"))
