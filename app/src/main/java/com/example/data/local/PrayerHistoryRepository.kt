package com.example.data.local

import com.example.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerHistoryRepository(
    private val historyDao: PrayerHistoryDao,
    private val dailyProgressDao: DailyPrayerProgressDao
) {

    val recentHistory: Flow<List<PrayerHistoryEntity>> = historyDao.getRecentHistory()

    val recentDailyProgress: Flow<List<DailyPrayerProgressEntity>> =
        dailyProgressDao.observeRecentDays(limit = 30)

    fun observeTodayProgress(): Flow<DailyPrayerProgressEntity> {
        val key = todayKey()
        return dailyProgressDao.observeDay(key).map { entity ->
            entity ?: DailyPrayerProgressEntity(dayKey = key)
        }
    }

    fun getCompletedTodayFlow(): Flow<Set<PrayerType>> {
        return observeTodayProgress().map { it.completedTypes() }
    }

    fun observeDaysInRange(
        startDayKey: String,
        endDayKey: String
    ): Flow<List<DailyPrayerProgressEntity>> =
        dailyProgressDao.observeDaysInRange(startDayKey, endDayKey)

    suspend fun togglePrayerCompletedToday(prayerType: PrayerType) {
        dailyProgressDao.togglePrayerCompleted(todayKey(), prayerType)
    }

    suspend fun togglePrayerCompletedForDay(dayKey: String, prayerType: PrayerType) {
        dailyProgressDao.togglePrayerCompleted(dayKey, prayerType)
    }

    suspend fun setPrayerCompletedToday(prayerType: PrayerType, completed: Boolean) {
        dailyProgressDao.setPrayerCompleted(todayKey(), prayerType, completed)
    }

    suspend fun recordCompletion(
        prayerTypeId: String,
        trackId: String,
        rakatCount: Int,
        coachMode: String,
        cityId: String,
        durationSeconds: Int
    ) {
        val dayKey = todayKey()
        historyDao.insertHistory(
            PrayerHistoryEntity(
                prayerTypeId = prayerTypeId,
                trackId = trackId,
                rakatCount = rakatCount,
                coachMode = coachMode,
                cityId = cityId,
                dayKey = dayKey,
                durationSeconds = durationSeconds
            )
        )
        dailyProgressDao.setPrayerCompleted(
            dayKey = dayKey,
            prayerType = PrayerType.fromId(prayerTypeId),
            completed = true
        )
    }

    suspend fun deleteEntry(id: Long) = historyDao.deleteById(id)

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
