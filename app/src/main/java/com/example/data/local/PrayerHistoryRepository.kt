package com.example.data.local

import com.example.domain.model.PrayerType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerHistoryRepository(
    private val historyDao: PrayerHistoryDao,
    private val dailyProgressDao: DailyPrayerProgressDao
) {

    companion object {
        val TASHKENT_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Tashkent")

        fun formatDayKey(date: Date = Date(), timeZone: TimeZone = TASHKENT_TIME_ZONE): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                this.timeZone = timeZone
            }.format(date)
        }
    }

    val recentHistory: Flow<List<PrayerHistoryEntity>> = historyDao.getRecentHistory()

    val recentDailyProgress: Flow<List<DailyPrayerProgressEntity>> =
        dailyProgressDao.observeRecentDays(limit = 30)

    fun observeTodayProgress(dayKeyFlow: Flow<String>? = null): Flow<DailyPrayerProgressEntity> {
        return if (dayKeyFlow != null) {
            dayKeyFlow.distinctUntilChanged().flatMapLatest { key ->
                dailyProgressDao.observeDay(key).map { entity ->
                    entity ?: DailyPrayerProgressEntity(dayKey = key)
                }
            }
        } else {
            flow {
                emit(todayKey())
            }.flatMapLatest { key ->
                dailyProgressDao.observeDay(key).map { entity ->
                    entity ?: DailyPrayerProgressEntity(dayKey = key)
                }
            }
        }
    }

    fun getCompletedTodayFlow(dayKeyFlow: Flow<String>? = null): Flow<Set<PrayerType>> {
        return observeTodayProgress(dayKeyFlow).map { it.completedTypes() }
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
        durationSeconds: Int,
        markDailyCompleted: Boolean = true
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
        if (markDailyCompleted) {
            dailyProgressDao.setPrayerCompleted(
                dayKey = dayKey,
                prayerType = PrayerType.fromId(prayerTypeId),
                completed = true
            )
        }
    }

    suspend fun deleteEntry(id: Long) = historyDao.deleteById(id)

    suspend fun getAllDailyProgress(): List<DailyPrayerProgressEntity> =
        dailyProgressDao.getAllDays()

    suspend fun restoreDailyProgress(items: List<DailyPrayerProgressEntity>) {
        if (items.isNotEmpty()) {
            dailyProgressDao.upsertAll(items)
        }
    }

    fun todayKey(): String = formatDayKey()
}
