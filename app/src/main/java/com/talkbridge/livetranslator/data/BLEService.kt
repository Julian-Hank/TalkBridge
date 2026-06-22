package com.talkbridge.livetranslator.data

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.RequiresPermission
import com.talkbridge.livetranslator.ui.connect.BLEDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BLEService : Service() {

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val binder = LocalBinder()

    private val _events = MutableSharedFlow<BLEEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<BLEEvent> = _events

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() : BLEService {
            return this@BLEService
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            bluetoothGatt = null
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(data: ByteArray){
        TODO()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt?.discoverServices()
                        _events.tryEmit(BLEEvent.DeviceConnected(
                            BLEDevice(
                                device = (gatt?.device as BluetoothDevice),
                                name = "", //temp
                                address = "",
                                rssi = 0
                            )
                        ))
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        gatt?.close()
                        bluetoothGatt = null
                        _events.tryEmit(BLEEvent.DeviceDisconnected)
                    }
                }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

        }
    }
}