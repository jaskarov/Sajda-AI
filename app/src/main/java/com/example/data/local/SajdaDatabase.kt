package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrayerHistoryEntity::class,
        DailyPrayerProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SajdaDatabase : RoomDatabase() {
    abstract fun prayerHistoryDao(): PrayerHistoryDao
    abstract fun dailyPrayerProgressDao(): DailyPrayerProgressDao

    companion object {
        @Volatile
        private var INSTANCE: SajdaDatabase? = null

        fun getInstance(context: Context): SajdaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SajdaDatabase::class.java,
                    "sajda_ai_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
