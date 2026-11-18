package com.example

import android.app.Application
import android.os.PowerManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.TtsReadinessInfo
import com.example.data.local.BackupRestoreResult
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.PrayerCalendarRepository
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerEntity
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.model.Gender
import com.example.domain.model.PrayerType
import com.example.domain.model.QazoPrayerType
import com.example.domain.model.TextScript
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseReadinessTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prayerCalendarRepository_seeds2027OfflineForAll14Cities() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PrayerCalendarRepository(context, db.prayerCalendarDao())
            val cal2027 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
                set(2027, Calendar.MAY, 10, 9, 0, 0)
            }
            val (today, tomorrow) = repo.getTodayAndTomorrow("toshkent", cal2027.timeInMillis)
            assertTrue(today != null && tomorrow != null)
            assertEquals(2027, today!!.year)
            assertEquals(5, today.month)
            assertEquals(10, today.day)
            assertEquals("uz_taqvim_365_2027_offline", today.source)
            assertTrue(today.bomdod.matches(Regex("\\d{2}:\\d{2}")))

            val count2026 = db.prayerCalendarDao().countForYear(2026)
            val count2027 = db.prayerCalendarDao().countForYear(2027)
            assertEquals(365 * 14, count2026)
            assertEquals(365 * 14, count2027)
        } finally {
            db.close()
        }
    }

    @Test
    fun backupManager_exportsAndRestoresQazoDailyHistoryAndSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefsRepo = UserPreferencesRepository(context)
            val historyRepo = PrayerHistoryRepository(
                db.prayerHistoryDao(),
                db.dailyPrayerProgressDao()
            )
            val qazoRepo = QazoPrayerRepository(db.qazoPrayerDao())
            val backupManager = SajdaBackupManager(prefsRepo, historyRepo, qazoRepo)

            prefsRepo.restoreSettings(
                UserSettings(
                    onboardingCompleted = true,
                    gender = Gender.FEMALE,
                    script = TextScript.CYRILLIC,
                    selectedCityId = "samarqand",
                    preferOfflineMp3 = true,
                    voiceSpeed = 1.15f,
                    darkRoomLuminanceThreshold = 34f,
                    seatedPrayerMode = true
                )
            )
            qazoRepo.setCount(QazoPrayerType.BOMDOD, 5)
            qazoRepo.setCount(QazoPrayerType.VITR, 2)
            historyRepo.restoreDailyProgress(
                listOf(
                    DailyPrayerProgressEntity(
                        dayKey = "2026-11-18",
                        bomdodCompleted = true,
                        peshinCompleted = true
                    )
                )
            )

            val exportedJson = backupManager.exportToJson(nowMillis = 1_763_456_000_000L)

            // Overwrite state with different values before restoring
            prefsRepo.restoreSettings(
                UserSettings(
                    onboardingCompleted = true,
                    gender = Gender.MALE,
                    script = TextScript.LATIN,
                    selectedCityId = "fargona",
                    seatedPrayerMode = false
                )
            )
            qazoRepo.restoreSnapshot(QazoPrayerEntity())
            db.dailyPrayerProgressDao().deleteDay("2026-11-18")

            val result = backupManager.importFromJson(exportedJson)
            assertTrue(result is BackupRestoreResult.Success)
            val success = result as BackupRestoreResult.Success
            assertEquals(1, success.daysRestored)
            assertEquals(7, success.qazoTotal)

            val restoredQazo = qazoRepo.getSnapshot()
            assertEquals(5, restoredQazo.bomdodCount)
            assertEquals(2, restoredQazo.vitrCount)

            val restoredDay = db.dailyPrayerProgressDao().getDay("2026-11-18")
            assertTrue(restoredDay?.isCompleted(PrayerType.BOMDOD) == true)
            assertTrue(restoredDay?.isCompleted(PrayerType.PESHIN) == true)

            val restoredPrefs = prefsRepo.settingsFlow.first()
            assertEquals(Gender.FEMALE, restoredPrefs.gender)
            assertEquals(TextScript.CYRILLIC, restoredPrefs.script)
            assertEquals("samarqand", restoredPrefs.selectedCityId)
            assertTrue(restoredPrefs.seatedPrayerMode)
        } finally {
            db.close()
        }
    }

    @Test
    fun backupManager_rejectsMalformedJsonGracefully() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val backupManager = SajdaBackupManager(
                UserPreferencesRepository(context),
                PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao()),
                QazoPrayerRepository(db.qazoPrayerDao())
            )
            val emptyResult = backupManager.importFromJson("   ")
            assertTrue(emptyResult is BackupRestoreResult.Failure)

            val invalidResult = backupManager.importFromJson("{\"appId\":\"other\",\"schemaVersion\":1}")
            assertTrue(invalidResult is BackupRestoreResult.Failure)
        } finally {
            db.close()
        }
    }

    @Test
    fun settingsScreen_showsBackupTtsAndOpensPrivacyPolicyDialog() {
        composeRule.setContent {
            SajdaAiTheme {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(),
                        ttsReadiness = TtsReadinessInfo(
                            isEngineReady = true,
                            hasUzbekLocale = false,
                            hasFallbackLocale = true
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

        composeRule.onNodeWithTag("settings_tts_settings_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_export_backup_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_import_backup_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_privacy_policy_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_privacy_policy_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_privacy_policy_close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_privacy_policy_dialog").assertDoesNotExist()
    }

    @Test
    fun permissionsScreen_showsBatteryOptimizationCardWhenRestricted() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val pm = app.getSystemService(PowerManager::class.java)
        shadowOf(pm).setIgnoringBatteryOptimizations(app.packageName, false)

        composeRule.setContent {
            SajdaAiTheme {
                PermissionsScreen(
                    script = TextScript.LATIN,
                    isFromSettings = true,
                    onContinue = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("permission_battery_button").performScrollTo().assertIsDisplayed()
    }
}
