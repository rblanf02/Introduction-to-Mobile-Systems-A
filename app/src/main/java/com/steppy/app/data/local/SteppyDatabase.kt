package com.steppy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun dateToText(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun textToDate(text: String?): LocalDate? = text?.let { LocalDate.parse(it) }
}

@Database(
    entities = [DailyRecord::class, UserChallenge::class, ProgressBaseline::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SteppyDatabase : RoomDatabase() {

    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userChallengeDao(): UserChallengeDao
    abstract fun progressBaselineDao(): ProgressBaselineDao

    companion object {
        private const val NAME = "steppy.db"

        fun build(context: Context): SteppyDatabase =
            Room.databaseBuilder(context.applicationContext, SteppyDatabase::class.java, NAME)
                .build()
    }
}
