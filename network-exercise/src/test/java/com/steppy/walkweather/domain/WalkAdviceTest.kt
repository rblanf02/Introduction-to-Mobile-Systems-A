package com.steppy.walkweather.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class WalkAdviceTest {

    private fun hour(h: Int = 14, temp: Double = 18.0, rain: Int = 0, code: Int = 0, wind: Double = 10.0) =
        HourForecast(LocalDateTime.of(2026, 11, 26, h, 0), temp, rain, code, wind)

    @Test
    fun dryMildCalm_isGood() = assertEquals(WalkRating.GOOD, WalkAdvice.rate(hour()))

    @Test
    fun coldButDry_isOk() = assertEquals(WalkRating.OK, WalkAdvice.rate(hour(temp = 2.0)))

    @Test
    fun likelyRain_isPoor() = assertEquals(WalkRating.POOR, WalkAdvice.rate(hour(rain = 70, code = 61)))

    @Test
    fun thunderstorm_isPoor() = assertEquals(WalkRating.POOR, WalkAdvice.rate(hour(code = 95, rain = 10)))

    @Test
    fun strongWind_isPoor() = assertEquals(WalkRating.POOR, WalkAdvice.rate(hour(wind = 45.0)))

    @Test
    fun bestHour_isTheFirstGoodOne_elseTheFirstOk() {
        val hours = listOf(hour(13, rain = 80), hour(14, temp = 3.0), hour(15), hour(16))
        assertEquals(15, WalkAdvice.bestHour(hours)!!.time.hour)
        assertEquals(14, WalkAdvice.bestHour(hours.take(2))!!.time.hour)
        assertNull(WalkAdvice.bestHour(hours.take(1)))
    }

    @Test
    fun weatherCodes_haveText() {
        assertEquals("Clear", WalkAdvice.describe(0))
        assertEquals("Rain", WalkAdvice.describe(63))
        assertEquals("Thunderstorm", WalkAdvice.describe(96))
        assertEquals("Unknown", WalkAdvice.describe(1234))
    }
}
