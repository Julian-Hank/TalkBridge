package com.talkbridge.livetranslator.ui.facetoface

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.TalkBridgeBottomNavBar
import com.talkbridge.livetranslator.ui.navigation.NavigationDestination
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon

object FaceToFaceDestination : NavigationDestinationWithIcon {
    override val route = "face_to_face"
    override val titleRes = R.string.conversation
    override val icon = R.drawable.facetoface
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceToFaceScreen(
    modifier: Modifier = Modifier,
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            TalkBridgeBottomNavBar(
                onNavigationButtonClick = onNavigationButtonClick,
                currentRoute = FaceToFaceDestination.route
            )
        }
    ) { innerPadding ->
        FaceToFaceBody(
            modifier = modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
fun FaceToFaceBody(modifier: Modifier = Modifier) {

}