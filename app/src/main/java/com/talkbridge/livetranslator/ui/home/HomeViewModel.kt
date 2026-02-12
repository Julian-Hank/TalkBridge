package com.talkbridge.livetranslator.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.audio.AudioOutputManager
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
//    private lateinit var audioOutputManager: AudioOutputManager
//    private lateinit var audioRecorder: AudioRecorder

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val audioRecorder = AudioRecorder()

    fun startRecording() {
        if (!homeUiState.value.active){
            _homeUiState.update { currentState ->
                currentState.copy(
                    active = true
                )
            }
        }
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                try {
                    // TODO audioData an Backend senden
                    val max = audioData.maxOrNull() ?: 0
                    val min = audioData.minOrNull() ?: 0
    //                Log.i("HomeViewModel", audioData.toString())
                    Log.i("HomeViewModel", "Audio Data - Size: ${audioData.size}, Max: $max, Min: $min")
                } catch (e: Exception) {
                    TODO("Not yet implemented")
                }
            }
        }
    }

    fun stopRecording() {
        if (homeUiState.value.active){
            _homeUiState.update { currentState ->
                currentState.copy(
                    active = false
                )
            }
        }
        audioRecorder.stopRecording()
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }

    fun setOriginLanguage(
        language: LanguageData
    ){
        _homeUiState.update { currentState ->
            currentState.copy(
                sourceLanguage = language
            )
        }
    }

    fun setTargetLanguage(
        language: LanguageData
    ){
        _homeUiState.update { currentState ->
            currentState.copy(
                targetLanguage = language
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
    val active: Boolean = false,
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val currentText: String? = null
)
