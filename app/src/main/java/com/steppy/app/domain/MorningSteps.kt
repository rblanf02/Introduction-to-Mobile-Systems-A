package com.steppy.app.domain

import java.time.LocalTime

/** "Morning Mover" counts the steps taken before noon. */
object MorningSteps {

    val NOON: LocalTime = LocalTime.NOON

    /**
     * Today's steps before noon, as known at [time]: before noon that is all of today's
     * steps so far; from noon on it can't change any more, so null (keep the last value).
     */
    fun stepsBeforeNoon(time: LocalTime, todaySteps: Int): Int? =
        if (time.isBefore(NOON)) todaySteps else null
}
