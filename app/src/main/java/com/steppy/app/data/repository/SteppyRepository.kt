package com.steppy.app.data.repository

import android.content.Context
import com.steppy.app.domain.ChallengeProgressInputs
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.catalogueChallenges
import java.time.LocalDate

/**
 * Loads and saves today's progress (steps, streak, XP, challenges), so storage can change
 * without touching any Compose screen. Settings are kept apart, in
 * [com.steppy.app.data.settings.SettingsRepository].
 */
interface SteppyRepository {
    /** Today's progress. Settings fields keep their defaults; the ViewModel fills them in. */
    fun loadInitialState(): SteppyUiState
    fun saveState(state: SteppyUiState)
    fun clearAllData()
}

/**
 * Local implementation backed by SharedPreferences: the data survives an app restart.
 * The settings that used to be in this file are moved to DataStore on first launch.
 */
class LocalSteppyRepository(context: Context) : SteppyRepository {
    private val prefs = context.getSharedPreferences("steppy_local_db", Context.MODE_PRIVATE)

    override fun loadInitialState(): SteppyUiState {
        val steps = prefs.getInt("today_steps", 0)
        val stepsDate = prefs.getString("today_date", null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val streak = prefs.getInt("streak", 0)
        val baseXp = prefs.getInt("base_xp", 0)
        val activeIds = prefs.getStringSet("active_challenges", emptySet()) ?: setOf("c1", "c2")
        val completedIds = prefs.getStringSet("completed_challenges", emptySet()) ?: emptySet()

        val progressInputs = ChallengeProgressInputs.forToday(
            date = stepsDate ?: LocalDate.now(),
            todaySteps = steps,
            streak = streak,
            weekSteps = steps
        )
        val challenges = catalogueChallenges(progressInputs).map { item ->
            val completed = completedIds.contains(item.id)
            item.copy(
                isActive = activeIds.contains(item.id),
                isCompleted = completed,
                currentProgress = if (completed) item.targetProgress else item.currentProgress
            )
        }

        return SteppyUiState(
            todayDate = stepsDate,
            todaySteps = steps,
            currentStreak = streak,
            baseXP = baseXp,
            totalHistoricalSteps = steps,
            challenges = challenges
        )
    }

    override fun saveState(state: SteppyUiState) {
        val activeIds = state.challenges.filter { it.isActive }.map { it.id }.toSet()
        val completedIds = state.challenges.filter { it.isCompleted }.map { it.id }.toSet()
        prefs.edit()
            // Never write the settings keys here: the DataStore migration would copy them again
            .putInt("today_steps", state.todaySteps)
            .putString("today_date", state.todayDate?.toString())
            .putInt("streak", state.currentStreak)
            .putInt("base_xp", state.baseXP)
            .putStringSet("active_challenges", activeIds)
            .putStringSet("completed_challenges", completedIds)
            .apply()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
    }
}