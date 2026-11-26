package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val goal = 10000
    private val start = LocalDate.of(2026, 11, 2)

    private fun day(offset: Int): LocalDate = start.plusDays(offset.toLong())

    private fun met(offset: Int) = DayProgress(day(offset), goal, goal)

    private fun missed(offset: Int) = DayProgress(day(offset), goal - 1, goal)

    /** [pattern]: 'x' = goal met, '.' = missed. The last character is today. */
    private fun run(pattern: String, initial: StreakState = StreakState()): StreakState {
        val days = pattern.mapIndexed { i, c -> if (c == 'x') met(i) else missed(i) }
        return StreakCalculator.calculate(initial, days, day(0), day(pattern.length - 1))
    }

    @Test
    fun newUser_startsAtZero() {
        val result = run(".")
        assertEquals(0, result.streak)
        assertEquals(0, result.hearts)
        assertNull(result.streakLostOn)
    }

    @Test
    fun goalMetEachDay_streakGrows() {
        assertEquals(3, run("xxx").streak)
    }

    @Test
    fun goalNotMetYetToday_streakKeepsYesterdaysValue() {
        val result = run("xx.")
        assertEquals(2, result.streak)
        assertNull(result.streakLostOn)
    }

    @Test
    fun sevenDayStreak_earnsOneHeart() {
        assertEquals(0, run("xxxxxx").hearts)
        assertEquals(1, run("xxxxxxx").hearts)
    }

    @Test
    fun hearts_neverMoreThanTwo() {
        val result = run("x".repeat(21))
        assertEquals(21, result.streak)
        assertEquals(StreakCalculator.MAX_HEARTS, result.hearts)
    }

    @Test
    fun missedDay_withTwoHearts_usesOneAndKeepsStreak() {
        val result = run(".x", StreakState(streak = 14, hearts = 2))
        assertEquals(15, result.streak)
        assertEquals(1, result.hearts)
    }

    @Test
    fun missedDay_withOneHeart_usesItAndKeepsStreak() {
        val result = run(".x", StreakState(streak = 7, hearts = 1))
        assertEquals(8, result.streak)
        assertEquals(0, result.hearts)
        assertNull(result.streakLostOn)
    }

    @Test
    fun missedDay_withZeroHearts_resetsStreak() {
        val result = run("xxx.x")
        assertEquals(1, result.streak)
        assertEquals(0, result.hearts)
        assertEquals(day(3), result.streakLostOn)
    }

    @Test
    fun frozenDay_doesNotCountTowardsNextHeart() {
        // 7 days (heart earned), frozen day uses it, then 6 more days: streak 13, no new heart yet
        val result = run("xxxxxxx.xxxxxx")
        assertEquals(13, result.streak)
        assertEquals(0, result.hearts)
        // One more day reaches 14 and earns a heart again
        assertEquals(1, run("xxxxxxx.xxxxxxx").hearts)
    }

    @Test
    fun dayWithoutRecord_countsAsMissed() {
        val days = listOf(met(0), met(2))
        val result = StreakCalculator.calculate(StreakState(), days, day(0), day(2))
        assertEquals(1, result.streak)
        assertEquals(day(1), result.streakLostOn)
    }

    @Test
    fun goalIsTakenFromEachDay() {
        // Day 0 had a goal of 3,000; the goal is now 10,000. 5,000 steps on day 0 still counts.
        val days = listOf(DayProgress(day(0), 5000, 3000), DayProgress(day(1), 5000, 10000))
        val result = StreakCalculator.calculate(StreakState(), days, day(0), day(1))
        assertEquals(1, result.streak)
    }

    @Test
    fun startsFromBaseline() {
        val result = run("xx", StreakState(streak = 40, hearts = 1))
        assertEquals(42, result.streak)
        assertEquals(2, result.hearts) // day 42 is a multiple of 7
    }

    @Test
    fun streakAlreadyZero_missedDay_doesNotRecordLoss() {
        assertNull(run("..x").streakLostOn)
    }

    @Test
    fun stateBefore_leavesTodayOut_evenWhenItsGoalIsMet() {
        val days = listOf(met(0), met(1), met(2))
        val before = StreakCalculator.stateBefore(StreakState(), days, day(0), day(2))
        assertEquals(2, before.streak)
    }

    @Test
    fun stateBefore_dayWithNoRecord_countsAsMissed() {
        val before = StreakCalculator.stateBefore(StreakState(), listOf(met(0)), day(0), day(2))
        assertEquals(0, before.streak)
        assertEquals(day(1), before.streakLostOn)
    }

    @Test
    fun stateBefore_thenWithToday_matchesCalculate() {
        val days = "xxxxxx.xxxxxxx".mapIndexed { i, c -> if (c == 'x') met(i) else missed(i) }
        val today = days.last()
        val before = StreakCalculator.stateBefore(StreakState(), days, day(0), today.date)
        assertEquals(
            StreakCalculator.calculate(StreakState(), days, day(0), today.date),
            StreakCalculator.withToday(before, today)
        )
    }

    @Test
    fun withToday_goalNotMet_changesNothing() {
        val before = StreakState(streak = 5, hearts = 1)
        assertEquals(before, StreakCalculator.withToday(before, missed(0)))
    }

    @Test
    fun withToday_seventhDay_earnsAHeart() {
        val result = StreakCalculator.withToday(StreakState(streak = 6), met(0))
        assertEquals(7, result.streak)
        assertEquals(1, result.hearts)
    }
}
