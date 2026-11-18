package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerRepository
import com.example.data.local.SajdaBackupManager
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.Gender
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.QazoPrayerType
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.PrayerCoachUiState
import com.example.ui.viewmodel.PrayerCoachViewModel
import com.example.vision.ExtractedPoseLandmark
import com.example.vision.PoseFrameLandmarks
import com.example.vision.PrayerPoseAnalyzer
import com.google.mlkit.vision.pose.PoseLandmark
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhaseTwoRealLifeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun lm(
        type: Int,
        normX: Float,
        normY: Float,
        w: Int = 480,
        h: Int = 640
    ): ExtractedPoseLandmark = ExtractedPoseLandmark(
        type = type,
        x = normX * w,
        y = normY * h,
        z = 0f,
        normalizedX = normX,
        normalizedY = normY,
        inFrameLikelihood = 0.95f
    )

    @Test
    fun prayerPoseAnalyzer_detectsHijabOccludedEarsSalam_bothOppositeSides() {
        val analyzer = PrayerPoseAnalyzer(onPoseResult = {})

        // Seated in Qa'da wearing ro'mol: NO ears detected, only eyes, nose, shoulders, hips
        fun hijabSalamFrame(timestampMs: Long, noseX: Float, leftEyeX: Float, rightEyeX: Float) =
            PoseFrameLandmarks(
                landmarks = listOf(
                    lm(PoseLandmark.NOSE, noseX, 0.42f),
                    lm(PoseLandmark.LEFT_EYE, leftEyeX, 0.40f),
                    lm(PoseLandmark.RIGHT_EYE, rightEyeX, 0.40f),
                    lm(PoseLandmark.LEFT_SHOULDER, 0.40f, 0.52f),
                    lm(PoseLandmark.RIGHT_SHOULDER, 0.60f, 0.52f),
                    lm(PoseLandmark.LEFT_HIP, 0.43f, 0.72f),
                    lm(PoseLandmark.RIGHT_HIP, 0.57f, 0.72f)
                ),
                frameWidth = 480,
                frameHeight = 640,
                rotationDegrees = 0,
                timestampMs = timestampMs
            )

        // Centered looking forward -> QADA (headYawDirection == 0)
        val centered = analyzer.evaluateFrame(
            hijabSalamFrame(0L, noseX = 0.50f, leftEyeX = 0.46f, rightEyeX = 0.54f)
        )
        assertEquals(PrayerPose.QADA, centered.rawPose)
        assertEquals(0, centered.headYawDirection)

        // Right turn wearing headscarf -> SALAM (+1)
        val rightTurn1 = analyzer.evaluateFrame(
            hijabSalamFrame(500L, noseX = 0.56f, leftEyeX = 0.49f, rightEyeX = 0.575f)
        )
        assertEquals(PrayerPose.SALAM, rightTurn1.rawPose)
        assertEquals(1, rightTurn1.headYawDirection)

        // Left turn wearing headscarf -> SALAM (-1)
        val leftTurn = analyzer.evaluateFrame(
            hijabSalamFrame(1000L, noseX = 0.44f, leftEyeX = 0.425f, rightEyeX = 0.51f)
        )
        assertEquals(PrayerPose.SALAM, leftTurn.rawPose)
        assertEquals(-1, leftTurn.headYawDirection)
        assertNotEquals(rightTurn1.headYawDirection, leftTurn.headYawDirection)
    }

    @Test
    fun prayerPoseAnalyzer_detectsLooseDressQiyomAndRuku_whenKneesCovered() {
        val analyzer = PrayerPoseAnalyzer(onPoseResult = {})

        // Standing in long loose dress: knees & ankles veiled, only upper body visible
        val looseStanding = PoseFrameLandmarks(
            landmarks = listOf(
                lm(PoseLandmark.NOSE, 0.50f, 0.16f),
                lm(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.26f),
                lm(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.26f),
                lm(PoseLandmark.LEFT_HIP, 0.44f, 0.54f),
                lm(PoseLandmark.RIGHT_HIP, 0.56f, 0.54f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 0L
        )
        analyzer.evaluateFrame(looseStanding)
        val qiyomSnap = analyzer.evaluateFrame(looseStanding.copy(timestampMs = 450L))
        assertEquals(PrayerPose.QIYOM, qiyomSnap.confirmedPose)

        // Bent forward in Ruku while loose dress still hides knees
        val looseRuku = PoseFrameLandmarks(
            landmarks = listOf(
                lm(PoseLandmark.NOSE, 0.50f, 0.50f),
                lm(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.47f),
                lm(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.47f),
                lm(PoseLandmark.LEFT_HIP, 0.44f, 0.55f),
                lm(PoseLandmark.RIGHT_HIP, 0.56f, 0.55f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 950L
        )
        val rukuRaw = analyzer.evaluateFrame(looseRuku)
        assertEquals(PrayerPose.RUKU, rukuRaw.rawPose)
        val rukuConfirmed = analyzer.evaluateFrame(looseRuku.copy(timestampMs = 1400L))
        assertEquals(PrayerPose.RUKU, rukuConfirmed.confirmedPose)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prayerCoachViewModel_salamRequiresBothOppositeSidesBeforeAdvancing() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val viewModel = PrayerCoachViewModel(ApplicationProvider.getApplicationContext())
        try {
            viewModel.startActivePrayerSession(
                track = PrayerTrack.BOMDOD_SUNNAT,
                mode = CoachMode.CAMERA_AI,
                fallbackReason = FallbackReason.NONE
            )
            // Advance manually to final step (SALAM)
            val salamIdx = viewModel.uiState.value.steps.indexOfLast { it.expectedPose == PrayerPose.SALAM }
            repeat(salamIdx) { viewModel.advanceToNextStep() }
            assertEquals(PrayerPose.SALAM, viewModel.uiState.value.currentStep?.expectedPose)

            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            viewModel.completeStepAudioForTest()

            // Turn only to right side (+1) -> salamProgressStage becomes 1, stays on Salam
            viewModel.onPoseSnapshot(
                PoseAnalysisSnapshot(
                    confirmedPose = PrayerPose.SALAM,
                    rawPose = PrayerPose.SALAM,
                    isFullBodyVisible = true,
                    headYawDirection = 1
                )
            )
            assertFalse(viewModel.uiState.value.isCompleted)
            assertEquals(1, viewModel.uiState.value.salamProgressStage)

            // Turn to opposite side (-1) -> salamProgressStage becomes 2 & completes prayer
            viewModel.onPoseSnapshot(
                PoseAnalysisSnapshot(
                    confirmedPose = PrayerPose.SALAM,
                    rawPose = PrayerPose.SALAM,
                    isFullBodyVisible = true,
                    headYawDirection = -1
                )
            )
            assertTrue(viewModel.uiState.value.isCompleted)
        } finally {
            viewModel.cleanup()
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prayerCoachViewModel_qazoCompletionLogsHistoryWithoutMarkingTodayFard() = runBlocking {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = SajdaDatabase.getInstance(app)
        val dayKey = PrayerHistoryRepository.formatDayKey()
        db.dailyPrayerProgressDao().setPrayerCompleted(dayKey, PrayerType.XUFTON, false)
        db.qazoPrayerDao().setCount(QazoPrayerType.XUFTON, 3)

        val viewModel = PrayerCoachViewModel(app)
        try {
            viewModel.startActivePrayerSession(
                track = PrayerTrack.XUFTON_FARZ,
                mode = CoachMode.AUDIO_ONLY,
                qazoTypeId = QazoPrayerType.XUFTON.id
            )
            val totalSteps = viewModel.uiState.value.steps.size
            repeat(totalSteps) { viewModel.advanceToNextStep() }
            delay(250L)

            assertTrue(viewModel.uiState.value.isCompleted)
            assertEquals(2, db.qazoPrayerDao().getQazo()?.xuftonCount)
            val todayProgress = db.dailyPrayerProgressDao().observeDay(dayKey).first()
            assertFalse(
                "Qazo Xufton must NOT falsely check off today's regular Xufton prayer",
                todayProgress?.xuftonCompleted ?: false
            )
        } finally {
            viewModel.cleanup()
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prayerCoachViewModel_extendContinuationCountdownAddsTenSeconds() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val viewModel = PrayerCoachViewModel(ApplicationProvider.getApplicationContext())
        try {
            viewModel.startActivePrayerSession(
                track = PrayerTrack.BOMDOD_SUNNAT,
                mode = CoachMode.AUDIO_ONLY
            )
            val totalSteps = viewModel.uiState.value.steps.size
            repeat(totalSteps) { viewModel.advanceToNextStep() }
            assertEquals(5, viewModel.uiState.value.nextTrackCountdownSec)

            viewModel.extendContinuationCountdown(10)
            assertEquals(15, viewModel.uiState.value.nextTrackCountdownSec)
            assertEquals(15, viewModel.uiState.value.nextTrackCountdownTotalSec)
        } finally {
            viewModel.cleanup()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun activePrayerScreen_skipsVolumeBoostInDarkRoomAndShowsQazoBadge() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val quietDawnVol = (maxVol * 0.25f).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, quietDawnVol, 0)

        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_FARZ, Gender.MALE)
        val state = PrayerCoachUiState(
            track = PrayerTrack.BOMDOD_FARZ,
            steps = steps,
            currentStepIndex = 0,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.LOW_LIGHT_DARK_ROOM,
            qazoTypeId = QazoPrayerType.BOMDOD.id
        )

        composeRule.setContent {
            SajdaAiTheme {
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
        composeRule.waitForIdle()

        assertEquals(quietDawnVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        composeRule.onNodeWithTag("active_qazo_badge").assertIsDisplayed()
    }

    @Test
    fun calibrationScreen_showsQazoBadgeWhenPrayingQazo() {
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.ASR_FARZ, Gender.MALE)
        composeRule.setContent {
            SajdaAiTheme {
                CalibrationScreen(
                    uiState = PrayerCoachUiState(
                        track = PrayerTrack.ASR_FARZ,
                        steps = steps,
                        qazoTypeId = QazoPrayerType.ASR.id
                    ),
                    onPoseResult = {},
                    onSpeakCalibrationPrompt = {},
                    onStartActiveSession = { _, _ -> },
                    onExit = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calibration_qazo_badge").assertIsDisplayed()
    }

    @Test
    fun learnScreen_tahoratAutoplayButtonTriggersSequentialCallback() {
        var autoplayTriggered = false
        composeRule.setContent {
            SajdaAiTheme {
                LearnScreen(
                    uiState = MainUiState(isReady = true),
                    onPlayAudio = { _, _ -> },
                    onStartTrack = {},
                    initialTab = 1,
                    onToggleTahoratAutoPlay = { items ->
                        autoplayTriggered = items.size == 8
                    }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tahorat_autoplay_button").performClick()
        composeRule.waitForIdle()
        assertTrue(autoplayTriggered)
    }

    @Test
    fun settingsScreen_andBackupManager_roundTripAutoVolumeAndAcousticChime() {
        var autoBoost by mutableStateOf(true)
        var chimeEnabled by mutableStateOf(true)

        composeRule.setContent {
            SajdaAiTheme {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(
                            autoBoostPrayerVolume = autoBoost,
                            acousticChimeEnabled = chimeEnabled
                        ),
                        isReady = true
                    ),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onAutoBoostVolumeToggle = { autoBoost = it },
                    onAcousticChimeToggle = { chimeEnabled = it },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_auto_volume_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_acoustic_chime_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(autoBoost)
        assertFalse(chimeEnabled)

        runBlocking(Dispatchers.IO) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val prefs = UserPreferencesRepository(app)
            val db = SajdaDatabase.getInstance(app)
            val manager = SajdaBackupManager(
                prefsRepo = prefs,
                historyRepo = PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao()),
                qazoRepo = QazoPrayerRepository(db.qazoPrayerDao())
            )
            prefs.setAutoBoostPrayerVolume(false)
            prefs.setAcousticChimeEnabled(false)
            val exported = manager.exportToJson()
            prefs.setAutoBoostPrayerVolume(true)
            prefs.setAcousticChimeEnabled(true)
            manager.importFromJson(exported)
            val restored = prefs.settingsFlow.first()
            assertFalse(restored.autoBoostPrayerVolume)
            assertFalse(restored.acousticChimeEnabled)
        }
    }
}
