package com.nutria.app.feature.activity

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.nutria.app.R
import com.nutria.app.app.MainActivity
import com.nutria.app.core.data.WorkoutSessionSummary
import com.nutria.app.core.model.ConnectionFailure
import com.nutria.app.core.model.HeartRateBandState
import com.nutria.app.core.model.HeartRateReading
import com.nutria.app.core.model.HeartRateRules
import com.nutria.app.core.model.WorkoutEndStatus
import com.nutria.app.core.model.WorkoutEvent
import com.nutria.app.core.model.WorkoutEventKind
import com.nutria.app.core.model.WorkoutSession
import com.nutria.app.core.model.WorkoutSessionStatus
import com.nutria.app.app.NutriaApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * GATT owner for an explicitly selected standard Heart Rate band. It is promoted
 * to a connected-device foreground service only after the user starts a workout.
 */
class WorkoutBleService : Service() {
    data class DiscoveredBand(
        val token: String,
        val displayName: String,
    )

    data class WorkoutFeatureUiState(
        val bandState: HeartRateBandState = HeartRateBandState.Disconnected,
        val devices: List<DiscoveredBand> = emptyList(),
        val session: WorkoutSession? = null,
        val latestReading: HeartRateReading? = null,
        val recentSessions: List<WorkoutSessionSummary> = emptyList(),
        val storageError: Boolean = false,
    )

