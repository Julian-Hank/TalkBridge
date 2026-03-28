package com.talkbridge.livetranslator.ui.transcribe

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeBottomNavBar
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.onSecondary
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.theme.tertiary
import kotlin.math.round


object TranscribeDestination : NavigationDestinationWithIcon {
    override val route = "transcribe"
    override val titleRes = R.string.transcribe
    override val icon = R.drawable.speech_to_text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(
    uiState: TranscribeUiState,
    waveAmplitudes: List<Float>,
    modifier: Modifier = Modifier,
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onFinishClick: () -> Unit = {},
    onViewTranscriptionClick: (Long) -> Unit = {},
    onAutoDetectSwitchClick: (Boolean) -> Unit,
    onLanguageItemClick: () -> Unit = {},
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit,
    openTranscriptionsOverview: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TalkBridgeTopAppBar(
                title = null,
                canNavigateBack = false,
                openSettings = openSettings,
                actionIcon = R.drawable.outline_history_24,
                onActionClick = openTranscriptionsOverview
            )
        },
        bottomBar = {
            TalkBridgeBottomNavBar(
                onNavigationButtonClick = onNavigationButtonClick,
                currentRoute = TranscribeDestination.route
            )
        }
    ) { innerPadding ->
        TranscribeBody(
            uiState = uiState,
            waveAmplitudes = waveAmplitudes,
            onStartClick = onStartClick,
            onStopClick = onStopClick,
            onPauseClick = onPauseClick,
            onResumeClick = onResumeClick,
            onDeleteClick = onDeleteClick,
            onFinishClick = onFinishClick,
            onViewTranscriptionClick = onViewTranscriptionClick,
            onAutoDetectSwitchClick = onAutoDetectSwitchClick,
            onLanguageItemClick = onLanguageItemClick,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
fun TranscribeBody(
    uiState: TranscribeUiState,
    waveAmplitudes: List<Float>,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFinishClick: () -> Unit,
    onViewTranscriptionClick: (Long) -> Unit,
    onAutoDetectSwitchClick: (Boolean) -> Unit,
    onLanguageItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(.25f))
        Text(
            text = stringResource(R.string.transcription),
            textAlign = TextAlign.Center,
            color = primary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(.575f)
                .fillMaxWidth()
        )
        if (uiState.transcriptionState == TranscriptionState.INACTIVE){
            InactiveTranscribeBody(
                autoDetectLanguage = uiState.autoDetectLanguage,
                selectedLanguage = uiState.selectedLanguage,
                onStartClick = onStartClick,
                onLanguageItemClick = onLanguageItemClick,
                onAutoDetectSwitchClick = onAutoDetectSwitchClick,
                modifier = Modifier.weight(3f)
            )
        } else if (uiState.transcriptionState == TranscriptionState.CONNECTING || uiState.transcriptionState == TranscriptionState.TRANSCRIBING || uiState.transcriptionState == TranscriptionState.FINISHED){
            FinishedTranscribeBody(
                state = uiState.transcriptionState,
                timeLeft = uiState.timeLeft,
                progress = uiState.transcriptionProgress,
                modifier = Modifier.weight(3f),
                onViewTranscriptionClick = onViewTranscriptionClick,
                createdItemId = uiState.createdItemId,
            )
        } else { // Recording / Paused / stopped
            ActiveTranscribeBody(
                state = uiState.transcriptionState,
                elapsedTime = uiState.timeRecorded,
                onStopClick = onStopClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onDeleteClick = onDeleteClick,
                onFinishClick = onFinishClick,
                amplitudes = waveAmplitudes,
                modifier = Modifier.weight(3f)
            )
        }
    }
}

