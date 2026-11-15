package com.example.alarm

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerTimeSlot
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.UzbekCity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages WorkManager background tasks for the five daily prayers
 * (Bomdod, Peshin, Asr, Shom, Xufton) using O'zbekiston prayer times.
 */
class PrayerWorkScheduler(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = GetPrayerTimesUseCase()
) {

    private val legacyAlarmScheduler by lazy { PrayerAlarmScheduler(context) }

    fun syncAllPrayers(
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis(),
        ensurePeriodicSync: Boolean = true
    ) {
        val city = UzbekistanCities.findById(settings.selectedCityId)
        val todaySummary = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = settings.alarmMap,
            nowMillis = nowMillis
        )

        todaySummary.slots
            .filter { it.type.isPrayable }
            .forEach { slot ->
                // Clear any legacy AlarmManager alarms to prevent duplicate notifications
                legacyAlarmScheduler.cancelSlot(slot.type)

                if (slot.alarmEnabled) {
                    scheduleNextOccurrence(
                        prayerType = slot.type,
                        settings = settings,
                        nowMillis = nowMillis
                    )
                } else {
                    cancelPrayer(slot.type)
                }
            }

        if (ensurePeriodicSync) {
            schedulePeriodicDailySync()
        }
    }

    fun scheduleNextOccurrence(
        prayerType: PrayerType,
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!prayerType.isPrayable) return
        if (settings.alarmMap[prayerType] == false) {
            cancelPrayer(prayerType)
            return
        }

        val city = UzbekistanCities.findById(settings.selectedCityId)
        val script = settings.script
        val nextSlotAndTrigger = computeNextSlotAndTriggerMillis(
            prayerType = prayerType,
            city = city,
            alarmMap = settings.alarmMap,
            nowMillis = nowMillis
        ) ?: return

        val (slot, triggerMillis) = nextSlotAndTrigger
        val delayMillis = (triggerMillis - nowMillis).coerceAtLeast(0L)

        val inputData = Data.Builder()
            .putString(PrayerNotificationWorker.KEY_PRAYER_TYPE_ID, prayerType.id)
            .putString(PrayerNotificationWorker.KEY_PRAYER_NAME, prayerType.title.get(script))
            .putString(PrayerNotificationWorker.KEY_PRAYER_TIME, slot.timeFormatted)
            .putString(PrayerNotificationWorker.KEY_CITY_NAME, city.name.get(script))
            .putInt(
                PrayerNotificationWorker.KEY_NOTIFICATION_ID,
                PrayerNotificationWorker.NOTIFICATION_ID_BASE + prayerType.ordinal
            )
            .build()

        val request = OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_PRAYER_NOTIFICATION)
            .addTag(tagForPrayer(prayerType))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(prayerType),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelPrayer(type: PrayerType) {
        legacyAlarmScheduler.cancelSlot(type)
        workManager.cancelUniqueWork(uniqueWorkName(type))
    }

    fun schedulePeriodicDailySync() {
        val periodicRequest = PeriodicWorkRequestBuilder<DailyPrayerScheduleSyncWorker>(
            12,
            TimeUnit.HOURS
        )
            .addTag(TAG_DAILY_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_DAILY_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    internal fun computeNextSlotAndTriggerMillis(
        prayerType: PrayerType,
        city: UzbekCity,
        alarmMap: Map<PrayerType, Boolean>,
        nowMillis: Long
    ): Pair<PrayerTimeSlot, Long>? {
        val todaySummary = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = alarmMap,
            nowMillis = nowMillis
        )
        val todaySlot = todaySummary.slots.firstOrNull { it.type == prayerType } ?: return null
        val todayTrigger = triggerMillisForDay(nowMillis, todaySlot, addDays = 0)
        if (todayTrigger > nowMillis) {
            return todaySlot to todayTrigger
        }

        val tomorrowMillis = nowMillis + TimeUnit.DAYS.toMillis(1)
        val tomorrowSummary = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = alarmMap,
            nowMillis = tomorrowMillis
        )
        val tomorrowSlot = tomorrowSummary.slots.firstOrNull { it.type == prayerType }
            ?: todaySlot
        val tomorrowTrigger = triggerMillisForDay(nowMillis, tomorrowSlot, addDays = 1)
        return tomorrowSlot to tomorrowTrigger
    }

    private fun triggerMillisForDay(
        baseMillis: Long,
        slot: PrayerTimeSlot,
        addDays: Int
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = baseMillis
            if (addDays != 0) {
                add(Calendar.DAY_OF_MONTH, addDays)
            }
            set(Calendar.HOUR_OF_DAY, slot.hour)
            set(Calendar.MINUTE, slot.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        const val TAG_PRAYER_NOTIFICATION = "sajda_prayer_notification"
        const val TAG_DAILY_SYNC = "sajda_daily_prayer_sync"
        const val UNIQUE_DAILY_SYNC_WORK = "sajda_daily_prayer_sync_work"

        fun uniqueWorkName(type: PrayerType): String = "sajda_prayer_notif_${type.id}"
        fun tagForPrayer(type: PrayerType): String = "prayer_${type.id}"
    }
}
