package com.worktime.checkin.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

object ThemeState {
    var mode by mutableStateOf(ThemeMode.System)
}
