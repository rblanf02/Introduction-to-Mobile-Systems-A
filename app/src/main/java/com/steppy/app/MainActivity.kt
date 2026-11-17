package com.steppy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steppy.app.ui.LocalSteppyRepository
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.navigation.SteppyApp
import com.steppy.app.ui.theme.SteppyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LocalSteppyRepository(applicationContext)

        setContent {
            SteppyTheme {
                val steppyViewModel: SteppyViewModel = viewModel(
                    factory = SteppyViewModel.Factory(repository)
                )
                SteppyApp(viewModel = steppyViewModel)
            }
        }
    }
}