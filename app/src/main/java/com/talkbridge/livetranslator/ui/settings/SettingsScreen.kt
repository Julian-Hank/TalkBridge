package com.talkbridge.livetranslator.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
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
    LazyColumn (
        modifier = modifier
    ) {
        item {
            SettingsHeading(
                text = stringResource(R.string.general),
            )
//            Spacer(modifier = Modifier.height(16.dp))
        }
        item{
            SettingsItem{
                TextField(
                    value = uiState.customIP,
                    onValueChange = { viewModel.setCustomIP(it) },
                    label = { Text(stringResource(R.string.custom_ip)) },
                    modifier = Modifier.fillMaxWidth()
                )
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
                        text = stringResource(R.string.better_translations),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.useBetterTranslation,
                        onCheckedChange = { viewModel.toggleUseBetterTranslation() },
                        modifier = Modifier.weight(2.5f)
                    )
                }
            }
        }
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

