package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_history",
    indices = [
        Index(value = ["completedAtMillis"]),
        Index(value = ["dayKey"])
    ]
)
data class PrayerHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayerTypeId: String,
    val trackId: String,
    val rakatCount: Int,
    val coachMode: String,
    val cityId: String,
    val dayKey: String,
    val durationSeconds: Int,
    val completedAtMillis: Long = System.currentTimeMillis()
)
