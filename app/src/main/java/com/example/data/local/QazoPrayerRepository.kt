package com.example.data.local

import com.example.domain.model.QazoPrayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QazoPrayerRepository(
    private val qazoDao: QazoPrayerDao
) {
    val qazoFlow: Flow<QazoPrayerEntity> = qazoDao.observeQazo().map { entity ->
        entity ?: QazoPrayerEntity()
    }

    suspend fun increment(type: QazoPrayerType) {
        qazoDao.adjustCount(type, delta = 1)
    }

    suspend fun decrement(type: QazoPrayerType) {
        qazoDao.adjustCount(type, delta = -1)
    }

    suspend fun setCount(type: QazoPrayerType, count: Int) {
        qazoDao.setCount(type, count)
    }
}
