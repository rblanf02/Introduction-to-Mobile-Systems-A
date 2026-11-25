package com.steppy.app.domain

import java.time.LocalDate


/**
 * Object represent a daily record
 *
 * @param date the day of the record belong to
 * @param lastCounter last value read (since reboot)
 * @param todaySteps step counted
 */

data class DailyStepRecord(
    val date: LocalDate,
    val lastCounter: Int,
    val todaySteps: Int
)

object DailyStepCalculator {

    /**
     * @param previous what we saved after the last reading, or null the first time
     * @param counterNow the value the sensor gives us now
     * @param today the current date
     * @return the new record to save for current steps
     */
    fun calculate (previous: DailyStepRecord?,counterNow: Int, today: LocalDate) : DailyStepRecord {

        if (previous == null || previous.date != today){
            return DailyStepRecord(today,counterNow,0)
        }

        if(counterNow < previous.lastCounter){
            val newSteps = counterNow
            val dailySteeps = previous.todaySteps + newSteps
            return DailyStepRecord(today,newSteps, dailySteeps)
        }

        val newSteps = counterNow - previous.lastCounter
        return DailyStepRecord(today, counterNow,previous.todaySteps + newSteps)


    }
}