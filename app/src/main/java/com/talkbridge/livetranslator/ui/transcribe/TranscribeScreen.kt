package com.talkbridge.livetranslator.ui.transcribe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeBottomNavBar
import com.talkbridge.livetranslator.TalkBridgeTopAppBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon

object TranscribeDestination : NavigationDestinationWithIcon {
    override val route = "transcribe"
    override val titleRes = R.string.transcribe
    override val icon = R.drawable.speech_to_text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit
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
                currentRoute = TranscribeDestination.route
            )
        }
    ) { innerPadding ->
        TranscribeBody(
            modifier = modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
fun TranscribeBody(modifier: Modifier = Modifier) {

}