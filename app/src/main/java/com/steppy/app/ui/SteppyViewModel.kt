package com.steppy.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.steppy.app.data.repository.SteppyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SteppyViewModel(
    private val repository: SteppyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(repository.loadInitialState())
    val uiState: StateFlow<SteppyUiState> = _uiState.asStateFlow()

    private fun updateAndPersist(transform: (SteppyUiState) -> SteppyUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            repository.saveState(updated)
            updated
        }
    }

    fun saveProfileSetup(
        username: String,
        motivation: MainMotivation,
        activityLevel: ActivityLevel,
        dailyGoal: Int
    ) {
        val cleanName = username.trim().ifEmpty { "Walker" }
        updateAndPersist { state ->
            state.copy(
                username = cleanName,
                motivation = motivation,
                activityLevel = activityLevel,
                dailyGoal = dailyGoal.coerceIn(500, 50000),
                weeklyBars = defaultWeeklyBars(dailyGoal, state.todaySteps)
            )
        }
    }

    fun selectMascot(mascot: Mascot) {
        updateAndPersist { it.copy(selectedMascot = mascot) }
    }

    fun completeOnboarding(manualFallbackMode: Boolean) {
        updateAndPersist {
            it.copy(
                isOnboardingCompleted = true,
                isManualMode = manualFallbackMode
            )
        }
    }

    fun addSteps(stepsToAdd: Int) {
        if (stepsToAdd <= 0) return
        updateAndPersist { state ->
            val wasGoalMet = state.isTodayGoalMet
            val newSteps = (state.todaySteps + stepsToAdd).coerceAtMost(200000)
            val isNowGoalMet = newSteps >= state.dailyGoal
            val newStreak = if (!wasGoalMet && isNowGoalMet) state.currentStreak + 1 else state.currentStreak
            val newWeeklyTotal = state.weeklyBars.filter { !it.isToday }.sumOf { it.steps } + newSteps

            val updatedChallenges = state.challenges.map { ch ->
                val newProgress = when (ch.id) {
                    "c1" -> newSteps
                    "c2" -> newStreak
                    "c4" -> newWeeklyTotal
                    "c5" -> newSteps.coerceAtMost(2000)
                    else -> ch.currentProgress
                }
                val completedNow = newProgress >= ch.targetProgress
                ch.copy(
                    currentProgress = newProgress,
                    isCompleted = ch.isCompleted || completedNow
                )
            }

            state.copy(
                todaySteps = newSteps,
                currentStreak = newStreak,
                totalHistoricalSteps = newWeeklyTotal,
                weeklyBars = defaultWeeklyBars(state.dailyGoal, newSteps),
                challenges = updatedChallenges,
                statusBannerMessage = if (!wasGoalMet && isNowGoalMet) {
                    "Daily goal reached! Streak +1 and +50 bonus XP!"
                } else null
            )
        }
    }

    fun onSensorStepsUpdated(totalTodayStepsFromSensor: Int) {
        if (totalTodayStepsFromSensor < 0) return
        val diff = totalTodayStepsFromSensor - _uiState.value.todaySteps
        if (diff > 0) addSteps(diff)
    }

    fun activateChallenge(challengeId: String) {
        updateAndPersist { state ->
            val activeCount = state.challenges.count { it.isActive && !it.isCompleted }
            if (activeCount >= 3) {
                return@updateAndPersist state.copy(
                    statusBannerMessage = "You can have a maximum of 3 active challenges at once."
                )
            }
            val updated = state.challenges.map {
                if (it.id == challengeId) it.copy(isActive = true) else it
            }
            state.copy(
                challenges = updated,
                statusBannerMessage = "Challenge activated!"
            )
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun updateDailyGoal(newGoal: Int) {
        val validGoal = newGoal.coerceIn(500, 50000)
        updateAndPersist { state ->
            state.copy(
                dailyGoal = validGoal,
                weeklyBars = defaultWeeklyBars(validGoal, state.todaySteps),
                statusBannerMessage = "Daily goal updated to $validGoal steps."
            )
        }
    }

    fun updateUsernameAndMascot(newName: String, newMascot: Mascot) {
        val clean = newName.trim().ifEmpty { "Walker" }
        updateAndPersist {
            it.copy(
                username = clean,
                selectedMascot = newMascot,
                statusBannerMessage = "Profile updated."
            )
        }
    }

    fun toggleReminders(enabled: Boolean) {
        updateAndPersist { it.copy(remindersEnabled = enabled) }
    }

    fun resetAllData() {
        repository.clearAllData()
        _uiState.value = SteppyUiState(isOnboardingCompleted = false)
    }

    class Factory(private val repository: SteppyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SteppyViewModel(repository) as T
        }
    }
}