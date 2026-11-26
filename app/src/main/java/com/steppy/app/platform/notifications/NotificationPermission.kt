package com.steppy.app.platform.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** POST_NOTIFICATIONS (Android 13+) and the user's notification switch for Steppy. */
object NotificationPermission {

    /** Only a runtime permission from Android 13 (API 33); before that notifications are on by default. */
    val needsRuntimeRequest: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Only a string; it is only requested or checked on Android 13+ ([needsRuntimeRequest]). */
    @SuppressLint("InlinedApi")
    const val PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

    /** True when Steppy may show notifications: permission granted and not turned off in Settings. */
    fun isGranted(context: Context): Boolean {
        if (needsRuntimeRequest &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * True when the system dialog can be shown: Android 13+ and the user hasn't denied it for good.
     * Otherwise the only way is Steppy's notification settings.
     */
    fun canAsk(activity: Activity, alreadyAsked: Boolean): Boolean {
        if (!needsRuntimeRequest) return false
        if (ContextCompat.checkSelfPermission(activity, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Granted, but switched off in Settings: the dialog won't help
            return false
        }
        return !alreadyAsked || ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
