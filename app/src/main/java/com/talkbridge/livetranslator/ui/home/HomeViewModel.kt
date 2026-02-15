package com.talkbridge.livetranslator.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG: String = "HomeViewModel"

class HomeViewModel(): ViewModel() {
    private val talkBridgeClient = TalkBridgeClient()
    private val audioRecorder = AudioRecorder()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    init {
        setupClientCallbacks()
    }

    private fun setupClientCallbacks() {
        talkBridgeClient.onReady = { handleServerReady() }
        talkBridgeClient.onConnected = { handleServerConnected() }
        talkBridgeClient.onError = { error -> handleError(error) }
        talkBridgeClient.ontranslationResponse = { text -> setCurrentText(text) }
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
        talkBridgeClient.connect(
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
            _homeUiState.update { currentState ->
                currentState.copy(
                    sourceLanguage = language
                )
            }
        }
    }

    fun updateTargetLanguage(language: LanguageData){
        val sourceLanguage = homeUiState.value.sourceLanguage

        if (language == sourceLanguage){
            swapLanguages()
        } else {
            _homeUiState.update { currentState ->
                currentState.copy(
                    targetLanguage = language
                )
            }
        }
    }

    fun swapLanguages(){
        val sourceLanguage = homeUiState.value.sourceLanguage
        val targetLanguage = homeUiState.value.targetLanguage

        _homeUiState.update { currentState ->
            currentState.copy(
                sourceLanguage = targetLanguage,
                targetLanguage = sourceLanguage
            )
        }
    }

    private val stringResToLang = mapOf(
        R.string.chinese to "cn",
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
        R.string.vietnamese to "vi"
    )


    private fun stringResToLanguagecode(@StringRes stringRes: Int): String =
        stringResToLang[stringRes] ?: "en"
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val currentText: String? = null,
)

enum class ConnectionState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    READY,
    FAILED
}