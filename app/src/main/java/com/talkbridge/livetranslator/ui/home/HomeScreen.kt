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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.theme.stopColor
import com.talkbridge.livetranslator.ui.theme.tertiary

object HomeDestination : NavigationDestinationWithIcon {
    override val route = "home"
    override val titleRes = R.string.live_translate
    override val icon = R.drawable.live_translate_icon
//    override val icon = R.drawable.talkbridge_logo_navbar
}

private const val TAG: String = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit = {},
    onTargetLanguageClick: () -> Unit = {},
    onSourceLanguageClick: () -> Unit = {},
    onStartButtonClick: () -> Unit = {},
    onPauseButtonClick: () -> Unit = {},
    onLanguageSwapClick: () -> Unit = {},
    onBackButtonClick: () -> Unit = {},
    uiState: HomeUiState,
) {

    val canNavigateBack = uiState.connectionState !=
            ConnectionState.NOT_CONNECTED
            && uiState.connectionState != ConnectionState.CONNECTING
            && uiState.connectionState != ConnectionState.CONNECTED

    Scaffold(
        modifier = modifier,
        topBar = {
            TalkBridgeTopAppBar(
                title = if (!canNavigateBack) null else stringResource(R.string.live_translate),
                canNavigateBack = canNavigateBack,
                navigateUp = onBackButtonClick,
                openSettings = openSettings
            )
        },
        bottomBar = {
            if (!canNavigateBack)  {
                TalkBridgeBottomNavBar(
                    onNavigationButtonClick = onNavigationButtonClick,
                    currentRoute = HomeDestination.route
                )
            }
        }
    ) { innerPadding ->
        HomeBody(
            uiState = uiState,
            onLanguageSwapClick = onLanguageSwapClick ,
            onSourceLanguageClick = onSourceLanguageClick,
            onTargetLanguageClick = onTargetLanguageClick,
            onPauseButtonClick = onStartButtonClick,
            onStopButtonClick = onPauseButtonClick,
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
    onPauseButtonClick: () -> Unit,
    onStopButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.connectionState){
        ConnectionState.NOT_CONNECTED -> {
            InactiveHomeBody(
                uiState = uiState,
                onSwapClick = onLanguageSwapClick,
                onSourceLanguageClick = onSourceLanguageClick,
                onTargetLanguageClick = onTargetLanguageClick,
                onStartButtonClick = onPauseButtonClick,
                modifier = modifier
            )
        }
        ConnectionState.CONNECTING -> {
            LoadingBody(
                connected = false,
                modifier = modifier
            )
        }
        ConnectionState.CONNECTED -> {
            LoadingBody(
                connected = true,
                modifier = modifier
            )
        }
        ConnectionState.READY -> {
            ActiveHomeBody(
                uiState = uiState,
                onPauseButtonClick = onStopButtonClick,
                modifier = modifier,
                isPaused = false
            )
        }
        ConnectionState.PAUSED -> {
            ActiveHomeBody(
                uiState = uiState,
                onPauseButtonClick = onStopButtonClick,
                modifier = modifier,
                isPaused = true
            )
        }
        ConnectionState.FAILED -> {
            ConnectionFailureBody(
                onRetryButtonClick = onPauseButtonClick,
                modifier = modifier
            )
        }
    }
}

@Composable
fun LoadingBody(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(if (!connected) R.string.waiting_for_connection else R.string.connection_established),
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
            text = stringResource(R.string.connection_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = error,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onRetryButtonClick() },
        ) {
            Text(stringResource(R.string.try_again))
        }
    }
}

@Composable
fun ActiveHomeBody(
    uiState: HomeUiState,
    onPauseButtonClick: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    DisposableEffect(true) {
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = false
        }
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.weight(.5f))
        TextResultsWrapper(
            uiState = uiState,
            modifier =  Modifier.weight(6f)
        )
        Spacer(modifier = Modifier.weight(.5f))
        PauseButton(
            onClick = onPauseButtonClick,
            isPaused = isPaused,
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
fun TextResultsWrapper(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier
        .padding(bottom = 8.dp)
    ) {
        if (uiState.textResults != null){
            items(items = uiState.textResults.toList().reversed()){ translationEntry ->
                TextResultContainer(
                    originalText = translationEntry.original,
                    translatedText = translationEntry.translated
                )
            }
        } else {
            item{
                TextResultContainer(
                    originalText = stringResource(R.string.listening)
                )
            }
        }
    }
}

@Composable
fun TextResultContainer(
    originalText: String,
    translatedText: String? = null,
    isCurrent: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color.White else Color(0xFFe0dfdf) //fix hardcoded
        )
    ) {
        Column {
            Text(
                text = originalText,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                textAlign = TextAlign.Start,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = translatedText ?: "...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Start,
                color = primary,
                style = MaterialTheme.typography.displayMedium
            )
        }
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
                    modifier = Modifier.weight(1f),
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
                textAlign = TextAlign.Center,
                fontSize = 14.sp
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
                    color = secondary,
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
fun PauseButton(
    onClick: () -> Unit = {},
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = if (isPaused) secondary else stopColor,
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Icon
            if (isPaused){
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    tint = Color.White,
                    contentDescription = stringResource(R.string.resume)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.baseline_pause_24),
                    tint = Color.White,
                    contentDescription = stringResource(R.string.pause),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
//        Text(
//            text = stringResource(R.string.stop_live_translate),
//            textAlign = TextAlign.Center,
//            color = stopColor,
//            style = MaterialTheme.typography.displayMedium,
//            modifier = Modifier.padding(top = 8.dp)
//        )
    }
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
private fun ScreenConnectingPreview() {
    TalkBridgeLiveTheme {
        HomeScreen(
            uiState = HomeUiState(connectionState = ConnectionState.CONNECTING)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenConnectedPreview() {
    TalkBridgeLiveTheme {
        HomeScreen(
            uiState = HomeUiState(connectionState = ConnectionState.CONNECTED)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenActivePreview() {
    TalkBridgeLiveTheme {
        HomeScreen(
            uiState = HomeUiState(connectionState = ConnectionState.READY)
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





