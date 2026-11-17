package com.example.alarm

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.PrayerCalendarDayEntity
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerTimeSlot
import com.example.domain.model.PrayerType
import com.example.domain.model.UzbekCity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Coordinates exact AlarmManager reminders (Bomdod, Peshin, Asr, Shom, Xufton)
 * with periodic WorkManager daily calendar sync so notifications arrive on time
 * without duplicate alerts.
 */
class PrayerWorkScheduler(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase = GetPrayerTimesUseCase(),
    private val alarmScheduler: PrayerAlarmScheduler = PrayerAlarmScheduler(context)
) {

    fun syncAllPrayers(
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis(),
        ensurePeriodicSync: Boolean = true,
        todayCalendarDay: PrayerCalendarDayEntity? = null,
        tomorrowCalendarDay: PrayerCalendarDayEntity? = null
    ) {
        val city = UzbekistanCities.findById(settings.selectedCityId)
        val todaySummary = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = settings.alarmMap,
            nowMillis = nowMillis,
            todayCalendarDay = todayCalendarDay,
            tomorrowCalendarDay = tomorrowCalendarDay
        )

        todaySummary.slots
            .filter { it.type.isPrayable }
            .forEach { slot ->
                // Ensure no legacy duplicate WorkManager notification job remains active
                workManager.cancelUniqueWork(uniqueWorkName(slot.type))

                if (slot.alarmEnabled) {
                    scheduleNextOccurrence(
                        prayerType = slot.type,
                        settings = settings,
                        nowMillis = nowMillis,
                        todayCalendarDay = todayCalendarDay,
                        tomorrowCalendarDay = tomorrowCalendarDay
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
        nowMillis: Long = System.currentTimeMillis(),
        todayCalendarDay: PrayerCalendarDayEntity? = null,
        tomorrowCalendarDay: PrayerCalendarDayEntity? = null
    ) {
        if (!prayerType.isPrayable) return
        workManager.cancelUniqueWork(uniqueWorkName(prayerType))

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
            nowMillis = nowMillis,
            todayCalendarDay = todayCalendarDay,
            tomorrowCalendarDay = tomorrowCalendarDay
        ) ?: return

        val (slot, triggerMillis) = nextSlotAndTrigger

        alarmScheduler.scheduleExactPrayer(
            type = prayerType,
            triggerMillis = triggerMillis,
            prayerName = prayerType.title.get(script),
            prayerTime = slot.timeFormatted,
            cityName = city.name.get(script),
            script = script
        )
    }

    fun cancelPrayer(type: PrayerType) {
        alarmScheduler.cancelSlot(type)
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
        nowMillis: Long,
        todayCalendarDay: PrayerCalendarDayEntity? = null,
        tomorrowCalendarDay: PrayerCalendarDayEntity? = null
    ): Pair<PrayerTimeSlot, Long>? {
        val todaySummary = getPrayerTimesUseCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = alarmMap,
            nowMillis = nowMillis,
            todayCalendarDay = todayCalendarDay,
            tomorrowCalendarDay = tomorrowCalendarDay
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
            nowMillis = tomorrowMillis,
            todayCalendarDay = tomorrowCalendarDay,
            tomorrowCalendarDay = null
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
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
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
