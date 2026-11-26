package com.steppy.app.ui

import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.mergedWith
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.data.repository.replayStreakBefore
import com.steppy.app.data.settings.SettingsRepository
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeCatalogue
import com.steppy.app.domain.MainMotivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SteppyViewModelHistoryTest {

    private data class SavedDay(val steps: Int, val goal: Int, val source: StepSource)

    private class FakeHistory : DailyHistoryRepository {
        val records = mutableMapOf<LocalDate, DailyRecord>()
        val days: Map<LocalDate, SavedDay>
            get() = records.mapValues { (_, r) -> SavedDay(r.steps, r.goal, r.source) }
        var baseline: ProgressBaseline? = null
        override suspend fun saveDay(
            date: LocalDate,
            steps: Int,
            goal: Int,
            source: StepSource,
            stepsBeforeNoon: Int?
        ) {
            if (baseline == null) baseline = ProgressBaseline.newUser(date)
            // Same merge as the Room repository
            records[date] = DailyRecord(date, steps, stepsBeforeNoon, goal, source).mergedWith(records[date])
        }
        override suspend fun getDay(date: LocalDate) = records[date]
        override suspend fun clearAll() { records.clear(); baseline = null }
        override suspend fun exportAll() = HistoryData(null, emptyList(), emptyList())
        override suspend fun replaceAll(data: HistoryData) = Unit
        override suspend fun streakBefore(date: LocalDate) =
            replayStreakBefore(baseline, records.values.sortedBy { it.date }, date)
    }

    /** Progress and settings together; the settings part is served through a fake SettingsRepository. */
    private class FakeSettings(var saved: SteppyUiState) : SteppyRepository {
        override fun loadInitialState(): SteppyUiState = saved
        override fun saveState(state: SteppyUiState) { saved = state }
        override fun clearAllData() { saved = SteppyUiState() }
    }

    private class FakeSettingsRepository(var settings: UserSettings) : SettingsRepository {
        override suspend fun load() = settings
        override suspend fun save(settings: UserSettings) { this.settings = settings }
        override suspend fun clear() { settings = UserSettings() }
    }

    private val monday = LocalDate.of(2026, 11, 23)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)

    private var clock = monday
    /** Afternoon by default, so steps before noon only change in the tests about them. */
    private var time: LocalTime = LocalTime.of(15, 0)
    private val history = FakeHistory()

    private fun onboardedState(todayDate: LocalDate? = monday, steps: Int = 0, goal: Int = 6000) =
        SteppyUiState(isOnboardingCompleted = true, todayDate = todayDate, todaySteps = steps, dailyGoal = goal)

    private fun viewModel(settings: FakeSettings) =
        SteppyViewModel(
            settings,
            FakeSettingsRepository(settings.saved.settings()),
            history,
            timeOfDay = { time }
        ) { clock }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun finishingOnboarding_savesTodayWithItsGoal() {
        val vm = viewModel(FakeSettings(SteppyUiState()))
        vm.saveProfileSetup("Test", MainMotivation.MOVE_MORE, ActivityLevel.JUST_STARTING, 3000)
        assertTrue("Nothing is saved before onboarding ends", history.days.isEmpty())

        vm.completeOnboarding(manualFallbackMode = true)
        assertEquals(SavedDay(0, 3000, StepSource.MANUAL), history.days[monday])
    }

    @Test
    fun sensorSteps_updateTodaysRecord() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(2500)
        vm.onSensorStepsUpdated(3000)
        assertEquals(SavedDay(3000, 6000, StepSource.SENSOR), history.days[monday])
    }

    @Test
    fun goalChange_appliesToTodayButNotToPastDays() {
        val vm = viewModel(FakeSettings(onboardedState(goal = 6000)))
        vm.addSteps(4000)

        clock = tuesday
        vm.onSensorStepsUpdated(1000)
        vm.updateDailyGoal(10000)

        assertEquals(6000, history.days[monday]?.goal)
        assertEquals(SavedDay(1000, 10000, StepSource.SENSOR), history.days[tuesday])
    }

    @Test
    fun midnightWhileOpen_closesYesterdayAndStartsTodayAtZero() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(8000)

        clock = tuesday
        vm.checkDayRollover()

        assertEquals(0, vm.uiState.value.todaySteps)
        assertEquals(tuesday, vm.uiState.value.todayDate)
        assertEquals(8000, history.days[monday]?.steps)
        assertEquals(0, history.days[tuesday]?.steps)
    }

    @Test
    fun openingTheAppOnANewDay_startsAtZero() {
        val settings = FakeSettings(onboardedState(todayDate = monday, steps = 5000))
        clock = tuesday
        val vm = viewModel(settings)

        assertEquals(0, vm.uiState.value.todaySteps)
        assertEquals(5000, history.days[monday]?.steps)
        // The new day is also saved, so a restart doesn't roll over again
        assertEquals(tuesday, settings.saved.todayDate)
        assertEquals(0, settings.saved.todaySteps)
    }

    @Test
    fun sensorDropsAfterMidnight_countsTheNewDay() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(9000)

        // The sensor's daily count starts again from a low value on the new day
        clock = tuesday
        vm.onSensorStepsUpdated(300)

        assertEquals(300, vm.uiState.value.todaySteps)
        assertEquals(9000, history.days[monday]?.steps)
        assertEquals(300, history.days[tuesday]?.steps)
    }

    @Test
    fun sensorDropOnTheSameDay_isIgnored() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(9000)
        vm.onSensorStepsUpdated(300)
        assertEquals(9000, vm.uiState.value.todaySteps)
    }

    @Test
    fun dataWithoutADate_isTreatedAsToday() {
        val vm = viewModel(FakeSettings(onboardedState(todayDate = null, steps = 4200)))
        assertEquals(4200, vm.uiState.value.todaySteps)
        assertEquals(monday, vm.uiState.value.todayDate)
        assertEquals(4200, history.days[monday]?.steps)
    }

    @Test
    fun clockMovedBack_keepsCountingOnTheSameDay() {
        val vm = viewModel(FakeSettings(onboardedState(todayDate = tuesday, steps = 3000)))
        clock = monday
        vm.addSteps(500)
        assertEquals(3500, vm.uiState.value.todaySteps)
        assertEquals(3500, history.days[tuesday]?.steps)
        assertNull(history.days[monday])
    }

    @Test
    fun deleteMyData_clearsHistory_andTheNextOnboardingSavesAgain() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(3000)

        vm.resetAllData()
        assertTrue(history.days.isEmpty())

        vm.completeOnboarding(manualFallbackMode = false)
        assertEquals(0, history.days[monday]?.steps)
    }

    // Streak from the daily history (P2)

    @Test
    fun goalMetYesterday_newDayKeepsTheStreak_andTodaysGoalAddsOne() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(6000)
        assertEquals(1, vm.uiState.value.currentStreak)

        clock = tuesday
        vm.checkDayRollover()
        assertEquals(1, vm.uiState.value.currentStreak)

        vm.addSteps(6000)
        assertEquals(2, vm.uiState.value.currentStreak)
    }

    @Test
    fun missedDay_resetsTheStreakWhenTheAppOpens() {
        val settings = FakeSettings(onboardedState())
        viewModel(settings).addSteps(6000)
        assertEquals(1, settings.saved.currentStreak)

        // Tuesday has no record (the app wasn't opened), so it counts as missed
        clock = wednesday
        val vm = viewModel(settings)

        assertEquals(0, vm.uiState.value.currentStreak)
        assertEquals(0, settings.saved.currentStreak)
    }

    @Test
    fun missedDay_whileTheAppStaysOpen_resetsTheStreak() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(6000)

        clock = tuesday
        vm.addSteps(100)
        clock = wednesday
        vm.checkDayRollover()

        assertEquals(0, vm.uiState.value.currentStreak)
    }

    @Test
    fun sevenDayStreak_earnsAHeart_thatSavesTheStreakOnAMissedDay() {
        val vm = viewModel(FakeSettings(onboardedState()))
        repeat(7) { i ->
            clock = monday.plusDays(i.toLong())
            vm.addSteps(6000)
        }
        assertEquals(7, vm.uiState.value.currentStreak)
        assertEquals(1, vm.uiState.value.streakFreezes)

        // Day 8 is missed; on day 9 the heart is used and the streak stays
        clock = monday.plusDays(8)
        vm.checkDayRollover()

        assertEquals(7, vm.uiState.value.currentStreak)
        assertEquals(0, vm.uiState.value.streakFreezes)
    }

    @Test
    fun loweringTheGoalBelowTodaysSteps_countsTodayForTheStreak() {
        val vm = viewModel(FakeSettings(onboardedState(goal = 6000)))
        vm.addSteps(4000)
        assertEquals(0, vm.uiState.value.currentStreak)

        vm.updateDailyGoal(3000)
        assertEquals(1, vm.uiState.value.currentStreak)

        vm.updateDailyGoal(10000)
        assertEquals(0, vm.uiState.value.currentStreak)
    }

    @Test
    fun streakInStorageWithoutHistory_isKeptUntilADayIsSaved() {
        // Not onboarded: nothing is saved to the history, so there's nothing to replay
        val vm = viewModel(FakeSettings(SteppyUiState(todayDate = monday, currentStreak = 3)))
        assertEquals(3, vm.uiState.value.currentStreak)
    }

    // Manual entry and the day's step source (P3)

    @Test
    fun manualEntry_savesTheDayAsManual() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(1200)
        assertEquals(SavedDay(1200, 6000, StepSource.MANUAL), history.days[monday])
    }

    @Test
    fun sensorStepsAfterManualEntry_dayStaysManual() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(1000)
        vm.addSteps(500)
        vm.onSensorStepsUpdated(4000)

        assertEquals(SavedDay(4000, 6000, StepSource.MANUAL), history.days[monday])

        // The next day starts as a sensor day again
        clock = tuesday
        vm.onSensorStepsUpdated(200)
        assertEquals(StepSource.SENSOR, history.days[tuesday]?.source)
    }

    @Test
    fun sensorNotCounting_turnsOnManualMode_andTheDayIsManual() {
        val settings = FakeSettings(onboardedState())
        val vm = viewModel(settings)

        // No sensor, or the permission was denied in the system settings
        vm.onStepTrackingChanged(false)

        assertTrue(vm.uiState.value.isManualMode)
        assertTrue(settings.saved.isManualMode)
        assertEquals(StepSource.MANUAL, history.days[monday]?.source)
    }

    @Test
    fun permissionGrantedLater_turnsOffManualMode() {
        val vm = viewModel(FakeSettings(onboardedState().copy(isManualMode = true)))

        vm.onStepTrackingChanged(true)
        assertFalse(vm.uiState.value.isManualMode)

        clock = tuesday
        vm.onSensorStepsUpdated(800)
        assertEquals(SavedDay(800, 6000, StepSource.SENSOR), history.days[tuesday])
    }

    @Test
    fun sensorReportBeforeOnboardingEnds_doesNotOverrideTheOnboardingChoice() {
        val vm = viewModel(FakeSettings(SteppyUiState()))
        vm.onStepTrackingChanged(false)
        assertFalse(vm.uiState.value.isManualMode)

        vm.completeOnboarding(manualFallbackMode = false)
        assertFalse(vm.uiState.value.isManualMode)
    }

    // Steps before noon and the snapshot worker's records (P4)

    private val morning = LocalTime.of(10, 0)
    private val afternoon = LocalTime.of(13, 0)

    private fun withActiveMorningMover(state: SteppyUiState) = state.copy(
        challenges = state.challenges.map {
            if (it.id == ChallengeCatalogue.MORNING_MOVER) it.copy(isActive = true) else it
        }
    )

    private fun SteppyViewModel.morningMover() =
        uiState.value.challenges.single { it.id == ChallengeCatalogue.MORNING_MOVER }

    @Test
    fun stepsInTheMorning_areSavedAsStepsBeforeNoon() {
        time = morning
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(1500)

        assertEquals(1500, vm.uiState.value.stepsBeforeNoon)
        assertEquals(1500, history.records[monday]?.stepsBeforeNoon)
    }

    @Test
    fun stepsAfterNoon_doNotChangeStepsBeforeNoon() {
        time = morning
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(1500)

        time = afternoon
        vm.onSensorStepsUpdated(4500)

        assertEquals(1500, vm.uiState.value.stepsBeforeNoon)
        assertEquals(1500, history.records[monday]?.stepsBeforeNoon)
        assertEquals(4500, history.records[monday]?.steps)
    }

    @Test
    fun morningMover_countsOnlyStepsBeforeNoon() {
        time = afternoon
        val vm = viewModel(FakeSettings(withActiveMorningMover(onboardedState())))
        vm.onSensorStepsUpdated(5000)

        assertFalse(vm.morningMover().isCompleted)
        assertEquals(0, vm.morningMover().currentProgress)
    }

    @Test
    fun morningMover_completesWithEnoughStepsBeforeNoon() {
        time = morning
        val vm = viewModel(FakeSettings(withActiveMorningMover(onboardedState())))
        vm.onSensorStepsUpdated(2100)

        assertTrue(vm.morningMover().isCompleted)
    }

    @Test
    fun morningStepsSavedByTheWorker_areReadWhenTheAppOpens() = runTest {
        // The worker saved this morning's steps while the app was closed
        history.saveDay(monday, 2600, 6000, StepSource.SENSOR, stepsBeforeNoon = 2400)

        time = afternoon
        val vm = viewModel(FakeSettings(withActiveMorningMover(onboardedState())))

        assertEquals(2400, vm.uiState.value.stepsBeforeNoon)
        assertTrue(vm.morningMover().isCompleted)
    }

    @Test
    fun newDay_startsStepsBeforeNoonAtZero() {
        time = morning
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.onSensorStepsUpdated(1500)

        clock = tuesday
        vm.checkDayRollover()

        assertEquals(0, vm.uiState.value.stepsBeforeNoon)
        assertEquals(1500, history.records[monday]?.stepsBeforeNoon)
    }

    @Test
    fun dayClosedByTheWorker_isNotLoweredByTheAppsOlderCount() = runTest {
        // The app last counted 2,000 steps on Monday; the worker saved 9,000 later that day
        val settings = FakeSettings(onboardedState(steps = 2000))
        history.saveDay(monday, 9000, 6000, StepSource.SENSOR)

        clock = tuesday
        val vm = viewModel(settings)

        assertEquals(9000, history.records[monday]?.steps)
        assertEquals(0, vm.uiState.value.todaySteps)
        // Monday's goal was met, so the streak counts it
        assertEquals(1, vm.uiState.value.currentStreak)
    }
}
