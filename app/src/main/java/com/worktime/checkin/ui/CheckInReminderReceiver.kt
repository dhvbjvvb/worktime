package com.worktime.checkin.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.worktime.checkin.data.NotificationSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CheckInReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CheckInReminderScheduler.ACTION_CHECK_IN_REMINDER) {
            val reminderType = NotificationHelper.ReminderType.fromActionValue(
                intent.getStringExtra(CheckInReminderScheduler.EXTRA_REMINDER_TYPE)
            ) ?: return

            NotificationHelper.sendCheckInReminder(context, reminderType)

            val nextMinutes = intent.getIntExtra(CheckInReminderScheduler.EXTRA_REMINDER_MINUTES, -1)
            if (nextMinutes >= 0) {
                CheckInReminderScheduler.scheduleNext(context, reminderType, nextMinutes)
            } else {
                rescheduleFromSettings(context, reminderType)
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NotificationSettingsRepository(context.applicationContext)
                val settings = repository.settings.first()

                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    CheckInReminderScheduler.sync(context, settings)
                    return@launch
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleFromSettings(
        context: Context,
        reminderType: NotificationHelper.ReminderType
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = NotificationSettingsRepository(context.applicationContext)
                val settings = repository.settings.first()
                if (!settings.pushEnabled) return@launch

                val nextMinutes = when (reminderType) {
                    NotificationHelper.ReminderType.Start -> settings.startReminderMinutes
                    NotificationHelper.ReminderType.End -> settings.endReminderMinutes
                }
                CheckInReminderScheduler.scheduleNext(context, reminderType, nextMinutes)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
