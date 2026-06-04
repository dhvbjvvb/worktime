package com.worktime.checkin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.worktime.checkin.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeSettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.appDataStore

    val mode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[Keys.mode]?.let { saved ->
            ThemeMode.entries.firstOrNull { it.name == saved }
        } ?: ThemeMode.System
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.mode] = mode.name
        }
    }

    private object Keys {
        val mode = stringPreferencesKey("theme_mode")
    }
}
