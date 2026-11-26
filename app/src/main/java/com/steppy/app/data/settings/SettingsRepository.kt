package com.steppy.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStoreFile
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import kotlinx.coroutines.flow.first

/** What the user chose in onboarding and Profile. Progress (steps, streak, XP) is stored elsewhere. */
data class UserSettings(
    val isOnboardingCompleted: Boolean = false,
    val username: String = "Walker",
    val mascot: Mascot = Mascot.SLOTH,
    val mainGoal: MainMotivation = MainMotivation.MOVE_MORE,
    val activityLevel: ActivityLevel = ActivityLevel.SOMEWHAT_ACTIVE,
    val dailyGoal: Int = 10000,
    /** True when the user logs steps by hand (no permission or no sensor). */
    val isManualMode: Boolean = false,
    val remindersEnabled: Boolean = true
)

interface SettingsRepository {
    suspend fun load(): UserSettings
    suspend fun save(settings: UserSettings)
    /** "Delete my data". */
    suspend fun clear()
}

/** Settings in a Preferences DataStore (file `settings.preferences_pb`). */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override suspend fun load(): UserSettings = dataStore.data.first().toSettings()

    override suspend fun save(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDED] = settings.isOnboardingCompleted
            prefs[Keys.USERNAME] = settings.username
            prefs[Keys.MASCOT] = settings.mascot.name
            prefs[Keys.MAIN_GOAL] = settings.mainGoal.name
            prefs[Keys.ACTIVITY_LEVEL] = settings.activityLevel.name
            prefs[Keys.DAILY_GOAL] = settings.dailyGoal
            prefs[Keys.MANUAL_MODE] = settings.isManualMode
            prefs[Keys.REMINDERS] = settings.remindersEnabled
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /**
     * Key names match the old SharedPreferences keys in "steppy_local_db",
     * so [migrationFrom] can copy them as they are.
     */
    internal object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val USERNAME = stringPreferencesKey("username")
        val MASCOT = stringPreferencesKey("mascot")
        val MAIN_GOAL = stringPreferencesKey("main_goal")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val MANUAL_MODE = booleanPreferencesKey("manual_mode")
        val REMINDERS = booleanPreferencesKey("reminders")

        val ALL = listOf(
            ONBOARDED, USERNAME, MASCOT, MAIN_GOAL, ACTIVITY_LEVEL, DAILY_GOAL, MANUAL_MODE, REMINDERS
        )
    }

    companion object {
        private const val FILE_NAME = "settings"
        /** The SharedPreferences file the settings were kept in before DataStore. */
        const val OLD_PREFERENCES_NAME = "steppy_local_db"

        /**
         * Moves the settings keys out of the old SharedPreferences file on first read.
         * The progress keys stay there.
         */
        fun migrationFrom(context: Context, sharedPreferencesName: String = OLD_PREFERENCES_NAME) =
            SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPreferencesName,
            keysToMigrate = Keys.ALL.map { it.name }.toSet()
            )

        /** Create only one per process (AppContainer does this). */
        fun create(context: Context): DataStoreSettingsRepository {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                migrations = listOf(migrationFrom(appContext)),
                produceFile = { appContext.preferencesDataStoreFile(FILE_NAME) }
            )
            return DataStoreSettingsRepository(dataStore)
        }

        private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

        internal fun Preferences.toSettings(): UserSettings {
            val defaults = UserSettings()
            return UserSettings(
                isOnboardingCompleted = this[Keys.ONBOARDED] ?: defaults.isOnboardingCompleted,
                username = this[Keys.USERNAME] ?: defaults.username,
                mascot = enumOrDefault(this[Keys.MASCOT], defaults.mascot),
                mainGoal = enumOrDefault(this[Keys.MAIN_GOAL], defaults.mainGoal),
                activityLevel = enumOrDefault(this[Keys.ACTIVITY_LEVEL], defaults.activityLevel),
                dailyGoal = this[Keys.DAILY_GOAL] ?: defaults.dailyGoal,
                isManualMode = this[Keys.MANUAL_MODE] ?: defaults.isManualMode,
                remindersEnabled = this[Keys.REMINDERS] ?: defaults.remindersEnabled
            )
        }
    }
}
