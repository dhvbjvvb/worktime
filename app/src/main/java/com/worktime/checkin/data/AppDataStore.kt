package com.worktime.checkin.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(name = "app_settings")
