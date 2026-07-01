package com.talkbridge.livetranslator.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.talkbridge.livetranslator.ui.connect.BLEDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BLEEvent {
    object BluetoothUnavailable : BLEEvent()
    data class DeviceConnected(val bleDevice: BLEDevice) : BLEEvent()
    data class ServicesDiscovered(val bleDevice: BLEDevice) : BLEEvent()
    object DeviceDisconnected : BLEEvent()
    data class DeviceFound(val bleDevice: BLEDevice) : BLEEvent()

    data class BatteryLevelRead(val level: Int): BLEEvent()

    object ScanStopped: BLEEvent()

    data class ConnectionInfo(val info: String): BLEEvent()
}

class BLEConnectManager(private val context: Context) {
    private val bluetoothLEAvailable: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var scanning = false
    private val SCAN_PERIOD: Long = 10000
    private val handler = Handler(Looper.getMainLooper())

    private val _connectedDevice = MutableStateFlow<BLEDevice?>(null)
    val connectedDevice: StateFlow<BLEDevice?> = _connectedDevice.asStateFlow()

    private val _events = MutableSharedFlow<BLEEvent>(
        extraBufferCapacity = 32,
    )
    val events: SharedFlow<BLEEvent> = _events.asSharedFlow()

    private var bleService : BLEService? = null

    private var eventJob: Job? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Log.d("BLEConnect", "onServiceConnected")
            val localBinder = binder as BLEService.LocalBinder
            bleService = localBinder.getService()

            // Events vom Service weiterreichen
            eventJob = CoroutineScope(Dispatchers.Main).launch {
                bleService?.events?.collect { event ->
                    when (event) {
                        is BLEEvent.DeviceConnected -> _connectedDevice.value = event.bleDevice
                        is BLEEvent.ServicesDiscovered -> _connectedDevice.value = event.bleDevice
                        is BLEEvent.BatteryLevelRead -> _connectedDevice.value = _connectedDevice.value?.copy(batteryLevel = event.level)
                        is BLEEvent.DeviceDisconnected -> _connectedDevice.value = null
                        else -> {}
                    }
                    Log.d("BLEConnect", "Manager relaying event: $event")
                    _events.emit(event)
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            bleService = null
            eventJob?.cancel()
            eventJob = null
        }
    }

    init {
        if (bluetoothAdapter == null || !bluetoothLEAvailable) {
            _events.tryEmit(BLEEvent.BluetoothUnavailable)
        }
    }

    fun bind() {
        Log.d("BLEConnect", "bind")
        val intent = Intent(context, BLEService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbind(){
        context.unbindService(serviceConnection)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        stopScan()
        _events.tryEmit(BLEEvent.ScanStopped)
        Log.d("BLEConnect", "connect() called, bleService=$bleService")
        bleService?.connect(device)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bleService?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeTextCharacteristic(data: ByteArray) {
        bleService?.writeTextCharacteristic(data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun scanBLEDevice() {
        Log.d("BLEConnect", "scan start")

        if (scanning) {
            bluetoothLeScanner?.stopScan(bleScanCallback)
        }
        handler.removeCallbacksAndMessages(null)

        scanning = true
        bluetoothLeScanner?.startScan(bleScanCallback)

        handler.postDelayed({
            scanning = false
            _events.tryEmit(BLEEvent.ScanStopped)
            bluetoothLeScanner?.stopScan(bleScanCallback)
        }, SCAN_PERIOD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan(){
        if (scanning) {
            bluetoothLeScanner?.stopScan(bleScanCallback)
        }
        scanning = false
    }

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("BLEConnect scan result", result.toString())
            super.onScanResult(callbackType, result)
            if (result.isConnectable){
                _events.tryEmit(
                    BLEEvent.DeviceFound(
                        BLEDevice(
                            device = result.device,
                            name = result.device.name ?: "Unnamed",
                            address = result.device.address,
                            rssi = result.rssi
                        )
                    )
                )
            }
        }
    }

}

