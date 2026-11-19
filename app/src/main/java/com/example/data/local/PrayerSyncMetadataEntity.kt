package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks local Room cache coverage and last network sync metadata per Uzbekistan city.
 */
@Entity(tableName = "prayer_sync_metadata")
data class PrayerSyncMetadataEntity(
    @PrimaryKey val cityId: String,
    val cityName: String,
    val provider: String,
    val lastSyncedAtMillis: Long,
    val cachedDaysCount: Int,
    val lastSyncSuccess: Boolean,
    val lastError: String? = null
)
