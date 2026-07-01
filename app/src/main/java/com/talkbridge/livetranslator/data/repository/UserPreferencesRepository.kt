package com.talkbridge.livetranslator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    private companion object {
        const val TAG = "UserPreferencesRepo"
    }

    val customIPAddress: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CUSTOM_IP_ADDRESS] ?: ""
            }


    val currentLiveSourceLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CURRENT_LIVE_SOURCE_LANGUAGE] ?: "en"
            }

    val currentLiveTargetLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CURRENT_LIVE_TARGET_LANGUAGE] ?: "de"
            }

    val recentLiveLanguages: Flow<List<String>> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.RECENT_LIVE_LANGUAGES]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }

    val currentTranslateSourceLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CURRENT_TRANSLATE_SOURCE_LANGUAGE] ?: "en"
            }

    val currentTranslateTargetLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CURRENT_TRANSLATE_TARGET_LANGUAGE] ?: "de"
            }

    val recentTranslateLanguages: Flow<List<String>> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.RECENT_TRANSLATE_LANGUAGES]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }

    val currentTranscribeLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.CURRENT_TRANSCRIBE_LANGUAGE] ?: "de"
            }

    val recentTranscribeLanguages: Flow<List<String>> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.RECENT_TRANSCRIBE_LANGUAGES]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }

    val useBetterTranslation: Flow<Boolean> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.USE_BETTER_TRANSLATION] ?: true
            }

    val sendTranslatedText: Flow<Boolean> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.SEND_TRANSLATED_TEXT] ?: false
            }

    val stopOnAppClose: Flow<Boolean> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.STOP_ON_APP_CLOSE] ?: false
            }

    suspend fun saveCustomIP(
        ip: String
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CUSTOM_IP_ADDRESS] = ip
        }
    }

    suspend fun setUseBetterTranslation(
        value: Boolean
    ){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_BETTER_TRANSLATION] = value
        }
    }

    suspend fun setSendTranslatedText(
        value: Boolean
    ){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SEND_TRANSLATED_TEXT] = value
        }
    }

    suspend fun setStopOnAppClose(
        value: Boolean
    ){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.STOP_ON_APP_CLOSE] = value
        }
    }

    suspend fun saveCurrentLanguages(
        sourceLanguageKey: Preferences.Key<String>,
        targetLanguageKey: Preferences.Key<String>,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ) {
        dataStore.edit { preferences ->
            preferences[sourceLanguageKey] = sourceLanguageCode
            preferences[targetLanguageKey] = targetLanguageCode
        }
    }

    suspend fun saveTranscribeLanguage(
        language: String
    ){
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CURRENT_TRANSCRIBE_LANGUAGE] = language
        }
    }

    suspend fun addRecentLanguage(
        key: Preferences.Key<String>,
        newLanguageCode: String
    ) {
        dataStore.edit { preferences ->

            val current =
                preferences[key]
                    ?.split(",")
                    ?.toMutableList()
                    ?: mutableListOf()

            current.remove(newLanguageCode)
            current.add(0, newLanguageCode)

            val limited = current.take(4)

            preferences[key] = limited.joinToString(",")
        }
    }

}

object PreferenceKeys {
    val CURRENT_LIVE_SOURCE_LANGUAGE = stringPreferencesKey("current_live_source_language")
    val CURRENT_LIVE_TARGET_LANGUAGE = stringPreferencesKey("current_live_target_language")
    val RECENT_LIVE_LANGUAGES = stringPreferencesKey("recent_live_languages")

    val CURRENT_TRANSLATE_SOURCE_LANGUAGE = stringPreferencesKey("current_translate_source_language")
    val CURRENT_TRANSLATE_TARGET_LANGUAGE = stringPreferencesKey("current_translate_target_language")
    val RECENT_TRANSLATE_LANGUAGES = stringPreferencesKey("recent_translate_languages")

    val CURRENT_TRANSCRIBE_LANGUAGE = stringPreferencesKey("current_transcribe_language")
    val RECENT_TRANSCRIBE_LANGUAGES = stringPreferencesKey("recent_transcribe_languages")

    val CUSTOM_IP_ADDRESS = stringPreferencesKey("custom_ip_address")

    val USE_BETTER_TRANSLATION = booleanPreferencesKey("use_better_translation")
    val SEND_TRANSLATED_TEXT = booleanPreferencesKey("send_translated_text") //to esp32, default = false -> send original (transcribed) text

    val STOP_ON_APP_CLOSE = booleanPreferencesKey("stop_on_app_close")
}
