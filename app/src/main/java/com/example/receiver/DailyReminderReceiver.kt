package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.CatatKasApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule reminder on device reboot
            val app = context.applicationContext as? CatatKasApplication
            app?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = it.preferencesRepository.userPreferences.first()
                    if (prefs.reminderEnabled) {
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            hour = prefs.reminderHour,
                            minute = prefs.reminderMinute
                        )
                    }
                }
            }
            return
        }

        showReminderNotification(context)
        // Reschedule next day's alarm
        val app = context.applicationContext as? CatatKasApplication
        app?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = it.preferencesRepository.userPreferences.first()
                if (prefs.reminderEnabled) {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "catatkas_daily_reminder"
        const val NOTIFICATION_ID = 1001

        fun showReminderNotification(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.pengingat_harian),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pengingat harian untuk mencatat keuangan pribadi di CatatKas"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.pengingat_notif_title))
                .setContentText(context.getString(R.string.pengingat_notif_body))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.pengingat_notif_body))
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
