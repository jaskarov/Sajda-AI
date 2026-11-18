package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DailyPrayerProgressDao
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.SajdaDatabase
import com.example.domain.model.PrayerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyPrayerProgressDaoTest {

    private lateinit var database: SajdaDatabase
    private lateinit var dao: DailyPrayerProgressDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SajdaDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.dailyPrayerProgressDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun setAndToggleFiveDailyPrayers_persistsAccurateStatus() = runTest {
        val dayKey = "2026-11-15"
        dao.setPrayerCompleted(dayKey, PrayerType.BOMDOD, true)
        dao.setPrayerCompleted(dayKey, PrayerType.PESHIN, true)
        dao.setPrayerCompleted(dayKey, PrayerType.ASR, true)
        dao.setPrayerCompleted(dayKey, PrayerType.SHOM, true)
        dao.setPrayerCompleted(dayKey, PrayerType.XUFTON, true)

        val full = dao.observeDay(dayKey).first()!!
        assertEquals(5, full.completedCount())
        assertTrue(full.bomdodCompleted)
        assertTrue(full.peshinCompleted)
        assertTrue(full.asrCompleted)
        assertTrue(full.shomCompleted)
        assertTrue(full.xuftonCompleted)

        dao.togglePrayerCompleted(dayKey, PrayerType.ASR)
        val afterToggle = dao.getDay(dayKey)!!
        assertEquals(4, afterToggle.completedCount())
        assertFalse(afterToggle.isCompleted(PrayerType.ASR))
    }

    @Test
    fun observeRecentDays_returnsDescendingByDayKey() = runTest {
        dao.upsert(DailyPrayerProgressEntity(dayKey = "2026-11-13", bomdodCompleted = true))
        dao.upsert(DailyPrayerProgressEntity(dayKey = "2026-11-15", peshinCompleted = true))
        dao.upsert(DailyPrayerProgressEntity(dayKey = "2026-11-14", shomCompleted = true))

        val recent = dao.observeRecentDays(limit = 10).first()
        assertEquals(listOf("2026-11-15", "2026-11-14", "2026-11-13"), recent.map { it.dayKey })
    }

    @Test
    fun observeDaysInRange_andWeeklyAnalytics_computeAccuratePercentages() = runTest {
        // Week of 2026-11-09 (Mon) .. 2026-11-15 (Sun)
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(2026, java.util.Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        val (startKey, endKey) = com.example.domain.model.WeeklyPrayerAnalytics.weekBounds(
            weekOffset = 0,
            referenceTimeMillis = cal.timeInMillis
        )
        assertEquals("2026-11-09", startKey)
        assertEquals("2026-11-15", endKey)

        dao.upsert(
            DailyPrayerProgressEntity(
                dayKey = "2026-11-09",
                bomdodCompleted = true,
                peshinCompleted = true,
                asrCompleted = true,
                shomCompleted = true,
                xuftonCompleted = true
            )
        )
        dao.upsert(
            DailyPrayerProgressEntity(
                dayKey = "2026-11-15",
                bomdodCompleted = true,
                peshinCompleted = true
            )
        )
        // Out of range day should be excluded
        dao.upsert(
            DailyPrayerProgressEntity(
                dayKey = "2026-11-01",
                bomdodCompleted = true
            )
        )

        val rangeRows = dao.observeDaysInRange(startKey, endKey).first()
        assertEquals(2, rangeRows.size)

        val summary = com.example.domain.model.WeeklyPrayerAnalytics.buildSummary(
            records = rangeRows,
            weekOffset = 0,
            referenceTimeMillis = cal.timeInMillis
        )
        assertEquals(7, summary.totalCompleted)
        assertEquals(1, summary.perfectDaysCount)
        assertEquals(20, summary.overallRoundedPercent) // 7 / 35 = 20%

        val bomdodBar = summary.prayerBars.first { it.prayerType == PrayerType.BOMDOD }
        assertEquals(2, bomdodBar.completedDays)
        assertEquals(29, bomdodBar.roundedPercent) // 2 / 7 ≈ 28.57%

        val asrBar = summary.prayerBars.first { it.prayerType == PrayerType.ASR }
        assertEquals(1, asrBar.completedDays)
        assertEquals(14, asrBar.roundedPercent) // 1 / 7 ≈ 14.28%
    }
}
