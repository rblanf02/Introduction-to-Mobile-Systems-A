package com.steppy.app.data.repository

import android.content.Context
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.progressSnapshot
import com.steppy.app.ui.withProgress
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

/** Today's progress as it is stored: what [SteppyRepository] saves and a backup contains. */
data class ProgressSnapshot(
    /** The day [todaySteps] belongs to. Null in data saved before dates were stored. */
    val todayDate: LocalDate?,
    val todaySteps: Int,
    val streak: Int,
    /** XP from past days and completed challenges; today's step XP is added on top. */
    val baseXp: Int,
    val activeChallengeIds: Set<String>,
    val completedChallengeIds: Set<String>
)

/**
 * Local implementation backed by SharedPreferences: the data survives an app restart.
 * The settings that used to be in this file are moved to DataStore on first launch.
 */
class LocalSteppyRepository(context: Context) : SteppyRepository {
    private val prefs = context.getSharedPreferences("steppy_local_db", Context.MODE_PRIVATE)

    override fun loadInitialState(): SteppyUiState {
        val snapshot = ProgressSnapshot(
            todayDate = prefs.getString("today_date", null)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            todaySteps = prefs.getInt("today_steps", 0),
            streak = prefs.getInt("streak", 0),
            baseXp = prefs.getInt("base_xp", 0),
            activeChallengeIds = prefs.getStringSet("active_challenges", null) ?: emptySet(),
            completedChallengeIds = prefs.getStringSet("completed_challenges", null) ?: emptySet()
        )
        return SteppyUiState().withProgress(snapshot)
    }

    override fun saveState(state: SteppyUiState) {
        val snapshot = state.progressSnapshot()
        prefs.edit()
            // Never write the settings keys here: the DataStore migration would copy them again
            .putInt("today_steps", snapshot.todaySteps)
            .putString("today_date", snapshot.todayDate?.toString())
            .putInt("streak", snapshot.streak)
            .putInt("base_xp", snapshot.baseXp)
            .putStringSet("active_challenges", snapshot.activeChallengeIds)
            .putStringSet("completed_challenges", snapshot.completedChallengeIds)
            .apply()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
    }
}