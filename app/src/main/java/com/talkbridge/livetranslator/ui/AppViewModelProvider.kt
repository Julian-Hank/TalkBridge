package com.talkbridge.livetranslator.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.talkbridge.livetranslator.TalkBridgeApplication
import com.talkbridge.livetranslator.ui.home.HomeViewModel
import com.talkbridge.livetranslator.ui.settings.SettingsViewModel
import com.talkbridge.livetranslator.ui.transcribe.TranscribeViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                application = talkBridgeApplication(),
                userPreferencesRepository = talkBridgeApplication().container.userPreferencesRepository
            )
        }
        initializer {
            TranscribeViewModel(
                application = talkBridgeApplication(),
                userPreferencesRepository = talkBridgeApplication().container.userPreferencesRepository
            )
        }
        initializer {
            SettingsViewModel(
                userPreferencesRepository = talkBridgeApplication().container.userPreferencesRepository
            )
        }
    }
}


/**
 * Extension function to queries for [Application] object and returns an instance of
 * [TalkBridgeApplication].
 */
fun CreationExtras.talkBridgeApplication(): TalkBridgeApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as TalkBridgeApplication)