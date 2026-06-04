package com.worktime.checkin.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.worktime.checkin.MainActivity
import com.worktime.checkin.R

object NotificationHelper {
    private const val CHANNEL_ID = "check_in_reminders"
    private const val CHANNEL_NAME = "打卡提醒"
    private const val START_NOTIFICATION_ID = 1001
    private const val END_NOTIFICATION_ID = 1002
    @Volatile
    private var channelReady = false

    fun sendCheckInReminder(context: Context, reminderType: ReminderType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            reminderType.notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminderType.title)
            .setContentText(reminderType.message)
            .setContentIntent(contentIntent)
            .setColor(Color.rgb(26, 113, 246))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(reminderType.notificationId, notification)
    }

    fun ensureChannel(context: Context) {
        if (channelReady) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "上班和下班打卡提醒"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        channelReady = true
    }

    enum class ReminderType(
        val actionValue: String,
        val title: String,
        val message: String,
        val notificationId: Int
    ) {
        Start(
            actionValue = "start",
            title = "上班打卡提醒",
            message = "到上班时间了，记得完成打卡。",
            notificationId = START_NOTIFICATION_ID
        ),
        End(
            actionValue = "end",
            title = "下班打卡提醒",
            message = "到下班时间了，记得完成打卡。",
            notificationId = END_NOTIFICATION_ID
        );

        companion object {
            fun fromActionValue(value: String?): ReminderType? {
                return entries.firstOrNull { it.actionValue == value }
            }
        }
    }
}
