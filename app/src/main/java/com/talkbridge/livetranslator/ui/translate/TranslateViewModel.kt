package com.talkbridge.livetranslator.ui.translate

import androidx.lifecycle.ViewModel
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.ui.home.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TranslateViewModel: ViewModel() {
    private val _translateUiState = MutableStateFlow(TranslateUiState())
    val translateUiState: StateFlow<TranslateUiState> = _translateUiState.asStateFlow()

    fun updateSourceLanguage(language: LanguageData){
        _translateUiState.update { currentState ->
            currentState.copy(
                sourceLanguage = language
            )
        }
    }

    fun updateTargetLanguage(language: LanguageData){
        _translateUiState.update { currentState ->
            currentState.copy(
                targetLanguage = language
            )
        }
    }

    fun swapLanguages(){
        val sourceLanguage = translateUiState.value.sourceLanguage
        val targetLanguage = translateUiState.value.targetLanguage

        _translateUiState.update { currentState ->
            currentState.copy(
                sourceLanguage = targetLanguage,
                targetLanguage = sourceLanguage
            )
        }
    }

    fun setSourceLanguageText(text: String){
        _translateUiState.update { currentState ->
            currentState.copy(
                sourceLanguageText = text
            )
        }
    }
}

data class TranslateUiState(
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val sourceLanguageText: String? = null,
    val targetLanguageText: String? = null,
)