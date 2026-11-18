package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.domain.model.QazoPrayerType
import kotlinx.coroutines.flow.Flow

@Dao
interface QazoPrayerDao {

    @Query("SELECT * FROM qazo_prayers WHERE id = 1 LIMIT 1")
    fun observeQazo(): Flow<QazoPrayerEntity?>

    @Query("SELECT * FROM qazo_prayers WHERE id = 1 LIMIT 1")
    suspend fun getQazo(): QazoPrayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QazoPrayerEntity)

    @Transaction
    suspend fun adjustCount(type: QazoPrayerType, delta: Int) {
        val current = getQazo() ?: QazoPrayerEntity()
        upsert(current.withDelta(type, delta))
    }

    @Transaction
    suspend fun setCount(type: QazoPrayerType, count: Int) {
        val current = getQazo() ?: QazoPrayerEntity()
        upsert(current.withCount(type, count))
    }
}
