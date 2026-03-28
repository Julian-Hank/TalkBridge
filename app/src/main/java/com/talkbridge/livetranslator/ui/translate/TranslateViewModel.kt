package com.talkbridge.livetranslator.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import com.talkbridge.livetranslator.data.stringResToLanguagecode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TranslateViewModel(
    private val talkBridgeClient: TalkBridgeClient,
    private val userPreferencesRepository: UserPreferencesRepository
): ViewModel() {

    private val _translateUiState = MutableStateFlow(TranslateUiState())
    val translateUiState: StateFlow<TranslateUiState> = _translateUiState.asStateFlow()

    init{
        observePreferences()
        setupClientCallbacks()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.customIPAddress.collect {
                talkBridgeClient.updateIpAddress(it)
            }
        }
    }

    private fun setupClientCallbacks() {
        talkBridgeClient.onTranslationResponse = { handleTranslationResponse(it) }
        talkBridgeClient.onTranslationError = {
            _translateUiState.update { uiState ->
                uiState.copy(
                    targetLanguageText = "Server offline"
                )
            }
        }
    }

    fun swapLanguages(){
        val sourceLanguage = translateUiState.value.sourceLanguage
        val targetLanguage = translateUiState.value.targetLanguage
        val sourceLanguageText = translateUiState.value.sourceLanguageText
        val targetLanguageText = translateUiState.value.targetLanguageText

        _translateUiState.update { currentState ->
            currentState.copy(
                sourceLanguage = targetLanguage,
                targetLanguage = sourceLanguage,
                sourceLanguageText = targetLanguageText,
                targetLanguageText = sourceLanguageText
            )
        }
        sendTextForTranslation()
    }

    fun updateSourceLanguage(language: LanguageData){
        val targetLanguage = translateUiState.value.targetLanguage

        if (language == targetLanguage){
            swapLanguages()
        } else {
            _translateUiState.update { currentState ->
                currentState.copy(
                    sourceLanguage = language
                )
            }
        }
        sendTextForTranslation()
    }

    fun updateTargetLanguage(language: LanguageData){
        val sourceLanguage = translateUiState.value.sourceLanguage

        if (language == sourceLanguage){
            swapLanguages()
        } else {
            _translateUiState.update { currentState ->
                currentState.copy(
                    targetLanguage = language
                )
            }
        }
        sendTextForTranslation()
    }

    private var debounceJob: Job? = null

    fun setSourceLanguageText(text: String){
        _translateUiState.update { currentState ->
            currentState.copy(
                sourceLanguageText = text
            )
        }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(500)
            sendTextForTranslation()
        }
    }

    private fun sendTextForTranslation() {
        val text = translateUiState.value.sourceLanguageText?.trim() ?: ""
        if (text == ""){
            _translateUiState.update { uiState ->
                uiState.copy(
                    targetLanguageText = ""
                )
            }
        } else {
            talkBridgeClient.sendTextForTranslation(
                text = text,
                sourceLang = stringResToLanguagecode(translateUiState.value.sourceLanguage.languageName),
                targetLang = stringResToLanguagecode(translateUiState.value.targetLanguage.languageName),
            )
        }
    }

    private fun handleTranslationResponse(translation: String){
        _translateUiState.update { uiState ->
            uiState.copy(
                targetLanguageText = translation
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