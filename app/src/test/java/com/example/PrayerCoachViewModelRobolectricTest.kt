package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.Gender
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.ui.viewmodel.PrayerCoachViewModel
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerCoachViewModelRobolectricTest {

    private lateinit var viewModel: PrayerCoachViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PrayerCoachViewModel(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        viewModel.cleanup()
        Dispatchers.resetMain()
    }

    @Test
    fun prepareTrack_initializesStepsCorrectly() {
        viewModel.prepareTrack(PrayerTrack.BOMDOD_FARZ)
        val state = viewModel.uiState.value

        assertEquals(PrayerTrack.BOMDOD_FARZ, state.track)
        assertTrue("Bomdod Farz should have steps generated", state.steps.isNotEmpty())
        assertEquals(0, state.currentStepIndex)
        assertNotNull(state.currentStep)
    }

    @Test
    fun startActivePrayerSession_audioOnly_setsModeAndRemainingSec() {
        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.CAMERA_PERMISSION_DENIED
        )
        val state = viewModel.uiState.value

        assertEquals(CoachMode.AUDIO_ONLY, state.mode)
        assertEquals(FallbackReason.CAMERA_PERMISSION_DENIED, state.fallbackReason)
        assertTrue(state.audioOnlyRemainingSec >= 0)
    }

    @Test
    fun togglePause_pausesAndResumesSession() {
        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_FARZ,
            mode = CoachMode.CAMERA_AI,
            fallbackReason = FallbackReason.NONE
        )
        val state = viewModel.uiState.value
        assertEquals(PrayerTrack.BOMDOD_FARZ, state.track)
        assertFalse(state.isPaused)

        viewModel.togglePause()
        assertTrue(viewModel.uiState.value.isPaused)

        viewModel.togglePause()
        assertFalse(viewModel.uiState.value.isPaused)
    }

    @Test
    fun advanceToNextStep_navigatesToNextStep() {
        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.NONE
        )
        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        viewModel.advanceToNextStep()
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun completingBomdodSunnat_offersBomdodFarzContinuationAndStartsIt() {
        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.NONE
        )
        val totalSteps = viewModel.uiState.value.steps.size
        repeat(totalSteps) {
            viewModel.advanceToNextStep()
        }

        val completedState = viewModel.uiState.value
        assertTrue(completedState.isCompleted)
        assertEquals(PrayerTrack.BOMDOD_FARZ, completedState.pendingNextTrack)
        assertTrue(completedState.nextTrackCountdownSec in 1..5)

        viewModel.startNextContinuationTrack()
        val farzState = viewModel.uiState.value
        assertFalse(farzState.isCompleted)
        assertEquals(PrayerTrack.BOMDOD_FARZ, farzState.track)
        assertEquals(0, farzState.currentStepIndex)
        assertEquals(null, farzState.pendingNextTrack)
    }

    @Test
    fun cameraAi_requiresBothAudioDoneAndPoseMatched_andTriggersHapticPulse() {
        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.CAMERA_AI,
            fallbackReason = FallbackReason.NONE
        )
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(0, viewModel.uiState.value.hapticFeedbackPulse)

        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

        // 1. Pose matches TAKBIR, but audio is NOT finished yet -> must NOT advance
        viewModel.onPoseSnapshot(
            PoseAnalysisSnapshot(
                confirmedPose = PrayerPose.TAKBIR,
                rawPose = PrayerPose.TAKBIR,
                isFullBodyVisible = true
            )
        )
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(0, viewModel.uiState.value.hapticFeedbackPulse)

        // 2. Step completes audio while mismatched pose (QIYOM instead of TAKBIR) -> must NOT advance
        viewModel.onPoseSnapshot(
            PoseAnalysisSnapshot(
                confirmedPose = PrayerPose.QIYOM,
                rawPose = PrayerPose.QIYOM,
                isFullBodyVisible = true
            )
        )
        viewModel.completeStepAudioForTest()
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.stepAudioDone)

        // 3. Now skeleton confirms TAKBIR while audio is done -> advances & increments haptic pulse
        viewModel.onPoseSnapshot(
            PoseAnalysisSnapshot(
                confirmedPose = PrayerPose.TAKBIR,
                rawPose = PrayerPose.TAKBIR,
                isFullBodyVisible = true
            )
        )
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertEquals(1, viewModel.uiState.value.hapticFeedbackPulse)
    }

    @Test
    fun sequenceEngine_prependsTakbirAudioOnRakat2AndQada() {
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_FARZ, Gender.MALE)
        val rakat2Fotiha = steps.first { it.rakatNumber == 2 && it.recitationTitle.latin.contains("FOTIHA") }
        assertTrue(rakat2Fotiha.audioAssetId.startsWith("takbir,"))
        assertTrue(rakat2Fotiha.ttsPrompt.contains("Allohu akbar"))

        val qadaAttahiyat = steps.first { it.expectedPose == PrayerPose.QADA && it.recitationTitle.latin.contains("ATTAHIYAT") }
        assertTrue(qadaAttahiyat.audioAssetId.startsWith("takbir,"))
    }
}