    inner class LocalBinder : Binder() {
        fun service(): WorkoutBleService = this@WorkoutBleService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableUiState = MutableStateFlow(WorkoutFeatureUiState())
    val uiState: StateFlow<WorkoutFeatureUiState> = mutableUiState.asStateFlow()
    private val startupReady = CompletableDeferred<Boolean>()

    private val deviceByToken = linkedMapOf<String, BluetoothDevice>()
    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var currentMeasurement: BluetoothGattCharacteristic? = null
    private var currentDeviceName: String? = null
    private var activeSessionId: String? = null
    private var explicitlyClosingGatt = false
    private var notificationStarted = false
    private lateinit var workoutRepository: WorkoutRoomRepository

    private val scanTimeout = Runnable {
        if (mutableUiState.value.bandState == HeartRateBandState.Scanning) {
            stopScan()
            mutableUiState.value = mutableUiState.value.copy(bandState = HeartRateBandState.Disconnected)
        }
    }
    private val connectionTimeout = Runnable {
        if (mutableUiState.value.bandState == HeartRateBandState.Connecting) {
            failConnection(ConnectionFailure.CONNECTION_FAILED)
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (newState == BluetoothAdapter.STATE_TURNING_OFF || newState == BluetoothAdapter.STATE_OFF) {
                serviceScope.launch {
                    stopScan()
                    interruptOpenSession(WorkoutEventKind.BLUETOOTH_DISABLED)
                    explicitlyClosingGatt = true
                    closeGatt()
                    mutableUiState.value = mutableUiState.value.copy(
                        bandState = HeartRateBandState.BluetoothDisabled,
                        devices = emptyList(),
                    )
                    refreshNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        workoutRepository = (application as NutriaApplication).workoutRepository()
        registerBluetoothReceiver()
        serviceScope.launch {
            try {
                workoutRepository.recoverOpenSessions(Instant.now())
                startupReady.complete(true)
                workoutRepository.observeRecentSessions().collect { sessions ->
                    mutableUiState.value = mutableUiState.value.copy(recentSessions = sessions)
                }
            } catch (failure: CancellationException) {
                startupReady.cancel(failure)
                throw failure
            } catch (_: Exception) {
                startupReady.complete(false)
                mutableUiState.value = mutableUiState.value.copy(storageError = true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORKOUT -> {
                if (!hasBluetoothPermissions()) {
                    reportPermissionsMissing()
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                if (bluetoothAdapter()?.isEnabled != true) {
                    reportBluetoothDisabled()
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                try {
                    promoteToForeground()
                } catch (_: SecurityException) {
                    reportPermissionsMissing()
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                serviceScope.launch { startWorkoutAfterForegroundPromotion() }
            }
            ACTION_STOP_WORKOUT -> {
                serviceScope.launch {
                    val endStatus = if (mutableUiState.value.session?.status == WorkoutSessionStatus.INTERRUPTED) {
                        WorkoutEndStatus.INTERRUPTED
                    } else {
                        WorkoutEndStatus.COMPLETED
                    }
                    finishCurrentWorkout(endStatus)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationStarted = false
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // An explicitly started foreground workout remains available from its notification.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(scanTimeout)
        mainHandler.removeCallbacks(connectionTimeout)
        stopScan()
        explicitlyClosingGatt = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                unregisterReceiver(bluetoothStateReceiver)
            } else {
                @Suppress("DEPRECATION")
                unregisterReceiver(bluetoothStateReceiver)
            }
        } catch (_: IllegalArgumentException) {
            // Receiver was not registered due to a platform initialization failure.
        }
        closeGatt()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun reportPermissionsMissing() {
        serviceScope.launch { handlePermissionLoss(HeartRateBandState.PermissionRequired) }
    }

    fun reportPermissionsDenied() {
        serviceScope.launch { handlePermissionLoss(HeartRateBandState.PermissionDenied) }
    }

    fun reportBluetoothDisabled() {
        serviceScope.launch {
            stopScan()
            interruptOpenSession(WorkoutEventKind.BLUETOOTH_DISABLED)
            explicitlyClosingGatt = true
            closeGatt()
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.BluetoothDisabled,
                devices = emptyList(),
            )
            refreshNotification()
        }
    }

    fun reconcileOnForeground(hasPermissions: Boolean) {
        if (!hasPermissions) {
            val hadConnectionOrWorkout =
                mutableUiState.value.bandState is HeartRateBandState.Connected ||
                    mutableUiState.value.bandState is HeartRateBandState.Lost ||
                    mutableUiState.value.session != null
            if (hadConnectionOrWorkout) {
                reportPermissionsDenied()
            } else if (mutableUiState.value.bandState != HeartRateBandState.PermissionDenied) {
                reportPermissionsMissing()
            }
        } else if (bluetoothAdapter()?.isEnabled != true) {
            reportBluetoothDisabled()
        }
    }

    fun startScan() {
        serviceScope.launch {
            if (!hasBluetoothPermissions()) {
                reportPermissionsMissing()
                return@launch
            }
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                mutableUiState.value = mutableUiState.value.copy(
                    bandState = HeartRateBandState.Unsupported,
                    devices = emptyList(),
                )
                return@launch
            }
            val adapter = bluetoothAdapter()
            if (adapter == null) {
                mutableUiState.value = mutableUiState.value.copy(
                    bandState = HeartRateBandState.Unsupported,
                    devices = emptyList(),
                )
                return@launch
            }
            if (!adapter.isEnabled) {
                reportBluetoothDisabled()
                return@launch
            }
            stopScan()
            deviceByToken.clear()
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.Scanning,
                devices = emptyList(),
            )
            try {
                val scanner = adapter.bluetoothLeScanner
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        addScanResult(result)
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>) {
                        results.forEach(::addScanResult)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        mainHandler.removeCallbacks(scanTimeout)
                        mutableUiState.value = mutableUiState.value.copy(
                            bandState = HeartRateBandState.ConnectionFailed(ConnectionFailure.SCAN_FAILED),
                        )
                    }
                }
                scanCallback = callback
                scanner.startScan(
                    null,
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    callback,
                )
                mainHandler.postDelayed(scanTimeout, SCAN_TIMEOUT_MILLIS)
            } catch (_: SecurityException) {
                reportPermissionsMissing()
            } catch (_: IllegalStateException) {
                mutableUiState.value = mutableUiState.value.copy(
                    bandState = HeartRateBandState.ConnectionFailed(ConnectionFailure.SCAN_FAILED),
                )
            }
        }
    }

    fun cancelScan() {
        stopScan()
        if (mutableUiState.value.bandState == HeartRateBandState.Scanning) {
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.Disconnected,
                devices = emptyList(),
            )
        }
    }

    fun connectToBand(token: String) {
        serviceScope.launch {
            val device = deviceByToken[token]
            if (device == null || !hasBluetoothPermissions()) {
                reportPermissionsMissing()
                return@launch
            }
            val adapter = bluetoothAdapter()
            if (adapter?.isEnabled != true) {
                reportBluetoothDisabled()
                return@launch
            }
            stopScan()
            val canReadDeviceName = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            currentDeviceName = if (canReadDeviceName) {
                runCatching { device.name }.getOrNull()?.takeIf(String::isNotBlank)
            } else {
                null
            }
                ?: getString(R.string.workout_unknown_band)
            explicitlyClosingGatt = false
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.Connecting,
                devices = emptyList(),
            )
            try {
                closeGatt()
                mainHandler.removeCallbacks(connectionTimeout)
                mainHandler.postDelayed(connectionTimeout, CONNECTION_TIMEOUT_MILLIS)
                gatt = device.connectGatt(this@WorkoutBleService, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                if (gatt == null) failConnection(ConnectionFailure.CONNECTION_FAILED)
            } catch (_: SecurityException) {
                reportPermissionsMissing()
            } catch (_: IllegalArgumentException) {
                failConnection(ConnectionFailure.CONNECTION_FAILED)
            }
        }
    }

    fun disconnectBand() {
        serviceScope.launch {
            activeSessionId?.let { sessionId ->
                val session = mutableUiState.value.session
                if (session?.status == WorkoutSessionStatus.ACTIVE) {
                    interruptSession(sessionId, WorkoutEventKind.USER_DISCONNECTED)
                } else if (session?.status == WorkoutSessionStatus.INTERRUPTED) {
                    workoutRepository.finishWorkout(
                        sessionId,
                        Instant.now(),
                        WorkoutEndStatus.INTERRUPTED,
                    )
                }
                activeSessionId = null
                mutableUiState.value = mutableUiState.value.copy(session = null, latestReading = null)
            }
            explicitlyClosingGatt = true
            closeGatt()
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.Disconnected,
                devices = emptyList(),
            )
            if (notificationStarted && mutableUiState.value.session == null) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationStarted = false
                stopSelf()
            }
        }
    }

    fun requestStartWorkout() {
        if (mutableUiState.value.bandState !is HeartRateBandState.Connected) return
        val intent = Intent(this, WorkoutBleService::class.java).setAction(ACTION_START_WORKOUT)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (_: SecurityException) {
            reportPermissionsMissing()
        } catch (_: IllegalStateException) {
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.ConnectionFailed(ConnectionFailure.SERVICE_STOPPED),
            )
        }
    }

    fun requestStopWorkout() {
        val status = if (mutableUiState.value.session?.status == WorkoutSessionStatus.INTERRUPTED) {
            WorkoutEndStatus.INTERRUPTED
        } else {
            WorkoutEndStatus.COMPLETED
        }
        serviceScope.launch { finishCurrentWorkout(status) }
    }

    fun deleteWorkout(sessionId: String) {
        serviceScope.launch {
            runCatching { workoutRepository.deleteWorkout(sessionId) }
                .onFailure { mutableUiState.value = mutableUiState.value.copy(storageError = true) }
        }
    }

    fun observeSessionSummaries(localDate: LocalDate) =
        workoutRepository.observeSessionSummaries(localDate)

    private suspend fun startWorkoutAfterForegroundPromotion() {
        if (!startupReady.await()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationStarted = false
            mutableUiState.value = mutableUiState.value.copy(storageError = true)
            return
        }
        if (!hasBluetoothPermissions()) {
            interruptOpenSession(WorkoutEventKind.PERMISSION_LOST)
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationStarted = false
            reportPermissionsMissing()
            return
        }
        if (bluetoothAdapter()?.isEnabled != true) {
            interruptOpenSession(WorkoutEventKind.BLUETOOTH_DISABLED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationStarted = false
            reportBluetoothDisabled()
            return
        }
        if (mutableUiState.value.bandState !is HeartRateBandState.Connected) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationStarted = false
            mutableUiState.value = mutableUiState.value.copy(
                bandState = HeartRateBandState.Lost(ConnectionFailure.SERVICE_STOPPED),
            )
            return
        }
        if (mutableUiState.value.session?.status == WorkoutSessionStatus.ACTIVE) return
        try {
            val now = Instant.now()
            val session = workoutRepository.startWorkout(
                localDate = LocalDate.now(),
                zoneId = ZoneId.systemDefault(),
                startedAt = now,
            )
            activeSessionId = session.id
            mutableUiState.value = mutableUiState.value.copy(
                session = session,
                latestReading = null,
                storageError = false,
            )
            refreshNotification()
        } catch (_: Exception) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationStarted = false
            mutableUiState.value = mutableUiState.value.copy(storageError = true)
        }
    }

    private suspend fun finishCurrentWorkout(status: WorkoutEndStatus) {
        val sessionId = activeSessionId ?: mutableUiState.value.session?.id
        if (sessionId != null) {
            try {
                val now = Instant.now()
                workoutRepository.finishWorkout(sessionId, now, status)
                val oldSession = mutableUiState.value.session
                if (oldSession != null) {
                    mutableUiState.value = mutableUiState.value.copy(
                        session = oldSession.copy(
                            status = if (status == WorkoutEndStatus.COMPLETED) {
                                WorkoutSessionStatus.COMPLETED
                            } else {
                                WorkoutSessionStatus.INTERRUPTED
                            },
                            endedAt = now,
                        ),
                        latestReading = null,
                    )
                }
                activeSessionId = null
                if (notificationStarted) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationStarted = false
                    stopSelf()
                }
            } catch (_: Exception) {
                mutableUiState.value = mutableUiState.value.copy(storageError = true)
            }
        }
    }

