package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class MorningStepsTest {

    @Test
    fun beforeNoon_allOfTodaysStepsCount() {
        assertEquals(1800, MorningSteps.stepsBeforeNoon(LocalTime.of(9, 30), 1800))
        assertEquals(2500, MorningSteps.stepsBeforeNoon(LocalTime.of(11, 59, 59), 2500))
    }

    @Test
    fun fromNoon_theValueIsNoLongerUpdated() {
        assertNull(MorningSteps.stepsBeforeNoon(LocalTime.NOON, 3000))
        assertNull(MorningSteps.stepsBeforeNoon(LocalTime.of(23, 0), 3000))
    }

    @Test
    fun forToday_usesTheGivenStepsBeforeNoon() {
        val inputs = ChallengeProgressInputs.forToday(
            date = java.time.LocalDate.of(2026, 11, 25),
            todaySteps = 6000,
            streak = 0,
            weekSteps = 6000,
            stepsBeforeNoon = 1200
        )
        assertEquals(1200, ChallengeCatalogue.progress(ChallengeCatalogue.byId(ChallengeCatalogue.MORNING_MOVER)!!, inputs))
    }
}
