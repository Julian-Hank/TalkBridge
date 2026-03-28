package com.talkbridge.livetranslator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
): ViewModel() {

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        setAllPreferences()
    }

    private fun setAllPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.customIPAddress
                .collect {
                    _settingsUiState.update { uiState ->
                        uiState.copy(
                            customIP = it
                        )
                    }
                }
        }
    }

    fun setCustomIP(ip: String){
        _settingsUiState.update { uiState ->
            uiState.copy(
                customIP = ip
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.saveCustomIP(ip)
        }
    }
}

data class SettingsUiState(
    val customIP: String = ""
)