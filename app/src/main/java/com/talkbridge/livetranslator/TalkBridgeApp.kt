package com.talkbridge.livetranslator


import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.talkbridge.livetranslator.ui.facetoface.FaceToFaceDestination
import com.talkbridge.livetranslator.ui.home.HomeDestination
import com.talkbridge.livetranslator.ui.navigation.NavigationDestinationWithIcon
import com.talkbridge.livetranslator.ui.navigation.TalkBridgeNavHost
import com.talkbridge.livetranslator.ui.theme.TalkBridgeLiveTheme
import com.talkbridge.livetranslator.ui.theme.primary
import com.talkbridge.livetranslator.ui.theme.secondary
import com.talkbridge.livetranslator.ui.transcribe.TranscribeDestination
import com.talkbridge.livetranslator.ui.translate.TranslateDestination


@Composable
fun TalkBridgeApp(navController: NavHostController = rememberNavController()) {
    TalkBridgeNavHost(navController = navController)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkBridgeTopAppBar(
    title: String?,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {},
    openSettings: () -> Unit = {}
) {
//    val isDark = isSystemInDarkTheme()
    val iconTint = primary

    CenterAlignedTopAppBar(
        title = {
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    color = primary
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.talkbridge_logo_full_darkblue),
                    contentDescription = stringResource(R.string.app_logo),
                    modifier = Modifier.size(200.dp),
                    tint = iconTint
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button),
                        tint = iconTint
                    )
                }
            } else {
                IconButton(onClick = openSettings ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.settings),
                        tint = iconTint
                    )
                }
            }
        }
    )
}

@Composable
fun TalkBridgeBottomNavBar(
    onNavigationButtonClick: (NavigationDestinationWithIcon) -> Unit = {},
    currentRoute: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        FaceToFaceDestination,
        TranslateDestination,
        TranscribeDestination,
        HomeDestination
    )

    NavigationBar(
        containerColor = Color.Transparent
    ) {
//        val currentRoute =
//            navController.currentBackStackEntryAsState()
//                .value?.destination?.route

        items.forEach { item ->
//            Log.d("MAIN", (LocalDensity.current).toString())
            val tint = if (item.route == currentRoute) primary else secondary
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigationButtonClick(item) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .size(48.dp)
                    )
                },
//                label = {
//                    Text(stringResource(item.titleRes))
//                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun TalkBridgeBottomNavBarPreview() {
    TalkBridgeLiveTheme(darkTheme = false) {
        TalkBridgeBottomNavBar(
            currentRoute = HomeDestination.route
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun TalkBridgeTopAppBarPreview() {
    TalkBridgeLiveTheme(darkTheme = false) {
        TalkBridgeTopAppBar(
            title = null,
            canNavigateBack = false,
        )
    }
}