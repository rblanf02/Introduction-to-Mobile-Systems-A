package com.steppy.app.platform

import android.content.Context
import android.content.SharedPreferences
import com.steppy.app.domain.DailyStepCalculator
import com.steppy.app.domain.DailyStepRecord
import java.time.LocalDate

/**
 * Small local storage for role C (SharedPreferences file "steppy_platform").
 * Kept apart from the UI data, so the two parts never overwrite each other.
 */

class PlatformPreferences (context: Context){

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DATE = "step_record_date"
        private const val KEY_LAST_COUNTER = "step_record_last_counter"
        private const val KEY_TODAY_STEPS = "step_record_today_steps"
        private const val KEY_PERMISSION_ASKED = "permission_asked"

        private const val  PREFERENCE_NAME = "steppy_platform"
    }


    fun loadStepRecord(): DailyStepRecord? {
        val dateText: String? = prefs.getString(KEY_DATE, null)

        if(dateText == null){
            return  null
        }
        val date: LocalDate = LocalDate.parse(dateText)
        val lastCounter: Int = prefs.getInt(KEY_LAST_COUNTER, 0)
        val todaySteps: Int = prefs.getInt(KEY_TODAY_STEPS, 0)
        return DailyStepRecord(date, lastCounter, todaySteps)

    }


    fun saveStepRecord(record: DailyStepRecord) {
        prefs.edit()
            .putString(KEY_DATE, record.date.toString())
            .putInt(KEY_LAST_COUNTER, record.lastCounter)
            .putInt(KEY_TODAY_STEPS, record.todaySteps)
            .apply()
    }
    /**
     * Adds a new sensor reading to today's count and saves it. The app and the snapshot
     * worker both call this, so it runs one call at a time: a reading is never counted twice.
     */
    @Synchronized
    fun recordCounter(counterNow: Int, today: LocalDate): DailyStepRecord {
        val record = DailyStepCalculator.calculate(loadStepRecord(), counterNow, today)
        saveStepRecord(record)
        return record
    }

    fun wasPermissionAsked(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_ASKED, false)
    }

    fun setPermissionAsked() {
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
    }

}