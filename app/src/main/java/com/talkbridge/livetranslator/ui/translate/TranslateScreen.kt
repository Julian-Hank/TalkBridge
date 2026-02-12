package com.talkbridge.livetranslator.ui.translate

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
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon
import com.talkbridge.livetranslator.ui.transcribe.TranscribeDestination

object TranslateDestination : NavigationDestinationWithIcon {
    override val route = "translate"
    override val titleRes = R.string.translate
    override val icon = R.drawable.translate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    openSettings: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TalkBridgeTopAppBar(
                title = stringResource(TranslateDestination.titleRes),
                canNavigateBack = false,
                openSettings = openSettings
            )
        },
        bottomBar = {
            TalkBridgeBottomNavBar(
                onNavigationButtonClick = onNavigationButtonClick,
                currentRoute = TranslateDestination.route
            )
        }
    ) { innerPadding ->
        TranslateBody(
            modifier = modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
fun TranslateBody(modifier: Modifier = Modifier) {

}