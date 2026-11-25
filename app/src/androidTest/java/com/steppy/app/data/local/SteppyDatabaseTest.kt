package com.steppy.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SteppyDatabaseTest {

    private lateinit var db: SteppyDatabase
    private lateinit var records: DailyRecordDao
    private lateinit var challenges: UserChallengeDao
    private lateinit var baseline: ProgressBaselineDao

    private val nov25 = LocalDate.of(2026, 11, 25)

    private fun record(date: LocalDate, steps: Int = 5000, goal: Int = 6000) =
        DailyRecord(date, steps, stepsBeforeNoon = null, goal = goal, source = StepSource.SENSOR)

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SteppyDatabase::class.java
        ).build()
        records = db.dailyRecordDao()
        challenges = db.userChallengeDao()
        baseline = db.progressBaselineDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // --- DailyRecord ---

    @Test
    fun dailyRecord_upsertThenGet() = runBlocking {
        val saved = record(nov25).copy(stepsBeforeNoon = 2100, source = StepSource.MANUAL)
        records.upsert(saved)
        assertEquals(saved, records.get(nov25))
    }

    @Test
    fun dailyRecord_upsertSameDay_replacesRow() = runBlocking {
        records.upsert(record(nov25, steps = 1000))
        records.upsert(record(nov25, steps = 7842))
        assertEquals(7842, records.get(nov25)?.steps)
        assertEquals(1, records.getFrom(nov25).size)
    }

    @Test
    fun dailyRecord_keepsEachDaysGoal() = runBlocking {
        records.upsert(record(nov25.minusDays(1), goal = 3000))
        records.upsert(record(nov25, goal = 10000))
        assertEquals(3000, records.get(nov25.minusDays(1))?.goal)
        assertEquals(10000, records.get(nov25)?.goal)
    }

    @Test
    fun dailyRecord_rangeIsOrderedAndInclusive_acrossYears() = runBlocking {
        val dates = listOf(
            LocalDate.of(2027, 1, 2),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2026, 12, 30),
            LocalDate.of(2026, 12, 1)
        )
        records.upsertAll(dates.map { record(it) })

        val range = records.observeRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)).first()
        assertEquals(
            listOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 2)),
            range.map { it.date }
        )
        assertEquals(dates.sorted(), records.getFrom(LocalDate.of(2026, 1, 1)).map { it.date })
    }

    @Test
    fun dailyRecord_missingDay_isNull() = runBlocking {
        assertNull(records.get(nov25))
    }

    // --- UserChallenge ---

    @Test
    fun userChallenge_markCompleted_onlyOnce() = runBlocking {
        challenges.upsert(UserChallenge("c1", activatedOn = nov25, completedOn = null))

        assertEquals(1, challenges.markCompleted("c1", nov25))
        // A second completion must not change the date (XP is awarded once)
        assertEquals(0, challenges.markCompleted("c1", nov25.plusDays(1)))
        assertEquals(nov25, challenges.get("c1")?.completedOn)
    }

    @Test
    fun userChallenge_markCompleted_unknownId_changesNothing() = runBlocking {
        assertEquals(0, challenges.markCompleted("nope", nov25))
    }

    @Test
    fun userChallenge_observeAll() = runBlocking {
        challenges.upsert(UserChallenge("c2", activatedOn = nov25, completedOn = null))
        challenges.upsert(UserChallenge("c1", activatedOn = nov25.minusDays(2), completedOn = nov25))
        assertEquals(listOf("c1", "c2"), challenges.observeAll().first().map { it.challengeId })
    }

    // --- ProgressBaseline ---

    @Test
    fun baseline_isASingleRow() = runBlocking {
        assertNull(baseline.get())
        baseline.upsert(ProgressBaseline.newUser(nov25))
        val transferred = ProgressBaseline(
            asOf = LocalDate.of(2026, 6, 1),
            xpBefore = 4200,
            streakBefore = 210,
            heartsBefore = 2,
            activeDaysBefore = 230
        )
        baseline.upsert(transferred)
        assertEquals(transferred, baseline.get())
        assertEquals(transferred, baseline.observe().first())
    }

    // --- Delete my data ---

    @Test
    fun deleteAll_emptiesEveryTable() = runBlocking {
        records.upsert(record(nov25))
        challenges.upsert(UserChallenge("c1", nov25, null))
        baseline.upsert(ProgressBaseline.newUser(nov25))

        records.deleteAll()
        challenges.deleteAll()
        baseline.deleteAll()

        assertEquals(emptyList<DailyRecord>(), records.getFrom(LocalDate.of(2000, 1, 1)))
        assertEquals(emptyList<UserChallenge>(), challenges.getAll())
        assertNull(baseline.get())
    }
}
