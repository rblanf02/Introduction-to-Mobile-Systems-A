package com.steppy.app.platform.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.steppy.app.SteppyApplication
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.di.AppContainer
import com.steppy.app.domain.MorningSteps
import com.steppy.app.platform.permission.ActivityRecognitionPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Every 15 minutes, saves a step snapshot (see [StepSnapshot]), even when the app is closed.
 * So a day still closes with (almost) all of its steps when the app isn't opened, and steps
 * before noon are known for "Morning Mover" (±15 min).
 */
class StepSnapshotWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SteppyApplication).container
        StepSnapshot.take(applicationContext, container, container.settingsRepository.load())
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "step_snapshot"

        /** Starts the 15-minute snapshots; does nothing if they are already scheduled. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSnapshotWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

/** Reads the step counter once and saves today's steps to the daily history. */
object StepSnapshot {

    private const val SENSOR_TIMEOUT_MS = 10_000L

    /**
     * Does nothing before onboarding ends, without the permission or without a real sensor.
     * Steps after the last snapshot before midnight (at most 15 min) count for no day.
     */
    suspend fun take(context: Context, container: AppContainer, settings: UserSettings) {
        if (!settings.isOnboardingCompleted) return
        if (!ActivityRecognitionPermission.isGrated(context)) return

        val sensor = container.stepSensor
        if (!sensor.isAvailable()) return

        // The sensor sends its current value soon after a listener registers
        val counterNow = withTimeoutOrNull(SENSOR_TIMEOUT_MS) {
            sensor.stepSinceReboot().first()
        } ?: return // No reading this time; the next run tries again

        val now = LocalDateTime.now()
        val record = container.platformPreferences.recordCounter(counterNow, now.toLocalDate())

        container.dailyHistoryRepository.saveDay(
            date = record.date,
            steps = record.todaySteps,
            goal = settings.dailyGoal,
            source = StepSource.SENSOR,
            stepsBeforeNoon = MorningSteps.stepsBeforeNoon(now.toLocalTime(), record.todaySteps)
        )
    }
}
