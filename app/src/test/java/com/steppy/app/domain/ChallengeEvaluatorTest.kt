package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeEvaluatorTest {

    private fun challenge(
        id: String,
        target: Int = 10000,
        xp: Int = 100,
        progress: Int = 0,
        active: Boolean = false,
        completed: Boolean = false
    ) = ChallengeState(id, target, xp, progress, active, completed)

    private fun ActivationResult.challenges(): List<ChallengeState> =
        (this as ActivationResult.Activated).challenges

    // --- Activation ---

    @Test
    fun activate_movesChallengeToActive() {
        val result = ChallengeEvaluator.activate(listOf(challenge("c1")), "c1")
        assertTrue(result.challenges().single().isActive)
    }

    @Test
    fun activate_fourthChallenge_isBlocked() {
        val challenges = listOf(
            challenge("c1", active = true),
            challenge("c2", active = true),
            challenge("c3", active = true),
            challenge("c4")
        )
        assertEquals(ActivationResult.LimitReached, ChallengeEvaluator.activate(challenges, "c4"))
    }

    @Test
    fun activate_completedChallengesDoNotUseASlot() {
        val challenges = listOf(
            challenge("c1", active = true, completed = true),
            challenge("c2", active = true),
            challenge("c3", active = true),
            challenge("c4")
        )
        val result = ChallengeEvaluator.activate(challenges, "c4")
        assertEquals(3, ChallengeEvaluator.inProgressCount(result.challenges()))
    }

    @Test
    fun activate_completedChallenge_isNotOfferedAgain() {
        val challenges = listOf(challenge("c1", completed = true))
        assertEquals(ActivationResult.NotAvailable, ChallengeEvaluator.activate(challenges, "c1"))
    }

    @Test
    fun activate_alreadyActiveOrUnknown_isNotAvailable() {
        val challenges = listOf(challenge("c1", active = true))
        assertEquals(ActivationResult.NotAvailable, ChallengeEvaluator.activate(challenges, "c1"))
        assertEquals(ActivationResult.NotAvailable, ChallengeEvaluator.activate(challenges, "nope"))
    }

    // --- Completion and XP ---

    @Test
    fun evaluate_activeChallengeReachingTarget_completesAndAwardsXp() {
        val challenges = listOf(challenge("c1", target = 10000, xp = 100, active = true))
        val result = ChallengeEvaluator.evaluate(challenges, mapOf("c1" to 10000))
        assertTrue(result.challenges.single().isCompleted)
        assertEquals(100, result.xpAwarded)
        assertEquals(listOf("c1"), result.newlyCompleted)
    }

    @Test
    fun evaluate_belowTarget_onlyUpdatesProgress() {
        val challenges = listOf(challenge("c1", active = true))
        val result = ChallengeEvaluator.evaluate(challenges, mapOf("c1" to 7842))
        assertEquals(7842, result.challenges.single().progress)
        assertFalse(result.challenges.single().isCompleted)
        assertEquals(0, result.xpAwarded)
    }

    @Test
    fun evaluate_xpIsAwardedOnlyOnce() {
        val first = ChallengeEvaluator.evaluate(
            listOf(challenge("c1", xp = 100, active = true)), mapOf("c1" to 10000)
        )
        val second = ChallengeEvaluator.evaluate(first.challenges, mapOf("c1" to 12000))
        assertEquals(100, first.xpAwarded)
        assertEquals(0, second.xpAwarded)
        assertTrue(second.newlyCompleted.isEmpty())
    }

    @Test
    fun evaluate_completedChallengeKeepsItsFinalProgress() {
        val completed = challenge("c1", progress = 10000, active = true, completed = true)
        // The next day the step count starts again from 0
        val result = ChallengeEvaluator.evaluate(listOf(completed), mapOf("c1" to 0))
        assertEquals(10000, result.challenges.single().progress)
        assertTrue(result.challenges.single().isCompleted)
    }

    @Test
    fun evaluate_inactiveChallenge_neverCompletes() {
        val challenges = listOf(challenge("c1", target = 2000, active = false))
        val result = ChallengeEvaluator.evaluate(challenges, mapOf("c1" to 5000))
        assertFalse(result.challenges.single().isCompleted)
        assertEquals(0, result.xpAwarded)
    }

    @Test
    fun activatedChallenge_alreadyAtTarget_completesTheSameDay() {
        // 12,000 steps already walked today, then "10K Steps" is activated
        val challenges = listOf(challenge("c1", target = 10000, xp = 100, progress = 12000))
        val activated = ChallengeEvaluator.activate(challenges, "c1").challenges()
        val result = ChallengeEvaluator.evaluate(activated)
        assertTrue(result.challenges.single().isCompleted)
        assertEquals(100, result.xpAwarded)
    }

    @Test
    fun evaluate_severalCompleteAtOnce_addsAllRewards() {
        val challenges = listOf(
            challenge("c1", target = 10000, xp = 100, active = true),
            challenge("c5", target = 2000, xp = 120, active = true),
            challenge("c4", target = 50000, xp = 250, active = true)
        )
        val result = ChallengeEvaluator.evaluate(
            challenges, mapOf("c1" to 10000, "c5" to 2000, "c4" to 10000)
        )
        assertEquals(220, result.xpAwarded)
        assertEquals(listOf("c1", "c5"), result.newlyCompleted)
    }

    @Test
    fun evaluate_missingProgress_keepsCurrentValue() {
        val challenges = listOf(challenge("c3", progress = 4000, active = true))
        val result = ChallengeEvaluator.evaluate(challenges, mapOf("c1" to 9000))
        assertEquals(4000, result.challenges.single().progress)
    }
}
