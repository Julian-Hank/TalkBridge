package com.talkbridge.livetranslator.ui.connect

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.BLEConnectManager
import com.talkbridge.livetranslator.data.BLEEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ConnectViewModel(
    private val bleConnectManager: BLEConnectManager
): ViewModel() {

    private val _connectUiState = MutableStateFlow(ConnectUiState())
    val connectUiState: StateFlow<ConnectUiState> = _connectUiState.asStateFlow()

    init {
        observeBLEEvents()
        scanBleDevice()
    }

    fun scanBleDevice(){
        if (connectUiState.value.bluetoothAvailable){
            viewModelScope.launch {
                _connectUiState.update {
                    _connectUiState.value.copy(
                        availableBLEDevices = listOf()
                    )
                }
                bleConnectManager.scanBLEDevice()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectBLEDevice(device: BluetoothDevice){
        bleConnectManager.connect(device)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectBLEDevice(){
        bleConnectManager.disconnect()
    }

    private fun setBluetoothUnavailable(){
        _connectUiState.update {
            _connectUiState.value.copy(
                bluetoothAvailable = false
            )
        }
    }

    private fun observeBLEEvents() {
        viewModelScope.launch {
            bleConnectManager.events.collect { event ->
                when (event) {
                    is BLEEvent.BluetoothUnavailable -> setBluetoothUnavailable()
                    is BLEEvent.DeviceFound -> {
                        Log.d("BLEConnect", event.bleDevice.name)
                        val currentDevices = _connectUiState.value.availableBLEDevices
                        if (currentDevices.none { it.address == event.bleDevice.address }) {
                            _connectUiState.update {
                                val newList = it.availableBLEDevices + event.bleDevice
                                it.copy(availableBLEDevices = newList)
                            }
                        }
                    }
                    is BLEEvent.DeviceConnected -> {
                        Log.d("BLEConnect", "Connected")
                        _connectUiState.update {
                            _connectUiState.value.copy(
                                connectedBLEDevice = event.bleDevice
                            )
                        }
                    }
                    is BLEEvent.DeviceDisconnected -> {
                        Log.d("BLEConnect", "Disconnected")
                    }
                }
            }
        }
    }
}


data class ConnectUiState(
    val availableBLEDevices: List<BLEDevice> = listOf<BLEDevice>(),
    val connectedBLEDevice: BLEDevice? = null,
    val bluetoothAvailable: Boolean = true
)

data class BLEDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int
)