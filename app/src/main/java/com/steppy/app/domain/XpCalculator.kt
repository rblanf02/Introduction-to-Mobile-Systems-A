package com.steppy.app.domain

import kotlin.math.sqrt

/**
 * Where a total amount of XP sits between two levels.
 *
 * @param currentLevelMinXp XP needed to reach [level]
 * @param nextLevelXp XP needed to reach [level] + 1
 */
data class LevelProgress(
    val totalXp: Int,
    val level: Int,
    val currentLevelMinXp: Int,
    val nextLevelXp: Int
) {
    val xpToNextLevel: Int get() = (nextLevelXp - totalXp).coerceAtLeast(0)

    /** 0..1, how far the user is from [currentLevelMinXp] to [nextLevelXp]. */
    val fraction: Float
        get() {
            val range = (nextLevelXp - currentLevelMinXp).coerceAtLeast(1)
            return ((totalXp - currentLevelMinXp).toFloat() / range).coerceIn(0f, 1f)
        }
}

object XpCalculator {

    const val STEPS_PER_XP = 100
    const val GOAL_BONUS_XP = 50
    const val XP_PER_LEVEL_UNIT = 50

    /** XP earned on one day: `steps / 100`, plus 50 if the goal was met. */
    fun dayXp(steps: Int, goal: Int): Int {
        if (steps <= 0) return 0
        val bonus = if (steps >= goal) GOAL_BONUS_XP else 0
        return steps / STEPS_PER_XP + bonus
    }

    fun dayXp(day: DayProgress): Int = dayXp(day.steps, day.goal)

    /**
     * @param xpBefore XP earned before the first day in [days] (0 for a new user)
     * @param days daily progress, each day judged against its own goal
     * @param completedChallengeRewards the XP reward of each completed challenge, counted once
     */
    fun totalXp(
        xpBefore: Int,
        days: List<DayProgress>,
        completedChallengeRewards: List<Int>
    ): Int = xpBefore.coerceAtLeast(0) +
        days.sumOf { dayXp(it) } +
        completedChallengeRewards.sumOf { it.coerceAtLeast(0) }

    /** `level = floor(sqrt(xp / 50)) + 1`, so level 1 starts at 0 XP, level 2 at 50, level 3 at 200. */
    fun levelFor(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        var level = sqrt(totalXp / XP_PER_LEVEL_UNIT.toDouble()).toInt() + 1
        // Guard against floating-point rounding at exact thresholds
        while (minXpForLevel(level + 1) <= totalXp) level++
        while (level > 1 && minXpForLevel(level) > totalXp) level--
        return level
    }

    /** XP needed to reach [level]: `(level - 1)² × 50`. */
    fun minXpForLevel(level: Int): Int {
        val steps = (level - 1).coerceAtLeast(0)
        return steps * steps * XP_PER_LEVEL_UNIT
    }

    fun levelProgress(totalXp: Int): LevelProgress {
        val xp = totalXp.coerceAtLeast(0)
        val level = levelFor(xp)
        return LevelProgress(
            totalXp = xp,
            level = level,
            currentLevelMinXp = minXpForLevel(level),
            nextLevelXp = minXpForLevel(level + 1)
        )
    }
}
