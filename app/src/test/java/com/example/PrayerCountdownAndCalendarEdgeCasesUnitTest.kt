package com.example

import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerCountdownAndCalendarEdgeCasesUnitTest {

    private lateinit var useCase: GetPrayerTimesUseCase
    private val tashkentTz = TimeZone.getTimeZone("Asia/Tashkent")

    @Before
    fun setUp() {
        useCase = GetPrayerTimesUseCase()
    }

    @Test
    fun calculateForToday_midnightCrossing_nextPrayerIsBomdod() {
        val city = UzbekistanCities.findById("tashkent")
        val midnightMillis = Calendar.getInstance(tashkentTz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val schedule = useCase.calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = emptyMap(),
            nowMillis = midnightMillis
        )

        assertEquals(PrayerType.BOMDOD, schedule.nextPrayer)
        assertTrue(schedule.countdownSeconds > 0)
    }

    @Test
    fun calculateForToday_lateNightAfterXufton_nextPrayerIsNextDayBomdod() {
        val city = UzbekistanCities.findById("samarkand")
        val lateNightMillis = Calendar.getInstance(tashkentTz).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val schedule = useCase.calculateForToday(
            city = city,
            completedToday = setOf(PrayerType.XUFTON),
            alarmMap = emptyMap(),
            nowMillis = lateNightMillis
        )

        assertEquals(PrayerType.XUFTON, schedule.currentPrayer)
        assertEquals(PrayerType.BOMDOD, schedule.nextPrayer)
        assertTrue("Remaining seconds should be positive for tomorrow's Bomdod", schedule.countdownSeconds > 0)
    }

    @Test
    fun calculateForToday_all14RegionsHaveValidPrayerTimes() {
        val now = System.currentTimeMillis()
        for (city in UzbekistanCities.all) {
            val schedule = useCase.calculateForToday(
                city = city,
                completedToday = emptySet(),
                alarmMap = emptyMap(),
                nowMillis = now
            )

            assertEquals(6, schedule.slots.size)
            assertTrue("Qibla bearing should be between 200 and 260 degrees", schedule.qiblaBearingDegrees in 200.0..260.0)
            schedule.slots.forEach { slot ->
                assertTrue("Hour should be 0..23", slot.hour in 0..23)
                assertTrue("Minute should be 0..59", slot.minute in 0..59)
            }
        }
    }
}
