package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.Gender
import com.example.domain.model.PrayerTrack
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.PrayerCoachUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RakatProgressIndicatorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasStateDescription(expected: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)

    @Test
    fun activePrayerScreen_showsCompletedCurrentNextAndUpcomingRakatUnits() {
        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.PESHIN_FARZ, Gender.MALE)
        val rakat2FirstIndex = steps.indexOfFirst { it.rakatNumber == 2 }
        var stepIndex by mutableIntStateOf(0)

        composeRule.setContent {
            SajdaAiTheme {
                ActivePrayerScreen(
                    uiState = PrayerCoachUiState(
                        track = PrayerTrack.PESHIN_FARZ,
                        steps = steps,
                        currentStepIndex = stepIndex,
                        mode = CoachMode.AUDIO_ONLY
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

        composeRule.onNodeWithTag("rakat_progress_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("rakat_current_summary").assertTextEquals("1-rakat bajarilmoqda")
        composeRule.onNodeWithTag("rakat_next_summary").assertTextEquals("Keyingi: 2-rakat")

        composeRule.onNodeWithTag("rakat_unit_1").assert(hasStateDescription("Hozir"))
        composeRule.onNodeWithTag("rakat_unit_2").assert(hasStateDescription("Keyingi"))
        composeRule.onNodeWithTag("rakat_unit_3").assert(hasStateDescription("Navbatda"))
        composeRule.onNodeWithTag("rakat_unit_4").assert(hasStateDescription("Navbatda"))

        // Advance to Rakat 2
        stepIndex = rakat2FirstIndex
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rakat_current_summary").assertTextEquals("2-rakat bajarilmoqda")
        composeRule.onNodeWithTag("rakat_next_summary").assertTextEquals("Keyingi: 3-rakat")

        composeRule.onNodeWithTag("rakat_unit_1").assert(hasStateDescription("Tayyor"))
        composeRule.onNodeWithTag("rakat_unit_2").assert(hasStateDescription("Hozir"))
        composeRule.onNodeWithTag("rakat_unit_3").assert(hasStateDescription("Keyingi"))
        composeRule.onNodeWithTag("rakat_unit_4").assert(hasStateDescription("Navbatda"))
    }

    @Test
    fun activePrayerScreen_finalRakatShowsNextTrackOrFinalLabel() {
        val sunnatSteps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)
        val sunnatLastRakatIndex = sunnatSteps.indexOfFirst { it.rakatNumber == 2 }

        val asrSteps = PrayerSequenceEngine().buildSteps(PrayerTrack.ASR_FARZ, Gender.MALE)
        val asrLastRakatIndex = asrSteps.indexOfFirst { it.rakatNumber == 4 }

        var useContinuationTrack by androidx.compose.runtime.mutableStateOf(true)

        composeRule.setContent {
            SajdaAiTheme {
                val track = if (useContinuationTrack) PrayerTrack.BOMDOD_SUNNAT else PrayerTrack.ASR_FARZ
                val steps = if (useContinuationTrack) sunnatSteps else asrSteps
                val index = if (useContinuationTrack) sunnatLastRakatIndex else asrLastRakatIndex
                ActivePrayerScreen(
                    uiState = PrayerCoachUiState(
                        track = track,
                        steps = steps,
                        currentStepIndex = index,
                        mode = CoachMode.AUDIO_ONLY
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

        composeRule.onNodeWithTag("rakat_current_summary").assertTextEquals("2-rakat bajarilmoqda")
        composeRule.onNodeWithTag("rakat_next_summary").assertTextEquals("Keyingi: Farz")

        useContinuationTrack = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rakat_current_summary").assertTextEquals("4-rakat bajarilmoqda")
        composeRule.onNodeWithTag("rakat_next_summary").assertTextEquals("Yakuniy rakat")
    }
}
