package com.talkbridge.livetranslator.ui.connect

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.ble.BLEConnectManager
import com.talkbridge.livetranslator.data.ble.BLEEvent
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ConnectViewModel(
    private val bleConnectManager: BLEConnectManager,
    private val userPreferencesRepository: UserPreferencesRepository
): ViewModel() {

    private val _connectUiState = MutableStateFlow(
        ConnectUiState(connectedBLEDevice = bleConnectManager.connectedDevice.value)
    )
    val connectUiState: StateFlow<ConnectUiState> = _connectUiState.asStateFlow()

    init {
        observePreferences()
        observeBLEEvents()
        scanBleDevice()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.sendTranslatedText
                .collect { sendTranslatedText ->
                    _connectUiState.update {
                        it.copy(
                            sendTranslatedText = sendTranslatedText
                        )
                    }
                }
        }
    }

    fun toggleSendTranslatedText(){
        val sendTranslatedText = !connectUiState.value.sendTranslatedText
        viewModelScope.launch {
            userPreferencesRepository.setSendTranslatedText(sendTranslatedText)
        }
    }

    fun scanBleDevice(){
        if (connectUiState.value.bluetoothAvailable){
            viewModelScope.launch {
                _connectUiState.update {
                    _connectUiState.value.copy(
                        availableBLEDevices = listOf(),
                        scanning = true
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

    fun clearConnectionInfo(){
        _connectUiState.update {
            _connectUiState.value.copy(
                connectionInfo = null
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
                            it.copy(
                                connectedBLEDevice = event.bleDevice
                            )
                        }
                    }
                    is BLEEvent.DeviceDisconnected -> {
                        Log.d("BLEConnect", "Disconnected")
                        _connectUiState.update {
                            it.copy(
                                connectedBLEDevice = null
                            )
                        }
                    }
                    is BLEEvent.ConnectionInfo -> {
                        Log.d("BLEConnect", event.info)
                        _connectUiState.update {
                            it.copy(
                                connectionInfo = event.info
                            )
                        }
                    }
                    is BLEEvent.ServicesDiscovered -> {
                        Log.d("BLEConnect", "Services discovered: ${event.bleDevice.services.size}")
                        _connectUiState.update {
                            it.copy(connectedBLEDevice = event.bleDevice)
                        }
                    }
                    is BLEEvent.BatteryLevelRead -> {
                        _connectUiState.update {
                            it.copy(
                                connectedBLEDevice = it.connectedBLEDevice?.copy(
                                    batteryLevel = event.level
                                )
                            )
                        }
                    }
                    is BLEEvent.ScanStopped -> {
                        _connectUiState.update {
                            it.copy(
                                scanning = false
                            )
                        }
                    }
                }
            }
        }
    }
}


data class ConnectUiState(
    val availableBLEDevices: List<BLEDevice> = listOf<BLEDevice>(),
    val connectedBLEDevice: BLEDevice? = null,
    val bluetoothAvailable: Boolean = true,
    val connectionInfo: String? = null,
    val scanning: Boolean = false,
    val sendTranslatedText: Boolean = false
)

data class BLEDevice(
    val device: BluetoothDevice,
    val services: List<BluetoothGattService> = emptyList(),
    val batteryLevel: Int? = null,
    val isTalkBridgeCompatible: Boolean = false,
    val name: String = "Unnamed",
    val address: String,
    val rssi: Int
)