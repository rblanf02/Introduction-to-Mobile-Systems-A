package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class XpCalculatorTest {

    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun dayXp_isStepsDividedBy100() {
        assertEquals(78, XpCalculator.dayXp(steps = 7842, goal = 10000))
    }

    @Test
    fun dayXp_addsBonusWhenGoalMet() {
        assertEquals(150, XpCalculator.dayXp(steps = 10000, goal = 10000))
    }

    @Test
    fun dayXp_noStepsGivesNothing() {
        assertEquals(0, XpCalculator.dayXp(steps = 0, goal = 10000))
        assertEquals(0, XpCalculator.dayXp(steps = -5, goal = 10000))
    }

    @Test
    fun dayXp_usesThatDaysGoal() {
        // 5,000 steps meets a 3,000 goal but not a 10,000 goal
        assertEquals(100, XpCalculator.dayXp(DayProgress(monday, 5000, 3000)))
        assertEquals(50, XpCalculator.dayXp(DayProgress(monday, 5000, 10000)))
    }

    @Test
    fun totalXp_addsBaselineDaysAndChallenges() {
        val days = listOf(
            DayProgress(monday, 10000, 10000),            // 150
            DayProgress(monday.plusDays(1), 4000, 10000)  // 40
        )
        val total = XpCalculator.totalXp(xpBefore = 300, days = days, completedChallengeRewards = listOf(100, 250))
        assertEquals(300 + 150 + 40 + 100 + 250, total)
    }

    @Test
    fun totalXp_newUserIsZero() {
        assertEquals(0, XpCalculator.totalXp(0, emptyList(), emptyList()))
    }

    @Test
    fun level_thresholds() {
        assertEquals(1, XpCalculator.levelFor(0))
        assertEquals(1, XpCalculator.levelFor(49))
        assertEquals(2, XpCalculator.levelFor(50))
        assertEquals(2, XpCalculator.levelFor(199))
        assertEquals(3, XpCalculator.levelFor(200))
        assertEquals(4, XpCalculator.levelFor(450))
        assertEquals(11, XpCalculator.levelFor(5000))
    }

    @Test
    fun level_matchesMinXpForEveryLevel() {
        for (level in 1..200) {
            val min = XpCalculator.minXpForLevel(level)
            assertEquals(level, XpCalculator.levelFor(min))
            if (min > 0) assertEquals(level - 1, XpCalculator.levelFor(min - 1))
        }
    }

    @Test
    fun levelProgress_middleOfLevel() {
        // Level 2 goes from 50 to 200 XP; 125 XP is halfway
        val progress = XpCalculator.levelProgress(125)
        assertEquals(2, progress.level)
        assertEquals(50, progress.currentLevelMinXp)
        assertEquals(200, progress.nextLevelXp)
        assertEquals(75, progress.xpToNextLevel)
        assertEquals(0.5f, progress.fraction, 0.0001f)
    }

    @Test
    fun levelProgress_exactlyOnThreshold_startsAtZero() {
        val progress = XpCalculator.levelProgress(200)
        assertEquals(3, progress.level)
        assertEquals(0f, progress.fraction, 0.0001f)
        assertEquals(250, progress.xpToNextLevel)
    }
}
