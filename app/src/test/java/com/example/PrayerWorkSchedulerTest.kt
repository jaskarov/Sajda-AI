package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.alarm.DailyPrayerScheduleSyncWorker
import com.example.alarm.PrayerNotificationWorker
import com.example.alarm.PrayerWorkScheduler
import com.example.data.preferences.UserSettings
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class PrayerWorkSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun syncAllPrayers_enqueuesFiveDailyPrayerWorkAndPeriodicSync() {
        val scheduler = PrayerWorkScheduler(context, workManager)
        val settings = UserSettings(
            selectedCityId = "toshkent",
            alarmMap = PrayerType.entries.associateWith { it.isPrayable }
        )

        scheduler.syncAllPrayers(settings)

        val prayableTypes = listOf(
            PrayerType.BOMDOD,
            PrayerType.PESHIN,
            PrayerType.ASR,
            PrayerType.SHOM,
            PrayerType.XUFTON
        )
        prayableTypes.forEach { type ->
            val infos = workManager.getWorkInfosForUniqueWork(
                PrayerWorkScheduler.uniqueWorkName(type)
            ).get()
            assertEquals(1, infos.size)
            assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
        }

        val periodicInfos = workManager.getWorkInfosForUniqueWork(
            PrayerWorkScheduler.UNIQUE_DAILY_SYNC_WORK
        ).get()
        assertEquals(1, periodicInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, periodicInfos.first().state)
    }

    @Test
    fun syncAllPrayers_cancelsDisabledPrayerWork() {
        val scheduler = PrayerWorkScheduler(context, workManager)
        val allEnabled = UserSettings(
            selectedCityId = "fargona",
            alarmMap = PrayerType.entries.associateWith { it.isPrayable }
        )
        scheduler.syncAllPrayers(allEnabled)

        val asrDisabled = allEnabled.copy(
            alarmMap = allEnabled.alarmMap.toMutableMap().apply {
                put(PrayerType.ASR, false)
            }
        )
        scheduler.syncAllPrayers(asrDisabled)

        val asrInfos = workManager.getWorkInfosForUniqueWork(
            PrayerWorkScheduler.uniqueWorkName(PrayerType.ASR)
        ).get()
        assertEquals(WorkInfo.State.CANCELLED, asrInfos.first().state)
    }

    @Test
    fun computeNextSlotAndTriggerMillis_rollsOverToTomorrowWhenPassed() {
        val scheduler = PrayerWorkScheduler(context, workManager)
        val city = UzbekistanCities.findById("toshkent")
        val lateNight = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 23, 55, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = scheduler.computeNextSlotAndTriggerMillis(
            prayerType = PrayerType.BOMDOD,
            city = city,
            alarmMap = PrayerType.entries.associateWith { it.isPrayable },
            nowMillis = lateNight
        )!!

        assertTrue(result.second > lateNight)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = result.second }
        assertEquals(16, triggerCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun prayerNotificationWorker_executesAndSchedulesNextOccurrence() = runTest {
        val input = workDataOf(
            PrayerNotificationWorker.KEY_PRAYER_TYPE_ID to PrayerType.PESHIN.id,
            PrayerNotificationWorker.KEY_PRAYER_NAME to "Peshin",
            PrayerNotificationWorker.KEY_PRAYER_TIME to "12:15",
            PrayerNotificationWorker.KEY_CITY_NAME to "Toshkent",
            PrayerNotificationWorker.KEY_NOTIFICATION_ID to 2002
        )
        val worker = TestListenableWorkerBuilder<PrayerNotificationWorker>(context)
            .setInputData(input)
            .build()

        val result = worker.doWork()
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)

        val rescheduled = workManager.getWorkInfosForUniqueWork(
            PrayerWorkScheduler.uniqueWorkName(PrayerType.PESHIN)
        ).get()
        assertEquals(1, rescheduled.size)
        assertEquals(WorkInfo.State.ENQUEUED, rescheduled.first().state)
    }

    @Test
    fun dailyPrayerScheduleSyncWorker_succeedsAndPopulatesAllFivePrayers() = runTest {
        val worker = TestListenableWorkerBuilder<DailyPrayerScheduleSyncWorker>(context).build()
        val result = worker.doWork()
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)

        val tagged = workManager.getWorkInfosByTag(PrayerWorkScheduler.TAG_PRAYER_NOTIFICATION).get()
        assertEquals(5, tagged.size)
    }
}
