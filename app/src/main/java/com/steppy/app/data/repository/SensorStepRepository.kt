package com.steppy.app.data.repository

import android.app.Activity
import com.steppy.app.platform.PlatformPreferences
import com.steppy.app.platform.permission.ActivityRecognitionPermission
import com.steppy.app.platform.sensor.DailyStepCalculator
import com.steppy.app.platform.sensor.DailyStepRecord
import com.steppy.app.platform.sensor.StepCounterSource
import com.steppy.app.platform.sensor.StepSensorDataSource
import com.steppy.app.platform.sensor.StepTrackingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

class SensorStepRepository (
    private val activity: Activity,
    private val sensor: StepCounterSource,
    private val preferences: PlatformPreferences
) : StepRepository {
    override  val state: Flow<StepTrackingState> = flow {
        if (!sensor.isAvailable()){
            emit(StepTrackingState.Unavailable)
            return@flow
        }
        if (!ActivityRecognitionPermission.isGrated(activity)){

            val alredyAsked: Boolean = preferences.wasPermissionAsked()
            val permanently: Boolean = ActivityRecognitionPermission.isPermanentlyDenied(activity,alredyAsked)

            emit(StepTrackingState.PermissionDenied(permanently))

            return@flow

        }

        sensor.stepSinceReboot().collect { counterNow -> val previous: DailyStepRecord? =
        preferences.loadStepRecord()

            val today: LocalDate = LocalDate.now()
            val record: DailyStepRecord = DailyStepCalculator.calculate(previous, counterNow,today)

            preferences.saveStepRecord(record)
            emit(StepTrackingState.Tracking(record.todaySteps))
        }



    }



}