    private suspend fun interruptOpenSession(kind: WorkoutEventKind) {
        val sessionId = activeSessionId ?: mutableUiState.value.session
            ?.takeIf { it.status == WorkoutSessionStatus.ACTIVE }?.id ?: return
        interruptSession(sessionId, kind)
    }

    private suspend fun interruptSession(sessionId: String, kind: WorkoutEventKind) {
        val now = Instant.now()
        try {
            workoutRepository.recordInterruption(
                sessionId,
                WorkoutEvent(sessionId, kind, now),
            )
            mutableUiState.value = mutableUiState.value.copy(
                session = mutableUiState.value.session?.takeIf { it.id == sessionId }?.copy(
                    status = WorkoutSessionStatus.INTERRUPTED,
                    endedAt = now,
                ),
                latestReading = null,
            )
            refreshNotification()
        } catch (_: Exception) {
            mutableUiState.value = mutableUiState.value.copy(storageError = true)
        }
    }

    private fun addScanResult(result: ScanResult) {
        if (mutableUiState.value.bandState != HeartRateBandState.Scanning) return
        val device = result.device
        try {
            val token = UUID.randomUUID().toString()
            val name = device.name?.takeIf(String::isNotBlank) ?: getString(R.string.workout_unknown_band)
            val existing = deviceByToken.entries.firstOrNull { it.value.address == device.address }?.key
            if (existing != null) return
            deviceByToken[token] = device
            mutableUiState.value = mutableUiState.value.copy(
                devices = mutableUiState.value.devices + DiscoveredBand(token, name),
            )
        } catch (_: SecurityException) {
            reportPermissionsMissing()
        }
    }

