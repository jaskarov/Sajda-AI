package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.BootCompletedReceiver
import com.example.alarm.PrayerNotificationHelper
import com.example.data.local.BackupRestoreResult
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.ReminderSoundMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.TextScript
import com.example.domain.model.WeeklyPrayerAnalytics
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyProgressScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp")
class PacketThreePerformanceAndGrowthAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bootCompletedReceiver_supportsBootTimeTimezoneAndPackageReplaced() {
        assertTrue(Intent.ACTION_BOOT_COMPLETED in BootCompletedReceiver.SUPPORTED_ACTIONS)
        assertTrue(Intent.ACTION_TIME_CHANGED in BootCompletedReceiver.SUPPORTED_ACTIONS)
        assertTrue(Intent.ACTION_TIMEZONE_CHANGED in BootCompletedReceiver.SUPPORTED_ACTIONS)
        assertTrue(Intent.ACTION_MY_PACKAGE_REPLACED in BootCompletedReceiver.SUPPORTED_ACTIONS)
    }

    @Test
    fun prayerNotificationHelper_configuresTakbirChannelSound_vibrateOnly_andMonochromeIcon() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        // 1. Qori Takbir channel must bind R.raw.takbir sound URI
        val posted = PrayerNotificationHelper.showPrayerNotification(
            context = app,
            notificationId = 501,
            prayerName = "Bomdod",
            prayerTime = "05:00",
            cityName = "Toshkent",
            script = TextScript.LATIN,
            soundMode = ReminderSoundMode.TAKBIR
        )
        assertTrue(posted)

        val nm = app.getSystemService(NotificationManager::class.java)
        val shadowNm = shadowOf(nm)
        val postedNotif = shadowNm.getNotification(501)
        assertNotNull(postedNotif)
        assertEquals(R.drawable.ic_notification_prayer, postedNotif.smallIcon.resId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val takbirChannel = nm.getNotificationChannel(PrayerNotificationHelper.CHANNEL_ID_TAKBIR)
            assertNotNull(takbirChannel)
            assertEquals(PrayerNotificationHelper.takbirSoundUri(app), takbirChannel.sound)
            assertTrue(takbirChannel.shouldVibrate())

            // 2. Vibrate-only channel must have null sound for silent homes
            PrayerNotificationHelper.ensureChannel(app, TextScript.LATIN, ReminderSoundMode.VIBRATE_ONLY)
            val vibrateChannel = nm.getNotificationChannel(PrayerNotificationHelper.CHANNEL_ID_VIBRATE)
            assertNotNull(vibrateChannel)
            assertNull(vibrateChannel.sound)
            assertTrue(vibrateChannel.shouldVibrate())
        }
    }

    @Test
    fun prayerCalendarRepository_ensureCitySeededFast_seedsOnlySelectedCity365Days() = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PrayerCalendarRepository(app, db.prayerCalendarDao())
            repo.ensureCitySeededFast(cityId = "toshkent", year = 2026)

            val toshkentCount = db.prayerCalendarDao().countForCityYear("toshkent", 2026)
            val totalCount = db.prayerCalendarDao().countAll()

            assertEquals("Active city must have full 365 days seeded", 365, toshkentCount)
            assertEquals("Fast cold-start seed must NOT seed all 14 cities synchronously", 365, totalCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun sajdaBackupManager_exportsAndRestoresAtomicallyWithSessionsOledAndSoundMode() = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefs = UserPreferencesRepository(app)
            val historyRepo = PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao())
            val qazoRepo = QazoPrayerRepository(db.qazoPrayerDao())
            val manager = SajdaBackupManager(prefs, historyRepo, qazoRepo, db)

            // Bounded stream reader rejects oversized streams before allocating full buffer
            val overLimitErr = runCatching {
                SajdaBackupManager.readBoundedUtf8(ByteArrayInputStream(ByteArray(2048)), maxBytes = 1024)
            }.exceptionOrNull()
            assertTrue(overLimitErr is IllegalArgumentException)

            prefs.setOledNightMode(true)
            prefs.setReminderSoundMode(ReminderSoundMode.VIBRATE_ONLY)
            historyRepo.recordCompletion(
                prayerTypeId = "bomdod",
                trackId = "bomdod_farz",
                rakatCount = 2,
                coachMode = "CAMERA_AI",
                cityId = "toshkent",
                durationSeconds = 180
            )
            val exportedJson = manager.exportToJson()
            assertTrue(exportedJson.contains("\"oledNightMode\": true"))
            assertTrue(exportedJson.contains("\"reminderSoundModeId\": \"vibrate_only\""))
            assertTrue(exportedJson.contains("\"trackId\": \"bomdod_farz\""))

            // Wipe prefs back and restore atomically
            prefs.setOledNightMode(false)
            prefs.setReminderSoundMode(ReminderSoundMode.TAKBIR)

            val res = manager.importFromJson(exportedJson)
            assertTrue(res is BackupRestoreResult.Success)
            val success = res as BackupRestoreResult.Success
            assertEquals(1, success.sessionsRestored)

            val restoredSettings = prefs.settingsFlow.first()
            assertTrue(restoredSettings.oledNightMode)
            assertEquals(ReminderSoundMode.VIBRATE_ONLY, restoredSettings.reminderSoundMode)
        } finally {
            db.close()
        }
    }

    @Test
    fun homeScreen_exposesOledNightModeToggleAndIsolatedCountdown() {
        val schedule = GetPrayerTimesUseCase().calculateForToday(
            city = UzbekistanCities.findById("toshkent"),
            completedToday = emptySet(),
            alarmMap = emptyMap()
        )
        var oledToggledTo: Boolean? = null

        composeRule.setContent {
            SajdaAiTheme(oledNightMode = true) {
                HomeScreen(
                    uiState = MainUiState(
                        settings = UserSettings(oledNightMode = true),
                        schedule = schedule,
                        isReady = true
                    ),
                    onStartPrayer = {},
                    onToggleAlarm = {},
                    onToggleCompleted = {},
                    onCitySelected = {},
                    onOpenSettings = {},
                    onToggleOledNightMode = { oledToggledTo = it }
                )
            }
        }
        composeRule.onNodeWithTag("home_countdown_text").assertIsDisplayed()
        composeRule.onNodeWithTag("home_oled_toggle_button").assertIsDisplayed().performClick()
        assertEquals(false, oledToggledTo)
    }

    @Test
    fun settingsScreen_exposesOledSwitchAndTakbirReminderSoundSelector() {
        var oledToggledTo: Boolean? = null
        var soundSelected: ReminderSoundMode? = null
        var previewClicked = false

        composeRule.setContent {
            SajdaAiTheme(oledNightMode = false) {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(
                            oledNightMode = false,
                            reminderSoundMode = ReminderSoundMode.TAKBIR
                        ),
                        isReady = true
                    ),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onOledNightModeToggle = { oledToggledTo = it },
                    onReminderSoundModeChange = { soundSelected = it },
                    onPreviewTakbirSound = { previewClicked = true },
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithTag("settings_oled_night_mode_switch").performScrollTo().performClick()
        assertEquals(true, oledToggledTo)
        composeRule.onNodeWithTag("settings_sound_vibrate_only").performScrollTo().performClick()
        assertEquals(ReminderSoundMode.VIBRATE_ONLY, soundSelected)
        composeRule.onNodeWithTag("settings_preview_takbir_button").performScrollTo().performClick()
        assertTrue(previewClicked)
    }

    @Test
    fun weeklyProgressScreen_displaysTelegramFamilyShareCard() {
        val weeklySummary = WeeklyPrayerAnalytics.buildSummary(emptyList(), weekOffset = 0)
        composeRule.setContent {
            SajdaAiTheme {
                WeeklyProgressScreen(
                    uiState = MainUiState(
                        settings = UserSettings(script = TextScript.LATIN),
                        weeklySummary = weeklySummary,
                        isReady = true
                    ),
                    onShiftWeek = {},
                    onTogglePrayerForDay = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithTag("weekly_share_card").assertIsDisplayed()
        composeRule.onNodeWithTag("weekly_share_button").assertIsDisplayed()
    }
}
