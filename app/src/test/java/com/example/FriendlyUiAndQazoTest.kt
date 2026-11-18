package com.example

import android.Manifest
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QazoPrayerEntity
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.CoachMode
import com.example.domain.model.Gender
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.QazoPrayerType
import com.example.domain.model.WeeklyPrayerAnalytics
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyProgressScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.PrayerCoachUiState
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
class FriendlyUiAndQazoTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tracksForPrayer_matchesExpectedQuickTracksForEachTimeOfDay() {
        assertEquals(
            listOf(PrayerTrack.BOMDOD_SUNNAT, PrayerTrack.BOMDOD_FARZ),
            PrayerTrack.tracksForPrayer(PrayerType.BOMDOD)
        )
        assertEquals(
            listOf(PrayerTrack.PESHIN_SUNNAT_1, PrayerTrack.PESHIN_FARZ, PrayerTrack.PESHIN_SUNNAT_2),
            PrayerTrack.tracksForPrayer(PrayerType.PESHIN)
        )
        assertEquals(
            listOf(PrayerTrack.ASR_FARZ),
            PrayerTrack.tracksForPrayer(PrayerType.ASR)
        )
        assertEquals(
            listOf(PrayerTrack.SHOM_FARZ, PrayerTrack.SHOM_SUNNAT),
            PrayerTrack.tracksForPrayer(PrayerType.SHOM)
        )
        assertEquals(
            listOf(PrayerTrack.XUFTON_FARZ, PrayerTrack.XUFTON_SUNNAT, PrayerTrack.VITR_VOJIB),
            PrayerTrack.tracksForPrayer(PrayerType.XUFTON)
        )
    }

    @Test
    fun homeScreen_displaysDynamicQuickChipsForCurrentPrayerTime() {
        val city = UzbekistanCities.findById("toshkent")
        // 13:15 Tashkent -> Peshin time
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).apply {
            set(2026, Calendar.NOVEMBER, 18, 13, 15, 0)
        }
        val peshinSchedule = GetPrayerTimesUseCase().calculateForToday(
            city = city,
            completedToday = emptySet(),
            alarmMap = emptyMap(),
            nowMillis = cal.timeInMillis
        )
        assertEquals(PrayerType.PESHIN, peshinSchedule.currentPrayer)

        composeRule.setContent {
            SajdaAiTheme {
                HomeScreen(
                    uiState = MainUiState(
                        schedule = peshinSchedule,
                        quickTracks = PrayerTrack.tracksForPrayer(peshinSchedule.currentPrayer),
                        isReady = true
                    ),
                    onStartPrayer = {},
                    onToggleAlarm = {},
                    onToggleCompleted = {},
                    onCitySelected = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("track_chip_peshin_sunnat_1").assertIsDisplayed()
        composeRule.onNodeWithTag("track_chip_peshin_farz").assertIsDisplayed()
        composeRule.onNodeWithTag("track_chip_peshin_sunnat_2").assertIsDisplayed()
        composeRule.onNodeWithTag("track_chip_bomdod_sunnat").assertDoesNotExist()
    }

    @Test
    fun activePrayerScreen_hidesAngleDegreesAndShowsFriendlyPoseConfirmation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)

        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)
        val qiyomStepIndex = steps.indexOfFirst { it.expectedPose == PrayerPose.QIYOM }

        composeRule.setContent {
            SajdaAiTheme {
                ActivePrayerScreen(
                    uiState = PrayerCoachUiState(
                        track = PrayerTrack.BOMDOD_SUNNAT,
                        settings = UserSettings(seatedPrayerMode = true),
                        steps = steps,
                        currentStepIndex = qiyomStepIndex,
                        mode = CoachMode.CAMERA_AI,
                        poseSnapshot = PoseAnalysisSnapshot(
                            confirmedPose = PrayerPose.QIYOM,
                            torsoAngle = 168f,
                            kneeAngle = 172f,
                            isFullBodyVisible = true
                        )
                    ),
                    onPoseResult = {},
                    onTogglePause = {},
                    onNextStep = {},
                    onSwitchToAudioOnly = {},
                    onSwitchToCamera = {},
                    onExit = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("active_pose_status_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("QIYOM").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tasdiqlandi ✓").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("θt:", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("θk:", substring = true).assertDoesNotExist()
    }

    @Test
    fun settingsAndCalibrationScreens_toggleSeatedPrayerMode() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)

        var seatedEnabled by mutableStateOf(false)
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)

        composeRule.setContent {
            SajdaAiTheme {
                CalibrationScreen(
                    uiState = PrayerCoachUiState(
                        track = PrayerTrack.BOMDOD_SUNNAT,
                        settings = UserSettings(seatedPrayerMode = seatedEnabled),
                        steps = steps,
                        poseSnapshot = PoseAnalysisSnapshot(isFullBodyVisible = false)
                    ),
                    onPoseResult = {},
                    onSpeakCalibrationPrompt = {},
                    onStartActiveSession = { _, _ -> },
                    onSeatedModeToggle = { seatedEnabled = it },
                    onExit = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calibration_seated_mode_toggle").performClick()
        composeRule.waitForIdle()
        assertTrue(seatedEnabled)
    }

    @Test
    fun settingsScreen_displaysSeatedModeSwitchAndTogglesCallback() {
        var seatedEnabled by mutableStateOf(false)
        composeRule.setContent {
            SajdaAiTheme {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(seatedPrayerMode = seatedEnabled),
                        isReady = true
                    ),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onSeatedModeToggle = { seatedEnabled = it },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Stulda / O‘tirib o‘qish (Imo-ishora)").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_seated_mode_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(seatedEnabled)
    }

    @Test
    fun qazoPrayerDao_persistsIncrementDecrementAcrossSixPrayersAndClampsAtZero() = runTest {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SajdaDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val dao = db.qazoPrayerDao()

            dao.adjustCount(QazoPrayerType.BOMDOD, 2)
            dao.adjustCount(QazoPrayerType.PESHIN, 1)
            dao.adjustCount(QazoPrayerType.VITR, 3)
            dao.adjustCount(QazoPrayerType.VITR, -1)
            // Decrementing 0 stays clamped at 0
            dao.adjustCount(QazoPrayerType.ASR, -1)

            val entity = dao.observeQazo().first()!!
            assertEquals(2, entity.bomdodCount)
            assertEquals(1, entity.peshinCount)
            assertEquals(0, entity.asrCount)
            assertEquals(0, entity.shomCount)
            assertEquals(0, entity.xuftonCount)
            assertEquals(2, entity.vitrCount)
            assertEquals(5, entity.totalCount())
        } finally {
            db.close()
        }
    }

    @Test
    fun weeklyProgressScreen_displaysQazoLedgerWithSixPrayersAndAdjustsCounts() {
        var qazo by mutableStateOf(QazoPrayerEntity(bomdodCount = 3, vitrCount = 1))
        val summary = WeeklyPrayerAnalytics.buildSummary(emptyList(), weekOffset = 0)

        composeRule.setContent {
            SajdaAiTheme {
                WeeklyProgressScreen(
                    uiState = MainUiState(
                        weeklySummary = summary,
                        qazoProgress = qazo,
                        isReady = true
                    ),
                    onShiftWeek = {},
                    onTogglePrayerForDay = { _, _ -> },
                    onIncrementQazo = { type -> qazo = qazo.withDelta(type, 1) },
                    onDecrementQazo = { type -> qazo = qazo.withDelta(type, -1) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("qazo_ledger_card").assertIsDisplayed()
        composeRule.onNodeWithTag("qazo_total_badge").assertTextEquals("Jami: 4")
        composeRule.onNodeWithTag("qazo_count_bomdod").performScrollTo().assertTextEquals("3")
        composeRule.onNodeWithTag("qazo_count_vitr").performScrollTo().assertTextEquals("1")

        // Mark 1 Bomdod Qazo as prayed (-1) -> 2
        composeRule.onNodeWithTag("qazo_dec_bomdod").performScrollTo().performClick()
        // Add 1 Asr Qazo (+1) -> 1
        composeRule.onNodeWithTag("qazo_inc_asr").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("qazo_count_bomdod").performScrollTo().assertTextEquals("2")
        composeRule.onNodeWithTag("qazo_count_asr").performScrollTo().assertTextEquals("1")
        composeRule.onNodeWithTag("qazo_total_badge").performScrollTo().assertTextEquals("Jami: 4")
    }
}