    private fun stopScan() {
        mainHandler.removeCallbacks(scanTimeout)
        val callback = scanCallback ?: return
        try {
            bluetoothAdapter()?.bluetoothLeScanner?.stopScan(callback)
        } catch (_: SecurityException) {
            // A revocation will be represented by the permission state on foreground return.
        } catch (_: IllegalStateException) {
            // The adapter may have been disabled while the scan was active.
        }
        scanCallback = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (this@WorkoutBleService.gatt !== gatt) return
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    val discovered = gatt.discoverServices()
                    if (!discovered) failConnection(ConnectionFailure.CONNECTION_FAILED)
                } catch (_: SecurityException) {
                    reportPermissionsMissing()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                val expected = explicitlyClosingGatt
                closeGatt(gatt)
                if (expected) {
                    mutableUiState.value = mutableUiState.value.copy(
                        bandState = HeartRateBandState.Disconnected,
                        latestReading = null,
                    )
                } else {
                    serviceScope.launch {
                        val reason = if (status == BluetoothGatt.GATT_SUCCESS) {
                            ConnectionFailure.LINK_LOST
                        } else {
                            ConnectionFailure.CONNECTION_FAILED
                        }
                        interruptOpenSession(WorkoutEventKind.CONNECTION_LOST)
                        mutableUiState.value = mutableUiState.value.copy(
                            bandState = HeartRateBandState.Lost(reason),
                            latestReading = null,
                        )
                        refreshNotification()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (this@WorkoutBleService.gatt !== gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(ConnectionFailure.CONNECTION_FAILED)
                return
            }
            val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
            if (heartRateService == null) {
                failIncompatible(ConnectionFailure.MISSING_HEART_RATE_SERVICE)
                return
            }
            val measurement = heartRateService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            if (measurement == null ||
                measurement.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
            ) {
                failIncompatible(ConnectionFailure.MISSING_MEASUREMENT_CHARACTERISTIC)
                return
            }
            val descriptor = measurement.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor == null) {
                failIncompatible(ConnectionFailure.MISSING_MEASUREMENT_CHARACTERISTIC)
                return
            }
            try {
                if (!gatt.setCharacteristicNotification(measurement, true)) {
                    failConnection(ConnectionFailure.CONNECTION_FAILED)
                    return
                }
                currentMeasurement = measurement
                val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                        BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                if (!writeStarted) failConnection(ConnectionFailure.CONNECTION_FAILED)
            } catch (_: SecurityException) {
                reportPermissionsMissing()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (this@WorkoutBleService.gatt !== gatt) return
            if (descriptor.uuid != CLIENT_CHARACTERISTIC_CONFIG_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(ConnectionFailure.CONNECTION_FAILED)
                return
            }
            mainHandler.removeCallbacks(connectionTimeout)
            serviceScope.launch {
                val session = mutableUiState.value.session
                val sessionId = activeSessionId
                if (session != null && sessionId == session.id &&
                    session.status == WorkoutSessionStatus.INTERRUPTED
                ) {
                    try {
                        val now = Instant.now()
                        workoutRepository.recordExplicitReconnection(
                            sessionId,
                            WorkoutEvent(sessionId, WorkoutEventKind.RECONNECTED, now),
                        )
                        mutableUiState.value = mutableUiState.value.copy(
                            session = session.copy(
                                status = WorkoutSessionStatus.ACTIVE,
                                endedAt = null,
                            ),
                        )
                    } catch (_: Exception) {
                        mutableUiState.value = mutableUiState.value.copy(storageError = true)
                    }
                }
                mutableUiState.value = mutableUiState.value.copy(
                    bandState = HeartRateBandState.Connected(currentDeviceName),
                    latestReading = null,
                )
                refreshNotification()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) receiveMeasurement(gatt, value)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                receiveMeasurement(gatt, characteristic.value ?: return)
            }
        }
    }

