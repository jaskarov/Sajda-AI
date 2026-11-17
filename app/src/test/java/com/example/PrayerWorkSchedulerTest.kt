package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.alarm.DailyPrayerScheduleSyncWorker
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerWorkScheduler
import com.example.data.local.PrayerCalendarDayEntity
import com.example.data.preferences.UserSettings
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class PrayerWorkSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmScheduler: PrayerAlarmScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmScheduler = PrayerAlarmScheduler(context)
    }

    @Test
    fun syncAllPrayers_schedulesFiveExactAlarmsAndPeriodicSync() {
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
            assertTrue(
                "Expected AlarmManager slot scheduled for ${type.id}",
                alarmScheduler.isSlotScheduled(type)
            )
            // Ensure no duplicate WorkManager notification job exists
            val workInfos = workManager.getWorkInfosForUniqueWork(
                PrayerWorkScheduler.uniqueWorkName(type)
            ).get()
            assertTrue(workInfos.isEmpty())
        }

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertEquals(5, shadowAlarmManager.scheduledAlarms.size)

        val periodicInfos = workManager.getWorkInfosForUniqueWork(
            PrayerWorkScheduler.UNIQUE_DAILY_SYNC_WORK
        ).get()
        assertEquals(1, periodicInfos.size)
        val syncState = periodicInfos.first().state
        assertTrue(
            "Expected periodic sync ENQUEUED or RUNNING, was $syncState",
            syncState == WorkInfo.State.ENQUEUED || syncState == WorkInfo.State.RUNNING
        )
    }

    @Test
    fun syncAllPrayers_cancelsDisabledPrayerAlarm() {
        val scheduler = PrayerWorkScheduler(context, workManager)
        val allEnabled = UserSettings(
            selectedCityId = "fargona",
            alarmMap = PrayerType.entries.associateWith { it.isPrayable }
        )
        scheduler.syncAllPrayers(allEnabled)
        assertTrue(alarmScheduler.isSlotScheduled(PrayerType.ASR))

        val asrDisabled = allEnabled.copy(
            alarmMap = allEnabled.alarmMap.toMutableMap().apply {
                put(PrayerType.ASR, false)
            }
        )
        scheduler.syncAllPrayers(asrDisabled)

        assertFalse(alarmScheduler.isSlotScheduled(PrayerType.ASR))
        assertTrue(alarmScheduler.isSlotScheduled(PrayerType.BOMDOD))
    }

    @Test
    fun computeNextSlotAndTriggerMillis_rollsOverToTomorrowAndUsesCalendarDb() {
        val scheduler = PrayerWorkScheduler(context, workManager)
        val city = UzbekistanCities.findById("toshkent")
        val lateNight = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 23, 55, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayDb = PrayerCalendarDayEntity(
            cityId = "toshkent",
            year = 2026,
            month = 11,
            day = 15,
            bomdod = "05:53",
            quyosh = "07:13",
            peshin = "12:10",
            asr = "15:25",
            shom = "17:09",
            xufton = "18:26"
        )
        val tomorrowDb = todayDb.copy(day = 16, bomdod = "05:54")

        val result = scheduler.computeNextSlotAndTriggerMillis(
            prayerType = PrayerType.BOMDOD,
            city = city,
            alarmMap = PrayerType.entries.associateWith { it.isPrayable },
            nowMillis = lateNight,
            todayCalendarDay = todayDb,
            tomorrowCalendarDay = tomorrowDb
        )!!

        assertTrue(result.second > lateNight)
        assertEquals("05:54", result.first.timeFormatted)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = result.second }
        assertEquals(16, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(5, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(54, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun dailyPrayerScheduleSyncWorker_succeedsAndSchedulesExactAlarms() = runTest {
        val worker = TestListenableWorkerBuilder<DailyPrayerScheduleSyncWorker>(context).build()
        val result = worker.doWork()
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertEquals(5, shadowAlarmManager.scheduledAlarms.size)
    }
}
