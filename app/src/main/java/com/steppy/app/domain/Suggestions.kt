package com.steppy.app.domain

/** Suggestions based on what the user told us in Setup. They never change any rule. */
object Suggestions {

    /** The daily goal presets offered in Setup and Profile. */
    val GOAL_PRESETS: List<Int> = listOf(3000, 6000, 10000, 15000)

    /** The preset preselected for each activity level. 15,000 is never suggested, but can be picked. */
    fun suggestedGoal(level: ActivityLevel): Int = when (level) {
        ActivityLevel.JUST_STARTING -> 3000
        ActivityLevel.SOMEWHAT_ACTIVE -> 6000
        ActivityLevel.VERY_ACTIVE -> 10000
    }

    /** Challenge ids shown first for each main goal, in this order. */
    private fun firstChallengeIds(mainGoal: MainMotivation): List<String> = when (mainGoal) {
        MainMotivation.MOVE_MORE -> listOf(
            ChallengeCatalogue.TEN_K_STEPS,
            ChallengeCatalogue.FIFTY_K_WEEK,
            ChallengeCatalogue.WEEKEND_WALKER
        )
        MainMotivation.BUILD_STREAK -> listOf(ChallengeCatalogue.SEVEN_DAY_STREAK)
        // Everyday habits
        MainMotivation.STAY_HEALTHY -> listOf(
            ChallengeCatalogue.MORNING_MOVER,
            ChallengeCatalogue.TEN_K_STEPS
        )
    }

    /**
     * Puts the challenges that match [mainGoal] first. The others keep their catalogue order.
     *
     * @param id reads the challenge id from an item
     */
    fun <T> orderChallenges(challenges: List<T>, mainGoal: MainMotivation, id: (T) -> String): List<T> {
        val first = firstChallengeIds(mainGoal)
        return challenges.sortedBy { item ->
            val index = first.indexOf(id(item))
            if (index >= 0) index else first.size
        }
    }
}