    private fun receiveMeasurement(sourceGatt: BluetoothGatt, bytes: ByteArray) {
        if (gatt !== sourceGatt || mutableUiState.value.bandState !is HeartRateBandState.Connected) return
        val sessionId = activeSessionId ?: return
        if (mutableUiState.value.session?.status != WorkoutSessionStatus.ACTIVE) return
        val receivedAt = Instant.now()
        val reading = HeartRateRules.parseMeasurement(bytes, receivedAt) ?: return
        mutableUiState.value = mutableUiState.value.copy(latestReading = reading)
        serviceScope.launch {
            try {
                workoutRepository.appendReceivedReading(sessionId, reading)
                mutableUiState.value = mutableUiState.value.copy(storageError = false)
            } catch (_: Exception) {
                mutableUiState.value = mutableUiState.value.copy(storageError = true)
            }
        }
    }

    private fun failIncompatible(reason: ConnectionFailure) {
        mainHandler.removeCallbacks(connectionTimeout)
        explicitlyClosingGatt = true
        closeGatt()
        mutableUiState.value = mutableUiState.value.copy(
            bandState = HeartRateBandState.Incompatible(reason),
            devices = emptyList(),
            latestReading = null,
        )
    }

    private fun failConnection(reason: ConnectionFailure) {
        mainHandler.removeCallbacks(connectionTimeout)
        explicitlyClosingGatt = true
        closeGatt()
        mutableUiState.value = mutableUiState.value.copy(
            bandState = HeartRateBandState.ConnectionFailed(reason),
            latestReading = null,
        )
    }

