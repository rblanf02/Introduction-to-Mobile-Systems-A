package com.steppy.walkweather.domain

enum class WalkRating(val label: String) {
    GOOD("Good for a walk"),
    OK("OK with a jacket"),
    POOR("Better stay in")
}

/** Turns an hourly forecast into walking advice. Pure Kotlin, so it is unit-tested. */
object WalkAdvice {

    fun rate(hour: HourForecast): WalkRating {
        val code = hour.weatherCode
        val stormOrHeavy = code in 95..99 || code in setOf(65, 67, 75, 82, 86)
        if (stormOrHeavy || hour.precipitationChance >= 60 || hour.windKmh >= 40 ||
            hour.temperatureC < -5 || hour.temperatureC > 35
        ) return WalkRating.POOR

        val dryAndMild = hour.precipitationChance <= 20 && code in 0..3 &&
            hour.temperatureC in 8.0..28.0 && hour.windKmh < 25
        return if (dryAndMild) WalkRating.GOOD else WalkRating.OK
    }

    /** The first hour rated [WalkRating.GOOD], or else the first [WalkRating.OK] one. */
    fun bestHour(hours: List<HourForecast>): HourForecast? =
        hours.firstOrNull { rate(it) == WalkRating.GOOD } ?: hours.firstOrNull { rate(it) == WalkRating.OK }

    /** Short text for a WMO weather code. */
    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Unknown"
    }
}
