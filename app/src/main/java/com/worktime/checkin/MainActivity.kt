package com.worktime.checkin

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worktime.checkin.ui.navigation.AppNavHost
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.worktime.checkin.data.NotificationSettings
import com.worktime.checkin.data.NotificationSettingsRepository
import com.worktime.checkin.data.ThemeSettingsRepository
import com.worktime.checkin.ui.CheckInReminderScheduler
import com.worktime.checkin.ui.NotificationHelper
import com.worktime.checkin.ui.navigation.SettingsSubPage
import com.worktime.checkin.ui.theme.MyAppTheme
import com.worktime.checkin.ui.theme.ThemeMode
import com.worktime.checkin.ui.theme.ThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }
        NotificationHelper.ensureChannel(applicationContext)
        setContent {
            val context = LocalContext.current
            val themeRepository = remember { ThemeSettingsRepository(context.applicationContext) }
            val themeMode by themeRepository.mode.collectAsState(initial = ThemeMode.System)
            val notificationRepository = remember { NotificationSettingsRepository(context.applicationContext) }
            val notificationSettings by notificationRepository.settings.collectAsState(initial = NotificationSettings())
            SideEffect {
                ThemeState.mode = themeMode
            }
            LaunchedEffect(notificationSettings) {
                CheckInReminderScheduler.sync(context.applicationContext, notificationSettings)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }

            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            var settingsSubPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }

            Crossfade(
                targetState = darkTheme,
                animationSpec = tween(durationMillis = 400),
                label = "theme-transition"
            ) { isDark ->
                MyAppTheme(darkTheme = isDark) {
                    AppNavHost(
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            settingsSubPage = null
                        },
                        settingsSubPage = settingsSubPage,
                        onSettingsBack = { settingsSubPage = null },
                        onSettingsNavigate = { settingsSubPage = it }
                    )
                }
            }
        }
    }
}
