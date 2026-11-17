package com.example.data.local

import androidx.room.Entity

@Entity(
    tableName = "prayer_calendar_days",
    primaryKeys = ["cityId", "year", "month", "day"]
)
data class PrayerCalendarDayEntity(
    val cityId: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val bomdod: String,
    val quyosh: String,
    val peshin: String,
    val asr: String,
    val shom: String,
    val xufton: String,
    val source: String = "uz_taqvim_365",
    val syncedAtMillis: Long = 0L
)
