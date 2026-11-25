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
import com.steppy.app.data.repository.StepRepository
import com.steppy.app.platform.ui.StepTrackingSection
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.navigation.SteppyApp
import com.steppy.app.ui.theme.SteppyTheme

class MainActivity : ComponentActivity() {

    private var sensorRefreshKey: Int by mutableIntStateOf(0)

    private var sensorIsTracking: Boolean by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SteppyApplication).container
        val stepRepository: StepRepository = container.createStepRepository(this)

        setContent {
            SteppyTheme {
                val steppyViewModel: SteppyViewModel = viewModel(
                    factory = SteppyViewModel.Factory(container.steppyRepository)
                )
                SteppyApp(
                    viewModel = steppyViewModel,
                    showManualEntry = !sensorIsTracking,
                    sensorSection = {
                        StepTrackingSection(
                            stepRepository = stepRepository,
                            preferences = container.platformPreferences,
                            refreshKey = sensorRefreshKey,
                            onSensorSteps = steppyViewModel::onSensorStepsUpdated,
                            simulatedCounter = container.simulatedCounter,
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