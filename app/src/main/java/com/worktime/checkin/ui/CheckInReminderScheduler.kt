package com.worktime.checkin.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.worktime.checkin.data.NotificationSettings
import java.util.Calendar

object CheckInReminderScheduler {
    const val ACTION_CHECK_IN_REMINDER = "com.worktime.checkin.action.CHECK_IN_REMINDER"
    const val EXTRA_REMINDER_TYPE = "reminder_type"
    const val EXTRA_REMINDER_MINUTES = "reminder_minutes"

    private const val START_REQUEST_CODE = 2101
    private const val END_REQUEST_CODE = 2102

    fun sync(context: Context, settings: NotificationSettings) {
        val appContext = context.applicationContext
        if (settings.pushEnabled) {
            scheduleNext(appContext, NotificationHelper.ReminderType.Start, settings.startReminderMinutes)
            scheduleNext(appContext, NotificationHelper.ReminderType.End, settings.endReminderMinutes)
        } else {
            cancelAll(appContext)
        }
    }

    fun scheduleNext(
        context: Context,
        reminderType: NotificationHelper.ReminderType,
        minutesOfDay: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = nextTriggerAtMillis(minutesOfDay)
        val pendingIntent = reminderPendingIntent(context, reminderType, minutesOfDay)
        alarmManager.cancel(pendingIntent)

        if (canScheduleExactAlarm(alarmManager)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Fall through to an inexact alarm instead of crashing the app.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        NotificationHelper.ReminderType.entries.forEach { type ->
            alarmManager.cancel(reminderPendingIntent(context, type))
        }
    }

    private fun nextTriggerAtMillis(minutesOfDay: Int): Long {
        val hour = minutesOfDay / 60
        val minute = minutesOfDay % 60
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }

    private fun canScheduleExactAlarm(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun reminderPendingIntent(
        context: Context,
        reminderType: NotificationHelper.ReminderType,
        minutesOfDay: Int? = null
    ): PendingIntent {
        val requestCode = when (reminderType) {
            NotificationHelper.ReminderType.Start -> START_REQUEST_CODE
            NotificationHelper.ReminderType.End -> END_REQUEST_CODE
        }
        val intent = Intent(context, CheckInReminderReceiver::class.java).apply {
            action = ACTION_CHECK_IN_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, reminderType.actionValue)
            if (minutesOfDay != null) {
                putExtra(EXTRA_REMINDER_MINUTES, minutesOfDay)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
