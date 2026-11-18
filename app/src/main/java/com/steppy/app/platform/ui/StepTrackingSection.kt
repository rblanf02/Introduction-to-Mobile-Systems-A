package com.steppy.app.platform.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.steppy.app.data.repository.StepRepository
import com.steppy.app.platform.PlatformPreferences
import com.steppy.app.platform.permission.ActivityRecognitionPermission
import com.steppy.app.platform.sensor.StepTrackingState


@Composable
fun StepTrackingSection (stepRepository: StepRepository, preferences: PlatformPreferences, refreshKey: Int, onSensorSteps: (Int) -> Unit) {

    val context = LocalContext.current

    var trackingState: StepTrackingState? by remember { mutableStateOf(null) }

    var permissionRefresh: Int by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        preferences.setPermissionAsked()
        permissionRefresh = permissionRefresh + 1
    }

    LaunchedEffect(refreshKey, permissionRefresh) {
        stepRepository.state.collect { newState ->
            trackingState = newState
            if (newState is StepTrackingState.Tracking) {
                onSensorSteps(newState.todaySteps)
            }
        }
    }


    val currentState: StepTrackingState? = trackingState

    when (currentState) {

        null -> {
            // Still checking: show nothing
        }

        is StepTrackingState.Tracking -> {
            StepTrackingCard(
                text = "Step sensor active: ${currentState.todaySteps} steps counted by the phone today."
            )
        }

        StepTrackingState.Unavailable -> {
            StepTrackingCard(
                text = "This device has no step sensor. Use '+ Log steps' to add your steps by hand."
            )
        }

        is StepTrackingState.PermissionDenied -> {
            if (currentState.permanently) {
                StepTrackingCard(
                    text = "Step permission was denied permanently. Enable 'Physical activity' in Settings to count steps automatically.",
                    buttonText = "Open settings",
                    onButtonClick = { ActivityRecognitionPermission.openAppSettings(context) }
                )
            } else {
                StepTrackingCard(
                    text = "Steppy needs permission to count your steps automatically.",
                    buttonText = "Allow",
                    onButtonClick = { permissionLauncher.launch(ActivityRecognitionPermission.PERMISSION) }
                )
            }
        }

        is StepTrackingState.Manual -> {
            StepTrackingCard(
                text = "Manual mode: ${currentState.todaySteps} steps logged by hand today."
            )
        }
    }
}
    @Composable
    private fun StepTrackingCard(
        text: String,
        buttonText: String? = null,
        onButtonClick: () -> Unit = {}
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                if (buttonText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onButtonClick) {
                        Text(text = buttonText)
                    }
                }
            }
        }
    }







