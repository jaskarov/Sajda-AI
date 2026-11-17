package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PrayerTrack
import com.example.ui.viewmodel.PrayerCoachViewModel
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
}
