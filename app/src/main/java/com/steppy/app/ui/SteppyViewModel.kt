package com.steppy.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.steppy.app.data.backup.Backup
import com.steppy.app.data.backup.BackupCodec
import com.steppy.app.data.backup.BackupReadResult
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.data.settings.SettingsRepository
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivationResult
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeCatalogue
import com.steppy.app.domain.ChallengeEvaluation
import com.steppy.app.domain.ChallengeEvaluator
import com.steppy.app.domain.ChallengeProgressInputs
import com.steppy.app.domain.DayProgress
import com.steppy.app.domain.MainMotivation
import com.steppy.app.domain.MorningSteps
import com.steppy.app.domain.StreakCalculator
import com.steppy.app.domain.StreakState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime

class SteppyViewModel(
    private val repository: SteppyRepository,
    private val settingsRepository: SettingsRepository,
    private val history: DailyHistoryRepository,
    private val timeOfDay: () -> LocalTime = { LocalTime.now() },
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(repository.loadInitialState())
    val uiState: StateFlow<SteppyUiState> = _uiState.asStateFlow()

    /** Runs history writes one at a time, in order, so an older value never overwrites a newer one. */
    private val historyLock = Mutex()

    /** Runs settings writes one at a time, in order. */
    private val settingsLock = Mutex()

    /** The settings last read or written, so unchanged settings are not written again. */
    private var savedSettings: UserSettings? = null

    private val _pendingRestore = MutableStateFlow<Backup?>(null)

    /** A valid backup file the user picked, waiting for them to confirm the restore. */
    val pendingRestore: StateFlow<Backup?> = _pendingRestore.asStateFlow()

    /** Streak and hearts at the end of the day before [StreakBase.date], read from the history. */
    private data class StreakBase(val date: LocalDate, val state: StreakState)

    /** Null until the history has been read for the current day. */
    private var streakBase: StreakBase? = null

    /** The last state reported by the step sensor, or null before the first report. */
    private var sensorTracking: Boolean? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.load()
            savedSettings = settings
            _uiState.update { it.withSettings(settings).copy(isLoaded = true) }
            rollOverIfNewDay()
            applySensorTracking()
            refreshStreak()
        }
    }

    /**
     * The streak before today. Until the history has been read, it is worked out from the
     * saved streak, which already includes today if today's goal is met.
     */
    private fun streakBefore(state: SteppyUiState): StreakState =
        streakBase?.takeIf { it.date == state.todayDate }?.state
            ?: StreakState(
                streak = (state.currentStreak - if (state.isTodayGoalMet) 1 else 0).coerceAtLeast(0),
                hearts = state.streakFreezes
            )

    /** Streak and hearts once today's [steps] and [goal] are added to [before]. */
    private fun SteppyUiState.withStreak(before: StreakState, steps: Int, goal: Int): SteppyUiState {
        val now = StreakCalculator.withToday(before, DayProgress(todayDate ?: today(), steps, goal))
        return copy(currentStreak = now.streak, streakFreezes = now.hearts)
    }

    /**
     * Reads from the daily history:
     * - the streak and hearts, so a missed day resets the streak (or uses a heart)
     * - today's steps before noon, which the snapshot worker may have saved while the app was closed
     * Then checks the challenges again with them. Queued behind earlier history writes,
     * such as yesterday's final record.
     */
    private fun refreshStreak() {
        val date = _uiState.value.todayDate ?: return
        viewModelScope.launch {
            val (before, todayRecord) = historyLock.withLock {
                history.streakBefore(date) to history.getDay(date)
            }
            // The day changed meanwhile; the refresh for the new day sets it
            if (_uiState.value.todayDate != date) return@launch
            if (before == null && todayRecord?.stepsBeforeNoon == null) return@launch
            before?.let { streakBase = StreakBase(date, it) }
            updateAndPersist { state ->
                val refreshed = (before?.let { state.withStreak(it, state.todaySteps, state.dailyGoal) } ?: state)
                    .copy(stepsBeforeNoon = maxOf(state.stepsBeforeNoon, todayRecord?.stepsBeforeNoon ?: 0))
                val (evaluated, evaluation) = evaluateChallenges(refreshed)
                evaluated.copy(
                    statusBannerMessage = completionMessage(refreshed, evaluation) ?: refreshed.statusBannerMessage
                )
            }
        }
    }

    /** Reads every challenge's progress from [state] and adds the XP of newly completed ones. */
    private fun evaluateChallenges(state: SteppyUiState): Pair<SteppyUiState, ChallengeEvaluation> {
        val inputs = ChallengeProgressInputs.forToday(
            date = state.todayDate ?: today(),
            todaySteps = state.todaySteps,
            streak = state.currentStreak,
            weekSteps = state.totalHistoricalSteps,
            stepsBeforeNoon = state.stepsBeforeNoon
        )
        val evaluation = ChallengeEvaluator.evaluate(
            state.challenges.map { it.toChallengeState() },
            ChallengeCatalogue.progressById(inputs)
        )
        val evaluated = state.copy(
            baseXP = state.baseXP + evaluation.xpAwarded,
            challenges = state.challenges.withChallengeStates(evaluation.challenges)
        )
        return evaluated to evaluation
    }

    /** Today's steps by noon if it is still morning on [date]; null once noon has passed. */
    private fun stepsBeforeNoonNow(date: LocalDate, todaySteps: Int): Int? =
        if (date == today()) MorningSteps.stepsBeforeNoon(timeOfDay(), todaySteps) else null

    /** @param source how the steps in this update were counted; null when no steps were added */
    private fun updateAndPersist(
        source: StepSource? = null,
        transform: (SteppyUiState) -> SteppyUiState
    ) {
        // Saving before the settings are read would overwrite them with the defaults
        if (!_uiState.value.isLoaded) return
        val updated = _uiState.updateAndGet(transform)
        repository.saveState(updated)
        saveSettingsIfChanged(updated.settings())
        saveTodayRecord(updated, source)
    }

    private fun saveSettingsIfChanged(settings: UserSettings) {
        if (settings == savedSettings) return
        savedSettings = settings
        viewModelScope.launch {
            settingsLock.withLock { settingsRepository.save(settings) }
        }
    }

    /**
     * MANUAL when [added] steps were entered by hand, or when the sensor isn't counting.
     * The history keeps a day MANUAL once it was, so a later SENSOR save doesn't undo it.
     */
    private fun stepSource(state: SteppyUiState, added: StepSource? = null): StepSource =
        if (added == StepSource.MANUAL || state.isManualMode) StepSource.MANUAL else StepSource.SENSOR

    /** Saves today's steps with today's goal, so a later goal change never rewrites past days. */
    private fun saveTodayRecord(state: SteppyUiState, added: StepSource? = null) {
        val date = state.todayDate ?: return
        if (!state.isOnboardingCompleted) return
        saveRecord(
            date,
            state.todaySteps,
            state.dailyGoal,
            stepSource(state, added),
            stepsBeforeNoonNow(date, state.todaySteps)
        )
    }

    private fun saveRecord(
        date: LocalDate,
        steps: Int,
        goal: Int,
        source: StepSource,
        stepsBeforeNoon: Int? = null
    ) {
        viewModelScope.launch {
            historyLock.withLock { history.saveDay(date, steps, goal, source, stepsBeforeNoon) }
        }
    }

    /**
     * Starts a new day if the date changed since the steps were last counted: yesterday's
     * record is saved with its final steps and goal, and today starts at 0.
     * The streak is then read again from the history, so a missed day resets it.
     * Called at startup, when the app comes back to the screen, and before every step update.
     */
    fun checkDayRollover() {
        if (rollOverIfNewDay()) refreshStreak()
    }

    /** The day change itself; returns true if a new day started. */
    private fun rollOverIfNewDay(): Boolean {
        val now = today()
        val state = _uiState.value
        // Runs again once loading ends
        if (!state.isLoaded) return false
        val countedDate = state.todayDate

        if (countedDate == null) {
            // Data saved before dates were stored: assume the steps are today's
            updateAndPersist { it.copy(todayDate = now) }
            return false
        }
        // Same day, or the clock moved back: keep counting on the current day
        if (!now.isAfter(countedDate)) return false

        if (state.isOnboardingCompleted) {
            saveRecord(countedDate, state.todaySteps, state.dailyGoal, stepSource(state))
        }
        updateAndPersist {
            it.copy(
                todayDate = now,
                todaySteps = 0,
                stepsBeforeNoon = 0,
                weeklyBars = defaultWeeklyBars(it.dailyGoal, 0),
                statusBannerMessage = null
            )
        }
        return true
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

    /** Steps the user entered by hand ("+ Log steps"); the day is saved as MANUAL. */
    fun addSteps(stepsToAdd: Int) {
        addSteps(stepsToAdd, StepSource.MANUAL)
    }

    private fun addSteps(stepsToAdd: Int, source: StepSource) {
        if (stepsToAdd <= 0 || !_uiState.value.isLoaded) return
        checkDayRollover()
        updateAndPersist(source) { state ->
            val wasGoalMet = state.isTodayGoalMet
            val newSteps = (state.todaySteps + stepsToAdd).coerceAtMost(200000)
            val isNowGoalMet = newSteps >= state.dailyGoal
            val newWeeklyTotal = state.weeklyBars.filter { !it.isToday }.sumOf { it.steps } + newSteps
            val date = state.todayDate ?: today()

            val stepped = state.withStreak(streakBefore(state), newSteps, state.dailyGoal).copy(
                todaySteps = newSteps,
                stepsBeforeNoon = stepsBeforeNoonNow(date, newSteps) ?: state.stepsBeforeNoon,
                totalHistoricalSteps = newWeeklyTotal,
                weeklyBars = defaultWeeklyBars(state.dailyGoal, newSteps)
            )
            val (evaluated, evaluation) = evaluateChallenges(stepped)

            val goalMessage = if (!wasGoalMet && isNowGoalMet) {
                "Daily goal reached! Streak +1 and +50 bonus XP!"
            } else null

            evaluated.copy(
                statusBannerMessage = listOfNotNull(goalMessage, completionMessage(state, evaluation))
                    .joinToString(" ")
                    .ifEmpty { null }
            )
        }
    }

    fun onSensorStepsUpdated(totalTodayStepsFromSensor: Int) {
        // Before loading ends the sensor value is dropped; the sensor sends it again
        if (totalTodayStepsFromSensor < 0 || !_uiState.value.isLoaded) return
        // On a new day the sensor count drops back to a low value; start today at 0 first
        checkDayRollover()
        val diff = totalTodayStepsFromSensor - _uiState.value.todaySteps
        if (diff > 0) addSteps(diff, StepSource.SENSOR)
    }

    /**
     * Whether the step sensor is counting. When it isn't (no sensor, or the permission is
     * denied), manual mode turns on, so Today offers manual entry and the day is saved as MANUAL.
     * When the permission is granted later, manual mode turns off again.
     */
    fun onStepTrackingChanged(isTracking: Boolean) {
        sensorTracking = isTracking
        applySensorTracking()
    }

    private fun applySensorTracking() {
        val isTracking = sensorTracking ?: return
        val state = _uiState.value
        // Before onboarding ends, the choice made there decides
        if (!state.isLoaded || !state.isOnboardingCompleted) return
        if (state.isManualMode != !isTracking) {
            updateAndPersist { it.copy(isManualMode = !isTracking) }
        }
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
            // A new goal can meet (or un-meet) today's goal, which changes today's streak
            state.withStreak(streakBefore(state), state.todaySteps, validGoal).copy(
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

    /** The notification permission was denied: reminders stay off, everything else works. */
    fun onNotificationsDenied() {
        updateAndPersist {
            it.copy(
                remindersEnabled = false,
                statusBannerMessage = "Notifications are off, so there are no reminders. Steppy works the same without them."
            )
        }
    }

    fun resetAllData() {
        repository.clearAllData()
        _uiState.value = SteppyUiState(isLoaded = true, todayDate = today())
        streakBase = StreakBase(today(), StreakState())
        savedSettings = UserSettings()
        viewModelScope.launch {
            settingsLock.withLock { settingsRepository.clear() }
        }
        viewModelScope.launch {
            historyLock.withLock { history.clearAll() }
        }
    }

    /**
     * The backup file's text: settings, today's progress and the full history.
     * Waits for earlier history writes, so today's latest steps are included.
     */
    suspend fun createBackupJson(): String {
        val history = historyLock.withLock { history.exportAll() }
        val state = _uiState.value
        return BackupCodec.encode(
            Backup(
                createdOn = today(),
                settings = state.settings(),
                progress = state.progressSnapshot(),
                history = history
            )
        )
    }

    fun onBackupSaved(success: Boolean) {
        showMessage(if (success) "Backup saved." else "The backup couldn't be saved. Try another location.")
    }

    /**
     * Checks a file picked for restore. A valid backup waits in [pendingRestore] for the
     * user to confirm; nothing is replaced yet.
     *
     * @param text the file's text, or null if it couldn't be read
     */
    fun onRestoreFileRead(text: String?) {
        if (text == null) {
            showMessage("The file couldn't be opened. Nothing was changed.")
            return
        }
        when (val result = BackupCodec.decode(text)) {
            is BackupReadResult.Invalid -> showMessage("${result.reason} Nothing was changed.")
            is BackupReadResult.Valid -> _pendingRestore.value = result.backup
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    /** Replaces the settings, today's progress and the whole history with the pending backup. */
    fun confirmRestore() {
        val backup = _pendingRestore.value ?: return
        _pendingRestore.value = null

        val restored = SteppyUiState(isLoaded = true)
            .withSettings(backup.settings)
            .withProgress(backup.progress)
            .copy(statusBannerMessage = "Backup restored.")
        _uiState.value = restored
        streakBase = null
        repository.saveState(restored)
        savedSettings = backup.settings
        viewModelScope.launch {
            settingsLock.withLock { settingsRepository.save(backup.settings) }
        }
        viewModelScope.launch {
            historyLock.withLock { history.replaceAll(backup.history) }
        }
        // A backup from an earlier day: close that day and start today at 0.
        // Its history write queues behind the restore above.
        rollOverIfNewDay()
        // The backup's manual mode came from the other phone; this phone's sensor decides
        applySensorTracking()
        // The streak comes from the restored history, read after the writes above
        refreshStreak()
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
    }

    class Factory(
        private val repository: SteppyRepository,
        private val settingsRepository: SettingsRepository,
        private val history: DailyHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SteppyViewModel(repository, settingsRepository, history) as T
        }
    }
}