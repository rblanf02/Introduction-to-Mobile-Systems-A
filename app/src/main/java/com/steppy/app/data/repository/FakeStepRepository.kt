package com.steppy.app.data.repository

import com.steppy.app.platform.sensor.StepTrackingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeStepRepository : StepRepository {

    private val _state = MutableStateFlow<StepTrackingState>(StepTrackingState.Manual(0))

    override val state: StateFlow<StepTrackingState> = _state.asStateFlow()

    override suspend fun addManualSteps(steps: Int) {
        if (steps <= 0) {
            return
        }

        _state.update { current ->
            val previousSteps = when (current) {
                is StepTrackingState.Manual -> current.todaySteps
                else -> 0
            }
            StepTrackingState.Manual(previousSteps + steps)
        }
    }


}