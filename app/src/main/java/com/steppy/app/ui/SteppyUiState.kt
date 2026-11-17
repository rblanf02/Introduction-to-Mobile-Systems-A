package com.steppy.app.ui

import android.content.Context
import kotlin.math.floor
import kotlin.math.sqrt

enum class Mascot(val displayName: String, val emoji: String) {
    SLOTH("Sloth", "🦥"),
    PUPPY("Puppy", "🐶"),
    OSTRICH("Ostrich", "🦤"),
    IGUANA("Iguana", "🦎")
}

enum class MainMotivation(val label: String) {
    MOVE_MORE("Move more"),
    BUILD_STREAK("Build a streak"),
    STAY_HEALTHY("Stay healthy")
}

enum class ActivityLevel(val label: String) {
    JUST_STARTING("Just starting"),
    SOMEWHAT_ACTIVE("Somewhat active"),
    VERY_ACTIVE("Very active")
}

data class ChallengeUiItem(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val unit: String,
    val isActive: Boolean,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (targetProgress > 0) {
            (currentProgress.toFloat() / targetProgress).coerceIn(0f, 1f)
        } else 0f
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
    val isOnboardingCompleted: Boolean = false,
    val username: String = "Aitor",
    val dateLabel: String = "Tuesday, May 21",
    val motivation: MainMotivation = MainMotivation.MOVE_MORE,
    val activityLevel: ActivityLevel = ActivityLevel.SOMEWHAT_ACTIVE,
    val selectedMascot: Mascot = Mascot.SLOTH,
    val dailyGoal: Int = 10000,
    val todaySteps: Int = 0,
    val currentStreak: Int = 0,
    val streakFreezes: Int = 1,
    val weeklyActiveDays: Int = 0,
    val baseXP: Int = 0,
    val totalHistoricalSteps: Int = 0,
    val totalActiveDays: Int = 0,
    val isManualMode: Boolean = false,
    val remindersEnabled: Boolean = true,
    val language: String = "English",
    val statusBannerMessage: String? = null,
    val weeklyBars: List<DayBarUi> = defaultWeeklyBars(goal = 10000, todaySteps = 0),
    val challenges: List<ChallengeUiItem> = defaultChallenges(todaySteps = 0, streak = 0, weeklySteps = 0)) {
    val stepProgressFraction: Float
        get() = if (dailyGoal > 0) (todaySteps.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f

    val stepsRemaining: Int
        get() = (dailyGoal - todaySteps).coerceAtLeast(0)

    val isTodayGoalMet: Boolean
        get() = todaySteps >= dailyGoal

    val totalXP: Int
        get() = baseXP + (todaySteps / 100) + (if (isTodayGoalMet) 50 else 0)

    val level: Int
        get() = if (totalXP <= 0) 1 else floor(sqrt(totalXP / 50.0)).toInt() + 1

    val currentLevelMinXP: Int
        get() = ((level - 1) * (level - 1)) * 50

    val nextLevelXP: Int
        get() = (level * level) * 50

    val xpToNextLevel: Int
        get() = (nextLevelXP - totalXP).coerceAtLeast(0)

    val levelProgressFraction: Float
        get() {
            val range = (nextLevelXP - currentLevelMinXP).coerceAtLeast(1)
            return ((totalXP - currentLevelMinXP).toFloat() / range).coerceIn(0f, 1f)
        }
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

fun defaultChallenges(todaySteps: Int, streak: Int, weeklySteps: Int): List<ChallengeUiItem> = listOf(
    ChallengeUiItem(
        id = "c1",
        title = "10K Steps Challenge",
        description = "Reach 10,000 steps in a single day",
        xpReward = 100,
        currentProgress = todaySteps,
        targetProgress = 10000,
        unit = "steps",
        isActive = false,
        isCompleted = todaySteps >= 10000
    ),
    ChallengeUiItem(
        id = "c2",
        title = "7-Day Streak",
        description = "Keep your daily streak alive for 7 days",
        xpReward = 150,
        currentProgress = streak,
        targetProgress = 7,
        unit = "days",
        isActive = false,
        isCompleted = streak >= 7
    ),
    ChallengeUiItem(
        id = "c3",
        title = "Weekend Walker",
        description = "Complete 3 walks this weekend",
        xpReward = 100,
        currentProgress = 0,
        targetProgress = 3,
        unit = "walks",
        isActive = false
    ),
    ChallengeUiItem(
        id = "c4",
        title = "50K This Week",
        description = "Reach 50,000 steps in one week",
        xpReward = 250,
        currentProgress = weeklySteps,
        targetProgress = 50000,
        unit = "steps",
        isActive = false,
        isCompleted = weeklySteps >= 50000
    ),
    ChallengeUiItem(
        id = "c5",
        title = "Morning Mover",
        description = "Take 2,000 steps before noon",
        xpReward = 120,
        currentProgress = todaySteps.coerceAtMost(2000),
        targetProgress = 2000,
        unit = "steps",
        isActive = false,
        isCompleted = todaySteps >= 2000
    )
)

/**
 * Contrato de datos para que tus compañeros conecten Room, DataStore y StepSensorManager
 * sin modificar ninguna pantalla de Compose.
 */
interface SteppyRepository {
    fun loadInitialState(): SteppyUiState
    fun saveState(state: SteppyUiState)
    fun clearAllData()
}

/**
 * Implementación persistente local lista para usar: los datos sobreviven al reiniciar la app.
 */
class LocalSteppyRepository(context: Context) : SteppyRepository {
    private val prefs = context.getSharedPreferences("steppy_local_db", Context.MODE_PRIVATE)

    override fun loadInitialState(): SteppyUiState {
        val onboarded = prefs.getBoolean("onboarded", false)
        val username = prefs.getString("username", "Aitor") ?: "Aitor"
        val mascotName = prefs.getString("mascot", Mascot.SLOTH.name) ?: Mascot.SLOTH.name
        val mascot = runCatching { Mascot.valueOf(mascotName) }.getOrDefault(Mascot.SLOTH)
        val goal = prefs.getInt("daily_goal", 10000)
        val steps = prefs.getInt("today_steps", 0)
        val streak = prefs.getInt("streak", 0)
        val baseXp = prefs.getInt("base_xp", 0)
        val manual = prefs.getBoolean("manual_mode", false)
        val reminders = prefs.getBoolean("reminders", true)
        val activeIds = prefs.getStringSet("active_challenges", emptySet()) ?: setOf("c1", "c2")

        val challenges = defaultChallenges(steps, streak, steps).map { item ->
            item.copy(isActive = activeIds.contains(item.id))
        }

        return SteppyUiState(
            isOnboardingCompleted = onboarded,
            username = username,
            selectedMascot = mascot,
            dailyGoal = goal,
            todaySteps = steps,
            currentStreak = streak,
            baseXP = baseXp,
            totalHistoricalSteps =  steps,
            isManualMode = manual,
            remindersEnabled = reminders,
            weeklyBars = defaultWeeklyBars(goal, steps),
            challenges = challenges
        )
    }

    override fun saveState(state: SteppyUiState) {
        val activeIds = state.challenges.filter { it.isActive }.map { it.id }.toSet()
        prefs.edit()
            .putBoolean("onboarded", state.isOnboardingCompleted)
            .putString("username", state.username)
            .putString("mascot", state.selectedMascot.name)
            .putInt("daily_goal", state.dailyGoal)
            .putInt("today_steps", state.todaySteps)
            .putInt("streak", state.currentStreak)
            .putInt("base_xp", state.baseXP)
            .putBoolean("manual_mode", state.isManualMode)
            .putBoolean("reminders", state.remindersEnabled)
            .putStringSet("active_challenges", activeIds)
            .apply()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
    }
}