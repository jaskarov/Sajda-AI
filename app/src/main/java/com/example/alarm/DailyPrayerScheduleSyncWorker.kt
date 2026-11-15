package com.example.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Periodic WorkManager worker that recalculates O'zbekiston prayer times daily
 * and refreshes the five daily prayer notification tasks.
 */
class DailyPrayerScheduleSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.settingsFlow.first()
        PrayerWorkScheduler(applicationContext).syncAllPrayers(
            settings = settings,
            ensurePeriodicSync = false
        )
        return Result.success()
    }
}
