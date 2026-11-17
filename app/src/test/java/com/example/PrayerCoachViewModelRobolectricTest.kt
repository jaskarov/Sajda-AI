package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PrayerTrack
import com.example.ui.viewmodel.PrayerCoachViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerCoachViewModelRobolectricTest {

    private lateinit var viewModel: PrayerCoachViewModel

    @Before
    fun setUp() {
        viewModel = PrayerCoachViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun prepareTrack_initializesStepsCorrectly() = runTest {
        viewModel.prepareTrack(PrayerTrack.BOMDOD_FARZ)
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals(PrayerTrack.BOMDOD_FARZ, state.track)
        assertTrue("Bomdod Farz should have steps generated", state.steps.isNotEmpty())
        assertEquals(0, state.currentStepIndex)
        assertNotNull(state.currentStep)
    }

    @Test
    fun startActivePrayerSession_audioOnly_setsModeAndRemainingSec() = runTest {
        viewModel.prepareTrack(PrayerTrack.BOMDOD_SUNNAT)
        ShadowLooper.idleMainLooper()

        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.CAMERA_PERMISSION_DENIED
        )
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals(CoachMode.AUDIO_ONLY, state.mode)
        assertEquals(FallbackReason.CAMERA_PERMISSION_DENIED, state.fallbackReason)
        assertTrue(state.audioOnlyRemainingSec > 0)
    }

    @Test
    fun togglePause_pausesAndResumesSession() = runTest {
        viewModel.prepareTrack(PrayerTrack.BOMDOD_FARZ)
        ShadowLooper.idleMainLooper()

        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_FARZ,
            mode = CoachMode.CAMERA_AI,
            fallbackReason = FallbackReason.NONE
        )
        ShadowLooper.idleMainLooper()

        assertFalse(viewModel.uiState.value.isPaused)

        viewModel.togglePause()
        assertTrue(viewModel.uiState.value.isPaused)

        viewModel.togglePause()
        assertFalse(viewModel.uiState.value.isPaused)
    }

    @Test
    fun advanceToNextStep_navigatesToNextStep() = runTest {
        viewModel.prepareTrack(PrayerTrack.BOMDOD_SUNNAT)
        ShadowLooper.idleMainLooper()

        viewModel.startActivePrayerSession(
            track = PrayerTrack.BOMDOD_SUNNAT,
            mode = CoachMode.AUDIO_ONLY,
            fallbackReason = FallbackReason.NONE
        )
        ShadowLooper.idleMainLooper()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        viewModel.advanceToNextStep()
        ShadowLooper.idleMainLooper()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }
}
