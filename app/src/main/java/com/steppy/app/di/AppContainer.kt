package com.steppy.app.di

import android.app.Activity
import android.content.Context
import com.steppy.app.data.local.SteppyDatabase
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.LocalSteppyRepository
import com.steppy.app.data.repository.RoomDailyHistoryRepository
import com.steppy.app.data.repository.SensorStepRepository
import com.steppy.app.data.repository.StepRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.data.settings.DataStoreSettingsRepository
import com.steppy.app.data.settings.SettingsRepository
import com.steppy.app.platform.PlatformPreferences
import com.steppy.app.platform.debug.DebugFlags
import com.steppy.app.platform.debug.SimulatedStepCounter
import com.steppy.app.platform.sensor.StepCounterSource
import com.steppy.app.platform.sensor.StepSensorDataSource

/**
 * Manual dependency injection: creates every app-wide dependency once.
 * Lives in [com.steppy.app.SteppyApplication], so it survives screen rotation.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Today's progress (steps, streak, XP, challenges). */
    val steppyRepository: SteppyRepository = LocalSteppyRepository(appContext)

    /** Goal, nickname, main goal, mascot and settings. Only one DataStore may use the file. */
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository.create(appContext)

    /** Daily history, activated challenges and the progress baseline. Opened on first use. */
    val database: SteppyDatabase by lazy { SteppyDatabase.build(appContext) }

    val dailyHistoryRepository: DailyHistoryRepository by lazy { RoomDailyHistoryRepository(database) }

    val platformPreferences: PlatformPreferences = PlatformPreferences(appContext)

    private val realSensor = StepSensorDataSource(appContext)

    /** Not null only when the phone has no step sensor and simulation is turned on. */
    val simulatedCounter: SimulatedStepCounter? =
        if (DebugFlags.SIMULATE_STEP_SENSOR_WHEN_MISSING && !realSensor.isAvailable()) {
            SimulatedStepCounter()
        } else null

    private val counterSource: StepCounterSource = simulatedCounter ?: realSensor

    /** Needs the Activity to check the runtime permission, so it is created per Activity. */
    fun createStepRepository(activity: Activity): StepRepository =
        SensorStepRepository(
            activity = activity,
            sensor = counterSource,
            preferences = platformPreferences
        )
}
