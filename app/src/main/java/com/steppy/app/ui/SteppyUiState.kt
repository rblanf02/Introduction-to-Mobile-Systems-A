package com.steppy.app.ui

import com.steppy.app.data.repository.ProgressSnapshot
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeCatalogue
import com.steppy.app.domain.ChallengeDefinition
import com.steppy.app.domain.ChallengeProgressInputs
import com.steppy.app.domain.ChallengeState
import com.steppy.app.domain.LevelProgress
import com.steppy.app.domain.MainMotivation
import com.steppy.app.domain.XpCalculator
import java.time.LocalDate

enum class Mascot(val displayName: String, val emoji: String) {
    SLOTH("Sloth", "🦥"),
    PUPPY("Puppy", "🐶"),
    OSTRICH("Ostrich", "🦤"),
    IGUANA("Iguana", "🦎")
}

/** A catalogue challenge plus the user's progress on it. */
data class ChallengeUiItem(
    val definition: ChallengeDefinition,
    val currentProgress: Int = 0,
    val isActive: Boolean = false,
    val isCompleted: Boolean = false
) {
    val id: String get() = definition.id
    val title: String get() = definition.title
    val description: String get() = definition.description
    val xpReward: Int get() = definition.xpReward
    val targetProgress: Int get() = definition.target
    val unit: String get() = definition.unit

    val progressFraction: Float
        get() = if (targetProgress > 0) {
            (currentProgress.toFloat() / targetProgress).coerceIn(0f, 1f)
        } else 0f
}

fun ChallengeUiItem.toChallengeState(): ChallengeState = ChallengeState(
    id = id,
    target = targetProgress,
    xpReward = xpReward,
    progress = currentProgress,
    isActive = isActive,
    isCompleted = isCompleted
)

/** Copies progress, active and completed from [states] onto the matching UI items. */
fun List<ChallengeUiItem>.withChallengeStates(states: List<ChallengeState>): List<ChallengeUiItem> {
    val byId = states.associateBy { it.id }
    return map { item ->
        val state = byId[item.id] ?: return@map item
        item.copy(
            currentProgress = state.progress,
            isActive = state.isActive,
            isCompleted = state.isCompleted
        )
    }
}

data class DayBarUi(
    val dayLabel: String,
    val fullDayName: String,
    val steps: Int,
    val goal: Int,
    val isToday: Boolean = false
) {
    val goalMet: Boolean get() = steps >= goal
    val fillFraction: Float get() = if (goal > 0) (steps.toFloat() / goal).coerceIn(0.12f, 1f) else 0.12f
}

