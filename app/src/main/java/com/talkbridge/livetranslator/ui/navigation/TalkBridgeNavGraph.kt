package com.talkbridge.livetranslator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.talkbridge.livetranslator.ui.settings.SettingsViewModel
import com.talkbridge.livetranslator.ui.transcribe.TranscribeDestination
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemViewDestination
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemViewModel
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemViewScreen
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemsOverviewDestination
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemsScreen
import com.talkbridge.livetranslator.ui.transcribe.TranscribeItemsViewModel
import com.talkbridge.livetranslator.ui.transcribe.TranscribeScreen
import com.talkbridge.livetranslator.ui.transcribe.TranscribeViewModel
import com.talkbridge.livetranslator.ui.translate.TranslateDestination
import com.talkbridge.livetranslator.ui.translate.TranslateScreen
import com.talkbridge.livetranslator.ui.translate.TranslateViewModel

@Composable
fun TalkBridgeNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
//        enterTransition = {
////            scaleIn(initialScale = 0.9f) + fadeIn()
//        },
//        exitTransition = {
//            scaleOut(targetScale = 0.9f) + fadeOut()
//        },
//        popEnterTransition = {
//            scaleIn(initialScale = 0.9f) + fadeIn()
//        },
//        popExitTransition = {
//            scaleOut(targetScale = 0.9f) + fadeOut()
//        },
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
                    navController.navigate("${LanguageSelectDestination.route}/source/${HomeDestination.route}")
                },
                onTargetLanguageClick = {
                    navController.navigate("${LanguageSelectDestination.route}/target/${HomeDestination.route}")
                },
                onLanguageSwapClick = { viewModel.swapLanguages() },
                onStartButtonClick = { viewModel.connectWithServer() },
                onPauseButtonClick = { viewModel.pauseRecording() },
                onBackButtonClick = { viewModel.stopRecording() }
            )
        }
        composable(
            route = LanguageSelectDestination.routeWithArgs,
            arguments = listOf(
                navArgument(LanguageSelectDestination.languageTypeArg) { type = NavType.StringType },
                navArgument(LanguageSelectDestination.callerRouteArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val languageType = backStackEntry.arguments?.getString(
                LanguageSelectDestination.languageTypeArg
            ) ?: "source"
            val callerRoute = backStackEntry.arguments?.getString(
                LanguageSelectDestination.callerRouteArg
            ) ?: HomeDestination.route

            when (callerRoute) {
                HomeDestination.route -> {
                    val homeBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(HomeDestination.route)
                    }
                    val homeViewModel: HomeViewModel = viewModel(
                        viewModelStoreOwner = homeBackStackEntry,
                        factory = AppViewModelProvider.Factory
                    )
                    val recentLanguages = homeViewModel.homeUiState.collectAsState().value.recentLanguages

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
                        },
                        recentLanguages = recentLanguages
                    )
                }

                TranslateDestination.route -> {
                    val translateBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TranslateDestination.route)
                    }
                    val translateViewModel: TranslateViewModel = viewModel(
                        viewModelStoreOwner = translateBackStackEntry,
                        factory = AppViewModelProvider.Factory
                    )
                    val recentLanguages = translateViewModel.translateUiState.collectAsState().value.recentLanguages

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
                        },
                        recentLanguages = recentLanguages
                    )
                }

                TranscribeDestination.route -> {
                    val transcribeBackStackEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TranscribeDestination.route)
                    }
                    val transcribeViewModel: TranscribeViewModel = viewModel(
                        viewModelStoreOwner = transcribeBackStackEntry,
                        factory = AppViewModelProvider.Factory
                    )
                    val recentLanguages = transcribeViewModel.transcribeUiState.collectAsState().value.recentLanguages

                    LanguageSelectScreen(
                        languageType = languageType,
                        onBackButtonClick = { navController.navigateUp() },
                        onLanguageSelected = { language ->
                            transcribeViewModel.updateSourceLanguage(language)
                            navController.navigateUp()
                        },
                        recentLanguages = recentLanguages//transcribeViewModel.transcribeUiState.collectAsState().value.recentLanguages
                    )
                }


            }
        }
        composable(route = TranslateDestination.route) {
            val viewModel: TranslateViewModel = viewModel(
                factory = AppViewModelProvider.Factory
            )

            val uiState by viewModel.translateUiState.collectAsState()

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
                },
                onSourceLanguageClick = {
                    navController.navigate("${LanguageSelectDestination.route}/source/${TranslateDestination.route}")
                },
                onTargetLanguageClick = {
                    navController.navigate("${LanguageSelectDestination.route}/target/${TranslateDestination.route}")
                },
                onInputChanged = { viewModel.setSourceLanguageText(it) },
                onLanguageSwapClick = { viewModel.swapLanguages() },
                uiState = uiState
            )
        }
        composable(route = TranscribeDestination.route) {
            val viewModel = viewModel<TranscribeViewModel>(
                factory = AppViewModelProvider.Factory
            )
            val uiState by viewModel.transcribeUiState.collectAsState()
            val amplitudes by viewModel.waveAmplitudes.collectAsState()

            TranscribeScreen(
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
                    navController.navigate(SettingsDestination.route)
                },
                onAutoDetectSwitchClick = { autoDetect ->
                    viewModel.setAutoDetectLanguage(autoDetect)
                },
                onLanguageItemClick = {
                    navController.navigate("${LanguageSelectDestination.route}/source/${TranscribeDestination.route}")
                },
                onStartClick = { viewModel.startRecording() },
                onStopClick = { viewModel.stopRecording() },
                onPauseClick = { viewModel.pauseRecording() },
                onResumeClick = { viewModel.resumeRecording() },
                onDeleteClick = { viewModel.deleteRecording() },
                onFinishClick = { viewModel.sendRecording() },
                waveAmplitudes = amplitudes,
                onViewTranscriptionClick = { id ->
                    navController.navigate("${TranscribeItemViewDestination.route}/$id")
                    viewModel.resetUiState()
                },
                openTranscriptionsOverview = {
                    navController.navigate(TranscribeItemsOverviewDestination.route)
                }
            )
        }
        composable(
            route = TranscribeItemViewDestination.routeWithArgs,
            arguments = listOf(
                navArgument(TranscribeItemViewDestination.transcribeItemArg) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val viewModel = viewModel<TranscribeItemViewModel>(
                factory = AppViewModelProvider.Factory
            )

            TranscribeItemViewScreen(
                viewModel = viewModel,
                onBackButtonClick = { navController.navigateUp() }
            )
        }
        composable(route = TranscribeItemsOverviewDestination.route) {
            val viewModel = viewModel<TranscribeItemsViewModel>(
                factory = AppViewModelProvider.Factory
            )

            val uiState by viewModel.transcriptionItemsUiState.collectAsState()

            TranscribeItemsScreen(
                uiState = uiState,
                onBackButtonClick = { navController.navigateUp() },
                navigateToTranscription = { id ->
                    navController.navigate("${TranscribeItemViewDestination.route}/$id")
                },
                deleteItem = { id -> viewModel.deleteItem(id) }
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
            val viewModel = viewModel<SettingsViewModel>(
                factory = AppViewModelProvider.Factory
            )

            SettingsScreen(
                viewModel = viewModel,
                onBackButtonClick = { navController.navigateUp() }
            )
        }
    }
}