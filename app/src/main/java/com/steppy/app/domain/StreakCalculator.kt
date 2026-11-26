package com.steppy.app.domain

import java.time.LocalDate

/**
 * Steps and goal of one day. The goal is the one that applied on that day,
 * so changing the goal never rewrites history.
 */
data class DayProgress(
    val date: LocalDate,
    val steps: Int,
    val goal: Int
) {
    val goalMet: Boolean get() = steps >= goal
}

/**
 * @param streak current streak length in days
 * @param hearts streak freezes available (0..[StreakCalculator.MAX_HEARTS])
 * @param streakLostOn the most recent day a streak > 0 was reset to 0, or null if it never happened
 */
data class StreakState(
    val streak: Int = 0,
    val hearts: Int = 0,
    val streakLostOn: LocalDate? = null
)

object StreakCalculator {

    const val DAYS_PER_HEART = 7
    const val MAX_HEARTS = 2

    /**
     * Replays every day from [from] to [today] (both included) on top of [start].
     *
     * - Goal met: streak +1, and every 7th day of the streak earns 1 heart (max 2).
     * - Past day missed: uses 1 heart and the streak stays the same; with 0 hearts the streak resets to 0.
     * - A day with no entry in [days] counts as missed.
     * - Today is still in progress: it only counts once the goal is met, never as a miss.
     *
     * @param start the state on the day before [from] (all zeros for a new user)
     * @param days the recorded days; entries outside [from]..[today] are ignored
     */
    fun calculate(
        start: StreakState,
        days: List<DayProgress>,
        from: LocalDate,
        today: LocalDate
    ): StreakState {
        val byDate: Map<LocalDate, DayProgress> = days.associateBy { it.date }

        var streak = start.streak
        var hearts = start.hearts.coerceIn(0, MAX_HEARTS)
        var streakLostOn = start.streakLostOn

        var date = from
        while (!date.isAfter(today)) {
            val goalMet = byDate[date]?.goalMet ?: false

            if (goalMet) {
                streak += 1
                if (streak % DAYS_PER_HEART == 0) {
                    hearts = (hearts + 1).coerceAtMost(MAX_HEARTS)
                }
            } else if (date != today) {
                if (hearts > 0) {
                    hearts -= 1
                } else {
                    if (streak > 0) streakLostOn = date
                    streak = 0
                }
            }

            date = date.plusDays(1)
        }

        return StreakState(streak, hearts, streakLostOn)
    }

    /**
     * The state at the end of the day before [today]: every past day is replayed and
     * today is left out, so the result doesn't change while today's steps grow.
     * Add today on top with [withToday].
     */
    fun stateBefore(
        start: StreakState,
        days: List<DayProgress>,
        from: LocalDate,
        today: LocalDate
    ): StreakState = calculate(start, days.filter { it.date.isBefore(today) }, from, today)

    /** [before] plus today's progress: +1 (and maybe a heart) once today's goal is met. */
    fun withToday(before: StreakState, today: DayProgress): StreakState =
        calculate(before, listOf(today), today.date, today.date)
}
