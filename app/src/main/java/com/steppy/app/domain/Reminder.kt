package com.steppy.app.domain

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** The evening reminder: when it is due and what it says. */
object Reminder {

    /** Fixed for now; choosing the time belongs to Profile (U8) and onboarding (U3). */
    val DEFAULT_TIME: LocalTime = LocalTime.of(19, 0)

    /** Time from [now] until the next [time] (later today, or tomorrow if it has passed). */
    fun delayUntilNext(now: LocalDateTime, time: LocalTime = DEFAULT_TIME): Duration {
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /**
     * True from [time] until midnight. A reminder that runs late (the phone was off) but
     * after midnight is skipped, so it never nudges about a day that has just started.
     */
    fun isDue(now: LocalDateTime, time: LocalTime = DEFAULT_TIME): Boolean =
        !now.toLocalTime().isBefore(time)

    /**
     * The reminder text, or null when today's goal is already met (no nudge needed).
     *
     * @param streak the streak that today's goal would keep going (the days before today)
     */
    fun message(todaySteps: Int, goal: Int, streak: Int, locale: Locale = Locale.getDefault()): String? {
        val left = goal - todaySteps
        if (left <= 0) return null
        val steps = NumberFormat.getIntegerInstance(locale).format(left)
        return if (streak > 0) {
            "$steps steps left to keep your $streak-day streak."
        } else {
            "$steps steps left to reach today's goal."
        }
    }
}
