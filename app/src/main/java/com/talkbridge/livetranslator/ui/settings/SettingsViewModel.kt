package com.talkbridge.livetranslator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
                .combine(userPreferencesRepository.useBetterTranslation){ customIP, useBetterTranslation ->
                    customIP to useBetterTranslation
                }
                .collect { (customIp, useBetterTranslation) ->
                    _settingsUiState.update { uiState ->
                        uiState.copy(
                            customIP = customIp,
                            useBetterTranslation = useBetterTranslation
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

    fun toggleUseBetterTranslation(){
        val useBetterTranslation = !settingsUiState.value.useBetterTranslation
        viewModelScope.launch {
            userPreferencesRepository.setUseBetterTranslation(useBetterTranslation)
        }
    }
}

data class SettingsUiState(
    val customIP: String = "",
    val useBetterTranslation: Boolean = true
)