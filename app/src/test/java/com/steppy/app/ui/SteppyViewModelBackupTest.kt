package com.steppy.app.ui

import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.UserChallenge
import com.steppy.app.data.local.mergedWith
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.data.repository.replayStreakBefore
import com.steppy.app.data.settings.SettingsRepository
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.MainMotivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SteppyViewModelBackupTest {

    private class FakeProgress(var saved: SteppyUiState = SteppyUiState()) : SteppyRepository {
        override fun loadInitialState(): SteppyUiState = saved
        override fun saveState(state: SteppyUiState) { saved = state }
        override fun clearAllData() { saved = SteppyUiState() }
    }

    private class FakeSettingsRepository(var settings: UserSettings = UserSettings()) : SettingsRepository {
        override suspend fun load() = settings
        override suspend fun save(settings: UserSettings) { this.settings = settings }
        override suspend fun clear() { settings = UserSettings() }
    }

    /** Keeps a full [HistoryData], like the Room tables. */
    private class FakeHistory(var data: HistoryData = HistoryData(null, emptyList(), emptyList())) :
        DailyHistoryRepository {
        override suspend fun saveDay(
            date: LocalDate,
            steps: Int,
            goal: Int,
            source: StepSource,
            stepsBeforeNoon: Int?
        ) {
            // Same merge as the Room repository
            val record = DailyRecord(date, steps, stepsBeforeNoon, goal, source)
                .mergedWith(data.days.firstOrNull { it.date == date })
            data = data.copy(
                baseline = data.baseline ?: ProgressBaseline.newUser(date),
                days = (data.days.filter { it.date != date } + record).sortedBy { it.date }
            )
        }
        override suspend fun clearAll() { data = HistoryData(null, emptyList(), emptyList()) }
        override suspend fun exportAll() = data
        override suspend fun getDay(date: LocalDate) = data.days.firstOrNull { it.date == date }
        override suspend fun replaceAll(data: HistoryData) { this.data = data }
        override suspend fun streakBefore(date: LocalDate) = replayStreakBefore(data.baseline, data.days, date)
    }

    private val monday = LocalDate.of(2026, 11, 23)
    private val tuesday = monday.plusDays(1)
    private var clock = tuesday

    private val oldPhoneSettings = UserSettings(
        isOnboardingCompleted = true,
        username = "Old phone",
        mascot = Mascot.OSTRICH,
        mainGoal = MainMotivation.STAY_HEALTHY,
        dailyGoal = 7000
    )

    private class Phone(
        val progress: FakeProgress,
        val settings: FakeSettingsRepository,
        val history: FakeHistory,
        val vm: SteppyViewModel
    )

    private fun phone(
        progress: SteppyUiState = SteppyUiState(),
        settings: UserSettings = UserSettings(),
        history: HistoryData = HistoryData(null, emptyList(), emptyList())
    ): Phone {
        val p = FakeProgress(progress)
        val s = FakeSettingsRepository(settings)
        val h = FakeHistory(history)
        return Phone(p, s, h, SteppyViewModel(p, s, h, timeOfDay = { LocalTime.of(15, 0) }) { clock })
    }

    /** An onboarded phone with yesterday's record, today's steps and an activated challenge. */
    private fun oldPhone(): Phone {
        val old = phone(
            progress = SteppyUiState(todayDate = tuesday, todaySteps = 0, currentStreak = 4, baseXP = 900),
            settings = oldPhoneSettings,
            history = HistoryData(
                baseline = ProgressBaseline.newUser(monday),
                days = listOf(DailyRecord(monday, 8000, null, 7000, StepSource.SENSOR)),
                challenges = listOf(UserChallenge("c1", monday, null))
            )
        )
        old.vm.activateChallenge("c1")
        old.vm.addSteps(3000)
        return old
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun backupAndRestore_onAnotherPhone_givesTheSameProgressAndHistory() = runTest {
        val old = oldPhone()
        val json = old.vm.createBackupJson()

        val new = phone()
        new.vm.onRestoreFileRead(json)
        new.vm.confirmRestore()

        val oldState = old.vm.uiState.value
        val newState = new.vm.uiState.value
        assertEquals(oldState.settings(), newState.settings())
        assertEquals(oldState.progressSnapshot(), newState.progressSnapshot())
        assertEquals(oldState.totalXP, newState.totalXP)
        assertEquals(oldState.level, newState.level)
        assertEquals(old.history.data, new.history.data)
        // Saved to every store, so it survives a restart
        assertEquals(oldPhoneSettings, new.settings.settings)
        assertEquals(oldState.progressSnapshot(), new.progress.saved.progressSnapshot())
        assertEquals("Backup restored.", newState.statusBannerMessage)
    }

    @Test
    fun pickingAFile_changesNothingUntilConfirmed() = runTest {
        val json = oldPhone().vm.createBackupJson()
        val new = phone(progress = SteppyUiState(todayDate = tuesday, todaySteps = 123))

        new.vm.onRestoreFileRead(json)

        assertNotNull(new.vm.pendingRestore.value)
        assertEquals(123, new.vm.uiState.value.todaySteps)
        assertTrue(new.history.data.days.isEmpty())
    }

    @Test
    fun cancellingTheRestore_keepsTheCurrentData() = runTest {
        val json = oldPhone().vm.createBackupJson()
        val new = phone(progress = SteppyUiState(todayDate = tuesday, todaySteps = 123))

        new.vm.onRestoreFileRead(json)
        new.vm.cancelRestore()
        new.vm.confirmRestore()

        assertNull(new.vm.pendingRestore.value)
        assertEquals(123, new.vm.uiState.value.todaySteps)
        assertEquals(UserSettings(), new.settings.settings)
    }

    @Test
    fun invalidFile_showsWhyAndChangesNothing() {
        val new = phone(progress = SteppyUiState(todayDate = tuesday, todaySteps = 123))

        new.vm.onRestoreFileRead("not a backup")

        assertNull(new.vm.pendingRestore.value)
        assertEquals(123, new.vm.uiState.value.todaySteps)
        assertEquals(
            "This isn't a Steppy backup file. Nothing was changed.",
            new.vm.uiState.value.statusBannerMessage
        )
    }

    @Test
    fun unreadableFile_showsAMessage() {
        val new = phone()
        new.vm.onRestoreFileRead(null)
        assertEquals("The file couldn't be opened. Nothing was changed.", new.vm.uiState.value.statusBannerMessage)
    }

    @Test
    fun backupFromAnEarlierDay_closesThatDayAndStartsTodayAtZero() = runTest {
        val json = oldPhone().vm.createBackupJson()

        clock = tuesday.plusDays(1)
        val new = phone()
        new.vm.onRestoreFileRead(json)
        new.vm.confirmRestore()

        val state = new.vm.uiState.value
        assertEquals(clock, state.todayDate)
        assertEquals(0, state.todaySteps)
        assertEquals(3000, new.history.data.days.single { it.date == tuesday }.steps)
        assertEquals(0, new.history.data.days.single { it.date == clock }.steps)
    }

    @Test
    fun backupFile_includesTodaysLatestSteps() = runTest {
        val old = oldPhone()
        old.vm.addSteps(250)
        val json = old.vm.createBackupJson()

        val new = phone()
        new.vm.onRestoreFileRead(json)
        new.vm.confirmRestore()

        assertEquals(3250, new.vm.uiState.value.todaySteps)
        assertEquals(3250, new.history.data.days.single { it.date == tuesday }.steps)
    }
}
