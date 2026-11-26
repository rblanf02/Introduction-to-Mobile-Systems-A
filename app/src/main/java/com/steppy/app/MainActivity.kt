package com.steppy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.steppy.app.data.repository.StepRepository
import com.steppy.app.platform.ui.StepTrackingSection
import com.steppy.app.platform.work.DailyReminderWorker
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.navigation.SteppyApp
import com.steppy.app.ui.theme.SteppyTheme

class MainActivity : ComponentActivity() {

    private var sensorRefreshKey: Int by mutableIntStateOf(0)

    private var sensorIsTracking: Boolean by mutableStateOf(false)

    private val container by lazy { (application as SteppyApplication).container }

    private val steppyViewModel: SteppyViewModel by viewModels {
        SteppyViewModel.Factory(
            container.steppyRepository,
            container.settingsRepository,
            container.dailyHistoryRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val stepRepository: StepRepository = container.createStepRepository(this)

        setContent {
            val state by steppyViewModel.uiState.collectAsState()
            // The daily reminder follows the settings: on after onboarding, off when turned off
            // or after "Delete my data". Notification permission is checked when it runs.
            val reminderWanted = state.isLoaded && state.isOnboardingCompleted && state.remindersEnabled
            LaunchedEffect(state.isLoaded, reminderWanted) {
                if (!state.isLoaded) return@LaunchedEffect
                if (reminderWanted) {
                    DailyReminderWorker.schedule(applicationContext)
                } else {
                    DailyReminderWorker.cancel(applicationContext)
                }
            }

            SteppyTheme {
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
                            onTrackingChanged = { tracking ->
                                sensorIsTracking = tracking
                                steppyViewModel.onStepTrackingChanged(tracking)
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorRefreshKey = sensorRefreshKey + 1
        // The app may have stayed open past midnight
        steppyViewModel.checkDayRollover()
    }
}