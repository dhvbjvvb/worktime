package com.worktime.checkin.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.NumberPicker
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.worktime.checkin.data.NotificationSettings
import com.worktime.checkin.data.NotificationSettingsRepository
import com.worktime.checkin.ui.CheckInReminderScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember { NotificationSettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = NotificationSettings())
    val scope = rememberCoroutineScope()
    var timePickerRequest by remember { mutableStateOf<TimePickerRequest?>(null) }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun saveAndSync(nextSettings: NotificationSettings) {
        CheckInReminderScheduler.sync(context, nextSettings)
    }

    fun setPushEnabled(enabled: Boolean) {
        if (enabled && !hasNotificationPermission()) {
            return
        }
        scope.launch {
            repository.setPushEnabled(enabled)
            saveAndSync(settings.copy(pushEnabled = enabled))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setPushEnabled(true)
        }
    }

    fun requestEnableNotifications() {
        if (hasNotificationPermission()) {
            setPushEnabled(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "消息通知",
                onBack = onBack
            )
        }
    ) { innerPadding ->
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
                    ReminderSwitchRow(
                        checked = settings.pushEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) requestEnableNotifications() else setPushEnabled(false)
                        }
                    )

                    SettingsDivider()

                    ReminderTimeRow(
                        title = "上班打卡提醒",
                        timeText = formatReminderTime(settings.startReminderMinutes),
                        enabled = settings.pushEnabled,
                        onClick = {
                            timePickerRequest = TimePickerRequest(
                                title = "上班打卡提醒",
                                initialMinutes = settings.startReminderMinutes,
                                onConfirm = { minutes ->
                                    scope.launch {
                                        repository.setStartReminderMinutes(minutes)
                                        saveAndSync(settings.copy(startReminderMinutes = minutes))
                                    }
                                }
                            )
                        }
                    )

                    SettingsDivider()

                    ReminderTimeRow(
                        title = "下班打卡提醒",
                        timeText = formatReminderTime(settings.endReminderMinutes),
                        enabled = settings.pushEnabled,
                        onClick = {
                            timePickerRequest = TimePickerRequest(
                                title = "下班打卡提醒",
                                initialMinutes = settings.endReminderMinutes,
                                onConfirm = { minutes ->
                                    scope.launch {
                                        repository.setEndReminderMinutes(minutes)
                                        saveAndSync(settings.copy(endReminderMinutes = minutes))
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    timePickerRequest?.let { request ->
        WheelTimePickerDialog(
            request = request,
            onDismiss = { timePickerRequest = null }
        )
    }
}

@Composable
private fun ReminderSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "打卡提醒",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (checked) "已开启，到点会提醒打卡" else "已关闭",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ReminderTimeRow(
    title: String,
    timeText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (enabled) "每天 $timeText" else "开启打卡提醒后生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WheelTimePickerDialog(
    request: TimePickerRequest,
    onDismiss: () -> Unit
) {
    var selectedHour by remember(request.initialMinutes) {
        mutableIntStateOf(request.initialMinutes / 60)
    }
    var selectedMinute by remember(request.initialMinutes) {
        mutableIntStateOf(request.initialMinutes % 60)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelNumberPicker(
                    value = selectedHour,
                    range = 0..23,
                    formatter = { it.toString().padStart(2, '0') },
                    onValueChange = { selectedHour = it }
                )
                Text(
                    text = ":",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                WheelNumberPicker(
                    value = selectedMinute,
                    range = 0..59,
                    formatter = { it.toString().padStart(2, '0') },
                    onValueChange = { selectedMinute = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    request.onConfirm(selectedHour * 60 + selectedMinute)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    formatter: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    val displayedValues = remember(range) {
        range.map(formatter).toTypedArray()
    }
    AndroidView(
        modifier = Modifier
            .width(82.dp)
            .height(160.dp),
        factory = { viewContext ->
            NumberPicker(viewContext).apply {
                isHapticFeedbackEnabled = true
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = true
                minValue = range.first
                maxValue = range.last
                this.displayedValues = displayedValues
                this.value = value
                setOnValueChangedListener { _, _, newValue ->
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onValueChange(newValue)
                }
            }
        },
        update = { picker ->
            picker.minValue = range.first
            picker.maxValue = range.last
            picker.displayedValues = displayedValues
            if (picker.value != value) picker.value = value
        }
    )
}

private data class TimePickerRequest(
    val title: String,
    val initialMinutes: Int,
    val onConfirm: (Int) -> Unit
)

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun formatReminderTime(minutesOfDay: Int): String {
    val hour = minutesOfDay / 60
    val minute = minutesOfDay % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
