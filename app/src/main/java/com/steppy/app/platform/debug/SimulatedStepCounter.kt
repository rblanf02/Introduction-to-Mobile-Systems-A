package com.steppy.app.platform.debug

import com.steppy.app.platform.sensor.StepCounterSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SimulatedStepCounter : StepCounterSource {

    private val counter = MutableStateFlow(0)

    override fun isAvailable(): Boolean {
        return true
    }

    override fun stepSinceReboot(): Flow<Int> {
        return counter
    }

    /** Called by the "+500 steps (SIMULATED)" button. */
    fun addSteps(steps: Int) {
        if (steps <= 0) {
            return
        }
        counter.value = counter.value + steps
    }
}