package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QazoPrayerEntity
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserSettings
import com.example.domain.model.CoachMode
import com.example.domain.model.Gender
import com.example.domain.model.PrayerTrack
import com.example.domain.model.QazoPrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.WeeklyPrayerAnalytics
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyProgressScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.PrayerCoachViewModel
import com.example.vision.PrayerPoseAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhaseOneCoachTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun qazoPrayerDao_addBulkForYears_populatesAllSixTypesAndMapsToTracks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, SajdaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.qazoPrayerDao()
            dao.addBulkForYears(2)

            val saved = dao.getQazo()!!
            assertEquals(730, saved.bomdodCount)
            assertEquals(730, saved.peshinCount)
            assertEquals(730, saved.asrCount)
            assertEquals(730, saved.shomCount)
            assertEquals(730, saved.xuftonCount)
            assertEquals(730, saved.vitrCount)
            assertEquals(730 * 6, saved.totalCount())

            assertEquals(PrayerTrack.BOMDOD_FARZ, QazoPrayerType.BOMDOD.toPrayerTrack())
            assertEquals(PrayerTrack.PESHIN_FARZ, QazoPrayerType.PESHIN.toPrayerTrack())
            assertEquals(PrayerTrack.ASR_FARZ, QazoPrayerType.ASR.toPrayerTrack())
            assertEquals(PrayerTrack.SHOM_FARZ, QazoPrayerType.SHOM.toPrayerTrack())
            assertEquals(PrayerTrack.XUFTON_FARZ, QazoPrayerType.XUFTON.toPrayerTrack())
            assertEquals(PrayerTrack.VITR_VOJIB, QazoPrayerType.VITR.toPrayerTrack())
            assertEquals(QazoPrayerType.XUFTON, QazoPrayerType.fromId("xufton"))
        } finally {
            db.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prayerCoachViewModel_qazoSessionDoesNotChainVitrAndDecrementsLedger() = runBlocking {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = SajdaDatabase.getInstance(app)
        val qazoDao = db.qazoPrayerDao()
        qazoDao.setCount(QazoPrayerType.XUFTON, 5)

        val viewModel = PrayerCoachViewModel(app)
        try {
            viewModel.startActivePrayerSession(
                track = PrayerTrack.XUFTON_FARZ,
                mode = CoachMode.AUDIO_ONLY,
                qazoTypeId = QazoPrayerType.XUFTON.id
            )

            val totalSteps = viewModel.uiState.value.steps.size
            repeat(totalSteps) {
                viewModel.advanceToNextStep()
            }

            // Wait briefly for IO coroutine decrement in Room
            delay(250L)

            val state = viewModel.uiState.value
            assertTrue("Expected prayer session to complete", state.isCompleted)
            assertNull(
                "Qazo Xufton Farz must NOT auto-chain into Vitr Vojib",
                state.pendingNextTrack
            )
            assertTrue(state.qazoDecrementedOnCompletion)

            val afterCount = qazoDao.observeQazo().first()?.xuftonCount ?: -1
            assertEquals(4, afterCount)
        } finally {
            viewModel.cleanup()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun weeklyProgressScreen_qazoCalculatorAndDirectPrayButtonWork() {
        var addedYears = 0
        var prayedQazo: QazoPrayerType? = null
        val summary = WeeklyPrayerAnalytics.buildSummary(emptyList(), weekOffset = 0)

        composeRule.setContent {
            SajdaAiTheme {
                WeeklyProgressScreen(
                    uiState = MainUiState(
                        weeklySummary = summary,
                        qazoProgress = QazoPrayerEntity(bomdodCount = 10, peshinCount = 5),
                        isReady = true
                    ),
                    onShiftWeek = {},
                    onTogglePrayerForDay = { _, _ -> },
                    onAddQazoYears = { addedYears = it },
                    onPrayQazo = { prayedQazo = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("qazo_plan_summary").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("qazo_calculator_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("qazo_calculator_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("qazo_calc_chip_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("qazo_calculator_apply_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(3, addedYears)

        composeRule.onNodeWithTag("qazo_pray_bomdod").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(QazoPrayerType.BOMDOD, prayedQazo)
    }

    @Test
    fun learnScreen_displaysTahoratTabAndPlaysStepAudio() {
        var playedPrompt = ""
        composeRule.setContent {
            SajdaAiTheme {
                LearnScreen(
                    uiState = MainUiState(isReady = true),
                    onPlayAudio = { prompt, _ -> playedPrompt = prompt },
                    onStartTrack = {},
                    initialTab = 1
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("learn_tab_1").assertIsDisplayed()
        composeRule.onNodeWithTag("wudu_farz_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("learn_tahorat_list")
            .performScrollToNode(hasTestTag("listen_wudu_wudu_step_4"))
        composeRule.onNodeWithTag("listen_wudu_wudu_step_4").performClick()
        composeRule.waitForIdle()
        assertTrue(playedPrompt.contains("Birinchi farz", ignoreCase = true))

        composeRule.onNodeWithTag("learn_tahorat_list")
            .performScrollToNode(hasTestTag("wudu_breakers_card"))
        composeRule.onNodeWithTag("wudu_breakers_card").assertIsDisplayed()
    }

    @Test
    fun onboardingAndSettings_supportSeniorLargeTextAndLowPowerCamera() {
        var selectedSeated = false
        var selectedLarge = false
        var completed = false

        composeRule.setContent {
            SajdaAiTheme {
                OnboardingScreen(
                    initialGender = Gender.MALE,
                    initialScript = TextScript.LATIN,
                    initialCityId = "toshkent",
                    onAccessibilitySelected = { seated, large ->
                        selectedSeated = seated
                        selectedLarge = large
                    },
                    onComplete = { _, _, _ -> completed = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("onboarding_mode_senior").performScrollTo().performClick()
        composeRule.onNodeWithTag("onboarding_continue_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(completed)
        assertTrue(selectedSeated)
        assertTrue(selectedLarge)
    }

    @Test
    fun settingsScreen_togglesLargeTextAndLowPowerCameraSwitches() {
        var largeText by mutableStateOf(false)
        var lowPower by mutableStateOf(false)

        composeRule.setContent {
            SajdaAiTheme(largeTextMode = largeText) {
                SettingsScreen(
                    uiState = MainUiState(
                        settings = UserSettings(
                            largeTextMode = largeText,
                            lowPowerCameraMode = lowPower
                        ),
                        isReady = true
                    ),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onLargeTextModeToggle = { largeText = it },
                    onLowPowerCameraToggle = { lowPower = it },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_large_text_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_low_power_camera_switch").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(largeText)
        assertTrue(lowPower)
        composeRule.onNodeWithText("Katta shrift rejimi (Keksalar uchun)").assertIsDisplayed()
    }

    @Test
    fun prayerPoseAnalyzer_lowPowerModeFlagTogglesCleanly() {
        val analyzer = PrayerPoseAnalyzer(lowPowerMode = true, onPoseResult = {})
        assertTrue(analyzer.lowPowerMode)
        analyzer.lowPowerMode = false
        assertTrue(!analyzer.lowPowerMode)
        analyzer.close()
    }
}
