package com.steppy.app.platform.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.steppy.app.SteppyApplication
import com.steppy.app.domain.Reminder
import com.steppy.app.domain.StreakState
import com.steppy.app.platform.notifications.ReminderNotifier
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * The evening reminder. Runs once a day at [Reminder.DEFAULT_TIME]; each run schedules the next.
 * Shows a notification only when today's goal isn't met yet, notifications are allowed and
 * reminders are on. Without notification permission it does nothing: the app works the same.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SteppyApplication).container
        val settings = container.settingsRepository.load()
        // Turned off, or "Delete my data": stop the chain (the app schedules it again when needed)
        if (!settings.isOnboardingCompleted || !settings.remindersEnabled) return Result.success()

        val now = LocalDateTime.now()
        if (Reminder.isDue(now)) {
            // Fresh steps first, so the reminder doesn't nudge someone who just reached the goal
            StepSnapshot.take(applicationContext, container, settings)

            val date = now.toLocalDate()
            val history = container.dailyHistoryRepository
            val today = history.getDay(date)
            val streak = (history.streakBefore(date) ?: StreakState()).streak
            Reminder.message(
                todaySteps = today?.steps ?: 0,
                goal = today?.goal ?: settings.dailyGoal,
                streak = streak
            )?.let { ReminderNotifier.show(applicationContext, it) }
        }

        // Runs once this run has finished
        enqueueNext(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "daily_reminder"

        /** Schedules the next reminder, unless one is already waiting. */
        fun schedule(context: Context) = enqueueNext(context, ExistingWorkPolicy.KEEP)

        /** Reminders turned off: removes the waiting reminder. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }

        private fun enqueueNext(context: Context, policy: ExistingWorkPolicy) {
            val delay = Reminder.delayUntilNext(LocalDateTime.now())
            val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, policy, request)
        }
    }
}
