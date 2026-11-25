package com.steppy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.steppy.app.domain.DayProgress
import com.steppy.app.domain.StreakState
import java.time.LocalDate

enum class StepSource { SENSOR, MANUAL }

/** One row per day. Streak, hearts, XP and the charts are calculated from these rows. */
@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    /** Saved by the snapshot worker (P4); used by "Morning Mover". Null until then. */
    val stepsBeforeNoon: Int?,
    /** The goal *on that day*: changing the goal must not rewrite history. */
    val goal: Int,
    val source: StepSource
)

/** A challenge the user activated. Challenges never activated have no row. */
@Entity(tableName = "user_challenges")
data class UserChallenge(
    @PrimaryKey val challengeId: String,
    val activatedOn: LocalDate,
    /** Null while active; XP is awarded once when it is set. */
    val completedOn: LocalDate?
)

/**
 * Everything earned before the first [DailyRecord] row. All zeros for a new user;
 * only non-zero after a transfer that sends a limited history window.
 */
@Entity(tableName = "progress_baseline")
data class ProgressBaseline(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    /** First day covered by [DailyRecord] rows. */
    val asOf: LocalDate,
    val xpBefore: Int,
    /** Streak length on the day before [asOf]. */
    val streakBefore: Int,
    /** Hearts available on the day before [asOf]. */
    val heartsBefore: Int,
    /** Goal-met days before [asOf] ("Active days" counter). */
    val activeDaysBefore: Int
) {
    companion object {
        const val SINGLE_ROW_ID = 0

        fun newUser(installDate: LocalDate) = ProgressBaseline(
            asOf = installDate,
            xpBefore = 0,
            streakBefore = 0,
            heartsBefore = 0,
            activeDaysBefore = 0
        )
    }
}

fun DailyRecord.toDayProgress(): DayProgress = DayProgress(date, steps, goal)

fun ProgressBaseline.toStreakState(): StreakState =
    StreakState(streak = streakBefore, hearts = heartsBefore)
