package com.talkbridge.livetranslator.ui.home

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

class HomeViewModel: ViewModel() {
    private val talkBridgeClient = TalkBridgeClient()
    private val audioRecorder = AudioRecorder()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    init {
        setupClientCallbacks()
    }

    private fun setupClientCallbacks() {
        talkBridgeClient.onReady = { handleServerReady() }
        talkBridgeClient.onError = { error -> handleError(error) }
        talkBridgeClient.onStop = {  }
        talkBridgeClient.ontranslationResponse = { text -> setCurrentText(text) }
    }

    private fun handleServerReady() {
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.CONNECTED
            )
        }
        startRecording()
    }

    private fun handleError(error: String){
        talkBridgeClient.disconnect()
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.FAILED
            )
        }
        stopRecording()
    }

    fun connectWithServer(){
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.CONNECTING
            )
        }
        talkBridgeClient.connect(
            sourceLang = "english", //zum testen
            targetLang = "german",
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
        if (homeUiState.value.connectionState == ConnectionState.CONNECTED){
            _homeUiState.update { currentState ->
                currentState.copy(
                    connectionState = ConnectionState.NOT_CONNECTED
                )
            }
        }
        audioRecorder.stopRecording()
        resetCurrentText()
    }

    fun resetConnectionState(){
        _homeUiState.update { currentState ->
            currentState.copy(
                connectionState = ConnectionState.CONNECTED
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
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val currentText: String? = null,
)

enum class ConnectionState {
    NOT_CONNECTED,
    CONNECTED,
    CONNECTING,
    FAILED
}