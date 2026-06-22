package com.talkbridge.livetranslator.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class BLEEvent {
    object BluetoothUnavailable : BLEEvent()
    data class DeviceConnected(val bleDevice: BLEDevice) : BLEEvent()
//    object DeviceConnected : BLEEvent()
    object DeviceDisconnected : BLEEvent()
    data class DeviceFound(val bleDevice: BLEDevice) : BLEEvent()
}

class BLEConnectManager(private val context: Context) {
    private val bluetoothLEAvailable: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var scanning = false
    private val SCAN_PERIOD: Long = 10000
    private val handler = Handler(Looper.getMainLooper())


    private val _events = MutableSharedFlow<BLEEvent>(
        extraBufferCapacity = 32,
    )
    val events: SharedFlow<BLEEvent> = _events.asSharedFlow()

    private var bleService : BLEService? = null

    private var eventJob: Job? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as BLEService.LocalBinder
            bleService = localBinder.getService()

            // Events vom Service weiterreichen
            eventJob = CoroutineScope(Dispatchers.Main).launch {
                bleService?.events?.collect {
                    _events.emit(it)
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
        val intent = Intent(context, BLEService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbind(){
        val intent = Intent(context, BLEService::class.java)
        context.unbindService(serviceConnection)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        bleService?.connect(device)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bleService?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(data: ByteArray) {
        bleService?.writeCharacteristic(data)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disconnectBLEDevice(gatt: BluetoothGatt){
        gatt.disconnect()
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
            bluetoothLeScanner?.stopScan(bleScanCallback)
        }, SCAN_PERIOD)
    }

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("BLEConnect scan result", result.toString())
            super.onScanResult(callbackType, result)
            if (result.isConnectable){
                _events.tryEmit(BLEEvent.DeviceFound(BLEDevice(result.device, result.device.name ?: "Unnamed", result.device.address, result.rssi)))
            }
        }
    }

}

