package com.steppy.app.data.repository

import androidx.room.withTransaction
import com.steppy.app.data.backup.HistoryData
import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.SteppyDatabase
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

    private suspend fun deleteEverything() {
        db.dailyRecordDao().deleteAll()
        db.userChallengeDao().deleteAll()
        db.progressBaselineDao().deleteAll()
    }
}
