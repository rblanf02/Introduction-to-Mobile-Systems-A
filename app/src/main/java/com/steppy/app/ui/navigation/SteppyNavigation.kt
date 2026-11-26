package com.steppy.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.steppy.app.platform.backup.rememberBackupFileActions
import com.steppy.app.platform.notifications.rememberReminderControls
import com.steppy.app.ui.SteppyViewModel
import com.steppy.app.ui.components.BottomTab
import com.steppy.app.ui.components.SteppyBottomNavigationBar
import com.steppy.app.ui.screens.*

@Composable
fun SteppyApp(viewModel: SteppyViewModel,
              sensorSection: @Composable () -> Unit = {},
              showManualEntry: Boolean = true) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingRestore by viewModel.pendingRestore.collectAsState()

    LaunchedEffect(uiState.statusBannerMessage) {
        uiState.statusBannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    if (!uiState.isLoaded) {
        // Reading the settings takes a few milliseconds; show an empty screen, not onboarding
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else if (!uiState.isOnboardingCompleted) {
        OnboardingFlowScreen(
            uiState = uiState,
            onSaveSetup = viewModel::saveProfileSetup,
            onSelectMascot = viewModel::selectMascot,
            onFinishOnboarding = viewModel::completeOnboarding
        )
    } else {
        pendingRestore?.let { backup ->
            RestoreConfirmDialog(
                backup = backup,
                onConfirm = viewModel::confirmRestore,
                onDismiss = viewModel::cancelRestore
            )
        }
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
                        sensorSection = sensorSection,
                        showManualEntry = showManualEntry
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
                    val backupFiles = rememberBackupFileActions(viewModel)
                    val reminders = rememberReminderControls(viewModel)
                    ProfileScreen(
                        uiState = uiState,
                        onUpdateGoal = viewModel::updateDailyGoal,
                        onUpdateProfile = viewModel::updateUsernameAndMascot,
                        onToggleReminders = reminders.onToggleReminders,
                        notificationsAllowed = reminders.notificationsAllowed,
                        onAllowNotifications = reminders.onAllowNotifications,
                        onSendTestReminder = reminders.onSendTestReminder,
                        onResetAllData = viewModel::resetAllData,
                        onBackupToFile = backupFiles.saveBackup,
                        onRestoreFromFile = backupFiles.pickRestoreFile
                    )
                }
            }
        }
    }
}