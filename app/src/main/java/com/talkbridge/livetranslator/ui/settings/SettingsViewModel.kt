package com.talkbridge.livetranslator.ui.settings

import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
            userPreferencesRepository.costumIPAdress
                .collect {
                    _settingsUiState.update { uiState ->
                        uiState.copy(
                            costumIP = it
                        )
                    }
                }
        }
    }

    fun setCostumIP(ip: String){
        _settingsUiState.update { uiState ->
            uiState.copy(
                costumIP = ip
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.saveCostumIP(ip)
        }
    }

//    fun toggleServerSelection(){
//        val currentSelection = settingsUiState.value.serverSelection
//        _settingsUiState.update { uiState ->
//            uiState.copy(
//                serverSelection = if (currentSelection == "local") "render" else "local"
//            )
//        }
//        viewModelScope.launch {
//            userPreferencesRepository.setServerSelection(settingsUiState.value.serverSelection)
//        }
//    }
}

data class SettingsUiState(
    val costumIP: String = ""
)