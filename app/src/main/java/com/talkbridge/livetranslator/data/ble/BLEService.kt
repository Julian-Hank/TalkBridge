package com.talkbridge.livetranslator.data.ble

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.talkbridge.livetranslator.ui.connect.BLEDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

class BLEService : Service() {

    companion object {
        private val SERVICE_UUID = UUID.fromString("def5dd34-cdc6-4555-bc4f-eadff20b57b5")           //  ESP32 Service UUID
        private val TEXT_CHARACTERISTIC_UUID = UUID.fromString("3a768f0d-dfe3-4325-90bf-8b0d792884cf")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private var textCharacteristic: BluetoothGattCharacteristic? = null
    private val binder = LocalBinder()

    private val _events = MutableSharedFlow<BLEEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<BLEEvent> = _events

    override fun onBind(intent: Intent): IBinder {
        Log.d("BLEService", "onBind")
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
        bluetoothGatt?.close()
        _events.tryEmit(BLEEvent.ConnectionInfo("Trying to connect..."))
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val characteristic = textCharacteristic ?: return
        characteristic.value = data
        gatt.writeCharacteristic(characteristic)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d("BLEConnect", "onConnectionStateChange: status=$status newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        val device = gatt?.device ?: return
                        _events.tryEmit(BLEEvent.ConnectionInfo("Device Connected"))
                        gatt.discoverServices()
                        Log.d("BLEConnect", "$device")
                        val emitted = _events.tryEmit(
                            BLEEvent.DeviceConnected(
                                BLEDevice(
                                    device = device,
                                    name = device.name ?: "Unnamed",
                                    address = device.address,
                                    rssi = 0
                                )
                            )
                        )
                        Log.d("BLEConnect", "DeviceConnected emitted: $emitted")
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        gatt?.close()
                        bluetoothGatt = null
                        _events.tryEmit(BLEEvent.ConnectionInfo("Device Disconnected"))
                        _events.tryEmit(BLEEvent.DeviceDisconnected)
                    }
                }
            } else {
                Log.e("BLEConnect", "Connect failed with status $status")
                gatt?.close()
                bluetoothGatt = null
                val statusDescription = when (status) {
                    8 -> "Nicht autorisiert (GATT AUTHEN)"
                    13 -> "Ungültige Attribut-Länge"
                    15 -> "Ungenügende Verschlüsselung"
                    19 -> "Verbindung vom Gerät beendet (Timeout/Entfernung)"
                    22 -> "Verbindung lokal abgebrochen"
                    34 -> "LMP Response Timeout (Basisband-Fehler)"
                    62 -> "Verbindung konnte nicht aufgebaut werden"
                    133 -> "Allgemeiner GATT-Fehler"
                    257 -> "GATT-Fehler (Dienst-Zustand ungültig)"
                    else -> "Unbekannter Fehler ($status)"
                }

                _events.tryEmit(BLEEvent.ConnectionInfo("Verbindung fehlgeschlagen mit status: $status: \n $statusDescription"))
                _events.tryEmit(BLEEvent.DeviceDisconnected)
                // _events.tryEmit(BLEEvent.ConnectionFailed)
                return
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(BLEEvent.DeviceDisconnected)
                return
            }
            textCharacteristic = gatt?.getService(SERVICE_UUID)
                ?.getCharacteristic(TEXT_CHARACTERISTIC_UUID)

            if (textCharacteristic == null) {
                // Service/Characteristic nicht gefunden -> TODO BLEEvent.Error / BLEEvent.ConnectionFailed definieren
            }
        }
    }
}