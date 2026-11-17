package com.steppy.app.platform.sensor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate


class DailyStepCalculatorTest {

    private val monday = LocalDate.of(2026, 11, 16)
    private val tuesday = LocalDate.of(2026, 11, 17)

    @Test
    fun firstReading_startsAtZero() {
        val result = DailyStepCalculator.calculate(null, 5000, monday)
        assertEquals(0, result.todaySteps)
        assertEquals(5000, result.lastCounter)
    }

    @Test
    fun sameDay_addsNewSteps() {
        val previous = DailyStepRecord(monday, 5000, 100)
        val result = DailyStepCalculator.calculate(previous, 5300, monday)
        assertEquals(400, result.todaySteps)
    }

    @Test
    fun newDay_resetsToZero() {
        val previous = DailyStepRecord(monday, 5000, 8000)
        val result = DailyStepCalculator.calculate(previous, 5200, tuesday)
        assertEquals(0, result.todaySteps)
        assertEquals(tuesday, result.date)
    }

    @Test
    fun reboot_keepsStepsAlreadyCounted() {
        val previous = DailyStepRecord(monday, 5000, 1000)
        val result = DailyStepCalculator.calculate(previous, 50, monday)
        assertEquals(1050, result.todaySteps)
    }


}