@Composable
fun FinishedTranscribeBody(
    progress: Float,
    timeLeft: Int,
    state: TranscriptionState,
    modifier: Modifier = Modifier,
    onViewTranscriptionClick: (Long) -> Unit,
    createdItemId: Long,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state == TranscriptionState.CONNECTING){
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(80.dp),
                    strokeWidth = 8.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(secondary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        tint = onSecondary,
                        modifier = Modifier
                            .size(44.dp)
                            .align(alignment = Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(if (state == TranscriptionState.CONNECTING) R.string.connecting else if (state == TranscriptionState.TRANSCRIBING) R.string.recording_processed else R.string.transcription_ready)
            )
        }
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                trackColor = Color.LightGray,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (state == TranscriptionState.TRANSCRIBING){
                val roundedTimeLeft = (round(timeLeft / 10f) * 10).toInt()
                Text(
                    text = stringResource(R.string.time_left, if (roundedTimeLeft > 0) roundedTimeLeft else 10),
                    color = Color.LightGray
                )
            }
            if (state == TranscriptionState.FINISHED){
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    onViewTranscriptionClick(createdItemId)
                }) {
                    Text(
                        text = stringResource(R.string.view_transcription)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTranscribeBody(
    state: TranscriptionState,
    elapsedTime: Int,
    amplitudes: List<Float>,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    DisposableEffect(true) {
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = false
        }
    }

    val formattedTime = remember(elapsedTime) {
        val h = elapsedTime / 3600
        val m = (elapsedTime % 3600) / 60
        val s = elapsedTime % 60
        "%02d:%02d:%02d".format(h, m, s)
    }
    Column (
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Normal,
                color = primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (state == TranscriptionState.RECORDING && elapsedTime % 2 == 0){
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .offset(y = 8.dp)
                        .background(Color.Red, shape = CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Audio-Wellenform
        AudioWaveform(
            amplitudes = amplitudes,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )

        Spacer(modifier = Modifier.height(72.dp))

        // Stop- und Pause-Buttons
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            // Stop-Button (groß, dunkel)
            IconButton(
                onClick = if (state == TranscriptionState.STOPPED) onFinishClick else onStopClick,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF2D5F7A), shape = CircleShape)
            ) {
                if (state == TranscriptionState.STOPPED){
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(R.string.stop_recording),
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
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

            if (state == TranscriptionState.RECORDING){
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(68.dp)
                        .background(tertiary, shape = CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_pause_24),
                        contentDescription = stringResource(R.string.pause),
                        tint = primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if(state == TranscriptionState.PAUSED){
                IconButton(
                    onClick = onResumeClick,
                    modifier = Modifier
                        .size(68.dp)
                        .background(tertiary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.resume),
                        tint = primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (state == TranscriptionState.STOPPED){
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(68.dp)
                        .background(tertiary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.resume),
                        tint = primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF1A2E44),
    barWidthDp: Dp = 4.dp,
    barSpacingDp: Dp = 3.dp,
    minBarHeightFraction: Float = 0.08f
) {

    Canvas(
        modifier = modifier
            .padding(horizontal = 32.dp)
    ) {
        if (amplitudes.isEmpty()) return@Canvas

        val barWidth = barWidthDp.toPx()
        val barSpacing = barSpacingDp.toPx()
        val totalBarWidth = barWidth + barSpacing
        val maxBars = (size.width / totalBarWidth).toInt()

        val displayAmplitudes = if (amplitudes.size > maxBars) {
            amplitudes.takeLast(maxBars)
        } else {
            amplitudes
        }

        val centerY = size.height / 2f

        displayAmplitudes.forEachIndexed { index, amplitude ->
            val barHeight = (size.height * amplitude.coerceAtLeast(minBarHeightFraction))
            val x = index * totalBarWidth + barWidth / 2f

            drawLine(
                color = barColor,
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun InactiveTranscribeBody(
    autoDetectLanguage: Boolean,
    selectedLanguage: LanguageData,
    onLanguageItemClick: () -> Unit,
    onStartClick: () -> Unit,
    onAutoDetectSwitchClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {
        StartButton(
            onClick = onStartClick,
            modifier = Modifier.weight(1.75f)
        )
        Box (modifier = Modifier.fillMaxWidth()) {
            LanguageSelectCard(
                autoDetectLanguage = autoDetectLanguage,
                selectedLanguage = selectedLanguage,
                onLanguageItemClick = onLanguageItemClick,
                onSwitchClick = onAutoDetectSwitchClick,
                modifier = Modifier
                    .widthIn(max = 425.dp)
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.weight(.375f))
    }
}

@Composable
fun LanguageSelectCard(
    selectedLanguage: LanguageData,
    autoDetectLanguage: Boolean,
    onSwitchClick: (Boolean) -> Unit,
    onLanguageItemClick: () -> Unit = {},
    modifier: Modifier
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
            Column {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.auto_detect),
                        modifier = Modifier.padding(end = 16.dp),
                        color = if (autoDetectLanguage) primary else Color.Gray
                    )
                    Switch(
                        checked = autoDetectLanguage,
                        onCheckedChange = onSwitchClick,
                        enabled = true,
                        modifier = Modifier
                            .height(0.dp)
                            .scale(.75f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.choose_lang),
                    color = if (autoDetectLanguage) Color.Gray else primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LanguageSelectItem(
                    language = selectedLanguage.languageName,
                    flagRes = selectedLanguage.flag,
                    onClick = onLanguageItemClick,
                    autoDetectLanguage = autoDetectLanguage
                )
            }
        }
    }
}

@Composable
fun LanguageSelectItem(
    @StringRes language: Int,
    @DrawableRes flagRes: Int,
    onClick: () -> Unit,
    autoDetectLanguage: Boolean,
    modifier: Modifier = Modifier
) {
    Row (
        modifier = modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(flagRes),
            contentDescription = stringResource(language),
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            alpha = if (autoDetectLanguage) {
                0.5f
            } else 1f
        )
        
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stringResource(language),
            color = if (autoDetectLanguage) Color.Gray else primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StartButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
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
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.start_recording),
                    textAlign = TextAlign.Center,
                    color = tertiary,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
private fun INACTIVETranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.INACTIVE),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RECORDINGTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.RECORDING),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.5f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PAUSEDTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.PAUSED),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.5f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun STOPPEDTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.STOPPED),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.5f,0.5f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,0.2f,0.5f,0.4f,1f,0.7f,0.4f,0f,),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CONNECTINGTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.CONNECTING),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TRANSCRIBINGTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.TRANSCRIBING),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FINISHEDTranscribeBodyPreview() {
    TalkBridgeLiveTheme {
        TranscribeScreen(
            uiState = TranscribeUiState(transcriptionState = TranscriptionState.FINISHED),
            onStartClick = { },
            onStopClick = { },
            onLanguageItemClick = { },
            openSettings = { },
            onAutoDetectSwitchClick = { },
            waveAmplitudes = listOf(),
        )
    }
}
