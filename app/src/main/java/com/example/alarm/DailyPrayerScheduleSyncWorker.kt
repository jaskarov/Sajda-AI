package com.example.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Periodic WorkManager worker that seeds/syncs the 365-day O'zbekiston prayer calendar
 * from local assets & network (when online) and refreshes exact AlarmManager alerts.
 */
class DailyPrayerScheduleSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.settingsFlow.first()
        val db = SajdaDatabase.getInstance(applicationContext)
        val calendarRepo = PrayerCalendarRepository(
            context = applicationContext,
            dao = db.prayerCalendarDao(),
            syncMetadataDao = db.prayerSyncMetadataDao()
        )
        calendarRepo.syncCityPrayerTimes(settings.selectedCityId)
        val (todayDay, tomorrowDay) = calendarRepo.getTodayAndTomorrow(settings.selectedCityId)
        PrayerWorkScheduler(applicationContext).syncAllPrayers(
            settings = settings,
            ensurePeriodicSync = false,
            todayCalendarDay = todayDay,
            tomorrowCalendarDay = tomorrowDay
        )
        return Result.success()
    }
}
