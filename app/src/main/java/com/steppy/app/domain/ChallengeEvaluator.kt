package com.steppy.app.domain

/**
 * What the rules need to know about one challenge.
 *
 * @param progress current value towards [target] (steps or days)
 */
data class ChallengeState(
    val id: String,
    val target: Int,
    val xpReward: Int,
    val progress: Int,
    val isActive: Boolean,
    val isCompleted: Boolean
)

sealed interface ActivationResult {
    data class Activated(val challenges: List<ChallengeState>) : ActivationResult
    /** Already [ChallengeEvaluator.MAX_ACTIVE] challenges in progress. */
    data object LimitReached : ActivationResult
    /** Unknown id, already active, or already completed (one-time challenges are not offered again). */
    data object NotAvailable : ActivationResult
}

/**
 * @param xpAwarded XP from the challenges completed in this evaluation, to add to the total once
 * @param newlyCompleted ids of the challenges completed in this evaluation
 */
data class ChallengeEvaluation(
    val challenges: List<ChallengeState>,
    val xpAwarded: Int,
    val newlyCompleted: List<String>
)

/**
 * Challenge rules. They only read and return challenges, never the streak,
 * so a challenge that is not completed can never affect the streak.
 */
object ChallengeEvaluator {

    const val MAX_ACTIVE = 3

    /** Challenges that are active and not completed yet. Completed ones free their slot. */
    fun inProgressCount(challenges: List<ChallengeState>): Int =
        challenges.count { it.isActive && !it.isCompleted }

    fun activate(challenges: List<ChallengeState>, id: String): ActivationResult {
        val challenge = challenges.firstOrNull { it.id == id }
        if (challenge == null || challenge.isActive || challenge.isCompleted) {
            return ActivationResult.NotAvailable
        }
        if (inProgressCount(challenges) >= MAX_ACTIVE) {
            return ActivationResult.LimitReached
        }
        return ActivationResult.Activated(
            challenges.map { if (it.id == id) it.copy(isActive = true) else it }
        )
    }

    /**
     * Updates progress and completes every active challenge that reached its target.
     * A challenge can complete on the same day it is activated.
     *
     * @param progressById new progress per challenge id; challenges not in the map keep their progress
     */
    fun evaluate(
        challenges: List<ChallengeState>,
        progressById: Map<String, Int> = emptyMap()
    ): ChallengeEvaluation {
        var xpAwarded = 0
        val newlyCompleted = mutableListOf<String>()

        val updated = challenges.map { challenge ->
            // A completed challenge keeps its final progress and is never awarded again
            if (challenge.isCompleted) return@map challenge

            val progress = progressById[challenge.id] ?: challenge.progress
            val completesNow = challenge.isActive && progress >= challenge.target
            if (completesNow) {
                xpAwarded += challenge.xpReward
                newlyCompleted += challenge.id
            }
            challenge.copy(progress = progress, isCompleted = completesNow)
        }

        return ChallengeEvaluation(updated, xpAwarded, newlyCompleted)
    }
}
