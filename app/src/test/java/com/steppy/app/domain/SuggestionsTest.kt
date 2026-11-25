package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {

    private val catalogue = listOf("c1", "c2", "c3", "c4", "c5")

    private fun order(mainGoal: MainMotivation, ids: List<String> = catalogue) =
        Suggestions.orderChallenges(ids, mainGoal) { it }

    @Test
    fun suggestedGoal_matchesActivityLevel() {
        assertEquals(3000, Suggestions.suggestedGoal(ActivityLevel.JUST_STARTING))
        assertEquals(6000, Suggestions.suggestedGoal(ActivityLevel.SOMEWHAT_ACTIVE))
        assertEquals(10000, Suggestions.suggestedGoal(ActivityLevel.VERY_ACTIVE))
    }

    @Test
    fun suggestedGoal_isAlwaysOneOfThePresets() {
        ActivityLevel.entries.forEach {
            assertTrue(Suggestions.suggestedGoal(it) in Suggestions.GOAL_PRESETS)
        }
    }

    @Test
    fun buildStreak_putsStreakChallengeFirst() {
        assertEquals(listOf("c2", "c1", "c3", "c4", "c5"), order(MainMotivation.BUILD_STREAK))
    }

    @Test
    fun moveMore_putsStepChallengesFirst() {
        assertEquals(listOf("c1", "c4", "c3", "c2", "c5"), order(MainMotivation.MOVE_MORE))
    }

    @Test
    fun stayHealthy_putsDailyHabitsFirst() {
        assertEquals(listOf("c5", "c1", "c2", "c3", "c4"), order(MainMotivation.STAY_HEALTHY))
    }

    @Test
    fun ordering_neverAddsOrDropsChallenges() {
        // Only some challenges are still available (others are active or completed)
        val available = listOf("c3", "c5", "c2")
        assertEquals(listOf("c2", "c3", "c5"), order(MainMotivation.BUILD_STREAK, available))
        assertEquals(listOf("c3", "c5", "c2"), order(MainMotivation.MOVE_MORE, available))
        assertEquals(listOf("c5", "c3", "c2"), order(MainMotivation.STAY_HEALTHY, available))
    }

    @Test
    fun unknownChallenges_keepCatalogueOrderAtTheEnd() {
        assertEquals(listOf("c2", "x9", "x1"), order(MainMotivation.BUILD_STREAK, listOf("x9", "c2", "x1")))
    }
}
