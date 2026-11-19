package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.preferences.UserSettings
import com.example.domain.engine.GetPrayerTimesUseCase
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.PrayerTrack
import com.example.domain.model.TextScript
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InteractivePrayerGuideCatalog
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PrayerGuideOnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InteractiveOnboardingGuideTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun interactiveGuide_stepsThroughAllSixRuknsAndReturnsChosenPreferences() {
        var finishedGender: Gender? = null
        var finishedScript: TextScript? = null
        var finishedSeated: Boolean? = null
        val playedAssets = mutableListOf<String>()

        composeRule.setContent {
            SajdaAiTheme {
                PrayerGuideOnboardingScreen(
                    initialGender = Gender.MALE,
                    initialScript = TextScript.LATIN,
                    initialSeatedMode = false,
                    isFirstLaunch = true,
                    onPlayAudio = { _, assetId -> playedAssets.add(assetId) },
                    onFinish = { gender, script, seated ->
                        finishedGender = gender
                        finishedScript = script
                        finishedSeated = seated
                    }
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(6, InteractivePrayerGuideCatalog.steps.size)
        composeRule.onNodeWithText("NAMOZ TARTIBI • 1 / 6").assertIsDisplayed()

        // Tap Listen on Step 1 (Takbir)
        composeRule.onNodeWithTag("guide_listen_step_takbir").performClick()
        assertEquals(listOf("takbir"), playedAssets)

        // Toggle Female & Seated mode live and verify fiqh note adapts
        composeRule.onNodeWithTag("guide_gender_female").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guide_active_fiqh_note_step_takbir")
            .performScrollTo()
            .assertTextContains("Ayollar:", substring = true)

        composeRule.onNodeWithTag("guide_mode_seated").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guide_active_fiqh_note_step_takbir")
            .performScrollTo()
            .assertTextContains("Stulda o‘qish:", substring = true)

        // Mark step understood
        composeRule.onNodeWithTag("guide_mark_understood_step_takbir").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bu qadam tushunildi ✓").assertIsDisplayed()

        // Step forward through all remaining steps to Step 6 (Salam)
        repeat(5) {
            composeRule.onNodeWithTag("guide_next_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText("NAMOZ TARTIBI • 6 / 6").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_prev_button").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_finish_button").performClick()
        composeRule.waitForIdle()

        assertEquals(Gender.FEMALE, finishedGender)
        assertEquals(TextScript.LATIN, finishedScript)
        assertEquals(true, finishedSeated)
    }

    @Test
    fun interactiveGuide_replayModeSupportsDirectStepJumpAndPracticeCta() {
        var practiceStarted = false
        var playedAsset = ""

        composeRule.setContent {
            SajdaAiTheme {
                PrayerGuideOnboardingScreen(
                    initialGender = Gender.MALE,
                    initialScript = TextScript.LATIN,
                    initialSeatedMode = false,
                    isFirstLaunch = false,
                    onPlayAudio = { _, assetId -> playedAsset = assetId },
                    onStartPractice = { practiceStarted = true },
                    onFinish = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Jump directly to Step 3 (Ruku) via stepper pill
        composeRule.onNodeWithTag("guide_step_tab_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("NAMOZ TARTIBI • 3 / 6").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_listen_step_ruku").performClick()
        assertEquals("ruku", playedAsset)

        // Jump directly to Step 6 (Salam) and start practice
        composeRule.onNodeWithTag("guide_step_tab_5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guide_start_practice_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(practiceStarted)
    }

    @Test
    fun homeLearnAndSettingsScreens_exposeInteractivePrayerGuideEntryPoints() {
        var homeOpened = false
        val schedule = GetPrayerTimesUseCase().calculateForToday(
            city = UzbekistanCities.findById("toshkent"),
            completedToday = emptySet(),
            alarmMap = emptyMap()
        )
        composeRule.setContent {
            SajdaAiTheme {
                HomeScreen(
                    uiState = MainUiState(
                        schedule = schedule,
                        quickTracks = PrayerTrack.tracksForPrayer(schedule.currentPrayer),
                        isReady = true
                    ),
                    onStartPrayer = {},
                    onToggleAlarm = {},
                    onToggleCompleted = {},
                    onCitySelected = {},
                    onOpenSettings = {},
                    onOpenPrayerGuide = { homeOpened = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_prayer_list")
            .performScrollToNode(hasTestTag("home_open_prayer_guide_button"))
        composeRule.onNodeWithTag("home_open_prayer_guide_button").performClick()
        assertTrue(homeOpened)
    }

    @Test
    fun learnAndSettingsScreens_openInteractivePrayerGuideCallback() {
        var learnOpened = false
        composeRule.setContent {
            SajdaAiTheme {
                LearnScreen(
                    uiState = MainUiState(isReady = true),
                    onPlayAudio = { _, _ -> },
                    onStartTrack = {},
                    onOpenPrayerGuide = { learnOpened = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("learn_open_prayer_guide_button").performClick()
        assertTrue(learnOpened)
    }

    @Test
    fun settingsScreen_exposesInteractivePrayerGuideButton() {
        var settingsOpened = false
        composeRule.setContent {
            SajdaAiTheme {
                SettingsScreen(
                    uiState = MainUiState(settings = UserSettings(), isReady = true),
                    onGenderChange = {},
                    onScriptChange = {},
                    onCityChange = {},
                    onPreferOfflineMp3Toggle = {},
                    onVoiceSpeedChange = {},
                    onDarkThresholdChange = {},
                    onOpenPrayerGuide = { settingsOpened = true },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_open_prayer_guide_button").performScrollTo().performClick()
        assertTrue(settingsOpened)
    }
}
