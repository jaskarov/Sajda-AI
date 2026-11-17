package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.SajdaDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerCalendarSyncOfflineTest {

    private lateinit var context: Context
    private lateinit var database: SajdaDatabase
    private lateinit var repository: PrayerCalendarRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PrayerCalendarRepository(context, database.prayerCalendarDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun ensureSeededFromAssets_populatesDatabaseWith365Days() = runTest {
        repository.ensureSeededFromAssets()

        val totalInDb = database.prayerCalendarDao().countAll()
        assertTrue("Database count should be populated with at least 365 rows", totalInDb >= 365)
    }

    @Test
    fun getTodayAndTomorrow_returnsValidDataForMajorCities() = runTest {
        repository.ensureSeededFromAssets()

        val sampleCities = listOf("toshkent", "samarqand", "buxoro", "andijon", "nukus")
        for (cityId in sampleCities) {
            val (today, tomorrow) = repository.getTodayAndTomorrow(cityId)
            assertNotNull("Today should exist for $cityId", today)
            assertNotNull("Tomorrow should exist for $cityId", tomorrow)
            assertTrue("Bomdod time should not be blank", today!!.bomdod.isNotBlank())
            assertTrue("Peshin time should not be blank", today.peshin.isNotBlank())
            assertTrue("Asr time should not be blank", today.asr.isNotBlank())
            assertTrue("Shom time should not be blank", today.shom.isNotBlank())
            assertTrue("Xufton time should not be blank", today.xufton.isNotBlank())
        }
    }
}
