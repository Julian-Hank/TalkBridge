package com.talkbridge.livetranslator.ui.transcribe

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.currentComposer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.audio.AudioRecorder
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.sql.Time
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG: String = "TranscribeViewModel"

class TranscribeViewModel(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository
): AndroidViewModel(application) {
    private val context = getApplication<Application>()

    private val talkBridgeClient = TalkBridgeClient(context)
    private val audioRecorder = AudioRecorder()

    private val _transcribeUiState = MutableStateFlow(TranscribeUiState())
    val transcribeUiState: StateFlow<TranscribeUiState> = _transcribeUiState.asStateFlow()

    private val audioBuffer = mutableListOf<ByteArray>()
    private var recordingStartedTime: Long = 0L

    private var progressIncrements: Float = 0f

    private lateinit var customIPAddress: String

    init {
        setupClientCallbacks()
        observePreferences()
    }


    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.costumIPAdress.collect {
                customIPAddress = it
            }
        }
    }

    private fun setupClientCallbacks() {
        talkBridgeClient.onTranscriptionResponse = { response -> handleTranscriptionResponse(response) }
        talkBridgeClient.onEstimatedTimeResult = { time -> handleEstimatedTimeResult(time) }
        talkBridgeClient.onTranscriptionError = { handleServerError() }
    }

    private fun handleServerError(){
        _transcribeUiState.update { uiState ->
            uiState.copy(
                transcriptionState = TranscriptionState.INACTIVE
            )
        }
    }

    private fun handleTranscriptionResponse(result: String){
        Log.d(TAG, "Transcript: $result")
        _transcribeUiState.update { uiState ->
            uiState.copy(
                transcriptionProgress = 1f,
                transcriptionState = TranscriptionState.FINISHED,
                TEMP = "Temporäre anzeige\n$result"
            )
        }
    }

    fun handleEstimatedTimeResult(time: Int){
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
    fun setTimeLeft(timeLeft: Int){
        _transcribeUiState.update { uiState ->
            uiState.copy(
                timeLeft = timeLeft
            )
        }
        progressIncrements = 1f / timeLeft
        Log.d(TAG, progressIncrements.toString())
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
        if (customIPAddress == ""){
            talkBridgeClient.sendAudioForTranscript(
                audioData = finalAudio,
                lang = if (transcribeUiState.value.autoDetectLanguage) "auto" else stringResToLanguagecode(transcribeUiState.value.selectedLanguage.languageName)
            )
        } else{
            talkBridgeClient.sendAudioForTranscript(
                ipAddress = customIPAddress,
                audioData = finalAudio,
                lang = if (transcribeUiState.value.autoDetectLanguage) "auto" else stringResToLanguagecode(transcribeUiState.value.selectedLanguage.languageName)
            )
        }
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
        _transcribeUiState.update { currentState ->
            currentState.copy(
                selectedLanguage = language
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

    private fun stringResToLanguagecode(@StringRes stringRes: Int): String =
        stringResToLang[stringRes] ?: "en"
}

data class TranscribeUiState(
    val transcriptionState: TranscriptionState = TranscriptionState.INACTIVE,
    val timeRecorded: Int = 0,
    val autoDetectLanguage: Boolean = false,
    val selectedLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val transcriptionProgress: Float = 0f,
    val timeLeft: Int = 0,
    val TEMP: String = ""
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