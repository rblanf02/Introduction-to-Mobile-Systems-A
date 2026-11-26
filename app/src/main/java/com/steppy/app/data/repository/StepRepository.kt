package com.steppy.app.data.repository

import com.steppy.app.platform.sensor.StepTrackingState
import kotlinx.coroutines.flow.Flow

/**
 *
 *  state: describe current state, emit new value every time the permission or steps changes
 *
 */

interface StepRepository {
    val state : Flow<StepTrackingState>
}