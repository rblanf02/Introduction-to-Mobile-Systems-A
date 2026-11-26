package com.steppy.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val custom = UserSettings(
        isOnboardingCompleted = true,
        username = "Test",
        mascot = Mascot.OSTRICH,
        mainGoal = MainMotivation.STAY_HEALTHY,
        activityLevel = ActivityLevel.JUST_STARTING,
        dailyGoal = 3000,
        isManualMode = true,
        remindersEnabled = false
    )

    private fun dataStore(scope: CoroutineScope, name: String = "settings") =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "$name.preferences_pb") }
        )

    @Test
    fun emptyStore_givesTheDefaults() = runTest {
        val repo = DataStoreSettingsRepository(dataStore(backgroundScope))
        assertEquals(UserSettings(), repo.load())
    }

    @Test
    fun savedSettings_areReadBack() = runTest {
        val repo = DataStoreSettingsRepository(dataStore(backgroundScope))
        repo.save(custom)
        assertEquals(custom, repo.load())
    }

    @Test
    fun clear_givesTheDefaultsAgain() = runTest {
        val repo = DataStoreSettingsRepository(dataStore(backgroundScope))
        repo.save(custom)
        repo.clear()
        assertEquals(UserSettings(), repo.load())
    }

    @Test
    fun unknownEnumNames_fallBackToTheDefault() = runTest {
        val store = dataStore(backgroundScope)
        store.edit {
            it[stringPreferencesKey("mascot")] = "DRAGON"
            it[stringPreferencesKey("main_goal")] = ""
        }
        val loaded = DataStoreSettingsRepository(store).load()
        assertEquals(Mascot.SLOTH, loaded.mascot)
        assertEquals(MainMotivation.MOVE_MORE, loaded.mainGoal)
    }
}
