package com.steppy.app.data.repository

import android.content.Context
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeProgressInputs
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import com.steppy.app.ui.SteppyUiState
import com.steppy.app.ui.catalogueChallenges
import com.steppy.app.ui.defaultWeeklyBars
import java.time.LocalDate

/**
 * Loads and saves the app state, so storage (SharedPreferences today, Room/DataStore later)
 * can change without touching any Compose screen.
 */
interface SteppyRepository {
    fun loadInitialState(): SteppyUiState
    fun saveState(state: SteppyUiState)
    fun clearAllData()
}

/**
 * Local implementation backed by SharedPreferences: the data survives an app restart.
 */
class LocalSteppyRepository(context: Context) : SteppyRepository {
    private val prefs = context.getSharedPreferences("steppy_local_db", Context.MODE_PRIVATE)

    override fun loadInitialState(): SteppyUiState {
        val onboarded = prefs.getBoolean("onboarded", false)
        val username = prefs.getString("username", "Aitor") ?: "Aitor"
        val mascotName = prefs.getString("mascot", Mascot.SLOTH.name) ?: Mascot.SLOTH.name
        val mascot = runCatching { Mascot.valueOf(mascotName) }.getOrDefault(Mascot.SLOTH)
        val motivation = runCatching {
            MainMotivation.valueOf(prefs.getString("main_goal", null) ?: "")
        }.getOrDefault(MainMotivation.MOVE_MORE)
        val activityLevel = runCatching {
            ActivityLevel.valueOf(prefs.getString("activity_level", null) ?: "")
        }.getOrDefault(ActivityLevel.SOMEWHAT_ACTIVE)
        val goal = prefs.getInt("daily_goal", 10000)
        val steps = prefs.getInt("today_steps", 0)
        val stepsDate = prefs.getString("today_date", null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val streak = prefs.getInt("streak", 0)
        val baseXp = prefs.getInt("base_xp", 0)
        val manual = prefs.getBoolean("manual_mode", false)
        val reminders = prefs.getBoolean("reminders", true)
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
            isOnboardingCompleted = onboarded,
            username = username,
            selectedMascot = mascot,
            motivation = motivation,
            activityLevel = activityLevel,
            dailyGoal = goal,
            todayDate = stepsDate,
            todaySteps = steps,
            currentStreak = streak,
            baseXP = baseXp,
            totalHistoricalSteps =  steps,
            isManualMode = manual,
            remindersEnabled = reminders,
            weeklyBars = defaultWeeklyBars(goal, steps),
            challenges = challenges
        )
    }

    override fun saveState(state: SteppyUiState) {
        val activeIds = state.challenges.filter { it.isActive }.map { it.id }.toSet()
        val completedIds = state.challenges.filter { it.isCompleted }.map { it.id }.toSet()
        prefs.edit()
            .putBoolean("onboarded", state.isOnboardingCompleted)
            .putString("username", state.username)
            .putString("mascot", state.selectedMascot.name)
            .putString("main_goal", state.motivation.name)
            .putString("activity_level", state.activityLevel.name)
            .putInt("daily_goal", state.dailyGoal)
            .putInt("today_steps", state.todaySteps)
            .putString("today_date", state.todayDate?.toString())
            .putInt("streak", state.currentStreak)
            .putInt("base_xp", state.baseXP)
            .putBoolean("manual_mode", state.isManualMode)
            .putBoolean("reminders", state.remindersEnabled)
            .putStringSet("active_challenges", activeIds)
            .putStringSet("completed_challenges", completedIds)
            .apply()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
    }
}