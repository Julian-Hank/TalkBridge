package com.talkbridge.livetranslator.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.ClientEvent
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.languagecodeToLanguageObject
import com.talkbridge.livetranslator.data.repository.PreferenceKeys
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import com.talkbridge.livetranslator.data.stringResToLanguagecode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        observeClientEvents()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.currentTranslateSourceLanguage
                .combine(userPreferencesRepository.currentTranslateTargetLanguage) { source, target ->
                    source to target
                }
                .combine(userPreferencesRepository.recentTranslateLanguages) { pair, recent ->
                    Triple(pair.first, pair.second, recent)
                }
                .collect { (source, target, recent) ->

                    val recentLanguages = recent.map { languagecode ->
                        languagesMap.getValue(languagecodeToLanguageObject(languagecode))
                    }

                    _translateUiState.update { currentState ->
                        currentState.copy(
                            sourceLanguage = languagesMap.getValue(languagecodeToLanguageObject(source)),
                            targetLanguage = languagesMap.getValue(languagecodeToLanguageObject(target)),
                            recentLanguages = recentLanguages
                        )
                    }
                }
        }
    }


    private fun observeClientEvents() {
        viewModelScope.launch {
            talkBridgeClient.events.collect { event ->
                when (event) {
                    is ClientEvent.TranslationResult -> handleTranslationResponse(event.text)
                    is ClientEvent.TranslationError  -> _translateUiState.update { it.copy(targetLanguageText = "Server offline") }
                    else -> {}
                }
            }
        }
    }

    fun swapLanguages(){
        val sourceLanguage = translateUiState.value.sourceLanguage
        val targetLanguage = translateUiState.value.targetLanguage
        val sourceLanguageText = translateUiState.value.sourceLanguageText
        val targetLanguageText = translateUiState.value.targetLanguageText

        viewModelScope.launch {
            userPreferencesRepository.saveCurrentLanguages(
                sourceLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_SOURCE_LANGUAGE,
                targetLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_TARGET_LANGUAGE,
                sourceLanguageCode = stringResToLanguagecode(targetLanguage.languageName),
                targetLanguageCode = stringResToLanguagecode(sourceLanguage.languageName)
            )
        }

        _translateUiState.update { currentState ->
            currentState.copy(
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
            viewModelScope.launch {
                userPreferencesRepository.saveCurrentLanguages(
                    sourceLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_SOURCE_LANGUAGE,
                    targetLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_TARGET_LANGUAGE,
                    sourceLanguageCode = stringResToLanguagecode(language.languageName),
                    targetLanguageCode = stringResToLanguagecode(targetLanguage.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName),
                    key = PreferenceKeys.RECENT_TRANSLATE_LANGUAGES
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
            viewModelScope.launch {
                userPreferencesRepository.saveCurrentLanguages(
                    sourceLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_SOURCE_LANGUAGE,
                    targetLanguageKey = PreferenceKeys.CURRENT_TRANSLATE_TARGET_LANGUAGE,
                    sourceLanguageCode = stringResToLanguagecode(sourceLanguage.languageName),
                    targetLanguageCode = stringResToLanguagecode(language.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName),
                    key = PreferenceKeys.RECENT_TRANSLATE_LANGUAGES
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
    val recentLanguages: List<LanguageData>? = null
)