package com.steppy.app.platform.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.steppy.app.MainActivity
import com.steppy.app.R

/** Shows the daily reminder notification. */
object ReminderNotifier {

    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1

    /** Safe to call every time the app starts; Android keeps the user's channel settings. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "An evening nudge when today's step goal isn't reached yet"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Shows [text]; tapping it opens Today. Returns false (and shows nothing) when
     * notifications aren't allowed: the app works the same without them.
     */
    @SuppressLint("MissingPermission") // Checked by NotificationPermission.isGranted
    fun show(context: Context, text: String): Boolean {
        if (!NotificationPermission.isGranted(context)) return false

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for a walk?")
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }
}
