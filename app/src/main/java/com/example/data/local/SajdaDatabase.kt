package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrayerHistoryEntity::class,
        DailyPrayerProgressEntity::class,
        PrayerCalendarDayEntity::class,
        QazoPrayerEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SajdaDatabase : RoomDatabase() {
    abstract fun prayerHistoryDao(): PrayerHistoryDao
    abstract fun dailyPrayerProgressDao(): DailyPrayerProgressDao
    abstract fun prayerCalendarDao(): PrayerCalendarDao
    abstract fun qazoPrayerDao(): QazoPrayerDao

    companion object {
        @Volatile
        private var INSTANCE: SajdaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_prayer_progress` (
                        `dayKey` TEXT NOT NULL,
                        `bomdodCompleted` INTEGER NOT NULL DEFAULT 0,
                        `peshinCompleted` INTEGER NOT NULL DEFAULT 0,
                        `asrCompleted` INTEGER NOT NULL DEFAULT 0,
                        `shomCompleted` INTEGER NOT NULL DEFAULT 0,
                        `xuftonCompleted` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtMillis` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`dayKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prayer_calendar_days` (
                        `cityId` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `bomdod` TEXT NOT NULL,
                        `quyosh` TEXT NOT NULL,
                        `peshin` TEXT NOT NULL,
                        `asr` TEXT NOT NULL,
                        `shom` TEXT NOT NULL,
                        `xufton` TEXT NOT NULL,
                        `source` TEXT NOT NULL DEFAULT 'uz_taqvim_365',
                        `syncedAtMillis` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`cityId`, `year`, `month`, `day`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qazo_prayers` (
                        `id` INTEGER NOT NULL,
                        `bomdodCount` INTEGER NOT NULL DEFAULT 0,
                        `peshinCount` INTEGER NOT NULL DEFAULT 0,
                        `asrCount` INTEGER NOT NULL DEFAULT 0,
                        `shomCount` INTEGER NOT NULL DEFAULT 0,
                        `xuftonCount` INTEGER NOT NULL DEFAULT 0,
                        `vitrCount` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtMillis` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SajdaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SajdaDatabase::class.java,
                    "sajda_ai_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
