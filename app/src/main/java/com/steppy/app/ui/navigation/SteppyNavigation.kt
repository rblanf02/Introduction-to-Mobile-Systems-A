package com.steppy.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.components.BottomTab
import com.steppy.app.ui.components.SteppyBottomNavigationBar
import com.steppy.app.ui.screens.*

@Composable
fun SteppyApp(viewModel: SteppyViewModel,sensorSection: @Composable () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBannerMessage) {
        uiState.statusBannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    if (!uiState.isOnboardingCompleted) {
        OnboardingFlowScreen(
            uiState = uiState,
            onSaveSetup = viewModel::saveProfileSetup,
            onSelectMascot = viewModel::selectMascot,
            onFinishOnboarding = viewModel::completeOnboarding
        )
    } else {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                SteppyBottomNavigationBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(BottomTab.TODAY.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomTab.TODAY.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomTab.TODAY.route) {
                    TodayScreen(
                        uiState = uiState,
                        onAddSteps = viewModel::addSteps,
                        onNotificationClick = {
                            navController.navigate(BottomTab.PROFILE.route)
                        },
                        sensorSection = sensorSection
                    )
                }
                composable(BottomTab.PROGRESS.route) {
                    ProgressScreen(
                        uiState = uiState,
                        onNotificationClick = {
                            navController.navigate(BottomTab.PROFILE.route)
                        }
                    )
                }
                composable(BottomTab.CHALLENGES.route) {
                    ChallengesScreen(
                        uiState = uiState,
                        onActivateChallenge = viewModel::activateChallenge,
                        onNotificationClick = {
                            navController.navigate(BottomTab.PROFILE.route)
                        }
                    )
                }
                composable(BottomTab.PROFILE.route) {
                    ProfileScreen(
                        uiState = uiState,
                        onUpdateGoal = viewModel::updateDailyGoal,
                        onUpdateProfile = viewModel::updateUsernameAndMascot,
                        onToggleReminders = viewModel::toggleReminders,
                        onResetAllData = viewModel::resetAllData
                    )
                }
            }
        }
    }
}