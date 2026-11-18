package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.audio.PrayerAudioCoach
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.Gender
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerTrack
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.PrayerCoachUiState
import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalibrationAndVolumeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun boostPrayerStreamVolume_raisesLowVolumeTo85PercentAndRestoresOriginal() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val lowVol = (maxVol * 0.30f).toInt().coerceAtLeast(1)
        val expected85 = ceil(maxVol * 0.85f).toInt().coerceIn(0, maxVol)

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lowVol, 0)
        val saved = PrayerAudioCoach.boostPrayerStreamVolume(audioManager, 0.85f)
        assertEquals(lowVol, saved)
        assertEquals(expected85, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        // Already >= 85% should not return a downgrade saved value
        val noChange = PrayerAudioCoach.boostPrayerStreamVolume(audioManager, 0.85f)
        assertNull(noChange)

        PrayerAudioCoach.restorePrayerStreamVolume(audioManager, saved)
        assertEquals(lowVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun activePrayerScreen_boostsStreamMusicOnEnterAndRestoresOnDispose() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val originalVol = (maxVol * 0.35f).toInt().coerceAtLeast(1)
        val expectedBoostedVol = ceil(maxVol * 0.85f).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVol, 0)

        var showScreen by mutableStateOf(true)
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)
        val state = PrayerCoachUiState(
            track = PrayerTrack.BOMDOD_SUNNAT,
            steps = steps,
            currentStepIndex = 0,
            mode = CoachMode.AUDIO_ONLY
        )

        composeRule.setContent {
            SajdaAiTheme {
                if (showScreen) {
                    ActivePrayerScreen(
                        uiState = state,
                        onPoseResult = {},
                        onTogglePause = {},
                        onNextStep = {},
                        onSwitchToAudioOnly = {},
                        onSwitchToCamera = {},
                        onExit = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(expectedBoostedVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        showScreen = false
        composeRule.waitForIdle()
        assertEquals(originalVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun calibrationScreen_displaysPlacementGuideAndActivatesStartWhenBodyVisible() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)

        var fullBodyVisible by mutableStateOf(false)
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)

        composeRule.setContent {
            SajdaAiTheme {
                CalibrationScreen(
                    uiState = PrayerCoachUiState(
                        track = PrayerTrack.BOMDOD_SUNNAT,
                        steps = steps,
                        poseSnapshot = PoseAnalysisSnapshot(isFullBodyVisible = fullBodyVisible)
                    ),
                    onPoseResult = {},
                    onSpeakCalibrationPrompt = {},
                    onStartActiveSession = { _, _ -> },
                    onExit = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calibration_placement_guide_card").assertIsDisplayed()
        composeRule.onNodeWithText("Joynamozdan 2 qadam (1.8 – 2.2 metr) masofaga qo‘ying")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Telefonni kitob yoki devorga biroz qiya (70°–80°) suyab qo‘ying")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("calibration_start_now_button").assertIsNotEnabled()

        fullBodyVisible = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calibration_start_now_button").assertIsEnabled()
    }
}
