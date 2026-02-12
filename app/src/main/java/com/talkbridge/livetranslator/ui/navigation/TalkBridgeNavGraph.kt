package com.talkbridge.livetranslator.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.talkbridge.livetranslator.ui.AppViewModelProvider
import com.talkbridge.livetranslator.ui.common.LanguageSelectDestination
import com.talkbridge.livetranslator.ui.common.LanguageSelectScreen
import com.talkbridge.livetranslator.ui.facetoface.FaceToFaceDestination
import com.talkbridge.livetranslator.ui.facetoface.FaceToFaceScreen
import com.talkbridge.livetranslator.ui.home.HomeDestination
import com.talkbridge.livetranslator.ui.home.HomeScreen
import com.talkbridge.livetranslator.ui.home.HomeViewModel
import com.talkbridge.livetranslator.ui.settings.SettingsDestination
import com.talkbridge.livetranslator.ui.settings.SettingsScreen
import com.talkbridge.livetranslator.ui.transcribe.TranscribeDestination
import com.talkbridge.livetranslator.ui.transcribe.TranscribeScreen
import com.talkbridge.livetranslator.ui.translate.TranslateDestination
import com.talkbridge.livetranslator.ui.translate.TranslateScreen
import com.talkbridge.livetranslator.ui.translate.TranslateViewModel

@Composable
fun TalkBridgeNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
        modifier = modifier
    ) {
        composable(route = HomeDestination.route) {
            val viewModel = viewModel<HomeViewModel>(
                factory = AppViewModelProvider.Factory
            )
            val uiState by viewModel.homeUiState.collectAsState()

            HomeScreen(
                uiState = uiState,
                onNavigationButtonClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                openSettings = {
                    navController.navigate(SettingsDestination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSourceLanguageClick = {
                    navController.navigate("${LanguageSelectDestination.route}/source")
                },
                onTargetLanguageClick = {
                    navController.navigate("${LanguageSelectDestination.route}/target")
                },
                onLanguageSwapClick = { viewModel.swapLanguages() },
                onStartButtonClick = { viewModel.startRecording() },
                onStopButtonClick = { viewModel.stopRecording() }
            )
        }
        composable(
            route = LanguageSelectDestination.routeWithArgs,
            arguments = listOf(
                navArgument(LanguageSelectDestination.languageTypeArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val languageType = backStackEntry.arguments?.getString(
                LanguageSelectDestination.languageTypeArg
            ) ?: "source"

            val previousRoute = navController.previousBackStackEntry?.destination?.route

            when (previousRoute) {
                HomeDestination.route -> {
                    val homeViewModel: HomeViewModel = viewModel(
                        viewModelStoreOwner = navController.previousBackStackEntry!!,
                        factory = AppViewModelProvider.Factory
                    )
                    LanguageSelectScreen(
                        languageType = languageType,
                        onBackButtonClick = { navController.navigateUp() },
                        onLanguageSelected = { language ->
                            if (languageType == "source") {
                                homeViewModel.updateSourceLanguage(language)
                            } else {
                                homeViewModel.updateTargetLanguage(language)
                            }
                            navController.navigateUp()
                        }
                    )
                }

                TranslateDestination.route -> {
                    val translateViewModel: TranslateViewModel = viewModel(
                        viewModelStoreOwner = navController.previousBackStackEntry!!,
                        factory = AppViewModelProvider.Factory
                    )
                    LanguageSelectScreen(
                        languageType = languageType,
                        onBackButtonClick = { navController.navigateUp() },
                        onLanguageSelected = { language ->
                            if (languageType == "source") {
                                translateViewModel.updateSourceLanguage(language)
                            } else {
                                translateViewModel.updateTargetLanguage(language)
                            }
                            navController.navigateUp()
                        }
                    )
                }
            }
        }
        composable(route = TranslateDestination.route) {
            TranslateScreen(
                onNavigationButtonClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                openSettings = {
                    navController.navigate(SettingsDestination.route)
                }
            )
        }
        composable(route = TranscribeDestination.route) {
            TranscribeScreen(
                onNavigationButtonClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                openSettings = {
                    navController.navigate(SettingsDestination.route)
                }
            )
        }
        composable(route = FaceToFaceDestination.route) {
            FaceToFaceScreen(
                onNavigationButtonClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(route = SettingsDestination.route) {
            SettingsScreen(
                { navController.navigateUp() }
            )
        }
    }
}