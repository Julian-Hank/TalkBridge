package com.talkbridge.livetranslator.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeBottomNavBar
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.data.Language
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.error
import com.talkbridge.livetranslator.ui.theme.onTertiary
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.stopColor
import com.talkbridge.livetranslator.ui.theme.tertiary

object HomeDestination : NavigationDestinationWithIcon {
    override val route = "home"
    override val titleRes = R.string.app_name
    override val icon = R.drawable.talkbridge_logo_navbar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit = {},
    onTargetLanguageClick: () -> Unit = {},
    onSourceLanguageClick: () -> Unit = {},
    onStartButtonClick: () -> Unit = {},
    onStopButtonClick: () -> Unit = {},
    onLanguageSwapClick: () -> Unit = {},
    uiState: HomeUiState,
) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TalkBridgeTopAppBar(
                title = null,
                canNavigateBack = false,
                openSettings = openSettings
            )
        },
        bottomBar = {
            TalkBridgeBottomNavBar(
                onNavigationButtonClick = onNavigationButtonClick,
                currentRoute = HomeDestination.route
            )
        }
    ) { innerPadding ->
        HomeBody(
            uiState = uiState,
            onLanguageSwapClick = onLanguageSwapClick ,
            onSourceLanguageClick = onSourceLanguageClick,
            onTargetLanguageClick = onTargetLanguageClick,
            onStartButtonClick = onStartButtonClick,
            onStopButtonClick = onStopButtonClick,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun HomeBody(
    uiState: HomeUiState,
    onLanguageSwapClick: () -> Unit,
    onTargetLanguageClick: () -> Unit,
    onSourceLanguageClick: () -> Unit,
    onStartButtonClick: () -> Unit,
    onStopButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.connectionState){
        ConnectionState.CONNECTED -> {
            ActiveHomeBody(
                uiState = uiState,
                onStopButtonClick = onStopButtonClick,
                modifier = modifier
            )
        }
        ConnectionState.CONNECTING -> {
            LoadingBody(modifier = modifier)
        }
        ConnectionState.NOT_CONNECTED -> {
            InactiveHomeBody(
                uiState = uiState,
                onSwapClick = onLanguageSwapClick,
                onSourceLanguageClick = onSourceLanguageClick,
                onTargetLanguageClick = onTargetLanguageClick,
                onStartButtonClick = onStartButtonClick,
                modifier = modifier
            )
        }
        ConnectionState.FAILED -> {
            ConnectionFailureBody(onRetryButtonClick = onStartButtonClick, modifier = modifier)
        }
    }
}

@Composable
fun LoadingBody(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Warte auf Verbindung",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(2f)
        )
        CircularProgressIndicator(
            modifier = Modifier
                .weight(6f)
                .size(128.dp),
            strokeWidth = 8.dp
        )
    }
}

@Composable
fun ConnectionFailureBody(
    onRetryButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            text = "Verbindung fehlgeschlagen",
            style = MaterialTheme.typography.bodyMedium,
            color = error,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onRetryButtonClick() },
        ) {
            Text("Erneut versuchen")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Zurück",
            modifier = Modifier.clickable(
                onClick = {  }
            )
        )
    }
}

@Composable
fun ActiveHomeBody(
    uiState: HomeUiState,
    onStopButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.weight(.5f))
        TextResultContainer(
            text = uiState.currentText,
            modifier =  Modifier.weight(5f)
        )
        Spacer(modifier = Modifier.weight(.5f))
        StopButton(
            onClick = onStopButtonClick,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun InactiveHomeBody(
    uiState: HomeUiState,
    onSwapClick: () -> Unit,
    onTargetLanguageClick: () -> Unit,
    onSourceLanguageClick: () -> Unit,
    onStartButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(.25f))
        Text(
            text = stringResource(R.string.live_translate),
            textAlign = TextAlign.Center,
            color = primary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(.575f)
                .fillMaxWidth()
        )
        StartButton(
            onClick = onStartButtonClick,
            modifier = Modifier.weight(1.75f)
        )
        Box (modifier = Modifier.fillMaxWidth()) {
            LanguageSwapCard(
                sourceLanguage = uiState.sourceLanguage,
                targetLanguage = uiState.targetLanguage,
                onSwapClick =  onSwapClick,
                onSourceLanguageClick = onSourceLanguageClick,
                onTargetLanguageClick = onTargetLanguageClick,
                modifier = Modifier
                    .widthIn(max = 425.dp)
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.weight(.375f))
    }
}



