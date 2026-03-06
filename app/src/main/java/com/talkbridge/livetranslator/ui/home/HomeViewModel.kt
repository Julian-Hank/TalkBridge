package com.talkbridge.livetranslator.ui.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.Language
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG: String = "HomeViewModel"

class HomeViewModel(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository
): AndroidViewModel(application) {
    private val context = getApplication<Application>()

    private val talkBridgeClient = TalkBridgeClient(context)
    private val audioRecorder = AudioRecorder()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private lateinit var customIPAddress: String

    init {
        observePreferences()
        setupClientCallbacks()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.currentSourceLanguage
                .combine(userPreferencesRepository.currentTargetLanguage) { source, target ->
                    source to target
                }
                .combine(userPreferencesRepository.recentLanguages) { pair, recent ->
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
        viewModelScope.launch {
            userPreferencesRepository.costumIPAdress.collect {
                customIPAddress = it
            }
        }
    }

    private fun setupClientCallbacks() {
        talkBridgeClient.onReady = { handleServerReady() }
        talkBridgeClient.onConnected = { handleServerConnected() }
        talkBridgeClient.onError = { error -> handleError(error) }
        talkBridgeClient.onTranslationResponse = { text -> setCurrentText(text) }
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
        if (customIPAddress == ""){
            talkBridgeClient.connectWebsocket(
                sourceLang = stringResToLanguagecode(homeUiState.value.sourceLanguage.languageName),
                targetLang = stringResToLanguagecode(homeUiState.value.targetLanguage.languageName),
            )
        } else {
            talkBridgeClient.connectWebsocket(
                ipAddress = customIPAddress,
                sourceLang = stringResToLanguagecode(homeUiState.value.sourceLanguage.languageName),
                targetLang = stringResToLanguagecode(homeUiState.value.targetLanguage.languageName),
            )
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                try {
                    talkBridgeClient.sendAudio(audioData, context)
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
                    sourceLanguageCode = stringResToLanguagecode(language.languageName),
                    targetLanguageCode = stringResToLanguagecode(targetLanguage.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName)
                )
            }
//            _homeUiState.update { currentState ->
//                currentState.copy(
//                    sourceLanguage = language
//                )
//            }
        }
    }

    fun updateTargetLanguage(language: LanguageData){
        val sourceLanguage = homeUiState.value.sourceLanguage

        if (language == sourceLanguage){
            swapLanguages()
        } else {
            viewModelScope.launch {
                userPreferencesRepository.saveCurrentLanguages(
                    sourceLanguageCode = stringResToLanguagecode(sourceLanguage.languageName),
                    targetLanguageCode = stringResToLanguagecode(language.languageName)
                )
                userPreferencesRepository.addRecentLanguage(
                    newLanguageCode = stringResToLanguagecode(language.languageName)
                )
            }
//            _homeUiState.update { currentState ->
//                currentState.copy(
//                    targetLanguage = language
//                )
//            }
        }
    }

    fun swapLanguages(){
        val sourceLanguage = homeUiState.value.sourceLanguage
        val targetLanguage = homeUiState.value.targetLanguage

        viewModelScope.launch {
            userPreferencesRepository.saveCurrentLanguages(
                sourceLanguageCode = stringResToLanguagecode(targetLanguage.languageName),
                targetLanguageCode = stringResToLanguagecode(sourceLanguage.languageName)
            )
        }
//        _homeUiState.update { currentState ->
//            currentState.copy(
//                sourceLanguage = targetLanguage,
//                targetLanguage = sourceLanguage
//            )
//        }
    }

    private val stringResToLang = mapOf(
        R.string.dutch to "nl",
        R.string.english to "en",
        R.string.french to "fr",
        R.string.german to "de",
        R.string.italian to "it",
        R.string.japanese to "ja",
        R.string.korean to "ko",
        R.string.polish to "pl",
        R.string.portuguese to "pt",
        R.string.russian to "ru",
        R.string.spanish to "es",
        R.string.swedish to "sv",
        R.string.turkish to "tr",
        R.string.ukrainian to "uk",
        R.string.vietnamese to "vi",
        R.string.chinese to "zh"
    )

    private val langToLanguageObject = mapOf(
        "nl" to Language.DUTCH,
        "en" to Language.ENGLISH,
        "fr" to Language.FRENCH,
        "de" to Language.GERMAN,
        "it" to Language.ITALIAN,
        "ja" to Language.JAPANESE,
        "ko" to Language.KOREAN,
        "pl" to Language.POLISH,
        "pt" to Language.PORTUGUESE,
        "ru" to Language.RUSSIAN,
        "es" to Language.SPANISH,
        "sv" to Language.SWEDISH,
        "tr" to Language.TURKISH,
        "uk" to Language.UKRAINIAN,
        "vi" to Language.VIETNAMESE,
        "zh" to Language.CHINESE
    )

    private fun stringResToLanguagecode(@StringRes stringRes: Int): String =
        stringResToLang[stringRes] ?: "en"

    private fun languagecodeToLanguageObject(languagecode: String): Language =
        langToLanguageObject[languagecode] ?: Language.ENGLISH
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