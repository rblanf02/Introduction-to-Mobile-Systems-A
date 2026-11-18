package com.steppy.app.platform.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Anything that gives a step counter value (steps since the phone was rebooted).
 * Implementations: StepSensorDataSource (real hardware) and SimulatedStepCounter (SIMULATED, demos only).
 */
interface StepCounterSource {
    fun isAvailable(): Boolean
    fun stepSinceReboot(): Flow<Int>
}