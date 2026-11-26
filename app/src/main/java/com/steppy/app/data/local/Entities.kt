package com.steppy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.steppy.app.domain.DayProgress
import com.steppy.app.domain.StreakState
import java.time.LocalDate

/**
 * How a day's steps were counted. MANUAL means some steps were entered by hand, or the
 * sensor wasn't counting (no sensor, or permission denied) at some point that day.
 */
enum class StepSource { SENSOR, MANUAL }

/** A day that was MANUAL at any point stays MANUAL; later sensor updates never change it back. */
fun StepSource.mergedWith(earlier: StepSource?): StepSource =
    if (earlier == StepSource.MANUAL) StepSource.MANUAL else this

/**
 * Combines a new save of a day with the record already stored for it. The app and the
 * snapshot worker both save today, each with its own count, so the larger one wins:
 * - steps and steps before noon never go down (a late save with an older count changes nothing)
 * - steps before noon stay unknown (null) until a save before noon gives them
 * - the new goal is kept, and a MANUAL day stays MANUAL
 */
fun DailyRecord.mergedWith(existing: DailyRecord?): DailyRecord {
    if (existing == null) return this
    val mergedSteps = maxOf(steps, existing.steps)
    val mergedBeforeNoon = listOfNotNull(stepsBeforeNoon, existing.stepsBeforeNoon)
        .maxOrNull()
        ?.coerceAtMost(mergedSteps)
    return copy(
        steps = mergedSteps,
        stepsBeforeNoon = mergedBeforeNoon,
        source = source.mergedWith(existing.source)
    )
}

/** One row per day. Streak, hearts, XP and the charts are calculated from these rows. */
@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    /** Steps by noon, from the last save before 12:00 (±15 min); used by "Morning Mover". Null if none. */
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
