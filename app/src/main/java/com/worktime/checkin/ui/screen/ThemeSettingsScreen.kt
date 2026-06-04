package com.worktime.checkin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktime.checkin.data.ThemeSettingsRepository
import com.worktime.checkin.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private data class ThemeOption(
    val title: String,
    val description: String = "",
    val mode: ThemeMode,
    val tint: Color
)

private val themeOptions = listOf(
    ThemeOption("跟随系统", "自动在浅色与深色间切换", ThemeMode.System, Color(0xFF90909A)),
    ThemeOption("浅色模式", "始终使用浅色外观", ThemeMode.Light, Color(0xFFC08A3E)),
    ThemeOption("深色模式", "始终使用深色外观", ThemeMode.Dark, Color(0xFF4A5BC6))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ThemeSettingsRepository(context.applicationContext) }
    val selectedMode by repository.mode.collectAsState(initial = ThemeMode.System)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "\u4E3B\u9898\u8BBE\u7F6E",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        BackHandler(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column {
                    themeOptions.forEachIndexed { index, option ->
                        ThemeOptionRow(
                            option = option,
                            selected = selectedMode == option.mode,
                            onClick = {
                                scope.launch { repository.setMode(option.mode) }
                            }
                        )
                        if (index < themeOptions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(option.tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(option.tint)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            if (option.description.isNotEmpty()) {
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

