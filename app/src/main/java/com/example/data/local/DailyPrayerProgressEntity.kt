package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.PrayerType

/**
 * Room entity tracking daily completion status for each of the five obligatory prayers.
 * [dayKey] uses ISO date format ("yyyy-MM-dd").
 */
@Entity(tableName = "daily_prayer_progress")
data class DailyPrayerProgressEntity(
    @PrimaryKey val dayKey: String,
    val bomdodCompleted: Boolean = false,
    val peshinCompleted: Boolean = false,
    val asrCompleted: Boolean = false,
    val shomCompleted: Boolean = false,
    val xuftonCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun isCompleted(type: PrayerType): Boolean = when (type) {
        PrayerType.BOMDOD -> bomdodCompleted
        PrayerType.PESHIN -> peshinCompleted
        PrayerType.ASR -> asrCompleted
        PrayerType.SHOM -> shomCompleted
        PrayerType.XUFTON -> xuftonCompleted
        PrayerType.QUYOSH -> false
    }

    fun withPrayerStatus(
        type: PrayerType,
        completed: Boolean,
        timestampMillis: Long = System.currentTimeMillis()
    ): DailyPrayerProgressEntity = when (type) {
        PrayerType.BOMDOD -> copy(bomdodCompleted = completed, updatedAtMillis = timestampMillis)
        PrayerType.PESHIN -> copy(peshinCompleted = completed, updatedAtMillis = timestampMillis)
        PrayerType.ASR -> copy(asrCompleted = completed, updatedAtMillis = timestampMillis)
        PrayerType.SHOM -> copy(shomCompleted = completed, updatedAtMillis = timestampMillis)
        PrayerType.XUFTON -> copy(xuftonCompleted = completed, updatedAtMillis = timestampMillis)
        PrayerType.QUYOSH -> this
    }

    fun completedTypes(): Set<PrayerType> = buildSet {
        if (bomdodCompleted) add(PrayerType.BOMDOD)
        if (peshinCompleted) add(PrayerType.PESHIN)
        if (asrCompleted) add(PrayerType.ASR)
        if (shomCompleted) add(PrayerType.SHOM)
        if (xuftonCompleted) add(PrayerType.XUFTON)
    }

    fun completedCount(): Int =
        listOf(
            bomdodCompleted,
            peshinCompleted,
            asrCompleted,
            shomCompleted,
            xuftonCompleted
        ).count { it }
}
