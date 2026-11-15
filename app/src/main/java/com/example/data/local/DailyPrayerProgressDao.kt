package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPrayerProgressDao {

    @Query("SELECT * FROM daily_prayer_progress WHERE dayKey = :dayKey LIMIT 1")
    fun observeDay(dayKey: String): Flow<DailyPrayerProgressEntity?>

    @Query("SELECT * FROM daily_prayer_progress WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getDay(dayKey: String): DailyPrayerProgressEntity?

    @Query("SELECT * FROM daily_prayer_progress ORDER BY dayKey DESC LIMIT :limit")
    fun observeRecentDays(limit: Int = 30): Flow<List<DailyPrayerProgressEntity>>

    @Query(
        "SELECT * FROM daily_prayer_progress " +
            "WHERE dayKey BETWEEN :startDayKey AND :endDayKey " +
            "ORDER BY dayKey ASC"
    )
    fun observeDaysInRange(
        startDayKey: String,
        endDayKey: String
    ): Flow<List<DailyPrayerProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyPrayerProgressEntity)

    @Query("DELETE FROM daily_prayer_progress WHERE dayKey = :dayKey")
    suspend fun deleteDay(dayKey: String)

    @Transaction
    suspend fun setPrayerCompleted(
        dayKey: String,
        prayerType: PrayerType,
        completed: Boolean
    ) {
        if (!prayerType.isPrayable) return
        val current = getDay(dayKey) ?: DailyPrayerProgressEntity(dayKey = dayKey)
        upsert(current.withPrayerStatus(prayerType, completed))
    }

    @Transaction
    suspend fun togglePrayerCompleted(dayKey: String, prayerType: PrayerType) {
        if (!prayerType.isPrayable) return
        val current = getDay(dayKey) ?: DailyPrayerProgressEntity(dayKey = dayKey)
        val next = !current.isCompleted(prayerType)
        upsert(current.withPrayerStatus(prayerType, next))
    }
}
