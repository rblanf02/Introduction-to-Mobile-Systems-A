package com.steppy.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Dates are stored as ISO strings (yyyy-MM-dd), so comparing and sorting them as text follows the calendar.

@Dao
interface DailyRecordDao {

    @Upsert
    suspend fun upsert(record: DailyRecord)

    @Upsert
    suspend fun upsertAll(records: List<DailyRecord>)

    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun get(date: LocalDate): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyRecord?>

    /** Both ends included, oldest first. */
    @Query("SELECT * FROM daily_records WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyRecord>>

    /** Every day from [from] onwards, oldest first. Used with [ProgressBaseline.asOf]. */
    @Query("SELECT * FROM daily_records WHERE date >= :from ORDER BY date")
    suspend fun getFrom(from: LocalDate): List<DailyRecord>

    @Query("SELECT * FROM daily_records WHERE date >= :from ORDER BY date")
    fun observeFrom(from: LocalDate): Flow<List<DailyRecord>>

    /** Every day, oldest first. Used by the backup. */
    @Query("SELECT * FROM daily_records ORDER BY date")
    suspend fun getAll(): List<DailyRecord>

    @Query("DELETE FROM daily_records")
    suspend fun deleteAll()
}

@Dao
interface UserChallengeDao {

    @Upsert
    suspend fun upsert(challenge: UserChallenge)

    @Upsert
    suspend fun upsertAll(challenges: List<UserChallenge>)

    @Query("SELECT * FROM user_challenges WHERE challengeId = :id")
    suspend fun get(id: String): UserChallenge?

    @Query("SELECT * FROM user_challenges ORDER BY activatedOn")
    suspend fun getAll(): List<UserChallenge>

    @Query("SELECT * FROM user_challenges ORDER BY activatedOn")
    fun observeAll(): Flow<List<UserChallenge>>

    /** Sets [completedOn] only if the challenge is not completed yet. Returns 1 if it changed, 0 otherwise. */
    @Query(
        "UPDATE user_challenges SET completedOn = :completedOn " +
            "WHERE challengeId = :id AND completedOn IS NULL"
    )
    suspend fun markCompleted(id: String, completedOn: LocalDate): Int

    @Query("DELETE FROM user_challenges")
    suspend fun deleteAll()
}

@Dao
interface ProgressBaselineDao {

    @Upsert
    suspend fun upsert(baseline: ProgressBaseline)

    @Query("SELECT * FROM progress_baseline WHERE id = ${ProgressBaseline.SINGLE_ROW_ID}")
    suspend fun get(): ProgressBaseline?

    @Query("SELECT * FROM progress_baseline WHERE id = ${ProgressBaseline.SINGLE_ROW_ID}")
    fun observe(): Flow<ProgressBaseline?>

    @Query("DELETE FROM progress_baseline")
    suspend fun deleteAll()
}
