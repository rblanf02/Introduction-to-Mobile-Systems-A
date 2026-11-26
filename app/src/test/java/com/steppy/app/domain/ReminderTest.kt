package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class ReminderTest {

    private val day = LocalDate.of(2026, 11, 26)
    private val seven = LocalTime.of(19, 0)

    @Test
    fun delay_laterToday() {
        assertEquals(Duration.ofHours(4), Reminder.delayUntilNext(day.atTime(15, 0), seven))
    }

    @Test
    fun delay_atOrAfterTheTime_isTomorrow() {
        assertEquals(Duration.ofHours(24), Reminder.delayUntilNext(day.atTime(19, 0), seven))
        assertEquals(Duration.ofHours(23).plusMinutes(30), Reminder.delayUntilNext(day.atTime(19, 30), seven))
    }

    @Test
    fun isDue_fromTheTimeUntilMidnight() {
        assertTrue(Reminder.isDue(day.atTime(19, 0), seven))
        assertTrue(Reminder.isDue(day.atTime(23, 59), seven))
        // A run delayed past midnight is about a day that has just started
        assertFalse(Reminder.isDue(day.atTime(0, 30), seven))
        assertFalse(Reminder.isDue(day.atTime(18, 59), seven))
    }

    @Test
    fun message_withAStreak_mentionsTheStreak() {
        assertEquals(
            "1,200 steps left to keep your 12-day streak.",
            Reminder.message(todaySteps = 4800, goal = 6000, streak = 12, locale = Locale.US)
        )
    }

    @Test
    fun message_withoutAStreak_mentionsTheGoal() {
        assertEquals(
            "6,000 steps left to reach today's goal.",
            Reminder.message(todaySteps = 0, goal = 6000, streak = 0, locale = Locale.US)
        )
    }

    @Test
    fun message_goalMet_noReminder() {
        assertNull(Reminder.message(todaySteps = 6000, goal = 6000, streak = 3, locale = Locale.US))
        assertNull(Reminder.message(todaySteps = 9000, goal = 6000, streak = 0, locale = Locale.US))
    }
}
