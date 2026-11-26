package com.steppy.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** What a challenge counts. Every challenge is step-based or streak-based (PROJECT_PLAN §5). */
enum class ChallengeMetric(val unit: String) {
    /** Today's steps. */
    DAILY_STEPS("steps"),
    /** Current streak in days. */
    STREAK_DAYS("days"),
    /** Steps on Saturday and Sunday of the current week. */
    WEEKEND_STEPS("steps"),
    /** Steps in the current week. */
    WEEKLY_STEPS("steps"),
    /** Today's steps taken before noon. */
    STEPS_BEFORE_NOON("steps")
}

/**
 * A challenge as it is offered: fixed text, target and reward.
 * It holds no user progress; that lives in the UI state and in Room (`UserChallenge`).
 *
 * @param id stable id, saved with the user's challenges. Never change or reuse one.
 */
data class ChallengeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val target: Int,
    val metric: ChallengeMetric
) {
    val unit: String get() = metric.unit
}

/**
 * The values challenge progress is read from.
 *
 * @param weekendSteps steps on this week's Saturday and Sunday
 * @param stepsBeforeNoon steps before noon today
 */
data class ChallengeProgressInputs(
    val todaySteps: Int,
    val streak: Int,
    val weekSteps: Int,
    val weekendSteps: Int,
    val stepsBeforeNoon: Int
) {
    companion object {
        /**
         * Inputs when only today's steps are known.
         * Until the history is read (U7), weekend steps are today's steps on a Saturday or Sunday.
         *
         * @param stepsBeforeNoon today's steps by noon (see [MorningSteps]), 0 if unknown
         */
        fun forToday(
            date: LocalDate,
            todaySteps: Int,
            streak: Int,
            weekSteps: Int,
            stepsBeforeNoon: Int
        ) =
            ChallengeProgressInputs(
                todaySteps = todaySteps,
                streak = streak,
                weekSteps = weekSteps,
                weekendSteps = if (date.isWeekend()) todaySteps else 0,
                stepsBeforeNoon = stepsBeforeNoon
            )

        private fun LocalDate.isWeekend() =
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }
}

/** The challenges built into the app. Steppy has no backend, so this list is the whole catalogue. */
object ChallengeCatalogue {

    const val TEN_K_STEPS = "c1"
    const val SEVEN_DAY_STREAK = "c2"
    const val WEEKEND_WALKER = "c3"
    const val FIFTY_K_WEEK = "c4"
    const val MORNING_MOVER = "c5"

    /** In display order. */
    val ALL: List<ChallengeDefinition> = listOf(
        ChallengeDefinition(
            id = TEN_K_STEPS,
            title = "10K Steps Challenge",
            description = "Reach 10,000 steps in a single day",
            xpReward = 100,
            target = 10000,
            metric = ChallengeMetric.DAILY_STEPS
        ),
        ChallengeDefinition(
            id = SEVEN_DAY_STREAK,
            title = "7-Day Streak",
            description = "Keep your daily streak alive for 7 days",
            xpReward = 150,
            target = 7,
            metric = ChallengeMetric.STREAK_DAYS
        ),
        ChallengeDefinition(
            id = WEEKEND_WALKER,
            title = "Weekend Walker",
            description = "Walk 15,000 steps this weekend",
            xpReward = 100,
            target = 15000,
            metric = ChallengeMetric.WEEKEND_STEPS
        ),
        ChallengeDefinition(
            id = FIFTY_K_WEEK,
            title = "50K This Week",
            description = "Reach 50,000 steps in one week",
            xpReward = 250,
            target = 50000,
            metric = ChallengeMetric.WEEKLY_STEPS
        ),
        ChallengeDefinition(
            id = MORNING_MOVER,
            title = "Morning Mover",
            description = "Take 2,000 steps before noon",
            xpReward = 120,
            target = 2000,
            metric = ChallengeMetric.STEPS_BEFORE_NOON
        )
    )

    private val byId = ALL.associateBy { it.id }

    fun byId(id: String): ChallengeDefinition? = byId[id]

    /** Progress towards [definition]'s target, never above the target. */
    fun progress(definition: ChallengeDefinition, inputs: ChallengeProgressInputs): Int {
        val value = when (definition.metric) {
            ChallengeMetric.DAILY_STEPS -> inputs.todaySteps
            ChallengeMetric.STREAK_DAYS -> inputs.streak
            ChallengeMetric.WEEKEND_STEPS -> inputs.weekendSteps
            ChallengeMetric.WEEKLY_STEPS -> inputs.weekSteps
            ChallengeMetric.STEPS_BEFORE_NOON -> inputs.stepsBeforeNoon
        }
        return value.coerceIn(0, definition.target)
    }

    /** Progress for every challenge in the catalogue, by id. */
    fun progressById(inputs: ChallengeProgressInputs): Map<String, Int> =
        ALL.associate { it.id to progress(it, inputs) }
}
