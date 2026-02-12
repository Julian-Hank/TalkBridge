package com.talkbridge.livetranslator.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        const val TAG = "UserPreferencesRepo"
    }
}