package com.steppy.app.data.repository

import androidx.room.withTransaction
import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.SteppyDatabase
import com.steppy.app.data.local.toDayProgress
import com.steppy.app.data.local.toStreakState
import com.steppy.app.domain.StreakCalculator
import com.steppy.app.domain.StreakState
import java.time.LocalDate

/** Saves one record per day, each with the goal that applied on that day. */
interface DailyHistoryRepository {

    /**
     * Creates or updates the record for [date]. The first record ever saved also creates
     * the new-user [ProgressBaseline], starting on that date.
     */
    suspend fun saveDay(date: LocalDate, steps: Int, goal: Int, source: StepSource)

    /** "Delete my data": removes every record, challenge and the baseline. */
    suspend fun clearAll()

    /** Everything in the history, for a backup. */
    suspend fun exportAll(): HistoryData

    /** Restoring a backup: replaces the whole history with [data], all at once or not at all. */
    suspend fun replaceAll(data: HistoryData)

    /**
     * Streak and hearts at the end of the day before [date], replayed from the baseline.
     * A day with no record counts as missed. Null when there is no baseline yet (no day saved).
     */
    suspend fun streakBefore(date: LocalDate): StreakState?
}

/** The replay behind [DailyHistoryRepository.streakBefore], shared with the test fakes. */
fun replayStreakBefore(baseline: ProgressBaseline?, records: List<DailyRecord>, date: LocalDate): StreakState? {
    if (baseline == null) return null
    return StreakCalculator.stateBefore(
        start = baseline.toStreakState(),
        days = records.map { it.toDayProgress() },
        from = baseline.asOf,
        today = date
    )
}

class RoomDailyHistoryRepository(private val db: SteppyDatabase) : DailyHistoryRepository {

    override suspend fun saveDay(date: LocalDate, steps: Int, goal: Int, source: StepSource) {
        db.withTransaction {
            val baselineDao = db.progressBaselineDao()
            if (baselineDao.get() == null) {
                baselineDao.upsert(ProgressBaseline.newUser(date))
            }

            val recordDao = db.dailyRecordDao()
            val existing = recordDao.get(date)
            recordDao.upsert(
                DailyRecord(
                    date = date,
                    steps = steps,
                    // Written by the snapshot worker (P4), so keep it
                    stepsBeforeNoon = existing?.stepsBeforeNoon,
                    goal = goal,
                    source = source
                )
            )
        }
    }

    override suspend fun clearAll() {
        db.withTransaction { deleteEverything() }
    }

    override suspend fun exportAll(): HistoryData = db.withTransaction {
        HistoryData(
            baseline = db.progressBaselineDao().get(),
            days = db.dailyRecordDao().getAll(),
            challenges = db.userChallengeDao().getAll()
        )
    }

    override suspend fun replaceAll(data: HistoryData) {
        db.withTransaction {
            deleteEverything()
            data.baseline?.let { db.progressBaselineDao().upsert(it) }
            db.dailyRecordDao().upsertAll(data.days)
            db.userChallengeDao().upsertAll(data.challenges)
        }
    }

    override suspend fun streakBefore(date: LocalDate): StreakState? = db.withTransaction {
        val baseline = db.progressBaselineDao().get()
        val records = baseline?.let { db.dailyRecordDao().getFrom(it.asOf) }.orEmpty()
        replayStreakBefore(baseline, records, date)
    }

    private suspend fun deleteEverything() {
        db.dailyRecordDao().deleteAll()
        db.userChallengeDao().deleteAll()
        db.progressBaselineDao().deleteAll()
    }
}
