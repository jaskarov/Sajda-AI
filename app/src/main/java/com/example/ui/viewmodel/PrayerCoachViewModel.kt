package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.SajdaDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.domain.engine.PrayerSequenceEngine
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.PrayerStep
import com.example.domain.model.PrayerTrack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PrayerCoachUiState(
    val track: PrayerTrack = PrayerTrack.PESHIN_SUNNAT_1,
    val settings: UserSettings = UserSettings(),
    val steps: List<PrayerStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val mode: CoachMode = CoachMode.CAMERA_AI,
    val fallbackReason: FallbackReason = FallbackReason.NONE,
    val poseSnapshot: PoseAnalysisSnapshot = PoseAnalysisSnapshot(),
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val stepAudioDone: Boolean = false,
    val playbackProgress: Float = 0f,
    val audioStatus: AudioCoachStatus = AudioCoachStatus.Idle,
    val audioOnlyRemainingSec: Int = 0,
    val calibrationConfirmedOnce: Boolean = false,
    val totalSessionSeconds: Int = 0
) {
    val currentStep: PrayerStep? get() = steps.getOrNull(currentStepIndex)
}

class PrayerCoachViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val historyRepo = SajdaDatabase.getInstance(application).let { db ->
        PrayerHistoryRepository(db.prayerHistoryDao(), db.dailyPrayerProgressDao())
    }
    private val sequenceEngine = PrayerSequenceEngine()
    private val audioCoach = PrayerAudioCoach(application)

    private val _uiState = MutableStateFlow(PrayerCoachUiState())
    val uiState: StateFlow<PrayerCoachUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private var stepEnteredMillis: Long = 0L
    private var activeSessionStarted: Boolean = false
    private var lastAutoAdvanceStepIndex: Int = -1

    init {
        viewModelScope.launch {
            audioCoach.playbackProgress.collect { progress ->
                _uiState.update { it.copy(playbackProgress = progress) }
            }
        }
        viewModelScope.launch {
            audioCoach.status.collect { status ->
                _uiState.update { it.copy(audioStatus = status) }
            }
        }
    }

    fun prepareTrack(track: PrayerTrack, initialMode: CoachMode = CoachMode.CAMERA_AI) {
        viewModelScope.launch {
            val settings = prefsRepo.settingsFlow.first()
            val steps = sequenceEngine.buildSteps(track, settings.gender)
            _uiState.update {
                it.copy(
                    track = track,
                    settings = settings,
                    steps = steps,
                    currentStepIndex = 0,
                    mode = initialMode,
                    isCompleted = false,
                    isPaused = false,
                    stepAudioDone = false,
                    calibrationConfirmedOnce = false
                )
            }
        }
    }

    fun speakCalibrationPrompt(message: String) {
        val settings = _uiState.value.settings
        audioCoach.speak(
            text = message,
            gender = settings.gender,
            useGeminiTts = settings.useGeminiTts,
            speed = settings.voiceSpeed
        )
    }

    fun startActivePrayerSession(
        track: PrayerTrack,
        mode: CoachMode,
        fallbackReason: FallbackReason = FallbackReason.NONE
    ) {
        viewModelScope.launch {
            val settings = prefsRepo.settingsFlow.first()
            val steps = sequenceEngine.buildSteps(track, settings.gender)
            activeSessionStarted = true
            sessionStartMillis = SystemClock.elapsedRealtime()
            lastAutoAdvanceStepIndex = -1
            _uiState.update {
                it.copy(
                    track = track,
                    settings = settings,
                    steps = steps,
                    mode = mode,
                    fallbackReason = fallbackReason,
                    currentStepIndex = 0,
                    isPaused = false,
                    isCompleted = false,
                    stepAudioDone = false
                )
            }
            enterStep(0)
        }
    }

    fun onPoseSnapshot(snapshot: PoseAnalysisSnapshot) {
        _uiState.update { state ->
            val calibrated = state.calibrationConfirmedOnce || snapshot.isFullBodyVisible
            state.copy(
                poseSnapshot = snapshot,
                calibrationConfirmedOnce = calibrated
            )
        }

        val state = _uiState.value
        if (!activeSessionStarted || state.isPaused || state.isCompleted) return

        // Auto fallback when room is too dark in Camera AI mode
        if (state.mode == CoachMode.CAMERA_AI && snapshot.isLowLight) {
            switchToAudioOnly(FallbackReason.LOW_LIGHT_DARK_ROOM)
            return
        }

        // Camera AI auto-advance when pose matches expected step pose & audio finished
        if (state.mode == CoachMode.CAMERA_AI) {
            val step = state.currentStep ?: return
            val elapsedInStep = SystemClock.elapsedRealtime() - stepEnteredMillis
            val poseMatches = isPoseCompatible(snapshot.confirmedPose, step.expectedPose)
            if (state.stepAudioDone && poseMatches && elapsedInStep >= 1200L) {
                if (lastAutoAdvanceStepIndex != state.currentStepIndex) {
                    lastAutoAdvanceStepIndex = state.currentStepIndex
                    advanceToNextStep()
                }
            }
        }
    }

    fun switchToAudioOnly(reason: FallbackReason) {
        val current = _uiState.value
        if (current.mode == CoachMode.AUDIO_ONLY) return
        _uiState.update {
            it.copy(
                mode = CoachMode.AUDIO_ONLY,
                fallbackReason = reason
            )
        }
        if (activeSessionStarted && !current.isPaused && !current.isCompleted) {
            startAudioOnlyStepTimer()
        }
    }

    fun switchToCameraMode() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                mode = CoachMode.CAMERA_AI,
                fallbackReason = FallbackReason.NONE,
                audioOnlyRemainingSec = 0
            )
        }
    }

    fun togglePause() {
        val nextPaused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = nextPaused) }
        if (nextPaused) {
            audioCoach.pause()
            timerJob?.cancel()
        } else {
            audioCoach.resume()
            if (_uiState.value.mode == CoachMode.AUDIO_ONLY) {
                startAudioOnlyStepTimer()
            }
        }
    }

    fun advanceToNextStep() {
        val state = _uiState.value
        if (state.isCompleted) return
        val nextIdx = state.currentStepIndex + 1
        if (nextIdx >= state.steps.size) {
            completePrayerSession()
        } else {
            enterStep(nextIdx)
        }
    }

    private fun enterStep(index: Int) {
        timerJob?.cancel()
        val state = _uiState.value
        val step = state.steps.getOrNull(index) ?: return
        stepEnteredMillis = SystemClock.elapsedRealtime()
        _uiState.update {
            it.copy(
                currentStepIndex = index,
                stepAudioDone = false,
                playbackProgress = 0f,
                audioOnlyRemainingSec = (step.minHoldDurationMs / 1000L).toInt().coerceAtLeast(3)
            )
        }

        audioCoach.speak(
            text = step.ttsPrompt,
            gender = state.settings.gender,
            useGeminiTts = state.settings.useGeminiTts,
            speed = state.settings.voiceSpeed,
            onCompleted = {
                _uiState.update { it.copy(stepAudioDone = true) }
                // If Audio-Only mode and timer already expired, advance automatically
                val latest = _uiState.value
                if (latest.mode == CoachMode.AUDIO_ONLY &&
                    !latest.isPaused &&
                    latest.audioOnlyRemainingSec <= 0
                ) {
                    advanceToNextStep()
                }
            }
        )

        if (_uiState.value.mode == CoachMode.AUDIO_ONLY) {
            startAudioOnlyStepTimer()
        }
    }

    private fun startAudioOnlyStepTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                if (state.isPaused || state.isCompleted) break
                val rem = state.audioOnlyRemainingSec
                if (rem <= 0) {
                    if (state.stepAudioDone) {
                        advanceToNextStep()
                        break
                    }
                } else {
                    _uiState.update { it.copy(audioOnlyRemainingSec = rem - 1) }
                }
                delay(1000L)
            }
        }
    }

    private fun isPoseCompatible(detected: PrayerPose, expected: PrayerPose): Boolean {
        return when (expected) {
            PrayerPose.QAVMA -> detected == PrayerPose.QIYOM || detected == PrayerPose.QAVMA
            PrayerPose.JALSA -> detected == PrayerPose.QADA || detected == PrayerPose.JALSA
            PrayerPose.SALAM -> detected == PrayerPose.QADA || detected == PrayerPose.SALAM
            else -> detected == expected
        }
    }

    private fun completePrayerSession() {
        timerJob?.cancel()
        audioCoach.stop()
        val state = _uiState.value
        val durationSec = ((SystemClock.elapsedRealtime() - sessionStartMillis) / 1000L)
            .toInt()
            .coerceAtLeast(10)

        _uiState.update {
            it.copy(
                isCompleted = true,
                totalSessionSeconds = durationSec
            )
        }

        viewModelScope.launch {
            historyRepo.recordCompletion(
                prayerTypeId = state.track.parentPrayer.id,
                trackId = state.track.id,
                rakatCount = state.track.rakatCount,
                coachMode = state.mode.name,
                cityId = state.settings.selectedCityId,
                durationSeconds = durationSec
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioCoach.release()
    }
}
