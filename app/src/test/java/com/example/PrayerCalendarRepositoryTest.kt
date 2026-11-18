package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.SajdaDatabase
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class PrayerCalendarRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: SajdaDatabase
    private lateinit var repo: PrayerCalendarRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PrayerCalendarRepository(context, db.prayerCalendarDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun ensureSeededFromAssets_populatesAll14CitiesFor365Days() = runTest {
        repo.ensureSeededFromAssets()

        val totalRows = db.prayerCalendarDao().countAll()
        assertEquals(365 * 14, totalRows)

        UzbekistanCities.all.forEach { city ->
            assertEquals(365, db.prayerCalendarDao().countForCityYear(city.id, 2026))
        }
    }

    @Test
    fun getTodayAndTomorrow_drivesUseCaseWithExactDbTimes() = runTest {
        val nov15Night = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(2026, Calendar.NOVEMBER, 15, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val (today, tomorrow) = repo.getTodayAndTomorrow("toshkent", nov15Night)
        assertNotNull(today)
        assertNotNull(tomorrow)

        val summary = GetPrayerTimesUseCase().calculateForToday(
            city = UzbekistanCities.findById("toshkent"),
            completedToday = emptySet(),
            alarmMap = PrayerType.entries.associateWith { it.isPrayable },
            nowMillis = nov15Night,
            todayCalendarDay = today,
            tomorrowCalendarDay = tomorrow
        )

        assertEquals(PrayerType.BOMDOD, summary.nextPrayer)
        assertEquals(tomorrow!!.bomdod, summary.nextPrayerTimeFormatted)
        assertEquals(PrayerType.XUFTON, summary.currentPrayer)
    }

    @Test
    fun rawAudioResources_all15RecitationsAndDuasExist() {
        val requiredAssets = listOf(
            "fotiha", "kavsar", "ixlos", "falaq",
            "takbir", "sano", "ruku", "qavma",
            "sajda", "jalsa", "attahiyat", "salovat",
            "robbana", "qunut", "salam"
        )
        requiredAssets.forEach { assetName ->
            val resId = context.resources.getIdentifier(assetName, "raw", context.packageName)
            assertNotEquals("Missing raw audio resource: $assetName", 0, resId)
        }
    }
}
