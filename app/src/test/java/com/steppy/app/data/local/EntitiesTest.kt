package com.steppy.app.data.local

import com.steppy.app.domain.DayProgress
import com.steppy.app.domain.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EntitiesTest {

    private val converters = Converters()

    @Test
    fun dateConverter_roundTrip() {
        val date = LocalDate.of(2026, 11, 25)
        assertEquals("2026-11-25", converters.dateToText(date))
        assertEquals(date, converters.textToDate("2026-11-25"))
    }

    @Test
    fun dateConverter_null() {
        assertNull(converters.dateToText(null))
        assertNull(converters.textToDate(null))
    }

    @Test
    fun dateText_sortsInCalendarOrder() {
        // The DAOs compare dates as text, so this must hold across months and years
        val dates = listOf(
            LocalDate.of(2027, 1, 2),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2026, 2, 9),
            LocalDate.of(2026, 10, 1)
        )
        val byText = dates.sortedBy { converters.dateToText(it) }
        assertEquals(dates.sorted(), byText)
    }

    @Test
    fun dailyRecord_toDayProgress_keepsThatDaysGoal() {
        val record = DailyRecord(LocalDate.of(2026, 11, 25), 5000, null, 3000, StepSource.MANUAL)
        assertEquals(DayProgress(LocalDate.of(2026, 11, 25), 5000, 3000), record.toDayProgress())
        assertTrue(record.toDayProgress().goalMet)
    }

    @Test
    fun newUserBaseline_isAllZeros() {
        val install = LocalDate.of(2026, 11, 25)
        val baseline = ProgressBaseline.newUser(install)
        assertEquals(install, baseline.asOf)
        assertEquals(0, baseline.xpBefore)
        assertEquals(0, baseline.activeDaysBefore)
        assertEquals(StreakState(streak = 0, hearts = 0), baseline.toStreakState())
    }

    @Test
    fun baseline_toStreakState() {
        val baseline = ProgressBaseline(
            asOf = LocalDate.of(2026, 6, 1),
            xpBefore = 4200,
            streakBefore = 210,
            heartsBefore = 2,
            activeDaysBefore = 230
        )
        assertEquals(StreakState(streak = 210, hearts = 2), baseline.toStreakState())
    }

    @Test
    fun stepSource_manualDayStaysManual() {
        assertEquals(StepSource.SENSOR, StepSource.SENSOR.mergedWith(null))
        assertEquals(StepSource.SENSOR, StepSource.SENSOR.mergedWith(StepSource.SENSOR))
        assertEquals(StepSource.MANUAL, StepSource.MANUAL.mergedWith(StepSource.SENSOR))
        assertEquals(StepSource.MANUAL, StepSource.SENSOR.mergedWith(StepSource.MANUAL))
    }
}
