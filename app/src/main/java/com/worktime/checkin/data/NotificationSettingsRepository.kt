package com.worktime.checkin.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class NotificationSettings(
    val pushEnabled: Boolean = false,
    val startReminderMinutes: Int = 8 * 60 + 30,
    val endReminderMinutes: Int = 18 * 60
)

class NotificationSettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.appDataStore

    val settings: Flow<NotificationSettings> = dataStore.data.map { preferences ->
        NotificationSettings(
            pushEnabled = preferences[Keys.pushEnabled] ?: false,
            startReminderMinutes = preferences[Keys.startReminderMinutes] ?: (8 * 60 + 30),
            endReminderMinutes = preferences[Keys.endReminderMinutes] ?: (18 * 60)
        )
    }

    suspend fun setPushEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.pushEnabled] = enabled
        }
    }

    suspend fun setStartReminderMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.startReminderMinutes] = minutes
        }
    }

    suspend fun setEndReminderMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.endReminderMinutes] = minutes
        }
    }

    private object Keys {
        val pushEnabled = booleanPreferencesKey("notifications_push_enabled")
        val startReminderMinutes = intPreferencesKey("notifications_start_reminder_minutes")
        val endReminderMinutes = intPreferencesKey("notifications_end_reminder_minutes")
    }
}
