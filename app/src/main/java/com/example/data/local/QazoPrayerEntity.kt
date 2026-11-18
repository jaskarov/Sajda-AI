package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.QazoPrayerType

/**
 * Room entity storing remaining Qazo (missed) prayer counts for the 6 prayer types.
 */
@Entity(tableName = "qazo_prayers")
data class QazoPrayerEntity(
    @PrimaryKey val id: Int = 1,
    val bomdodCount: Int = 0,
    val peshinCount: Int = 0,
    val asrCount: Int = 0,
    val shomCount: Int = 0,
    val xuftonCount: Int = 0,
    val vitrCount: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun countFor(type: QazoPrayerType): Int = when (type) {
        QazoPrayerType.BOMDOD -> bomdodCount
        QazoPrayerType.PESHIN -> peshinCount
        QazoPrayerType.ASR -> asrCount
        QazoPrayerType.SHOM -> shomCount
        QazoPrayerType.XUFTON -> xuftonCount
        QazoPrayerType.VITR -> vitrCount
    }

    fun totalCount(): Int =
        bomdodCount + peshinCount + asrCount + shomCount + xuftonCount + vitrCount

    fun withDelta(
        type: QazoPrayerType,
        delta: Int,
        timestampMillis: Long = System.currentTimeMillis()
    ): QazoPrayerEntity {
        val next = (countFor(type) + delta).coerceAtLeast(0)
        return withCount(type, next, timestampMillis)
    }

    fun withCount(
        type: QazoPrayerType,
        count: Int,
        timestampMillis: Long = System.currentTimeMillis()
    ): QazoPrayerEntity {
        val safe = count.coerceAtLeast(0)
        return when (type) {
            QazoPrayerType.BOMDOD -> copy(bomdodCount = safe, updatedAtMillis = timestampMillis)
            QazoPrayerType.PESHIN -> copy(peshinCount = safe, updatedAtMillis = timestampMillis)
            QazoPrayerType.ASR -> copy(asrCount = safe, updatedAtMillis = timestampMillis)
            QazoPrayerType.SHOM -> copy(shomCount = safe, updatedAtMillis = timestampMillis)
            QazoPrayerType.XUFTON -> copy(xuftonCount = safe, updatedAtMillis = timestampMillis)
            QazoPrayerType.VITR -> copy(vitrCount = safe, updatedAtMillis = timestampMillis)
        }
    }

    fun addBulkForYears(
        years: Int,
        timestampMillis: Long = System.currentTimeMillis()
    ): QazoPrayerEntity {
        val safeYears = years.coerceIn(0, 100)
        if (safeYears == 0) return this
        val daysToAdd = safeYears * 365
        val maxCap = 36_500
        return copy(
            bomdodCount = (bomdodCount + daysToAdd).coerceAtMost(maxCap),
            peshinCount = (peshinCount + daysToAdd).coerceAtMost(maxCap),
            asrCount = (asrCount + daysToAdd).coerceAtMost(maxCap),
            shomCount = (shomCount + daysToAdd).coerceAtMost(maxCap),
            xuftonCount = (xuftonCount + daysToAdd).coerceAtMost(maxCap),
            vitrCount = (vitrCount + daysToAdd).coerceAtMost(maxCap),
            updatedAtMillis = timestampMillis
        )
    }
}
