package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrayerCalendarDayEntity
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.PrayerSyncOutcome
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserSettings
import com.example.data.remote.PrayerTimeFetchResult
import com.example.data.remote.RemoteUzbekistanPrayerTimesService
import com.example.data.remote.UzbekistanPrayerTimesApi
import com.example.data.remote.UzbekistanPrayerTimesService
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.UzbekCity
import com.example.domain.model.lText
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UzbekistanPrayerTimesServiceAndRoomTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: SajdaDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun remoteService_parsesIslomApiMonthlyResponseForUzbekistanCity() = runTest {
        val sampleIslomJson = """
            [
              {
                "region": "Toshkent",
                "date": "2026-11-18T00:00:00.000Z",
                "month": 11,
                "day": 18,
                "times": {
                  "tong_saharlik": "5:56",
                  "quyosh": "07:17",
                  "peshin": "12:12",
                  "asr": "15:18",
                  "shom_iftor": "17:02",
                  "hufton": "18:22"
                }
              }
            ]
        """.trimIndent()

        val fakeApi = object : UzbekistanPrayerTimesApi {
            override suspend fun fetchIslomApiMonthly(region: String, month: Int): Response<ResponseBody> {
                assertEquals("Toshkent", region)
                assertEquals(11, month)
                return Response.success(sampleIslomJson.toResponseBody("application/json".toMediaType()))
            }

            override suspend fun fetchAladhanMonthly(
                year: Int,
                month: Int,
                latitude: Double,
                longitude: Double,
                method: Int,
                methodSettings: String,
                school: Int,
                tune: String,
                timezone: String
            ): Response<ResponseBody> = error("Fallback should not be called when primary succeeds")
        }

        val service = RemoteUzbekistanPrayerTimesService(fakeApi)
        val result = service.fetchMonthlyPrayerTimes(
            city = UzbekistanCities.findById("toshkent"),
            year = 2026,
            month = 11,
            syncTimeMillis = 123456L
        )

        assertTrue(result is PrayerTimeFetchResult.Success)
        val success = result as PrayerTimeFetchResult.Success
        assertEquals(RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API, success.provider)
        assertEquals(1, success.days.size)
        assertEquals("05:56", success.days[0].bomdod)
        assertEquals("07:17", success.days[0].quyosh)
        assertEquals("12:12", success.days[0].peshin)
        assertEquals("15:18", success.days[0].asr)
        assertEquals("17:02", success.days[0].shom)
        assertEquals("18:22", success.days[0].xufton)
    }

    @Test
    fun remoteService_fallsBackToAladhanWhenIslomApiFails() = runTest {
        val sampleAladhanJson = """
            {
              "code": 200,
              "data": [
                {
                  "timings": {
                    "Fajr": "05:44 (+05)",
                    "Sunrise": "07:05 (+05)",
                    "Dhuhr": "12:04 (+05)",
                    "Asr": "15:11 (+05)",
                    "Maghrib": "16:54 (+05)",
                    "Isha": "18:14 (+05)"
                  },
                  "date": {
                    "gregorian": {
                      "day": "18",
                      "year": "2026",
                      "month": { "number": 11 }
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val fakeApi = object : UzbekistanPrayerTimesApi {
            override suspend fun fetchIslomApiMonthly(region: String, month: Int): Response<ResponseBody> {
                return Response.error(503, "Unavailable".toResponseBody("text/plain".toMediaType()))
            }

            override suspend fun fetchAladhanMonthly(
                year: Int,
                month: Int,
                latitude: Double,
                longitude: Double,
                method: Int,
                methodSettings: String,
                school: Int,
                tune: String,
                timezone: String
            ): Response<ResponseBody> {
                assertEquals("Asia/Tashkent", timezone)
                assertEquals(1, school)
                return Response.success(sampleAladhanJson.toResponseBody("application/json".toMediaType()))
            }
        }

        val service = RemoteUzbekistanPrayerTimesService(fakeApi)
        val result = service.fetchMonthlyPrayerTimes(
            city = UzbekistanCities.findById("samarqand"),
            year = 2026,
            month = 11,
            syncTimeMillis = 99999L
        )

        assertTrue(result is PrayerTimeFetchResult.Success)
        val success = result as PrayerTimeFetchResult.Success
        assertEquals(RemoteUzbekistanPrayerTimesService.PROVIDER_ALADHAN, success.provider)
        assertEquals("05:44", success.days[0].bomdod)
        assertEquals("16:54", success.days[0].shom)
    }

    @Test
    fun repositorySync_storesFetchedTimesInRoomAndServesOfflineWhenDisconnected() = runTest {
        var online = true
        val fakeService = object : UzbekistanPrayerTimesService {
            override suspend fun fetchMonthlyPrayerTimes(
                city: UzbekCity,
                year: Int,
                month: Int,
                syncTimeMillis: Long
            ): PrayerTimeFetchResult {
                if (year == 2026 && month == 11) {
                    return PrayerTimeFetchResult.Success(
                        provider = RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API,
                        days = listOf(
                            PrayerCalendarDayEntity(
                                cityId = city.id,
                                year = 2026,
                                month = 11,
                                day = 18,
                                bomdod = "05:55",
                                quyosh = "07:16",
                                peshin = "12:11",
                                asr = "15:17",
                                shom = "17:01",
                                xufton = "18:21",
                                source = RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API,
                                syncedAtMillis = syncTimeMillis
                            )
                        )
                    )
                }
                return PrayerTimeFetchResult.Failure("not mocked for next month")
            }
        }

        val repo = PrayerCalendarRepository(
            context = context,
            dao = db.prayerCalendarDao(),
            syncMetadataDao = db.prayerSyncMetadataDao(),
            remoteService = fakeService,
            isOnlineProvider = { online }
        )

        val nov18 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(2026, Calendar.NOVEMBER, 18, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 1. Online sync fetches and persists into Room + metadata table
        val syncOutcome = repo.syncCityPrayerTimes("toshkent", nov18)
        assertTrue(syncOutcome is PrayerSyncOutcome.Synced)
        val synced = syncOutcome as PrayerSyncOutcome.Synced
        assertEquals(RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API, synced.provider)
        assertEquals(1, synced.fetchedDaysCount)
        assertTrue(synced.totalCachedDays >= 365)

        val meta = repo.observeSyncMetadata("toshkent").first()
        assertNotNull(meta)
        assertEquals(RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API, meta!!.provider)
        assertTrue(meta.lastSyncSuccess)
        assertTrue(meta.cachedDaysCount >= 365)

        // 2. Go offline — locally stored Room prayer times remain accessible reactively
        online = false
        val offlineOutcome = repo.syncCityPrayerTimes("toshkent", nov18)
        assertTrue(offlineOutcome is PrayerSyncOutcome.OfflineCacheReady)

        val (today, _) = repo.getTodayAndTomorrow("toshkent", nov18)
        assertNotNull(today)
        assertEquals("05:55", today!!.bomdod)
        assertEquals("17:01", today.shom)
        assertEquals(RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API, today.source)

        val novemberRows = repo.observeMonthSchedule("toshkent", 2026, 11).first()
        assertEquals(30, novemberRows.size)
        assertEquals("05:55", novemberRows.first { it.day == 18 }.bomdod)

        val cachedCount = repo.observeCachedDayCount("toshkent").first()
        assertTrue(cachedCount >= 365)
    }

    @Test
    fun settingsScreen_displaysOfflineRoomStatusAndMonthlyCalendarSheet() {
        val sampleDays = listOf(
            PrayerCalendarDayEntity(
                cityId = "toshkent",
                year = 2026,
                month = 11,
                day = 18,
                bomdod = "05:55",
                quyosh = "07:16",
                peshin = "12:11",
                asr = "15:17",
                shom = "17:01",
                xufton = "18:21",
                source = RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API,
                syncedAtMillis = 1_763_456_400_000L
            )
        )

        composeRule.setContent {
            SajdaAiTheme {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(selectedCityId = "toshkent"),
                        calendarSource = RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API,
                        lastCalendarSyncMillis = 1_763_456_400_000L,
                        cachedCalendarDaysCount = 730,
                        monthlyCalendarDays = sampleDays,
                        calendarSyncStatusMessage = lText(
                            "Yangilandi (Islom.uz): 60 kun yuklandi • 730 kun oflayn Room bazasida",
                            "Янгиланди (Islom.uz): 60 кун юкланди • 730 кун офлайн Room базасида"
                        ),
                        isReady = true
                    ),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_calendar_source_label")
            .performScrollTo()
            .assertTextContains("Islom.uz API", substring = true)
            .assertTextContains("730 kun oflayn saqlangan", substring = true)

        composeRule.onNodeWithTag("settings_calendar_sync_status")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("settings_view_offline_calendar_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("offline_monthly_calendar_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("05:55").assertIsDisplayed()
        composeRule.onNodeWithText("18:21").assertIsDisplayed()
    }
}
