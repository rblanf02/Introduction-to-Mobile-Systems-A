package com.steppy.app.data.repository

import com.steppy.app.platform.sensor.StepTrackingState
import kotlinx.coroutines.flow.Flow

/**
 *
 * Public Interface StepRepository:
 *  state:describe current state, emit new value every time the permision or steps changes
 *  addManuealSteps: adds steps by the user (fallback when there is no sensor or no permision)
 *
 */

interface StepRepository {
    val state : Flow<StepTrackingState>
    suspend fun addManualSteps(steps:Int)
}