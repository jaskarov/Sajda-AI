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

    suspend fun addBulkForYears(years: Int) {
        qazoDao.addBulkForYears(years)
    }

    suspend fun getSnapshot(): QazoPrayerEntity = qazoDao.getQazo() ?: QazoPrayerEntity()

    suspend fun restoreSnapshot(entity: QazoPrayerEntity) {
        qazoDao.upsert(
            entity.copy(
                id = 1,
                bomdodCount = entity.bomdodCount.coerceAtLeast(0),
                peshinCount = entity.peshinCount.coerceAtLeast(0),
                asrCount = entity.asrCount.coerceAtLeast(0),
                shomCount = entity.shomCount.coerceAtLeast(0),
                xuftonCount = entity.xuftonCount.coerceAtLeast(0),
                vitrCount = entity.vitrCount.coerceAtLeast(0)
            )
        )
    }
}
