package com.steppy.app.ui

import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.data.settings.SettingsRepository
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.domain.StreakState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SteppyViewModelSettingsTest {

    private class FakeProgress(var saved: SteppyUiState = SteppyUiState()) : SteppyRepository {
        var saves = 0
        override fun loadInitialState(): SteppyUiState = saved
        override fun saveState(state: SteppyUiState) { saved = state; saves++ }
        override fun clearAllData() { saved = SteppyUiState() }
    }

    /** Settings that are only returned once [ready] completes, like a slow disk read. */
    private class FakeSettingsRepository(
        var settings: UserSettings,
        val ready: CompletableDeferred<Unit> = CompletableDeferred(Unit)
    ) : SettingsRepository {
        var saves = 0
        override suspend fun load(): UserSettings { ready.await(); return settings }
        override suspend fun save(settings: UserSettings) { this.settings = settings; saves++ }
        override suspend fun clear() { settings = UserSettings() }
    }

    private object NoHistory : DailyHistoryRepository {
        override suspend fun saveDay(
            date: LocalDate,
            steps: Int,
            goal: Int,
            source: StepSource,
            stepsBeforeNoon: Int?
        ) = Unit
        override suspend fun getDay(date: LocalDate): DailyRecord? = null
        override suspend fun clearAll() = Unit
        override suspend fun exportAll() = HistoryData(null, emptyList(), emptyList())
        override suspend fun replaceAll(data: HistoryData) = Unit
        override suspend fun streakBefore(date: LocalDate): StreakState? = null
    }

    private val monday = LocalDate.of(2026, 11, 23)
    private val onboarded = UserSettings(
        isOnboardingCompleted = true,
        username = "Test",
        mascot = Mascot.IGUANA,
        mainGoal = MainMotivation.BUILD_STREAK,
        activityLevel = ActivityLevel.VERY_ACTIVE,
        dailyGoal = 8000,
        isManualMode = true,
        remindersEnabled = false
    )

    private fun viewModel(progress: FakeProgress, settings: FakeSettingsRepository) =
        SteppyViewModel(progress, settings, NoHistory, timeOfDay = { LocalTime.of(15, 0) }) { monday }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startup_readsEverySettingIntoTheState() {
        val vm = viewModel(FakeProgress(), FakeSettingsRepository(onboarded))
        val state = vm.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(onboarded, state.settings())
        assertEquals(8000, state.weeklyBars.first().goal)
    }

    @Test
    fun changingASetting_savesIt() {
        val settings = FakeSettingsRepository(onboarded)
        val vm = viewModel(FakeProgress(), settings)

        vm.updateDailyGoal(12000)
        vm.updateUsernameAndMascot("Ana", Mascot.PUPPY)
        vm.toggleReminders(true)

        assertEquals(
            onboarded.copy(dailyGoal = 12000, username = "Ana", mascot = Mascot.PUPPY, remindersEnabled = true),
            settings.settings
        )
    }

    @Test
    fun addingSteps_doesNotRewriteTheSettings() {
        val settings = FakeSettingsRepository(onboarded)
        val vm = viewModel(FakeProgress(SteppyUiState(todayDate = monday)), settings)

        vm.addSteps(1000)
        vm.addSteps(1000)

        assertEquals(0, settings.saves)
    }

    @Test
    fun beforeSettingsAreRead_nothingIsShownOrSaved() {
        val ready = CompletableDeferred<Unit>()
        val progress = FakeProgress(SteppyUiState(todayDate = monday, todaySteps = 500))
        val settings = FakeSettingsRepository(onboarded, ready)
        val vm = viewModel(progress, settings)

        assertFalse(vm.uiState.value.isLoaded)
        vm.onSensorStepsUpdated(3000)
        vm.checkDayRollover()
        assertEquals(0, progress.saves)
        assertEquals(0, settings.saves)

        ready.complete(Unit)
        assertTrue(vm.uiState.value.isLoaded)
        assertTrue(vm.uiState.value.isOnboardingCompleted)
        assertEquals(500, vm.uiState.value.todaySteps)
        assertEquals(onboarded, settings.settings)
    }

    @Test
    fun deleteMyData_clearsTheSettings_andOnboardingSavesThemAgain() {
        val settings = FakeSettingsRepository(onboarded)
        val vm = viewModel(FakeProgress(), settings)

        vm.resetAllData()
        assertEquals(UserSettings(), settings.settings)
        assertTrue(vm.uiState.value.isLoaded)
        assertFalse(vm.uiState.value.isOnboardingCompleted)

        vm.saveProfileSetup("New", MainMotivation.STAY_HEALTHY, ActivityLevel.JUST_STARTING, 3000)
        vm.completeOnboarding(manualFallbackMode = false)
        assertEquals(
            UserSettings(
                isOnboardingCompleted = true,
                username = "New",
                mainGoal = MainMotivation.STAY_HEALTHY,
                activityLevel = ActivityLevel.JUST_STARTING,
                dailyGoal = 3000
            ),
            settings.settings
        )
    }

    @Test
    fun sensorReportBeforeSettingsAreRead_isAppliedOnceLoaded() {
        val ready = CompletableDeferred<Unit>()
        val settings = FakeSettingsRepository(onboarded.copy(isManualMode = true), ready)
        val vm = viewModel(FakeProgress(SteppyUiState(todayDate = monday)), settings)

        vm.onStepTrackingChanged(true)
        assertEquals(0, settings.saves)

        ready.complete(Unit)
        assertFalse(vm.uiState.value.isManualMode)
        assertFalse(settings.settings.isManualMode)
    }
}
