package com.steppy.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Uses its own file names, so it never touches the app's real data. */
@RunWith(AndroidJUnit4::class)
class SettingsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val oldPrefsName = "settings_migration_test_prefs"
    private val oldPrefs = context.getSharedPreferences(oldPrefsName, Context.MODE_PRIVATE)
    private lateinit var storeFile: File
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Before
    fun setUp() {
        oldPrefs.edit().clear().commit()
        storeFile = File(context.filesDir, "datastore/settings_migration_test.preferences_pb")
        storeFile.delete()
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        oldPrefs.edit().clear().commit()
        storeFile.delete()
    }

    @Test
    fun oldSettingsAreMoved_andProgressStaysBehind() = runBlocking {
        oldPrefs.edit()
            .putBoolean("onboarded", true)
            .putString("username", "Old")
            .putString("mascot", Mascot.PUPPY.name)
            .putString("main_goal", MainMotivation.BUILD_STREAK.name)
            .putString("activity_level", ActivityLevel.VERY_ACTIVE.name)
            .putInt("daily_goal", 12000)
            .putBoolean("manual_mode", true)
            .putBoolean("reminders", false)
            .putInt("today_steps", 4321)
            .commit()

        val store = PreferenceDataStoreFactory.create(
            scope = storeScope,
            migrations = listOf(DataStoreSettingsRepository.migrationFrom(context, oldPrefsName)),
            produceFile = { storeFile }
        )
        val settings = DataStoreSettingsRepository(store).load()

        assertEquals(
            UserSettings(
                isOnboardingCompleted = true,
                username = "Old",
                mascot = Mascot.PUPPY,
                mainGoal = MainMotivation.BUILD_STREAK,
                activityLevel = ActivityLevel.VERY_ACTIVE,
                dailyGoal = 12000,
                isManualMode = true,
                remindersEnabled = false
            ),
            settings
        )
        assertFalse("Settings keys are removed from the old file", oldPrefs.contains("username"))
        assertTrue("Progress keys stay", oldPrefs.contains("today_steps"))
    }
}
