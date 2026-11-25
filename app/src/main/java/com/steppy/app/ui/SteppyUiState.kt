package com.steppy.app.ui

import com.steppy.app.domain.LevelProgress
import com.steppy.app.domain.XpCalculator

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
