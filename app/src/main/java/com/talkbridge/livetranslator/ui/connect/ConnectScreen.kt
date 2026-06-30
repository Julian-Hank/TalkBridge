package com.talkbridge.livetranslator.ui.connect

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.tertiary

object ConnectDestination: NavigationDestination{
    override val route: String = "connect"
    override val titleRes: Int = R.string.ble_connect
}

@RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    uiState: ConnectUiState,
    viewModel: ConnectViewModel,
//    onConnectClick: (BluetoothDevice) -> Unit,
//    onDisconnectClick: () -> Unit,
//    onRescanClick: () -> Unit,
    onBackButtonPress: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(ConnectDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onBackButtonPress,
                actionIcon = R.drawable.outline_forward_media_24,
                onActionClick = { viewModel.scanBleDevice() }
            )
        },
    ) { innerPadding ->
        ConnectBody(
            uiState = uiState,
            onConnectClick = { viewModel.connectBLEDevice(it) },
            onDisconnectClick = { viewModel.disconnectBLEDevice() },
            modifier = Modifier.padding(innerPadding)
        )
        if (!uiState.connectionInfo.isNullOrBlank()){
            ConnectionToast(uiState.connectionInfo)
            viewModel.clearConnectionInfo()
        }
    }
}

@Composable
fun ConnectBody(
    uiState: ConnectUiState,
    onConnectClick: (BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.connectedBLEDevice == null){
        Column(modifier =  modifier) {
            Text(
                text = "This screen will search Bluetooth Low Energy (BLE) devices and makes it possible to connect with them. This way the connected and/or translated speech data can be transmitted to a device of your choosing.",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 12.sp
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            if (!uiState.bluetoothAvailable){
                Text(
                    text = "bluetooth unavailable",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                if (uiState.availableBLEDevices.isNotEmpty()){
                    LazyColumn(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(items = uiState.availableBLEDevices.sortedBy { -(it.rssi) }){ device ->
                            BLEItem(device, onConnectClick)
                        }
                    }
                } else {
                    Text(
                        text = "no devices",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else {
        DeviceDashBoard(
            device = uiState.connectedBLEDevice,
            onDisconnectClick = onDisconnectClick,
            modifier =  modifier
        )
    }
}

@Composable
fun ConnectionToast(text: String){
    Toast.makeText(LocalContext.current, text, Toast.LENGTH_SHORT).show()
}

@Composable
fun DeviceDashBoard(
    device: BLEDevice,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        //name (row akku?)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = device.name
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_battery_0_bar_24),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "76%", //temp
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                )
            }
        }
        //address
        Text(
            text = device.address
        )
        //Services?
        //Spacer
        Spacer(modifier = Modifier.height(16.dp))
        //verbundene Zeit?
        Box {
            Row (verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Übersetzten Text senden",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    modifier = Modifier.weight(2.5f)
                )
            }
        }
        Button(
            onClick = onDisconnectClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text(
                "disconnect"
            )
        }
        //switch setting
        //disconnect
    }
}

@Composable
fun DeviceDashBoard(
    deviceName: String,
    deviceAddress: String,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        //name (row akku?)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = deviceName
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_battery_0_bar_24),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "76%", //temp
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                )
            }
        }
        //address
        Text(
            text = deviceAddress
        )
        //Services?
        //Spacer
        Spacer(modifier = Modifier.height(16.dp))
        //verbundene Zeit?
        Box {
            Row (verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Übersetzten Text senden",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    modifier = Modifier.weight(2.5f)
                )
            }
        }
        Button(
            onClick = onDisconnectClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text(
                "disconnect"
            )
        }
        //switch setting
        //disconnect
    }
}

@Composable
fun BLEItem(
    bleDevice: BLEDevice,
    onConnectClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = tertiary
        ),
        onClick = { onConnectClick(bleDevice.device) }
    ){
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_bluetooth_24),
                contentDescription = null,
                tint = primary,
                modifier = Modifier.weight(1f)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(8f)
            ) {
                Text(
                    text = bleDevice.name,
                    color = primary
                )
                Text(
                    text = bleDevice.address,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Icon(
                painter = painterResource(
                    when {
                        bleDevice.rssi >= -60 -> R.drawable.signal_high
                        bleDevice.rssi >= -75 -> R.drawable.signal_medium
                        else -> R.drawable.signal_low
                    }
                ),
                contentDescription = null,
                tint = primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceDashBoardPreview() {
    TalkBridgeLiveTheme() {
        DeviceDashBoard(
            deviceName = "ESP32",
            deviceAddress = "56:23:A4:66:69:12",
            onDisconnectClick = {}
        )
    }
}