package com.talkbridge.livetranslator.ui.translate

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkbridge.livetranslator.R
import com.talkbridge.livetranslator.data.Language
import com.talkbridge.livetranslator.data.LanguageData
import com.talkbridge.livetranslator.data.LanguageDataSource.languagesMap
import com.talkbridge.livetranslator.data.TalkBridgeClient
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository
import com.talkbridge.livetranslator.ui.home.HomeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TranslateViewModel(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository
): AndroidViewModel(application) {
    private val context = getApplication<Application>()


    private val _translateUiState = MutableStateFlow(TranslateUiState())
    val translateUiState: StateFlow<TranslateUiState> = _translateUiState.asStateFlow()

    private val talkBridgeClient = TalkBridgeClient(context)

    private lateinit var customIPAddress: String

    init{
        observePreferences()
        setupClientCallbacks()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.costumIPAdress.collect {
                customIPAddress = it
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
            if (customIPAddress == ""){
                talkBridgeClient.sendTextForTranslation(
                    text = text,
                    sourceLang = stringResToLanguagecode(translateUiState.value.sourceLanguage.languageName),
                    targetLang = stringResToLanguagecode(translateUiState.value.targetLanguage.languageName),
                )
            } else {
                talkBridgeClient.sendTextForTranslation(
                    text = text,
                    sourceLang = stringResToLanguagecode(translateUiState.value.sourceLanguage.languageName),
                    targetLang = stringResToLanguagecode(translateUiState.value.targetLanguage.languageName),
                    ipAddress = customIPAddress
                )
            }
        }
    }

    fun handleTranslationResponse(translation: String){
        _translateUiState.update { uiState ->
            uiState.copy(
                targetLanguageText = translation
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

data class TranslateUiState(
    val sourceLanguage: LanguageData = LanguageData(R.string.english, R.drawable.uk_flag_circular),
    val targetLanguage: LanguageData = LanguageData(R.string.german, R.drawable.germany_flag_circular),
    val sourceLanguageText: String? = null,
    val targetLanguageText: String? = null,
)