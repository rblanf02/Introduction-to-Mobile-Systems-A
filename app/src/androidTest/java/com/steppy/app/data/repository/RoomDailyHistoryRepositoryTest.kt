package com.steppy.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.SteppyDatabase
import com.steppy.app.data.local.UserChallenge
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomDailyHistoryRepositoryTest {

    private lateinit var db: SteppyDatabase
    private lateinit var history: RoomDailyHistoryRepository

    private val monday = LocalDate.of(2026, 11, 23)

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SteppyDatabase::class.java
        ).build()
        history = RoomDailyHistoryRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun firstSave_createsNewUserBaselineOnThatDay() = runBlocking {
        history.saveDay(monday, 0, 6000, StepSource.SENSOR)
        assertEquals(ProgressBaseline.newUser(monday), db.progressBaselineDao().get())

        // Later days don't move the baseline
        history.saveDay(monday.plusDays(1), 100, 6000, StepSource.SENSOR)
        assertEquals(monday, db.progressBaselineDao().get()?.asOf)
    }

    @Test
    fun saveDay_keepsStepsBeforeNoon() = runBlocking {
        history.saveDay(monday, 1000, 6000, StepSource.SENSOR)
        val withNoon = db.dailyRecordDao().get(monday)!!.copy(stepsBeforeNoon = 2100)
        db.dailyRecordDao().upsert(withNoon)

        history.saveDay(monday, 7000, 10000, StepSource.MANUAL)

        val saved = db.dailyRecordDao().get(monday)!!
        assertEquals(7000, saved.steps)
        assertEquals(10000, saved.goal)
        assertEquals(StepSource.MANUAL, saved.source)
        assertEquals(2100, saved.stepsBeforeNoon)
    }

    @Test
    fun clearAll_emptiesEverything() = runBlocking {
        history.saveDay(monday, 1000, 6000, StepSource.SENSOR)
        db.userChallengeDao().upsert(UserChallenge("c1", monday, null))

        history.clearAll()

        assertNull(db.dailyRecordDao().get(monday))
        assertEquals(emptyList<UserChallenge>(), db.userChallengeDao().getAll())
        assertNull(db.progressBaselineDao().get())
    }

    @Test
    fun replaceAll_swapsTheWholeHistory_andExportAllReadsItBack() = runBlocking {
        history.saveDay(monday, 1234, 6000, StepSource.SENSOR)
        db.userChallengeDao().upsert(UserChallenge("c5", monday, null))

        val backup = HistoryData(
            baseline = ProgressBaseline(asOf = monday.minusDays(10), xpBefore = 500, streakBefore = 3, heartsBefore = 1, activeDaysBefore = 7),
            days = listOf(
                DailyRecord(monday.minusDays(1), 9000, 2100, 8000, StepSource.MANUAL),
                DailyRecord(monday, 4000, null, 8000, StepSource.SENSOR)
            ),
            challenges = listOf(UserChallenge("c1", monday.minusDays(1), monday))
        )
        history.replaceAll(backup)

        assertEquals(backup, history.exportAll())
    }

    @Test
    fun replaceAll_withEmptyHistory_leavesNothing() = runBlocking {
        history.saveDay(monday, 1234, 6000, StepSource.SENSOR)
        history.replaceAll(HistoryData(null, emptyList(), emptyList()))
        assertEquals(HistoryData(null, emptyList(), emptyList()), history.exportAll())
    }

    @Test
    fun manualDay_staysManual_whenSensorStepsAreSavedLater() = runBlocking {
        history.saveDay(monday, 500, 6000, StepSource.MANUAL)
        history.saveDay(monday, 4000, 6000, StepSource.SENSOR)
        assertEquals(StepSource.MANUAL, db.dailyRecordDao().get(monday)?.source)
        assertEquals(4000, db.dailyRecordDao().get(monday)?.steps)
    }

    @Test
    fun appAndWorkerSaves_keepTheLargerCount_andStepsBeforeNoon() = runBlocking {
        // Worker in the morning, then a later save from the app with an older count
        history.saveDay(monday, 2500, 6000, StepSource.SENSOR, stepsBeforeNoon = 2500)
        history.saveDay(monday, 1800, 6000, StepSource.SENSOR)

        val record = db.dailyRecordDao().get(monday)!!
        assertEquals(2500, record.steps)
        assertEquals(2500, record.stepsBeforeNoon)
        assertEquals(record, history.getDay(monday))
    }
}
