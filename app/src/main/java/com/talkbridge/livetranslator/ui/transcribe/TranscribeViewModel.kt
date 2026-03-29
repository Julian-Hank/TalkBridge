package com.talkbridge.livetranslator.ui.transcribe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.ClientEvent
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import com.talkbridge.livetranslator.data.languagecodeToLanguageObject
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.repository.PreferenceKeys
import com.talkbridge.livetranslator.data.repository.TranscriptionItemsRepository
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import com.talkbridge.livetranslator.data.stringResToLanguagecode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG: String = "TranscribeViewModel"

class TranscribeViewModel(
    private val talkBridgeClient: TalkBridgeClient,
    private val transcriptionItemsRepository: TranscriptionItemsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
): ViewModel() {

    private val audioRecorder = AudioRecorder()

    private val _transcribeUiState = MutableStateFlow(TranscribeUiState())
    val transcribeUiState: StateFlow<TranscribeUiState> = _transcribeUiState.asStateFlow()

    private val audioBuffer = mutableListOf<ByteArray>()
    private var recordingStartedTime: Long = 0L

    private var progressIncrements: Float = 0f

    init {
        observePreferences()
        observeClientEvents()
    }


    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.currentTranscribeLanguage
                .combine(userPreferencesRepository.recentTranscribeLanguages) { selected, recent ->
                    selected to recent
                }
                .collect { (selected, recent) ->

                    val recentLanguages = recent.map { languagecode ->
                        languagesMap.getValue(languagecodeToLanguageObject(languagecode))
                    }

                    _transcribeUiState.update { currentState ->
                        currentState.copy(
                            selectedLanguage = languagesMap.getValue(languagecodeToLanguageObject(selected)),
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
                    is ClientEvent.TranscriptionResult -> handleTranscriptionResponse(event.text)
                    is ClientEvent.EstimatedTime       -> handleEstimatedTimeResult(event.seconds)
                    is ClientEvent.TranscriptionError  -> handleServerError()
                    else -> {}
                }
            }
        }
    }

    private fun handleServerError(){
        resetUiState()
    }

    private fun handleTranscriptionResponse(result: String){
        Log.d(TAG, "Transcript: $result")
        viewModelScope.launch {
            val id = transcriptionItemsRepository.insertItem(
                TranscriptionItem(
                    content = result,
                    date = LocalDate.now(),
                    title = "Transcription"
                )
            )

            _transcribeUiState.update { uiState ->
                uiState.copy(
                    transcriptionProgress = 1f,
                    transcriptionState = TranscriptionState.FINISHED,
                    createdItemId = id
                )
            }
        }
    }

    fun resetUiState(){
        _transcribeUiState.update { uiState ->
            uiState.copy(
                transcriptionState = TranscriptionState.INACTIVE,
                timeRecorded = 0,
                autoDetectLanguage = false,
                transcriptionProgress  = 0f,
                timeLeft = 0,
                createdItemId = 0L
            )
        }
    }

    private fun handleEstimatedTimeResult(time: Int){
        Log.d(TAG, "Geschätzte Zeit: ${time}s")
        _transcribeUiState.update { uiState ->
            uiState.copy(
                transcriptionState = TranscriptionState.TRANSCRIBING
            )
        }
        setTimeLeft(time)
        viewModelScope.launch {
            while (transcribeUiState.value.transcriptionState != TranscriptionState.FINISHED){
                updateProgress()
                delay(1000)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun setTimeLeft(timeLeft: Int){
        _transcribeUiState.update { uiState ->
            uiState.copy(
                timeLeft = timeLeft
            )
        }
        progressIncrements = 1f / timeLeft
    }


    private fun updateProgress() {
        if (transcribeUiState.value.timeLeft > 0){
            _transcribeUiState.update { uiState ->
                uiState.copy(
                    timeLeft = transcribeUiState.value.timeLeft - 1,
                    transcriptionProgress = transcribeUiState.value.transcriptionProgress + progressIncrements
                )
            }
        }
    }

    fun sendRecording(){
        val finalAudio = if (audioBuffer.isNotEmpty()) audioBuffer.reduce { acc, bytes -> acc + bytes } else return
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.CONNECTING
            )
        }
        recordingStartedTime = 0L
        pendingChunks.clear()
        _waveAmplitudes.update { emptyList() }
        audioBuffer.clear()
        talkBridgeClient.sendAudioForTranscript(
            audioData = finalAudio,
            lang = if (transcribeUiState.value.autoDetectLanguage) "auto" else stringResToLanguagecode(transcribeUiState.value.selectedLanguage.languageName)
        )
    }

    fun deleteRecording(){
        audioBuffer.clear()
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.INACTIVE
            )
        }
        recordingStartedTime = 0L
        pendingChunks.clear()
        _waveAmplitudes.update { emptyList() }
    }

    fun pauseRecording(){
        audioRecorder.stopRecording()
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.PAUSED
            )
        }
    }

    fun resumeRecording(){
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.RECORDING
            )
        }
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                try {
                    storeAudioData(audioData)
                    updateTimeRecorded()
                } catch (e: Exception) {
                    Log.e("Transcribe", e.toString())
                }
            }
        }
    }

    fun stopRecording(){
        audioRecorder.stopRecording()
        chunkCounter = 0
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.STOPPED
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun startRecording(){
        recordingStartedTime = Clock.System.now().epochSeconds
        _transcribeUiState.update { currentState  ->
            currentState.copy(
                transcriptionState = TranscriptionState.RECORDING
            )
        }
        viewModelScope.launch {
            audioRecorder.startRecording { audioData ->
                try {
                    storeAudioData(audioData)
                    updateTimeRecorded()
                } catch (e: Exception) {
                    Log.e("Transcribe", e.toString())
                }
            }
        }
    }

    fun updateSourceLanguage(language: LanguageData){
        viewModelScope.launch {
            userPreferencesRepository.saveTranscribeLanguage(stringResToLanguagecode(language.languageName))
            userPreferencesRepository.addRecentLanguage(
                newLanguageCode = stringResToLanguagecode(language.languageName),
                key = PreferenceKeys.RECENT_TRANSCRIBE_LANGUAGES
            )
        }
    }

    fun setAutoDetectLanguage(autoDetect: Boolean){
        _transcribeUiState.update { currentState ->
            currentState.copy(
                autoDetectLanguage = autoDetect
            )
        }
    }

    private val MAX_BARS = 50

    private val _waveAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val waveAmplitudes: StateFlow<List<Float>> = _waveAmplitudes.asStateFlow()

    private val CHUNKS_PER_BAR = 4
    private var chunkCounter = 0
    private val pendingChunks = mutableListOf<ByteArray>()

    private fun generateWaveAmplitudes(audioData: ByteArray) {
        pendingChunks.add(audioData)
        chunkCounter++

        if (chunkCounter < CHUNKS_PER_BAR) return

        // Genug Chunks gesammelt -> einen Balken berechnen
        val combined = pendingChunks.reduce { acc, bytes -> acc + bytes }
        pendingChunks.clear()
        chunkCounter = 0

        val samples = ShortArray(combined.size / 2)
        ByteBuffer.wrap(combined)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(samples)

        val rms = samples
            .map { it.toFloat() }
            .let { list ->
                val mean = list.sumOf { (it * it).toDouble() } / list.size
                kotlin.math.sqrt(mean).toFloat()
            }

        // Normalisieren + verstärken damit die Balken größer wirken
        val normalized = (rms / Short.MAX_VALUE * 6f).coerceIn(0f, 1f)

        _waveAmplitudes.update { current ->
            (current + normalized).takeLast(MAX_BARS)
        }
    }

    private fun storeAudioData(audioData: ByteArray) {
        audioBuffer.add(audioData)
        generateWaveAmplitudes(audioData)
    }

    @OptIn(ExperimentalTime::class)
    private fun updateTimeRecorded(){
        val currentTime: Long = Clock.System.now().epochSeconds
        _transcribeUiState.update { currentState ->
            currentState.copy(
                timeRecorded = (currentTime - recordingStartedTime).toInt()
            )
        }
    }
}

data class TranscribeUiState(
    val transcriptionState: TranscriptionState = TranscriptionState.INACTIVE,
    val timeRecorded: Int = 0,
    val autoDetectLanguage: Boolean = false,
    val selectedLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val transcriptionProgress: Float = 0f,
    val timeLeft: Int = 0,
    val createdItemId: Long = 0L,
    val recentLanguages: List<LanguageData>? = null
)

enum class TranscriptionState {
    INACTIVE,
    RECORDING,
    PAUSED,
    STOPPED,
    CONNECTING,
    TRANSCRIBING,
    FINISHED
}