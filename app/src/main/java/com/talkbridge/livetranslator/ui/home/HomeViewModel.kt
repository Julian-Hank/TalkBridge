package com.talkbridge.livetranslator.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.ClientEvent
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.SERVER_RESPONSE
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.TalkBridgeForegroundService
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
    application: Application,
    private val talkBridgeClient: TalkBridgeClient,
    private val userPreferencesRepository: UserPreferencesRepository
): AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val audioRecorder = AudioRecorder()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private var isPaused = false

    private var stopOnAppClose = false

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
                .combine(userPreferencesRepository.stopOnAppClose) { triple, stopOnClose ->
                    Pair(triple, stopOnClose)
                }
                .collect { (triple, stopOnClose) ->
                    val (source, target, recent) = triple

                    stopOnAppClose = stopOnClose

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
                    is ClientEvent.Connected                    -> handleServerConnected()
                    is ClientEvent.Ready                        -> handleServerReady()
                    is ClientEvent.LiveTranslationResult        -> addNewTextResult(event.text, event.type)
                    is ClientEvent.LiveTranslationError         -> handleError(event.message)
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
        if (stopOnAppClose){
            startRecording()
        } else {
            context.startService(
                TalkBridgeForegroundService.startLiveIntent(
                    context
                )
            )
        }
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
        isPaused = false
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                if (!isPaused) {
                    try {
                        talkBridgeClient.sendAudio(audioData)
                    } catch (e: Exception) {
                        handleError(e.toString())
                    }
                }
            }
        }
    }

    fun stopRecording() {
        if (stopOnAppClose){
            talkBridgeClient.disconnect()
            audioRecorder.stopRecording()
        } else {
            context.startService(TalkBridgeForegroundService.stopIntent(context))
        }
        resetConnectionState()
    }

    fun pauseRecording() {
        when (homeUiState.value.connectionState) {
            ConnectionState.PAUSED -> {
                if (stopOnAppClose) {
                    startRecording()
                } else {
                    context.startService(TalkBridgeForegroundService.resumeIntent(context))
                }
                _homeUiState.update { it.copy(connectionState = ConnectionState.READY) }
            }
            ConnectionState.READY -> {
                if (stopOnAppClose) {
                    isPaused = true
                    audioRecorder.stopRecording()
                    talkBridgeClient.resetSession()
                } else {
                    context.startService(TalkBridgeForegroundService.pauseIntent(context))
                }
                _homeUiState.update { it.copy(connectionState = ConnectionState.PAUSED) }
            }
            else -> Log.w(TAG, "Nothing to Pause")
        }
    }

    fun resetConnectionState(){
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.NOT_CONNECTED,
                textResults = null
            )
        }
    }

    fun addNewTextResult(text: String, type: SERVER_RESPONSE) {
        val current = homeUiState.value.textResults?.toMutableList() ?: mutableListOf()
        val lastEntry = current.lastOrNull()

        when (type) {
            SERVER_RESPONSE.PARTIAL -> {
                when {
                    lastEntry?.translated == null && lastEntry != null -> { //nicht null aber translate null -> TranslationEntry("something", null)
                        current[current.lastIndex] = lastEntry.copy(original = text)
                    }
                    else -> {
                        current.add(TranslationEntry(original = text))
                    }
                }
            }
            SERVER_RESPONSE.FINAL -> {
                when {
                    lastEntry?.translated == null && lastEntry != null -> { //nicht null aber translate null -> TranslationEntry("something", null)
                        current[current.lastIndex] = lastEntry.copy(original = text)
                    }
                    else -> {
                        current.add(TranslationEntry(original = text))
                    }
                }
            }
            SERVER_RESPONSE.TRANSLATED -> {
                current[current.lastIndex] = lastEntry!!.copy(translated = text)
            }
        }

//        when {
//            lastEntry?.translated == null && lastEntry != null -> { //nicht null aber translate null -> TranslationEntry("something", null)
//                current[current.lastIndex] = lastEntry.copy(translated = text)
//            }
//            else -> {
//                current.add(TranslationEntry(original = text))
//            }
//        }
        Log.d(TAG, current.toString())
        _homeUiState.update { it.copy(textResults = current) }
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
//    val currentText: String? = null,
    val textResults: MutableList<TranslationEntry>? = null, //  z.b [TranslationEntry("Hallo", "Hello"),TranslationEntry("Wie geht es dir?", null)]
    val recentLanguages: List<LanguageData>? = null
)

enum class ConnectionState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    READY,
    PAUSED,
    FAILED
}

data class TranslationEntry(
    val original: String,
    val translated: String? = null  // null = noch ausstehend
)