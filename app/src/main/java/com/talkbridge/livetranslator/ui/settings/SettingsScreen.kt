package com.talkbridge.livetranslator.ui.settings

import androidx.compose.foundation.layout.Row
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
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination

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
            Text(text = "General", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
//            Spacer(modifier = Modifier.height(16.dp))
        }
        item{
            TextField(
                value = uiState.customIP,
                onValueChange = { viewModel.setCustomIP(it) },
                label = { Text("Custom IP") },
            )
        }

        item {
            Text(text = stringResource(R.string.live_translate), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
//            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row {
                Text("Higher Quality Translations\n(increases Latency)")
                Switch(
                    checked = false,
                    onCheckedChange = {  },
                )
            }
        }
    }
}