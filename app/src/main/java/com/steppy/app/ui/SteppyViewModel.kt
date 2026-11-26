package com.steppy.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.domain.ActivationResult
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeCatalogue
import com.steppy.app.domain.ChallengeEvaluation
import com.steppy.app.domain.ChallengeEvaluator
import com.steppy.app.domain.ChallengeProgressInputs
import com.steppy.app.domain.MainMotivation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class SteppyViewModel(
    private val repository: SteppyRepository,
    private val history: DailyHistoryRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(repository.loadInitialState())
    val uiState: StateFlow<SteppyUiState> = _uiState.asStateFlow()

    /** Runs history writes one at a time, in order, so an older value never overwrites a newer one. */
    private val historyLock = Mutex()

    init {
        checkDayRollover()
    }

    private fun updateAndPersist(transform: (SteppyUiState) -> SteppyUiState) {
        val updated = _uiState.updateAndGet(transform)
        repository.saveState(updated)
        saveTodayRecord(updated)
    }

    private fun stepSource(state: SteppyUiState): StepSource =
        if (state.isManualMode) StepSource.MANUAL else StepSource.SENSOR

    /** Saves today's steps with today's goal, so a later goal change never rewrites past days. */
    private fun saveTodayRecord(state: SteppyUiState) {
        val date = state.todayDate ?: return
        if (!state.isOnboardingCompleted) return
        saveRecord(date, state.todaySteps, state.dailyGoal, stepSource(state))
    }

    private fun saveRecord(date: LocalDate, steps: Int, goal: Int, source: StepSource) {
        viewModelScope.launch {
            historyLock.withLock { history.saveDay(date, steps, goal, source) }
        }
    }

    /**
     * Starts a new day if the date changed since the steps were last counted: yesterday's
     * record is saved with its final steps and goal, and today starts at 0.
     * Called at startup, when the app comes back to the screen, and before every step update.
     */
    fun checkDayRollover() {
        val now = today()
        val state = _uiState.value
        val countedDate = state.todayDate

        if (countedDate == null) {
            // Data saved before dates were stored: assume the steps are today's
            updateAndPersist { it.copy(todayDate = now) }
            return
        }
        // Same day, or the clock moved back: keep counting on the current day
        if (!now.isAfter(countedDate)) return

        if (state.isOnboardingCompleted) {
            saveRecord(countedDate, state.todaySteps, state.dailyGoal, stepSource(state))
        }
        updateAndPersist {
            it.copy(
                todayDate = now,
                todaySteps = 0,
                weeklyBars = defaultWeeklyBars(it.dailyGoal, 0),
                statusBannerMessage = null
            )
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
        checkDayRollover()
        updateAndPersist { state ->
            val wasGoalMet = state.isTodayGoalMet
            val newSteps = (state.todaySteps + stepsToAdd).coerceAtMost(200000)
            val isNowGoalMet = newSteps >= state.dailyGoal
            val newStreak = if (!wasGoalMet && isNowGoalMet) state.currentStreak + 1 else state.currentStreak
            val newWeeklyTotal = state.weeklyBars.filter { !it.isToday }.sumOf { it.steps } + newSteps

            val progressById = ChallengeCatalogue.progressById(
                ChallengeProgressInputs.forToday(
                    date = state.todayDate ?: today(),
                    todaySteps = newSteps,
                    streak = newStreak,
                    weekSteps = newWeeklyTotal
                )
            )
            val evaluation = ChallengeEvaluator.evaluate(
                state.challenges.map { it.toChallengeState() },
                progressById
            )

            val goalMessage = if (!wasGoalMet && isNowGoalMet) {
                "Daily goal reached! Streak +1 and +50 bonus XP!"
            } else null

            state.copy(
                todaySteps = newSteps,
                currentStreak = newStreak,
                totalHistoricalSteps = newWeeklyTotal,
                baseXP = state.baseXP + evaluation.xpAwarded,
                weeklyBars = defaultWeeklyBars(state.dailyGoal, newSteps),
                challenges = state.challenges.withChallengeStates(evaluation.challenges),
                statusBannerMessage = listOfNotNull(goalMessage, completionMessage(state, evaluation))
                    .joinToString(" ")
                    .ifEmpty { null }
            )
        }
    }

    fun onSensorStepsUpdated(totalTodayStepsFromSensor: Int) {
        if (totalTodayStepsFromSensor < 0) return
        // On a new day the sensor count drops back to a low value; start today at 0 first
        checkDayRollover()
        val diff = totalTodayStepsFromSensor - _uiState.value.todaySteps
        if (diff > 0) addSteps(diff)
    }

    fun activateChallenge(challengeId: String) {
        updateAndPersist { state ->
            val states = state.challenges.map { it.toChallengeState() }
            when (val result = ChallengeEvaluator.activate(states, challengeId)) {
                ActivationResult.LimitReached -> state.copy(
                    statusBannerMessage = "You can have a maximum of ${ChallengeEvaluator.MAX_ACTIVE} active challenges at once."
                )
                ActivationResult.NotAvailable -> state
                is ActivationResult.Activated -> {
                    // The target may already be reached today, so it can complete right away
                    val evaluation = ChallengeEvaluator.evaluate(result.challenges)
                    state.copy(
                        baseXP = state.baseXP + evaluation.xpAwarded,
                        challenges = state.challenges.withChallengeStates(evaluation.challenges),
                        statusBannerMessage = completionMessage(state, evaluation) ?: "Challenge activated!"
                    )
                }
            }
        }
    }

    private fun completionMessage(state: SteppyUiState, evaluation: ChallengeEvaluation): String? {
        if (evaluation.newlyCompleted.isEmpty()) return null
        val titles = state.challenges
            .filter { it.id in evaluation.newlyCompleted }
            .joinToString(", ") { it.title }
        return "Challenge completed: $titles! +${evaluation.xpAwarded} XP"
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun updateDailyGoal(newGoal: Int) {
        val validGoal = newGoal.coerceIn(500, 50000)
        checkDayRollover()
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
        _uiState.value = SteppyUiState(isOnboardingCompleted = false, todayDate = today())
        viewModelScope.launch {
            historyLock.withLock { history.clearAll() }
        }
    }

    class Factory(
        private val repository: SteppyRepository,
        private val history: DailyHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SteppyViewModel(repository, history) as T
        }
    }
}