    private fun closeGatt(target: BluetoothGatt? = gatt) {
        mainHandler.removeCallbacks(connectionTimeout)
        if (target == null) return
        try {
            target.disconnect()
            target.close()
        } catch (_: SecurityException) {
            target.close()
        } finally {
            if (gatt === target) {
                gatt = null
                currentMeasurement = null
            }
        }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? =
        getSystemService(BluetoothManager::class.java)?.adapter

    private suspend fun handlePermissionLoss(state: HeartRateBandState) {
        stopScan()
        interruptOpenSession(WorkoutEventKind.PERMISSION_LOST)
        explicitlyClosingGatt = true
        closeGatt()
        mutableUiState.value = mutableUiState.value.copy(
            bandState = state,
            devices = emptyList(),
            latestReading = null,
        )
        refreshNotification()
    }

    private fun hasBluetoothPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(bluetoothStateReceiver, filter)
            }
        } catch (_: SecurityException) {
            // Foreground reconciliation performs the same permission/adapter checks.
        }
    }

    private fun promoteToForeground() {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        notificationStarted = true
    }

    private fun refreshNotification() {
        if (!notificationStarted) return
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, WorkoutBleService::class.java).setAction(ACTION_STOP_WORKOUT)
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_PENDING_INTENT_REQUEST,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val mainIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            CONTENT_PENDING_INTENT_REQUEST,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val session = mutableUiState.value.session
        val text = if (session?.status == WorkoutSessionStatus.INTERRUPTED) {
            getString(R.string.workout_notification_interrupted)
        } else {
            getString(R.string.workout_notification_active)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.workout_notification_title))
            .setContentText(text)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.workout_stop),
                stopPendingIntent,
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.workout_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun closeGattAfterFailure(reason: ConnectionFailure) {
        failConnection(reason)
    }

    companion object {
        private const val ACTION_START_WORKOUT = "com.nutria.app.action.START_WORKOUT"
        private const val ACTION_STOP_WORKOUT = "com.nutria.app.action.STOP_WORKOUT"
        private const val NOTIFICATION_CHANNEL_ID = "nutria_workout"
        private const val NOTIFICATION_ID = 2301
        private const val STOP_PENDING_INTENT_REQUEST = 2302
        private const val CONTENT_PENDING_INTENT_REQUEST = 2303
        private const val SCAN_TIMEOUT_MILLIS = 20_000L
        private const val CONNECTION_TIMEOUT_MILLIS = 30_000L

        val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
