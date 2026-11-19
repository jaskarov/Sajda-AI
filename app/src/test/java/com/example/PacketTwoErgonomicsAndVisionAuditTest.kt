package com.example

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.preferences.UserSettings
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.Gender
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.domain.model.TextScript
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.PrayerGuideOnboardingScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.PrayerCoachUiState
import com.example.ui.viewmodel.PrayerCoachViewModel
import com.example.vision.ExtractedPoseLandmark
import com.example.vision.PoseFrameLandmarks
import com.example.vision.PrayerPoseAnalyzer
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PacketTwoErgonomicsAndVisionAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        runCatching {
            WorkManagerTestInitHelper.initializeTestWorkManager(
                app,
                Configuration.Builder().build()
            )
        }
    }

    @Test
    fun prayerPoseAnalyzer_calibratesReady_withHeadAndTorsoOnly_whenLegsCoveredByLongDress() {
        val analyzer = PrayerPoseAnalyzer(
            isFrontCamera = true,
            seatedMode = false,
            onPoseResult = {}
        )
        // Only head + shoulders + hips visible (knees & ankles hidden by wide namoz libosi or tight 2.5m room)
        val landmarks = PoseFrameLandmarks(
            landmarks = listOf(
                ExtractedPoseLandmark(PoseLandmark.NOSE, 240f, 110f, 0f, 0.50f, 0.17f, 0.95f),
                ExtractedPoseLandmark(PoseLandmark.LEFT_SHOULDER, 200f, 190f, 0f, 0.42f, 0.30f, 0.94f),
                ExtractedPoseLandmark(PoseLandmark.RIGHT_SHOULDER, 280f, 190f, 0f, 0.58f, 0.30f, 0.94f),
                ExtractedPoseLandmark(PoseLandmark.LEFT_HIP, 210f, 360f, 0f, 0.44f, 0.56f, 0.91f),
                ExtractedPoseLandmark(PoseLandmark.RIGHT_HIP, 270f, 360f, 0f, 0.56f, 0.56f, 0.91f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 1000L
        )

        val snapshot = analyzer.evaluateFrame(landmarks, luminance = 135f, isRoomTooDark = false)
        assertTrue(
            "Calibration must turn ready with head+torso even when ankles/knees are hidden",
            snapshot.isFullBodyVisible
        )
        assertTrue(
            "Calibration hint should confirm ready state",
            snapshot.calibrationHint.latin.contains("Joyingiz qulay", ignoreCase = true)
        )
        analyzer.close()
    }

    @Test
    fun prayerCoachViewModel_autoRescuesStuckSajdaInCameraAiMode_andAdvancesSafely() = runBlocking {
        val vm = PrayerCoachViewModel(app)
        vm.stuckRescueGraceMs = 40L
        vm.stuckRescueCountdownSeconds = 2
        vm.stuckRescueTickMs = 30L

        vm.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.CAMERA_AI,
            initialSettings = UserSettings(
                gender = Gender.FEMALE,
                acousticChimeEnabled = false
            )
        )
        // Advance to a Sajda step
        val sajdaIndex = vm.uiState.value.steps.indexOfFirst { it.expectedPose == PrayerPose.SAJDA }
        assertTrue("Expected a SAJDA step in Bomdod Sunnat", sajdaIndex > 0)
        repeat(sajdaIndex) { vm.advanceToNextStep() }
        assertEquals(sajdaIndex, vm.uiState.value.currentStepIndex)

        // Pose stays UNKNOWN (occluded by loose dress / child crossing frame), audio finishes
        vm.completeStepAudioForTest()

        // Advance Robolectric MainLooper clock across grace (40ms) + 2 countdown ticks (60ms)
        repeat(8) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(30))
            if (vm.uiState.value.currentStepIndex > sajdaIndex) return@repeat
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            "Stuck-pose rescue should automatically advance past occluded Sajda step",
            sajdaIndex + 1,
            vm.uiState.value.currentStepIndex
        )
        assertEquals(
            "Consecutive stuck rescues should increment",
            1,
            vm.uiState.value.consecutiveStuckRescues
        )
        assertEquals(0, vm.uiState.value.stuckPoseRescueCountdownSec)
        vm.cleanup()
    }

    @Test
    fun prayerCoachViewModel_cancelsStuckRescue_whenPaused() = runBlocking {
        val vm = PrayerCoachViewModel(app)
        vm.stuckRescueGraceMs = 40L
        vm.stuckRescueCountdownSeconds = 3
        vm.stuckRescueTickMs = 40L

        vm.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.CAMERA_AI,
            initialSettings = UserSettings(acousticChimeEnabled = false)
        )
        val startIdx = vm.uiState.value.currentStepIndex
        vm.completeStepAudioForTest()
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(55))

        // Pause mid-rescue
        vm.togglePause()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, vm.uiState.value.stuckPoseRescueCountdownSec)

        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
        assertEquals("Paused session must not auto-advance via rescue", startIdx, vm.uiState.value.currentStepIndex)
        vm.cleanup()
    }

    @Test
    fun prayerCoachViewModel_toggleCameraFacing_flipsAndPersistsToSession() {
        val vm = PrayerCoachViewModel(app)
        assertTrue(vm.uiState.value.useFrontCamera)

        vm.toggleCameraFacing()
        assertFalse(vm.uiState.value.useFrontCamera)

        vm.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.CAMERA_AI,
            initialSettings = UserSettings(),
            useFrontCamera = false
        )
        assertFalse(vm.uiState.value.useFrontCamera)

        vm.toggleCameraFacing()
        assertTrue(vm.uiState.value.useFrontCamera)
        vm.cleanup()
    }

    @Test
    fun calibrationScreen_showsPrivacyAssuranceCardBeforePermission_andAllowsAudioOnlyFallback() {
        shadowOf(app).denyPermissions(Manifest.permission.CAMERA)
        var startedMode: CoachMode? = null
        var startedReason: FallbackReason? = null

        composeRule.setContent {
            SajdaAiTheme {
                CompositionLocalProvider(LocalRuntimePermissions provides RuntimePermissionsState(cameraGranted = false)) {
                    CalibrationScreen(
                        uiState = PrayerCoachUiState(
                            track = PrayerTrack.BOMDOD_SUNNAT,
                            settings = UserSettings(script = TextScript.LATIN)
                        ),
                        onPoseResult = {},
                        onSpeakCalibrationPrompt = {},
                        onStartActiveSession = { mode, reason ->
                            startedMode = mode
                            startedReason = reason
                        },
                        onExit = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("calibration_privacy_card").assertIsDisplayed()
        composeRule.onNodeWithTag("calibration_grant_camera_button").assertIsDisplayed()
        composeRule.onNodeWithTag("calibration_privacy_audio_only_button").assertIsDisplayed()
        composeRule.onNodeWithText("100% Maxfiy va Oflayn AI Kamera").assertIsDisplayed()

        composeRule.onNodeWithTag("calibration_privacy_audio_only_button").performClick()
        composeRule.mainClock.advanceTimeBy(150L)
        composeRule.waitForIdle()

        assertEquals(CoachMode.AUDIO_ONLY, startedMode)
        assertEquals(FallbackReason.USER_SELECTED, startedReason)
    }

    @Test
    fun calibrationScreen_exposesRearCameraFlipAndTightRoomForceStartOverride() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        var flipClicked = false
        var startedMode: CoachMode? = null

        composeRule.setContent {
            SajdaAiTheme {
                CompositionLocalProvider(LocalRuntimePermissions provides RuntimePermissionsState(cameraGranted = true)) {
                    CalibrationScreen(
                        uiState = PrayerCoachUiState(
                            track = PrayerTrack.BOMDOD_SUNNAT,
                            poseSnapshot = PoseAnalysisSnapshot(isFullBodyVisible = false),
                            useFrontCamera = true
                        ),
                        onPoseResult = {},
                        onSpeakCalibrationPrompt = {},
                        onStartActiveSession = { mode, _ -> startedMode = mode },
                        onToggleCameraFacing = { flipClicked = true },
                        onExit = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("calibration_flip_camera_button").assertIsDisplayed().performClick()
        assertTrue("Rear camera toggle callback must fire", flipClicked)

        composeRule.onNodeWithTag("calibration_force_start_button").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(150L)
        composeRule.waitForIdle()
        assertEquals("Force-start override must launch Camera AI session", CoachMode.CAMERA_AI, startedMode)
    }

    @Test
    fun activePrayerScreen_rendersFullWidthTeleprompterAndStuckPoseRescueBanner() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)
        var flipClicked = false

        composeRule.setContent {
            SajdaAiTheme {
                CompositionLocalProvider(LocalRuntimePermissions provides RuntimePermissionsState(cameraGranted = true)) {
                    ActivePrayerScreen(
                        uiState = PrayerCoachUiState(
                            track = PrayerTrack.BOMDOD_SUNNAT,
                            settings = UserSettings(
                                script = TextScript.LATIN,
                                largeTextMode = false,
                                seatedPrayerMode = false
                            ),
                            steps = steps,
                            currentStepIndex = 1,
                            mode = CoachMode.CAMERA_AI,
                            stuckPoseRescueCountdownSec = 4,
                            consecutiveStuckRescues = 1
                        ),
                        onPoseResult = {},
                        onTogglePause = {},
                        onNextStep = {},
                        onSwitchToAudioOnly = {},
                        onSwitchToCamera = {},
                        onToggleCameraFacing = { flipClicked = true },
                        onExit = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("stuck_pose_rescue_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("active_teleprompter_card").assertIsDisplayed()
        composeRule.onNodeWithTag("active_flip_camera_button").assertIsDisplayed().performClick()
        assertTrue(flipClicked)

        // Toggle into 2-Meter Teleprompter (Katta matn) mode
        composeRule.onNodeWithTag("active_teleprompter_toggle").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2m Katta matn").assertIsDisplayed()
        composeRule.onNodeWithTag("active_pose_status_chip").assertIsDisplayed()
    }

    @Test
    fun prayerGuideOnboardingScreen_exposesProminentAlphabetSelectorOnScreenOne() {
        var latestScript = TextScript.LATIN

        composeRule.setContent {
            SajdaAiTheme {
                PrayerGuideOnboardingScreen(
                    initialScript = TextScript.LATIN,
                    isFirstLaunch = true,
                    onPreferencesUpdated = { _, script, _ -> latestScript = script },
                    onFinish = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("guide_script_selector_bar").assertIsDisplayed()
        composeRule.onNodeWithText("O‘zbekcha (Lotin)").assertIsDisplayed()
        composeRule.onNodeWithText("Ўзбекча (Кирилл)").assertIsDisplayed()

        // Tap prominent Cyrillic option
        composeRule.onNodeWithText("Ўзбекча (Кирилл)").performClick()
        composeRule.waitForIdle()

        assertEquals(TextScript.CYRILLIC, latestScript)
        composeRule.onNodeWithText("НАМОЗ ТАРТИБИ • 1 / 6").assertIsDisplayed()
    }

    @Test
    fun mainActivity_doesNotAmbushUserWithPermissionsOnLaunch() {
        val shadowApp = shadowOf(app)
        shadowApp.denyPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadowActivity = shadowOf(activity)
                assertNull(
                    "MainActivity must NOT immediately launch system permission dialog in onCreate()",
                    shadowActivity.lastRequestedPermission
                )
            }
        }
    }
}