@Composable
fun TextResultContainer(
    text: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = text ?: stringResource(R.string.listening),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            textAlign = TextAlign.Start,
            color = primary,
            style = MaterialTheme.typography.displayMedium
        )
    }
}


@Composable
fun LanguageSwapCard(
    sourceLanguage: LanguageData = LanguageDataSource.languagesMap.getValue(Language.GERMAN),
    targetLanguage: LanguageData = LanguageDataSource.languagesMap.getValue(Language.FRENCH),
    onSwapClick: () -> Unit = {},
    onTargetLanguageClick: () -> Unit = {},
    onSourceLanguageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = tertiary
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageItem(
                    language = sourceLanguage.languageName,
                    flagRes = sourceLanguage.flag,
                    onClick = { onSourceLanguageClick() },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                LanguageItem(
                    language = targetLanguage.languageName,
                    flagRes = targetLanguage.flag,
                    onClick = { onTargetLanguageClick() },
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(50.dp)
                    .background(
                        color = onTertiary,
                        shape = CircleShape
                    )
                    .clickable { onSwapClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    //imageVector = Icons.Filled.ArrowForward,
                    painter = painterResource(R.drawable.swap_icon),
                    contentDescription = stringResource(R.string.switch_languages),
                    tint = primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

}


@Composable
fun LanguageItem(
    @StringRes language: Int,
    @DrawableRes flagRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box {
        Column (
            modifier = modifier.clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(flagRes),
                contentDescription = stringResource(language),
                modifier = Modifier
                    .size(55.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(language),
                color = primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StartButton(
    active: Boolean = true,
    modifier: Modifier = Modifier,
    @StringRes startText: Int = R.string.start_live_translate,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onClick()
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text( text=stringResource(R.string.permission_dialog_title)) },
            text = { Text(text=stringResource(R.string.permission_dialog_description)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    // Öffne App-Einstellungen
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    /*
    // Permission wurde abgelehnt
            Toast.makeText(
                context,
                "Mikrofon-Berechtigung wird für die Live-Übersetzung benötigt",
                Toast.LENGTH_LONG
            ).show()
     */
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
        ) {

            // äußerer Kreis (Stroke)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF3A7CA5),
                    style = Stroke(width = 3.dp.toPx()),
                    radius = size.minDimension / 1.6f
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // innerer Kreis
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = tertiary,
                            shape = CircleShape
                        )
                        .clickable {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                onClick()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (active){
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    } else{
                        // Stop-Icon (Quadrat)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
                Text(
                    text = stringResource(if (active) startText else R.string.stop_recording),
                    textAlign = TextAlign.Center,
                    color = tertiary,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

    }
}

@Composable
fun StopButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    color = stopColor,
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Stop-Icon (Quadrat)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
            )

        }
        Text(
            text = stringResource(R.string.stop_live_translate),
            textAlign = TextAlign.Center,
            color = stopColor,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Composable
fun LoadingTest(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = Modifier
            .size(24.dp)
            .padding(8.dp),
        strokeWidth = 2.dp
    )
}

@Preview(showBackground = true)
@Composable
private fun ScreenInactivePreview() {
    TalkBridgeLiveTheme {
        HomeScreen(
            uiState = HomeUiState()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenActivePreview() {
    TalkBridgeLiveTheme {
        HomeScreen(
            uiState = HomeUiState(connectionState = ConnectionState.CONNECTED)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSwapCardPreview() {
    TalkBridgeLiveTheme {
        LanguageSwapCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun StartButtonPreview() {
    TalkBridgeLiveTheme {
        StartButton()
    }
}

@Preview(showBackground = true)
@Composable
private fun StopButtonPreview() {
    TalkBridgeLiveTheme {
        StopButton()
    }
}

@Preview(showBackground = true)
@Composable
private fun TextResultContainerPreview() {
    TalkBridgeLiveTheme {
        TextResultContainer()
    }
}