package com.talkbridge.livetranslator.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.common.htmlStringResource
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.primary

object SettingsDestination : NavigationDestination {
    override val route = "settings"
    override val titleRes = R.string.settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.settingsUiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(SettingsDestination.titleRes),
                canNavigateBack = true,
                navigateUp = onBackButtonClick
            )
        }
    ) { innerPadding ->
        SettingsBody(
            viewModel = viewModel,
            uiState = uiState,
            modifier = modifier.padding(innerPadding)
        )
    }
}

@Composable
fun SettingsBody(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    SettingsBody(
        uiState = uiState,
        onCustomIPChange = { viewModel.setCustomIP(it) },
        onStopOnAppCloseToggle = { viewModel.toggleStopOnAppClose() },
        onUseBetterTranslationToggle = { viewModel.toggleUseBetterTranslation() },
        modifier = modifier
    )
}

@Composable
fun SettingsBody(
    uiState: SettingsUiState,
    onCustomIPChange: (String) -> Unit,
    onStopOnAppCloseToggle: () -> Unit,
    onUseBetterTranslationToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn (
        modifier = modifier
    ) {
        item {
            SettingsHeading(
                text = stringResource(R.string.general),
            )
        }
        item{
            SettingsItem{
                TextField(
                    value = uiState.customIP,
                    onValueChange = onCustomIPChange,
                    label = { Text(stringResource(R.string.custom_ip)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row() {
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        item {
            SettingsItem {
                Row {
                    Text(
                        text = "Background Processing\nKeeps tasks running smoothly even if you minimize the app.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = !uiState.stopOnAppClose, //durch namensänderung der setting ( stop on app close -> Background Processing) ist es verneint
                        onCheckedChange = { onStopOnAppCloseToggle() },
                        modifier = Modifier.weight(2.5f)
                    )
                }
            }
        }

        item {
            SettingsHeading(
                text = stringResource(R.string.live_translate),
            )
//            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            SettingsItem {
                Row {
                    Text(
                        text = htmlStringResource(R.string.better_translations),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.useBetterTranslation,
                        onCheckedChange = { onUseBetterTranslationToggle() },
                        modifier = Modifier.weight(2.5f)
                    )
                }
            }
        }
        item {
            Row() {
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
//        item {
//            SettingsItem {
//                Row {
//                    Text(
//                        text = "Better Voice-Activity-Detection",
//                        style = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.weight(7f),
//                        fontSize = 14.sp
//                    )
//                    Spacer(modifier = Modifier.weight(1f))
//                    Switch(
////                        checked = uiState.useBetterVAD,
////                        onCheckedChange = { onUseBetterVADToggle() },
//                        checked = true,
//                        onCheckedChange = {  },
//                        modifier = Modifier.weight(2.5f)
//                    )
//                }
//            }
//        }
//        item {
//            val options = listOf("higher but slower", "faster but lower")
//            var selectedOption by remember { mutableStateOf(options[0]) }
//            SettingsItem {
//                Column {
//                    Text(
//                        text = "Translation quality",
//                        style = MaterialTheme.typography.bodySmall,
////                        modifier = Modifier.weight(7f),
//                        fontSize = 16.sp
//                    )
////                    Spacer(modifier = Modifier.weight(1f))
//                    Row {
//                        options.forEach { option ->
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier
////                                    .fillMaxWidth()
//                                    .selectable(
//                                        selected = (option == selectedOption),
//                                        onClick = { selectedOption = option },
//                                        role = Role.RadioButton
//                                    )
//                                    .padding(8.dp)
//                            ) {
//                                RadioButton(
//                                    selected = (option == selectedOption),
//                                    onClick = null
//                                )
//                                Text(
//                                    text = option,
//                                    fontSize = 14.sp,
//                                    modifier = Modifier.padding(start = 8.dp)
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }
}

@Composable
fun SettingsItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box (
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displayMedium,
        color = primary,
        modifier = modifier
            .padding(top = 16.dp, start = 12.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsBodyPreview() {
    TalkBridgeLiveTheme {
        SettingsBody(
            uiState = SettingsUiState(
                customIP = "192.168.1.1",
                useBetterTranslation = true,
                stopOnAppClose = false
            ),
            onCustomIPChange = {},
            onStopOnAppCloseToggle = {},
            onUseBetterTranslationToggle = {}
        )
    }
}

