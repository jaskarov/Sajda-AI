package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerSyncMetadataDao {

    @Query("SELECT * FROM prayer_sync_metadata WHERE cityId = :cityId LIMIT 1")
    fun observeMetadata(cityId: String): Flow<PrayerSyncMetadataEntity?>

    @Query("SELECT * FROM prayer_sync_metadata WHERE cityId = :cityId LIMIT 1")
    suspend fun getMetadata(cityId: String): PrayerSyncMetadataEntity?

    @Query("SELECT * FROM prayer_sync_metadata ORDER BY lastSyncedAtMillis DESC")
    fun observeAllMetadata(): Flow<List<PrayerSyncMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: PrayerSyncMetadataEntity)
}
