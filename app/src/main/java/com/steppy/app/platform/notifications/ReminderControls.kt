package com.steppy.app.platform.notifications

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.steppy.app.SteppyApplication
import com.steppy.app.domain.Reminder
import com.steppy.app.platform.debug.DebugFlags
import com.steppy.app.ui.SteppyViewModel

/**
 * What Profile needs for the reminder switch.
 *
 * @param notificationsAllowed false when Android blocks Steppy's notifications (denied or switched off)
 * @param onSendTestReminder null unless [DebugFlags.SHOW_TEST_REMINDER] is on
 */
class ReminderControls(
    val notificationsAllowed: Boolean,
    val onToggleReminders: (Boolean) -> Unit,
    val onAllowNotifications: () -> Unit,
    val onSendTestReminder: (() -> Unit)?
)

/**
 * Turning reminders on asks for POST_NOTIFICATIONS first when needed (Android 13+). If it is
 * denied, reminders stay off and the app works the same. Once the dialog can't be shown any
 * more (denied for good, or notifications switched off), Steppy's notification settings open.
 */
@Composable
fun rememberReminderControls(viewModel: SteppyViewModel): ReminderControls {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val preferences = (context.applicationContext as SteppyApplication).container.platformPreferences

    var allowed by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    // The user may come back from Settings with notifications switched on or off
    LifecycleResumeEffect(Unit) {
        allowed = NotificationPermission.isGranted(context)
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        preferences.setNotificationPermissionAsked()
        allowed = NotificationPermission.isGranted(context)
        if (granted) viewModel.toggleReminders(true) else viewModel.onNotificationsDenied()
    }

    val askOrOpenSettings: () -> Unit = {
        val canAsk = activity != null &&
            NotificationPermission.canAsk(activity, preferences.wasNotificationPermissionAsked())
        if (canAsk) {
            launcher.launch(NotificationPermission.PERMISSION)
        } else {
            NotificationPermission.openNotificationSettings(context)
        }
    }

    return ReminderControls(
        notificationsAllowed = allowed,
        onToggleReminders = { enabled ->
            when {
                !enabled -> viewModel.toggleReminders(false)
                allowed -> viewModel.toggleReminders(true)
                else -> askOrOpenSettings()
            }
        },
        onAllowNotifications = askOrOpenSettings,
        onSendTestReminder = if (DebugFlags.SHOW_TEST_REMINDER) {
            {
                val state = viewModel.uiState.value
                val text = Reminder.message(state.todaySteps, state.dailyGoal, state.currentStreak)
                    ?: "Today's goal is reached, so no reminder would be sent tonight."
                if (!ReminderNotifier.show(context, "(Test) $text")) askOrOpenSettings()
            }
        } else null
    )
}
