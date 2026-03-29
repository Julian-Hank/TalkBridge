package com.talkbridge.livetranslator.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.ClientEvent
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import com.talkbridge.livetranslator.data.languagecodeToLanguageObject
import com.talkbridge.livetranslator.data.repository.PreferenceKeys
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import com.talkbridge.livetranslator.data.stringResToLanguagecode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG: String = "HomeViewModel"

class HomeViewModel(
    private val talkBridgeClient: TalkBridgeClient,
    private val userPreferencesRepository: UserPreferencesRepository
): ViewModel() {

    private val audioRecorder = AudioRecorder()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    init {
        observePreferences()
        observeClientEvents()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.currentLiveSourceLanguage
                .combine(userPreferencesRepository.currentLiveTargetLanguage) { source, target ->
                    source to target
                }
                .combine(userPreferencesRepository.recentLiveLanguages) { pair, recent ->
                    Triple(pair.first, pair.second, recent)
                }
                .collect { (source, target, recent) ->

                    val recentLanguages = recent.map { languagecode ->
                        languagesMap.getValue(languagecodeToLanguageObject(languagecode))
                    }

                    _homeUiState.update { currentState ->
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
                    is ClientEvent.Connected            -> handleServerConnected()
                    is ClientEvent.Ready                -> handleServerReady()
                    is ClientEvent.LiveTranslationResult -> setCurrentText(event.text)
                    is ClientEvent.LiveTranslationError  -> handleError(event.message)
                    else -> { }
                }
            }
        }
    }

    private fun handleServerConnected() {
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.CONNECTED
            )
        }
    }

    private fun handleServerReady() {
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.READY
            )
        }
        startRecording()
    }

    private fun handleError(error: String){
        stopRecording()
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.FAILED
            )
        }
    }

    fun connectWithServer(){
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.CONNECTING
            )
        }
        talkBridgeClient.connectWebsocket(
            sourceLang = stringResToLanguagecode(homeUiState.value.sourceLanguage.languageName),
            targetLang = stringResToLanguagecode(homeUiState.value.targetLanguage.languageName),
        )
    }

    fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                try {
                    talkBridgeClient.sendAudio(audioData)
                } catch (e: Exception) {
                    handleError(e.toString())
                }
            }
        }
    }

    fun stopRecording() {
        talkBridgeClient.disconnect()
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.NOT_CONNECTED
            )
        }
        audioRecorder.stopRecording()
        resetCurrentText()
    }

    fun resetConnectionState(){
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.NOT_CONNECTED
            )
        }
    }

    fun setCurrentText(text: String){
        _homeUiState.update { currentState ->
            currentState.copy(
                currentText = text
            )
        }
    }

    fun resetCurrentText(){
        _homeUiState.update { currentState ->
            currentState.copy(
                currentText = null
            )
        }
    }

    fun updateSourceLanguage(language: LanguageData){
        val targetLanguage = homeUiState.value.targetLanguage

        if (language == targetLanguage){
            swapLanguages()
        } else{
            viewModelScope.launch {
                userPreferencesRepository.saveCurrentLanguages(
                    sourceLanguageKey = PreferenceKeys.CURRENT_LIVE_SOURCE_LANGUAGE,
                    targetLanguageKey = PreferenceKeys.CURRENT_LIVE_TARGET_LANGUAGE,
                    sourceLanguageCode = stringResToLanguagecode(language.languageName),
                    targetLanguageCode = stringResToLanguagecode(targetLanguage.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName),
                    key = PreferenceKeys.RECENT_LIVE_LANGUAGES
                )
            }
        }
    }

    fun updateTargetLanguage(language: LanguageData){
        val sourceLanguage = homeUiState.value.sourceLanguage

        if (language == sourceLanguage){
            swapLanguages()
        } else {
            viewModelScope.launch {
                userPreferencesRepository.saveCurrentLanguages(
                    sourceLanguageKey = PreferenceKeys.CURRENT_LIVE_SOURCE_LANGUAGE,
                    targetLanguageKey = PreferenceKeys.CURRENT_LIVE_TARGET_LANGUAGE,
                    sourceLanguageCode = stringResToLanguagecode(sourceLanguage.languageName),
                    targetLanguageCode = stringResToLanguagecode(language.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName),
                    key = PreferenceKeys.RECENT_LIVE_LANGUAGES
                )
            }
        }
    }

    fun swapLanguages(){
        val sourceLanguage = homeUiState.value.sourceLanguage
        val targetLanguage = homeUiState.value.targetLanguage

        viewModelScope.launch {
            userPreferencesRepository.saveCurrentLanguages(
                sourceLanguageKey = PreferenceKeys.CURRENT_LIVE_SOURCE_LANGUAGE,
                targetLanguageKey = PreferenceKeys.CURRENT_LIVE_TARGET_LANGUAGE,
                sourceLanguageCode = stringResToLanguagecode(targetLanguage.languageName),
                targetLanguageCode = stringResToLanguagecode(sourceLanguage.languageName)
            )
        }
    }
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val currentText: String? = null,
    val recentLanguages: List<LanguageData>? = null
)

enum class ConnectionState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    READY,
    FAILED
}