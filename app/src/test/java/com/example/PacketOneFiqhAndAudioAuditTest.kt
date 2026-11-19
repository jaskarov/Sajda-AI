package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.audio.TtsReadinessInfo
import com.example.data.preferences.UserSettings
import com.example.domain.engine.PrayerContentCatalog
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.Gender
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerTrack
import com.example.domain.model.QazoPrayerType
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.theme.SajdaAiTheme
import com.example.ui.viewmodel.PrayerCoachUiState
import com.example.ui.viewmodel.PrayerCoachViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PacketOneFiqhAndAudioAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentCatalog_hasCompleteArabicTranslitAndMeaningsWithZeroEllipsis() {
        assertTrue(PrayerContentCatalog.surahs.any { it.id == "nas" })
        assertTrue(PrayerContentCatalog.surahs.any { it.id == "qunut" })

        PrayerContentCatalog.surahs.forEach { item ->
            assertFalse("Arabic text truncated for ${item.id}", item.arabicText.contains("..."))
            assertFalse("Latin translit truncated for ${item.id}", item.translitLatin.contains("..."))
            assertFalse("Cyrillic translit truncated for ${item.id}", item.translitCyrillic.contains("..."))
            assertFalse("Latin meaning truncated for ${item.id}", item.meaning.latin.contains("..."))
            assertFalse("Cyrillic meaning truncated for ${item.id}", item.meaning.cyrillic.contains("..."))
        }

        val falaq = PrayerContentCatalog.findSurahById("falaq")
        assertTrue(falaq.arabicText.contains("النَّفَّاثَاتِ"))
        assertTrue(falaq.arabicText.contains("حَاسِدٍ إِذَا حَسَدَ"))

        val nas = PrayerContentCatalog.findSurahById("nas")
        assertTrue(nas.arabicText.contains("الْجِنَّةِ وَالنَّاسِ"))

        val attahiyat = PrayerContentCatalog.findSurahById("attahiyat")
        assertTrue(attahiyat.arabicText.contains("أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ"))

        val salovat = PrayerContentCatalog.findSurahById("salovat")
        assertTrue(salovat.arabicText.contains("اللَّهُمَّ بَارِكْ"))
        assertTrue(salovat.translitLatin.contains("kama barokta"))

        val qunut = PrayerContentCatalog.findSurahById("qunut")
        assertTrue(qunut.arabicText.contains("بِالْكُفَّارِ مُلْحِقٌ"))
        assertTrue(qunut.translitLatin.contains("bil kuffari mulhiq"))
    }

    @Test
    fun sequenceEngine_hasSpecificNiyats_vitrQunutTakbir_andNoDoubleTakbirInPostCues() {
        val engine = PrayerSequenceEngine()

        val niyats = PrayerTrack.entries.map { track ->
            val steps = engine.buildSteps(track, Gender.MALE)
            assertFalse("Track $track must have steps", steps.isEmpty())

            // No postRecitationCue may repeat "Allohu akbar" since the following rukn plays Takbir
            steps.forEach { step ->
                val cueLat = step.postRecitationCue?.latin.orEmpty()
                assertFalse(
                    "Double Takbir detected in $track step ${step.stepTitle.latin}: $cueLat",
                    cueLat.contains("Allohu akbar", ignoreCase = true)
                )
            }
            steps.first().meaning.latin
        }
        assertEquals(
            "Every PrayerTrack must have a distinct specific Niyat",
            PrayerTrack.entries.size,
            niyats.toSet().size
        )

        // Qazo Niyat check
        val qazoSteps = engine.buildSteps(PrayerTrack.ASR_FARZ, Gender.FEMALE, QazoPrayerType.ASR)
        assertTrue(qazoSteps.first().meaning.latin.contains("zimmamdagi qazo Asr"))

        // Hanafi Vitr 3rd rakat must have Qunut Takbir (TAKBIR) immediately before Qunut Duosi (QIYOM)
        val vitrSteps = engine.buildSteps(PrayerTrack.VITR_VOJIB, Gender.FEMALE)
        val qunutTakbirIdx = vitrSteps.indexOfFirst { it.recitationTitle.latin == "QUNUT TAKBIRI" }
        val qunutDuoIdx = vitrSteps.indexOfFirst { it.recitationTitle.latin == "QUNUT DUOSI" }
        assertTrue("Vitr must include Qunut Takbir step", qunutTakbirIdx > 0)
        assertEquals("Qunut Duosi must immediately follow Qunut Takbir", qunutTakbirIdx + 1, qunutDuoIdx)
        assertEquals(PrayerPose.TAKBIR, vitrSteps[qunutTakbirIdx].expectedPose)
        assertEquals("takbir", vitrSteps[qunutTakbirIdx].audioAssetId)
        assertEquals("qunut", vitrSteps[qunutDuoIdx].audioAssetId)
        assertFalse(vitrSteps[qunutDuoIdx].recitationLatin.contains("..."))
    }

    @Test
    fun prayerAudioCoach_preservesQueueAndGenderAcrossPauseResume_andGuardsForeignTts() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val coach = PrayerAudioCoach(context)
        try {
            coach.setTtsReadinessForTest(
                TtsReadinessInfo(isEngineReady = true, hasUzbekLocale = false, hasFallbackLocale = true)
            )
            assertTrue(coach.shouldAvoidForeignRobotTts(preferOfflineMp3 = true))
            assertFalse(coach.shouldAvoidForeignRobotTts(preferOfflineMp3 = false))

            coach.setTtsReadinessForTest(
                TtsReadinessInfo(isEngineReady = true, hasUzbekLocale = true, hasFallbackLocale = true)
            )
            assertFalse(coach.shouldAvoidForeignRobotTts(preferOfflineMp3 = true))

            // Multi-clip pause during inter-clip delay must preserve queued URI and FEMALE gender
            coach.enqueuePendingUriForTest(
                uri = Uri.parse("android.resource://${context.packageName}/raw/fotiha"),
                gender = Gender.FEMALE
            )
            coach.pause()
            assertEquals(1, coach.pendingQueuedCountForTest())
            assertEquals(Gender.FEMALE, coach.activeGenderForTest())

            coach.resume()
            assertEquals(Gender.FEMALE, coach.activeGenderForTest())
            // Resuming should have consumed the queued clip via ExoPlayer (not fallen back to TTS)
            assertFalse(coach.status.value is AudioCoachStatus.PlayingOfflineTts)
        } finally {
            coach.release()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prayerCoachViewModel_appliesInitialSettingsOnStepZero_andAutoPausesOnCallAndBackground() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PrayerCoachViewModel(app)
        try {
            val femaleSlowSettings = UserSettings(
                gender = Gender.FEMALE,
                voiceSpeed = 0.8f,
                seatedPrayerMode = true,
                autoBoostPrayerVolume = false,
                preferOfflineMp3 = true
            )
            viewModel.setTtsReadinessForTest(
                TtsReadinessInfo(isEngineReady = true, hasUzbekLocale = false, hasFallbackLocale = true)
            )

            viewModel.startActivePrayerSession(
                track = PrayerTrack.BOMDOD_SUNNAT,
                mode = CoachMode.AUDIO_ONLY,
                fallbackReason = FallbackReason.USER_SELECTED,
                initialSettings = femaleSlowSettings
            )

            val step0State = viewModel.uiState.value
            assertTrue(step0State.settingsLoaded)
            assertEquals(Gender.FEMALE, step0State.settings.gender)
            assertEquals(0.8f, step0State.settings.voiceSpeed, 0.01f)
            assertTrue(step0State.settings.seatedPrayerMode)
            assertFalse(step0State.settings.autoBoostPrayerVolume)
            assertTrue(
                "Step 0 must use female shoulder-height instruction immediately",
                step0State.currentStep?.coachInstruction?.latin?.contains("yelka") == true
            )

            // Post-recitation cue on step with cue (Sano) must NOT invoke foreign robotic TTS
            viewModel.advanceToNextStep() // Step 1: Sano (has postRecitationCue)
            viewModel.triggerPrimaryAudioCompletedForTest()
            assertFalse(
                "Must never fall back to English/foreign TTS for Uzbek post-recitation cue",
                viewModel.uiState.value.audioStatus is AudioCoachStatus.PlayingOfflineTts
            )

            // Incoming phone call (AUDIOFOCUS_LOSS_TRANSIENT) must pause session & flag interruption
            viewModel.simulateAudioFocusLossForTest(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            assertTrue(viewModel.uiState.value.isPaused)
            assertTrue(viewModel.uiState.value.pausedByInterruption)

            // Manual resume clears interruption flag
            viewModel.togglePause()
            assertFalse(viewModel.uiState.value.isPaused)
            assertFalse(viewModel.uiState.value.pausedByInterruption)

            // Lifecycle ON_PAUSE (app backgrounded) must also auto-pause session
            viewModel.pauseOnBackground()
            assertTrue(viewModel.uiState.value.isPaused)
            assertTrue(viewModel.uiState.value.pausedByInterruption)
        } finally {
            viewModel.cleanup()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun activePrayerScreen_waitsForSettingsLoaded_neverBoostsWhenDisabled_andShowsInterruptionBanner() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val quietVol = (maxVol * 0.20f).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, quietVol, 0)

        val steps = PrayerSequenceEngine().buildSteps(PrayerTrack.BOMDOD_SUNNAT, Gender.MALE)
        var uiState by mutableStateOf(
            PrayerCoachUiState(
                track = PrayerTrack.BOMDOD_SUNNAT,
                settings = UserSettings(autoBoostPrayerVolume = true),
                settingsLoaded = false, // Simulates pre-DataStore load on first frame
                steps = steps,
                currentStepIndex = 0,
                mode = CoachMode.AUDIO_ONLY
            )
        )

        composeRule.setContent {
            SajdaAiTheme {
                ActivePrayerScreen(
                    uiState = uiState,
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

        // Before DataStore finishes loading, volume MUST NOT be boosted
        assertEquals(quietVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        // Now DataStore resolves with user's persisted autoBoostPrayerVolume = false (5:00 AM Bomdod!)
        uiState = uiState.copy(
            settings = UserSettings(autoBoostPrayerVolume = false),
            settingsLoaded = true,
            isPaused = true,
            pausedByInterruption = true
        )
        composeRule.waitForIdle()

        assertEquals(quietVol, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        composeRule.onNodeWithTag("interruption_pause_banner").assertIsDisplayed()
    }
}
