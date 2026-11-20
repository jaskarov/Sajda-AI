package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerHistoryDao {
    @Query("SELECT * FROM prayer_history ORDER BY completedAtMillis DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<PrayerHistoryEntity>>

    @Query("SELECT * FROM prayer_history ORDER BY completedAtMillis DESC LIMIT 500")
    suspend fun getAllHistoryForBackup(): List<PrayerHistoryEntity>

    @Query("SELECT DISTINCT prayerTypeId FROM prayer_history WHERE dayKey = :dayKey")
    fun getCompletedPrayerTypesForDay(dayKey: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: PrayerHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHistory(items: List<PrayerHistoryEntity>)

    @Query("DELETE FROM prayer_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
