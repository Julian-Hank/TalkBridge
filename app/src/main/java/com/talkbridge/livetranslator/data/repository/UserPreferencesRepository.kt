package com.talkbridge.livetranslator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.talkbridge.livetranslator.data.LanguageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        const val TAG = "UserPreferencesRepo"
        val CURRENT_SOURCE_LANGUAGE = stringPreferencesKey("current_source_language")
        val CURRENT_TARGET_LANGUAGE = stringPreferencesKey("current_target_language")
        val RECENT_LANGUAGES = stringPreferencesKey("recent_languages")
    }

    val currentSourceLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[CURRENT_SOURCE_LANGUAGE] ?: "en"
            }

    val currentTargetLanguage: Flow<String> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[CURRENT_TARGET_LANGUAGE] ?: "de"
            }

    val recentLanguages: Flow<List<String>> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[RECENT_LANGUAGES]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }


    suspend fun saveCurrentLanguages(
        sourceLanguageCode: String,
        targetLanguageCode: String
    ) {
        dataStore.edit { preferences ->
            preferences[CURRENT_SOURCE_LANGUAGE] = sourceLanguageCode
            preferences[CURRENT_TARGET_LANGUAGE] = targetLanguageCode
        }
    }

    suspend fun addRecentLanguage(newLanguageCode: String) {
        dataStore.edit { preferences ->

            val current =
                preferences[RECENT_LANGUAGES]
                    ?.split(",")
                    ?.toMutableList()
                    ?: mutableListOf()

            current.remove(newLanguageCode)
            current.add(0, newLanguageCode)

            val limited = current.take(4)

            preferences[RECENT_LANGUAGES] =
                limited.joinToString(",")
        }
    }

}