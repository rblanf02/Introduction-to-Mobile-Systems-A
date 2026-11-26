package com.steppy.walkweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.steppy.walkweather.ui.WalkWeatherScreen
import com.steppy.walkweather.ui.WalkWeatherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WalkWeatherViewModel by viewModels {
        WalkWeatherViewModel.Factory((application as WalkWeatherApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                WalkWeatherScreen(viewModel)
            }
        }
    }
}
