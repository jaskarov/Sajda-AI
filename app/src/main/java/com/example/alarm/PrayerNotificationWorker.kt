package com.example.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerType
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that fires a local notification when a scheduled prayer time arrives
 * and automatically schedules the next day's occurrence using O'zbekiston prayer times.
 */
class PrayerNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prayerTypeId = inputData.getString(KEY_PRAYER_TYPE_ID)
            ?: return Result.failure()
        val prayerType = PrayerType.fromId(prayerTypeId)
        if (!prayerType.isPrayable) return Result.success()

        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.settingsFlow.first()
        val isEnabled = settings.alarmMap[prayerType] ?: true
        if (!isEnabled) return Result.success()

        val script = settings.script
        val city = UzbekistanCities.findById(settings.selectedCityId)
        val prayerName = inputData.getString(KEY_PRAYER_NAME)
            ?: prayerType.title.get(script)
        val prayerTime = inputData.getString(KEY_PRAYER_TIME).orEmpty()
        val cityName = inputData.getString(KEY_CITY_NAME)
            ?: city.name.get(script)
        val notificationId = inputData.getInt(
            KEY_NOTIFICATION_ID,
            NOTIFICATION_ID_BASE + prayerType.ordinal
        )

        PrayerNotificationHelper.showPrayerNotification(
            context = applicationContext,
            notificationId = notificationId,
            prayerName = prayerName,
            prayerTime = prayerTime,
            cityName = cityName,
            script = script,
            soundMode = settings.reminderSoundMode
        )

        // Schedule tomorrow's occurrence using O'zbekiston astronomical prayer times
        PrayerWorkScheduler(applicationContext).scheduleNextOccurrence(
            prayerType = prayerType,
            settings = settings,
            nowMillis = System.currentTimeMillis() + 61_000L
        )

        return Result.success()
    }

    companion object {
        const val KEY_PRAYER_TYPE_ID = "key_prayer_type_id"
        const val KEY_PRAYER_NAME = "key_prayer_name"
        const val KEY_PRAYER_TIME = "key_prayer_time"
        const val KEY_CITY_NAME = "key_city_name"
        const val KEY_NOTIFICATION_ID = "key_notification_id"
        const val NOTIFICATION_ID_BASE = 2000
    }
}