data class SteppyUiState(
    /** False until the settings have been read from storage; nothing is shown or saved before. */
    val isLoaded: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val username: String = "Walker",
    val dateLabel: String = "Tuesday, May 21",
    val motivation: MainMotivation = MainMotivation.MOVE_MORE,
    val activityLevel: ActivityLevel = ActivityLevel.SOMEWHAT_ACTIVE,
    val selectedMascot: Mascot = Mascot.SLOTH,
    val dailyGoal: Int = 10000,
    /** The day [todaySteps] belongs to. Null until the ViewModel sets it. */
    val todayDate: LocalDate? = null,
    val todaySteps: Int = 0,
    /**
     * Today's steps by noon, for "Morning Mover". Grows until noon, then stays. Kept in the
     * daily history (with the snapshot worker's value), not in the saved progress.
     */
    val stepsBeforeNoon: Int = 0,
    val currentStreak: Int = 0,
    /** Hearts: a new user has none; 1 is earned per 7-day streak (see StreakCalculator). */
    val streakFreezes: Int = 0,
    val weeklyActiveDays: Int = 0,
    val baseXP: Int = 0,
    val totalHistoricalSteps: Int = 0,
    val totalActiveDays: Int = 0,
    val isManualMode: Boolean = false,
    val remindersEnabled: Boolean = true,
    val language: String = "English",
    val statusBannerMessage: String? = null,
    val weeklyBars: List<DayBarUi> = defaultWeeklyBars(goal = 10000, todaySteps = 0),
    val challenges: List<ChallengeUiItem> = catalogueChallenges()
) {
    val stepProgressFraction: Float
        get() = if (dailyGoal > 0) (todaySteps.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f

    val stepsRemaining: Int
        get() = (dailyGoal - todaySteps).coerceAtLeast(0)

    val isTodayGoalMet: Boolean
        get() = todaySteps >= dailyGoal

    val totalXP: Int
        get() = baseXP + XpCalculator.dayXp(todaySteps, dailyGoal)

    private val levelProgress: LevelProgress
        get() = XpCalculator.levelProgress(totalXP)

    val level: Int
        get() = levelProgress.level

    val currentLevelMinXP: Int
        get() = levelProgress.currentLevelMinXp

    val nextLevelXP: Int
        get() = levelProgress.nextLevelXp

    val xpToNextLevel: Int
        get() = levelProgress.xpToNextLevel

    val levelProgressFraction: Float
        get() = levelProgress.fraction
}

/** The part of the state kept in [com.steppy.app.data.settings.SettingsRepository]. */
fun SteppyUiState.settings(): UserSettings = UserSettings(
    isOnboardingCompleted = isOnboardingCompleted,
    username = username,
    mascot = selectedMascot,
    mainGoal = motivation,
    activityLevel = activityLevel,
    dailyGoal = dailyGoal,
    isManualMode = isManualMode,
    remindersEnabled = remindersEnabled
)

fun SteppyUiState.withSettings(settings: UserSettings): SteppyUiState = copy(
    isOnboardingCompleted = settings.isOnboardingCompleted,
    username = settings.username,
    selectedMascot = settings.mascot,
    motivation = settings.mainGoal,
    activityLevel = settings.activityLevel,
    dailyGoal = settings.dailyGoal,
    isManualMode = settings.isManualMode,
    remindersEnabled = settings.remindersEnabled,
    weeklyBars = defaultWeeklyBars(settings.dailyGoal, todaySteps)
)

/** The part of the state kept in [com.steppy.app.data.repository.SteppyRepository]. */
fun SteppyUiState.progressSnapshot(): ProgressSnapshot = ProgressSnapshot(
    todayDate = todayDate,
    todaySteps = todaySteps,
    streak = currentStreak,
    baseXp = baseXP,
    activeChallengeIds = challenges.filter { it.isActive }.map { it.id }.toSet(),
    completedChallengeIds = challenges.filter { it.isCompleted }.map { it.id }.toSet()
)

/** Replaces today's progress and rebuilds the challenges from the catalogue. */
fun SteppyUiState.withProgress(progress: ProgressSnapshot): SteppyUiState {
    val inputs = ChallengeProgressInputs.forToday(
        date = progress.todayDate ?: LocalDate.now(),
        todaySteps = progress.todaySteps,
        streak = progress.streak,
        weekSteps = progress.todaySteps,
        // Read from the restored history right after
        stepsBeforeNoon = 0
    )
    val challenges = catalogueChallenges(inputs).map { item ->
        val completed = item.id in progress.completedChallengeIds
        item.copy(
            isActive = item.id in progress.activeChallengeIds,
            isCompleted = completed,
            currentProgress = if (completed) item.targetProgress else item.currentProgress
        )
    }
    return copy(
        todayDate = progress.todayDate,
        todaySteps = progress.todaySteps,
        stepsBeforeNoon = 0,
        currentStreak = progress.streak,
        baseXP = progress.baseXp,
        totalHistoricalSteps = progress.todaySteps,
        weeklyBars = defaultWeeklyBars(dailyGoal, progress.todaySteps),
        challenges = challenges
    )
}

fun defaultWeeklyBars(goal: Int, todaySteps: Int): List<DayBarUi> = listOf(
    DayBarUi("L", "Monday", 0, goal),
    DayBarUi("M", "Tuesday", 0, goal),
    DayBarUi("X", "Wednesday", 0, goal),
    DayBarUi("J", "Thursday", 0, goal),
    DayBarUi("V", "Friday", 0, goal),
    DayBarUi("S", "Saturday", 0, goal),
    DayBarUi("D", "Sunday", todaySteps, goal, isToday = true)
)

/**
 * Every catalogue challenge, not active, with progress from [inputs]
 * (or 0 when [inputs] is null).
 */
fun catalogueChallenges(inputs: ChallengeProgressInputs? = null): List<ChallengeUiItem> =
    ChallengeCatalogue.ALL.map { definition ->
        ChallengeUiItem(
            definition = definition,
            currentProgress = inputs?.let { ChallengeCatalogue.progress(definition, it) } ?: 0
        )
    }
