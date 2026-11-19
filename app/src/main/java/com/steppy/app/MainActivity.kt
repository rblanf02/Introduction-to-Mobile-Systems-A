package com.steppy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steppy.app.data.repository.SensorStepRepository
import com.steppy.app.data.repository.StepRepository
import com.steppy.app.platform.PlatformPreferences
import com.steppy.app.platform.debug.DebugFlags
import com.steppy.app.platform.debug.SimulatedStepCounter
import com.steppy.app.platform.sensor.StepCounterSource
import com.steppy.app.platform.sensor.StepSensorDataSource
import com.steppy.app.platform.ui.StepTrackingSection
import com.steppy.app.ui.LocalSteppyRepository
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.navigation.SteppyApp
import com.steppy.app.ui.theme.SteppyTheme

class MainActivity : ComponentActivity() {

    // Role C: changes every time the app comes back to the screen, so the sensor state is checked again
    private var sensorRefreshKey: Int by mutableIntStateOf(0)

    // Role C: true while the step sensor (real or SIMULATED) is counting. Then manual entry is hidden
    private var sensorIsTracking: Boolean by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LocalSteppyRepository(applicationContext)

        // Role C: step counter. The real sensor, or a SIMULATED one for demos on devices without it
        val realSensor = StepSensorDataSource(applicationContext)
        val useSimulation: Boolean =
            DebugFlags.SIMULATE_STEP_SENSOR_WHEN_MISSING && !realSensor.isAvailable()
        val simulatedCounter: SimulatedStepCounter? = if (useSimulation) SimulatedStepCounter() else null
        val counterSource: StepCounterSource = if (simulatedCounter != null) simulatedCounter else realSensor

        // Role C: step sensor objects (manual dependency injection)
        val platformPreferences = PlatformPreferences(applicationContext)
        val stepRepository: StepRepository = SensorStepRepository(
            activity = this,
            sensor = counterSource,
            preferences = platformPreferences
        )

        setContent {
            SteppyTheme {
                val steppyViewModel: SteppyViewModel = viewModel(
                    factory = SteppyViewModel.Factory(repository)
                )
                SteppyApp(
                    viewModel = steppyViewModel,
                    showManualEntry = !sensorIsTracking,
                    sensorSection = {
                        StepTrackingSection(
                            stepRepository = stepRepository,
                            preferences = platformPreferences,
                            refreshKey = sensorRefreshKey,
                            onSensorSteps = steppyViewModel::onSensorStepsUpdated,
                            simulatedCounter = simulatedCounter,
                            onTrackingChanged = { tracking -> sensorIsTracking = tracking }
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorRefreshKey = sensorRefreshKey + 1
    }
}