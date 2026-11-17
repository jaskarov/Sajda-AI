package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerCalendarDao {

    @Query(
        "SELECT * FROM prayer_calendar_days " +
            "WHERE cityId = :cityId AND year = :year AND month = :month AND day = :day " +
            "LIMIT 1"
    )
    fun observeDay(
        cityId: String,
        year: Int,
        month: Int,
        day: Int
    ): Flow<PrayerCalendarDayEntity?>

    @Query(
        "SELECT * FROM prayer_calendar_days " +
            "WHERE cityId = :cityId AND year = :year AND month = :month AND day = :day " +
            "LIMIT 1"
    )
    suspend fun getDay(
        cityId: String,
        year: Int,
        month: Int,
        day: Int
    ): PrayerCalendarDayEntity?

    @Query("SELECT COUNT(*) FROM prayer_calendar_days WHERE cityId = :cityId AND year = :year")
    suspend fun countForCityYear(cityId: String, year: Int): Int

    @Query("SELECT COUNT(*) FROM prayer_calendar_days")
    suspend fun countAll(): Int

    @Query("SELECT MAX(syncedAtMillis) FROM prayer_calendar_days WHERE cityId = :cityId")
    fun observeLatestSyncMillis(cityId: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<PrayerCalendarDayEntity>)
}
