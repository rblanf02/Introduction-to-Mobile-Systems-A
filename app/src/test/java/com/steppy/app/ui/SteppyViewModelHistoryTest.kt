package com.steppy.app.ui

import com.steppy.app.data.local.StepSource
import com.steppy.app.data.repository.DailyHistoryRepository
import com.steppy.app.data.repository.SteppyRepository
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SteppyViewModelHistoryTest {

    private data class SavedDay(val steps: Int, val goal: Int, val source: StepSource)

    private class FakeHistory : DailyHistoryRepository {
        val days = mutableMapOf<LocalDate, SavedDay>()
        override suspend fun saveDay(date: LocalDate, steps: Int, goal: Int, source: StepSource) {
            days[date] = SavedDay(steps, goal, source)
        }
        override suspend fun clearAll() = days.clear()
    }

    private class FakeSettings(var saved: SteppyUiState) : SteppyRepository {
        override fun loadInitialState(): SteppyUiState = saved
        override fun saveState(state: SteppyUiState) { saved = state }
        override fun clearAllData() { saved = SteppyUiState() }
    }

    private val monday = LocalDate.of(2026, 11, 23)
    private val tuesday = monday.plusDays(1)

    private var clock = monday
    private val history = FakeHistory()

    private fun onboardedState(todayDate: LocalDate? = monday, steps: Int = 0, goal: Int = 6000) =
        SteppyUiState(isOnboardingCompleted = true, todayDate = todayDate, todaySteps = steps, dailyGoal = goal)

    private fun viewModel(settings: FakeSettings) = SteppyViewModel(settings, history) { clock }

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
    fun addingSteps_updatesTodaysRecord() {
        val vm = viewModel(FakeSettings(onboardedState()))
        vm.addSteps(2500)
        vm.addSteps(500)
        assertEquals(SavedDay(3000, 6000, StepSource.SENSOR), history.days[monday])
    }

    @Test
    fun goalChange_appliesToTodayButNotToPastDays() {
        val vm = viewModel(FakeSettings(onboardedState(goal = 6000)))
        vm.addSteps(4000)

        clock = tuesday
        vm.addSteps(1000)
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
}
