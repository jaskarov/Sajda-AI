package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.audio.TtsReadinessInfo
import com.example.data.local.PrayerHistoryRepository
import com.example.data.local.QazoPrayerRepository
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
import com.example.domain.model.QazoPrayerType
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
    val settingsLoaded: Boolean = true,
    val steps: List<PrayerStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val mode: CoachMode = CoachMode.CAMERA_AI,
    val fallbackReason: FallbackReason = FallbackReason.NONE,
    val poseSnapshot: PoseAnalysisSnapshot = PoseAnalysisSnapshot(),
    val isPaused: Boolean = false,
    val pausedByInterruption: Boolean = false,
    val isCompleted: Boolean = false,
    val stepAudioDone: Boolean = false,
    val isPostRecitationCueActive: Boolean = false,
    val playbackProgress: Float = 0f,
    val audioStatus: AudioCoachStatus = AudioCoachStatus.Idle,
    val audioOnlyRemainingSec: Int = 0,
    val calibrationConfirmedOnce: Boolean = false,
    val totalSessionSeconds: Int = 0,
    val pendingNextTrack: PrayerTrack? = null,
    val nextTrackCountdownSec: Int = 0,
    val nextTrackCountdownTotalSec: Int = 5,
    val hapticFeedbackPulse: Int = 0,
    val qazoTypeId: String = "",
    val qazoDecrementedOnCompletion: Boolean = false,
    val salamProgressStage: Int = 0
) {
    val currentStep: PrayerStep? get() = steps.getOrNull(currentStepIndex)
}

class PrayerCoachViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val database = SajdaDatabase.getInstance(application)
    private val historyRepo = PrayerHistoryRepository(
        database.prayerHistoryDao(),
        database.dailyPrayerProgressDao()
    )
    private val qazoRepo = QazoPrayerRepository(database.qazoPrayerDao())
    private val sequenceEngine = PrayerSequenceEngine()
    private val audioCoach = PrayerAudioCoach(application)

    private val _uiState = MutableStateFlow(PrayerCoachUiState(settingsLoaded = false))
    val uiState: StateFlow<PrayerCoachUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var nextTrackCountdownJob: Job? = null
    private var startSessionJob: Job? = null
    private var postCueDelayJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private var stepEnteredMillis: Long = 0L
    private var activeSessionStarted: Boolean = false
    private var lastAutoAdvanceStepIndex: Int = -1
    private var salamFirstSideDir: Int = 0
    private var salamFirstTurnMillis: Long = 0L

    init {
        audioCoach.setOnAudioFocusLost {
            pauseOnExternalInterruption()
        }
        viewModelScope.launch {
            prefsRepo.settingsFlow.collect { loadedSettings ->
                _uiState.update { state ->
                    val needsRebuild = !activeSessionStarted &&
                        state.steps.isNotEmpty() &&
                        state.settings.gender != loadedSettings.gender
                    val updatedSteps = if (needsRebuild) {
                        sequenceEngine.buildSteps(
                            track = state.track,
                            gender = loadedSettings.gender,
                            qazoType = QazoPrayerType.fromId(state.qazoTypeId)
                        )
                    } else {
                        state.steps
                    }
                    state.copy(
                        settings = loadedSettings,
                        settingsLoaded = true,
                        steps = updatedSteps
                    )
                }
            }
        }
        viewModelScope.launch {
            audioCoach.playbackProgress.collect { progress ->
                _uiState.update { state ->
                    // Keep progress pinned at 1f while post-recitation cue shows/speaks after surah
                    if (state.isPostRecitationCueActive || state.stepAudioDone) state
                    else state.copy(playbackProgress = progress)
                }
            }
        }
        viewModelScope.launch {
            audioCoach.status.collect { status ->
                _uiState.update { it.copy(audioStatus = status) }
            }
        }
    }

    fun prepareTrack(
        track: PrayerTrack,
        initialMode: CoachMode = CoachMode.CAMERA_AI,
        qazoTypeId: String = "",
        initialSettings: UserSettings? = null
    ) {
        nextTrackCountdownJob?.cancel()
        startSessionJob?.cancel()
        postCueDelayJob?.cancel()
        val effectiveSettings = initialSettings ?: _uiState.value.settings
        val alreadyLoaded = initialSettings != null || _uiState.value.settingsLoaded
        val qazoType = QazoPrayerType.fromId(qazoTypeId)
        val initialSteps = sequenceEngine.buildSteps(track, effectiveSettings.gender, qazoType)
        lastAutoAdvanceStepIndex = -1
        salamFirstSideDir = 0
        salamFirstTurnMillis = 0L
        _uiState.update {
            it.copy(
                track = track,
                settings = effectiveSettings,
                settingsLoaded = alreadyLoaded,
                steps = initialSteps,
                currentStepIndex = 0,
                mode = initialMode,
                isCompleted = false,
                isPaused = false,
                pausedByInterruption = false,
                stepAudioDone = false,
                isPostRecitationCueActive = false,
                calibrationConfirmedOnce = false,
                pendingNextTrack = null,
                nextTrackCountdownSec = 0,
                nextTrackCountdownTotalSec = 5,
                qazoTypeId = qazoTypeId,
                qazoDecrementedOnCompletion = false,
                salamProgressStage = 0
            )
        }
        if (!alreadyLoaded) {
            viewModelScope.launch {
                val settings = prefsRepo.settingsFlow.first()
                val updatedSteps = sequenceEngine.buildSteps(track, settings.gender, qazoType)
                _uiState.update {
                    it.copy(
                        settings = settings,
                        settingsLoaded = true,
                        steps = updatedSteps
                    )
                }
            }
        }
    }

    fun speakCalibrationPrompt(message: String) {
        val settings = _uiState.value.settings
        if (audioCoach.shouldAvoidForeignRobotTts(settings.preferOfflineMp3)) {
            if (settings.acousticChimeEnabled) {
                audioCoach.playStepConfirmedChime()
            }
            return
        }
        audioCoach.speak(
            text = message,
            gender = settings.gender,
            preferOfflineMp3 = false,
            speed = settings.voiceSpeed
        )
    }

    fun setSeatedPrayerMode(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(seatedPrayerMode = enabled)) }
        viewModelScope.launch {
            prefsRepo.setSeatedPrayerMode(enabled)
        }
    }

    fun startActivePrayerSession(
        track: PrayerTrack,
        mode: CoachMode,
        fallbackReason: FallbackReason = FallbackReason.NONE,
        qazoTypeId: String = "",
        initialSettings: UserSettings? = null
    ) {
        nextTrackCountdownJob?.cancel()
        startSessionJob?.cancel()
        postCueDelayJob?.cancel()
        timerJob?.cancel()

        val effectiveSettings = initialSettings ?: _uiState.value.settings
        val alreadyLoaded = initialSettings != null || _uiState.value.settingsLoaded
        val qazoType = QazoPrayerType.fromId(qazoTypeId)
        val initialSteps = sequenceEngine.buildSteps(track, effectiveSettings.gender, qazoType)
        val firstStepDurationSec = initialSteps.firstOrNull()
            ?.let { (it.minHoldDurationMs / 1000L).toInt().coerceAtLeast(3) }
            ?: 4

        activeSessionStarted = true
        sessionStartMillis = SystemClock.elapsedRealtime()
        stepEnteredMillis = sessionStartMillis
        lastAutoAdvanceStepIndex = -1
        salamFirstSideDir = 0
        salamFirstTurnMillis = 0L

        _uiState.update {
            it.copy(
                track = track,
                settings = effectiveSettings,
                settingsLoaded = alreadyLoaded,
                steps = initialSteps,
                mode = mode,
                fallbackReason = fallbackReason,
                currentStepIndex = 0,
                isPaused = false,
                pausedByInterruption = false,
                isCompleted = false,
                stepAudioDone = false,
                isPostRecitationCueActive = false,
                playbackProgress = 0f,
                audioOnlyRemainingSec = firstStepDurationSec,
                pendingNextTrack = null,
                nextTrackCountdownSec = 0,
                nextTrackCountdownTotalSec = 5,
                qazoTypeId = qazoTypeId,
                qazoDecrementedOnCompletion = false,
                salamProgressStage = 0
            )
        }

        if (alreadyLoaded) {
            enterStep(0)
        } else {
            // Wait for persisted DataStore settings before firing step 0 audio so female,
            // senior voice-speed, seated mode, and auto-volume settings apply on step 0.
            startSessionJob = viewModelScope.launch {
                val loadedSettings = prefsRepo.settingsFlow.first()
                if (!isActive || _uiState.value.isCompleted) return@launch
                val currentState = _uiState.value
                val finalSteps = if (loadedSettings.gender != effectiveSettings.gender) {
                    sequenceEngine.buildSteps(track, loadedSettings.gender, qazoType)
                } else {
                    currentState.steps
                }
                _uiState.update {
                    it.copy(
                        settings = loadedSettings,
                        settingsLoaded = true,
                        steps = finalSteps
                    )
                }
                // Only start step 0 if user hasn't already manually advanced or paused
                val afterUpdate = _uiState.value
                if (!afterUpdate.isPaused && !afterUpdate.isCompleted && afterUpdate.currentStepIndex == 0) {
                    enterStep(0)
                }
            }
        }
    }

    fun startNextContinuationTrack() {
        nextTrackCountdownJob?.cancel()
        val state = _uiState.value
        val nextTrack = state.pendingNextTrack ?: return
        startActivePrayerSession(
            track = nextTrack,
            mode = state.mode,
            fallbackReason = state.fallbackReason,
            initialSettings = state.settings
        )
    }

    fun extendContinuationCountdown(extraSeconds: Int = 10) {
        val state = _uiState.value
        if (!state.isCompleted || state.pendingNextTrack == null) return
        nextTrackCountdownJob?.cancel()
        val updatedSec = (state.nextTrackCountdownSec + extraSeconds).coerceAtMost(60)
        _uiState.update {
            it.copy(
                nextTrackCountdownSec = updatedSec,
                nextTrackCountdownTotalSec = maxOf(it.nextTrackCountdownTotalSec, updatedSec)
            )
        }
        launchContinuationCountdown(updatedSec)
    }

    fun cancelContinuationCountdown() {
        nextTrackCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                pendingNextTrack = null,
                nextTrackCountdownSec = 0
            )
        }
    }

    fun onPoseSnapshot(snapshot: PoseAnalysisSnapshot) {
        val nowMs = SystemClock.elapsedRealtime()
        val previousState = _uiState.value
        val activeStep = previousState.currentStep
        var updatedSalamStage = previousState.salamProgressStage

        if (activeSessionStarted && !previousState.isPaused && !previousState.isCompleted &&
            activeStep?.expectedPose == PrayerPose.SALAM
        ) {
            val yaw = snapshot.headYawDirection
            val isSalamConfirmed = snapshot.confirmedPose == PrayerPose.SALAM
            if (isSalamConfirmed || yaw != 0) {
                if (salamFirstSideDir == 0 && (yaw != 0 || isSalamConfirmed)) {
                    if (isSalamConfirmed) {
                        salamFirstSideDir = if (yaw != 0) yaw else 1
                        salamFirstTurnMillis = nowMs
                        updatedSalamStage = maxOf(updatedSalamStage, 1)
                    }
                } else if (salamFirstSideDir != 0 && yaw != 0 && yaw != salamFirstSideDir && isSalamConfirmed) {
                    updatedSalamStage = 2
                } else if (salamFirstSideDir != 0 && isSalamConfirmed && (nowMs - salamFirstTurnMillis) >= 3500L) {
                    updatedSalamStage = 2
                }
            }
        }

        _uiState.update { state ->
            val calibrated = state.calibrationConfirmedOnce || snapshot.isFullBodyVisible
            state.copy(
                poseSnapshot = snapshot,
                calibrationConfirmedOnce = calibrated,
                salamProgressStage = updatedSalamStage
            )
        }

        val state = _uiState.value
        if (!activeSessionStarted || state.isPaused || state.isCompleted) return

        // Auto fallback when room is too dark in Camera AI mode
        if (state.mode == CoachMode.CAMERA_AI && snapshot.isLowLight) {
            switchToAudioOnly(FallbackReason.LOW_LIGHT_DARK_ROOM)
            return
        }

        // Camera AI auto-advance when pose matches expected step pose & audio (+ post cue) finished
        if (state.mode == CoachMode.CAMERA_AI) {
            val step = state.currentStep ?: return
            val elapsedInStep = nowMs - stepEnteredMillis
            val poseReady = isStepPoseReadyForAdvance(step, state)
            if (state.stepAudioDone && poseReady && elapsedInStep >= 1200L) {
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

    /**
     * Called automatically on incoming phone call / AudioFocus loss so the prayer timer
     * and audio never advance in the background while the worshipper is interrupted.
     */
    fun pauseOnExternalInterruption() {
        val state = _uiState.value
        if (!activeSessionStarted || state.isCompleted || state.isPaused) return
        postCueDelayJob?.cancel()
        timerJob?.cancel()
        audioCoach.pause()
        _uiState.update {
            it.copy(
                isPaused = true,
                pausedByInterruption = true
            )
        }
    }

    /**
     * Called from ActivePrayerScreen Lifecycle.Event.ON_PAUSE when app goes to background
     * or an incoming call overlay takes focus.
     */
    fun pauseOnBackground() {
        val state = _uiState.value
        if (state.isCompleted) {
            // Pause continuation auto-start countdown if user leaves screen
            nextTrackCountdownJob?.cancel()
            return
        }
        pauseOnExternalInterruption()
    }

    fun togglePause() {
        val state = _uiState.value
        val nextPaused = !state.isPaused
        _uiState.update {
            it.copy(
                isPaused = nextPaused,
                pausedByInterruption = if (nextPaused) it.pausedByInterruption else false
            )
        }
        if (nextPaused) {
            postCueDelayJob?.cancel()
            audioCoach.pause()
            timerJob?.cancel()
        } else {
            val latest = _uiState.value
            if (latest.isPostRecitationCueActive && !latest.stepAudioDone) {
                finalizeStepAudio(latest.currentStepIndex)
            } else {
                audioCoach.resume()
            }
            if (_uiState.value.mode == CoachMode.AUDIO_ONLY) {
                startAudioOnlyStepTimer()
            }
        }
    }

    fun advanceToNextStep() {
        postCueDelayJob?.cancel()
        val state = _uiState.value
        if (state.isCompleted) return
        _uiState.update { it.copy(hapticFeedbackPulse = it.hapticFeedbackPulse + 1) }
        val nextIdx = state.currentStepIndex + 1
        if (nextIdx >= state.steps.size) {
            completePrayerSession()
        } else {
            if (activeSessionStarted && state.settings.acousticChimeEnabled) {
                audioCoach.playStepConfirmedChime()
            }
            enterStep(nextIdx)
        }
    }

    private fun enterStep(index: Int) {
        timerJob?.cancel()
        postCueDelayJob?.cancel()
        val state = _uiState.value
        val step = state.steps.getOrNull(index) ?: return
        stepEnteredMillis = SystemClock.elapsedRealtime()
        salamFirstSideDir = 0
        salamFirstTurnMillis = 0L
        _uiState.update {
            it.copy(
                currentStepIndex = index,
                stepAudioDone = false,
                isPostRecitationCueActive = false,
                playbackProgress = 0f,
                salamProgressStage = 0,
                audioOnlyRemainingSec = (step.minHoldDurationMs / 1000L).toInt().coerceAtLeast(3)
            )
        }

        audioCoach.speak(
            text = step.ttsPrompt,
            gender = state.settings.gender,
            preferOfflineMp3 = state.settings.preferOfflineMp3,
            speed = state.settings.voiceSpeed,
            audioAssetId = step.audioAssetId,
            onCompleted = {
                handlePrimaryAudioCompleted(index, step)
            }
        )

        if (_uiState.value.mode == CoachMode.AUDIO_ONLY) {
            startAudioOnlyStepTimer()
        }
    }

    private fun handlePrimaryAudioCompleted(stepIndex: Int, step: PrayerStep) {
        val latest = _uiState.value
        if (latest.currentStepIndex != stepIndex || latest.isCompleted) return

        val cue = step.postRecitationCue?.get(latest.settings.script)
        if (!cue.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    playbackProgress = 1f,
                    isPostRecitationCueActive = true
                )
            }
            // Protect khushu: never interrupt serene Qori recitations with English/foreign robotic TTS.
            if (audioCoach.shouldAvoidForeignRobotTts(latest.settings.preferOfflineMp3)) {
                if (latest.settings.acousticChimeEnabled) {
                    audioCoach.playSoftTransitionChime()
                }
                postCueDelayJob?.cancel()
                postCueDelayJob = viewModelScope.launch {
                    delay(900L)
                    if (isActive && !_uiState.value.isPaused) {
                        finalizeStepAudio(stepIndex)
                    }
                }
            } else {
                audioCoach.speak(
                    text = cue,
                    gender = latest.settings.gender,
                    preferOfflineMp3 = false,
                    speed = latest.settings.voiceSpeed,
                    audioAssetId = "",
                    onCompleted = {
                        finalizeStepAudio(stepIndex)
                    }
                )
            }
        } else {
            finalizeStepAudio(stepIndex)
        }
    }

    private fun finalizeStepAudio(stepIndex: Int) {
        val latest = _uiState.value
        if (latest.currentStepIndex != stepIndex || latest.isCompleted) return
        _uiState.update {
            it.copy(
                stepAudioDone = true,
                isPostRecitationCueActive = false,
                playbackProgress = 1f
            )
        }
        val updated = _uiState.value
        if (updated.isPaused) return

        when (updated.mode) {
            CoachMode.AUDIO_ONLY -> {
                if (updated.audioOnlyRemainingSec <= 0) {
                    advanceToNextStep()
                }
            }
            CoachMode.CAMERA_AI -> {
                val step = updated.currentStep ?: return
                val elapsedInStep = SystemClock.elapsedRealtime() - stepEnteredMillis
                if (isStepPoseReadyForAdvance(step, updated) && elapsedInStep >= 1200L &&
                    lastAutoAdvanceStepIndex != updated.currentStepIndex
                ) {
                    lastAutoAdvanceStepIndex = updated.currentStepIndex
                    advanceToNextStep()
                }
            }
        }
    }

    private fun isStepPoseReadyForAdvance(
        step: PrayerStep,
        state: PrayerCoachUiState
    ): Boolean {
        val compatible = isPoseCompatible(
            detected = state.poseSnapshot.confirmedPose,
            expected = step.expectedPose,
            seatedMode = state.settings.seatedPrayerMode
        )
        if (!compatible) return false
        return if (step.expectedPose == PrayerPose.SALAM) {
            state.salamProgressStage >= 2
        } else {
            true
        }
    }

    @VisibleForTesting
    internal fun completeStepAudioForTest() {
        finalizeStepAudio(_uiState.value.currentStepIndex)
    }

    @VisibleForTesting
    internal fun triggerPrimaryAudioCompletedForTest() {
        val state = _uiState.value
        val step = state.currentStep ?: return
        handlePrimaryAudioCompleted(state.currentStepIndex, step)
    }

    @VisibleForTesting
    internal fun simulateAudioFocusLossForTest(focusChange: Int) {
        audioCoach.simulateAudioFocusChangeForTest(focusChange)
    }

    @VisibleForTesting
    internal fun setTtsReadinessForTest(info: TtsReadinessInfo) {
        audioCoach.setTtsReadinessForTest(info)
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

    private fun isPoseCompatible(
        detected: PrayerPose,
        expected: PrayerPose,
        seatedMode: Boolean = false
    ): Boolean {
        if (seatedMode) {
            return when (expected) {
                PrayerPose.QIYOM,
                PrayerPose.QAVMA,
                PrayerPose.JALSA,
                PrayerPose.QADA -> detected == PrayerPose.QIYOM ||
                    detected == PrayerPose.QAVMA ||
                    detected == PrayerPose.JALSA ||
                    detected == PrayerPose.QADA
                else -> detected == expected
            }
        }
        return when (expected) {
            PrayerPose.QAVMA -> detected == PrayerPose.QIYOM || detected == PrayerPose.QAVMA
            PrayerPose.JALSA -> detected == PrayerPose.QADA || detected == PrayerPose.JALSA
            PrayerPose.SALAM -> detected == PrayerPose.SALAM
            else -> detected == expected
        }
    }

    private fun completePrayerSession() {
        timerJob?.cancel()
        postCueDelayJob?.cancel()
        nextTrackCountdownJob?.cancel()
        audioCoach.stop()
        val state = _uiState.value
        val durationSec = ((SystemClock.elapsedRealtime() - sessionStartMillis) / 1000L)
            .toInt()
            .coerceAtLeast(10)
        val activeQazoType = QazoPrayerType.fromId(state.qazoTypeId)
        val isQazoSession = activeQazoType != null
        val nextTrack = if (isQazoSession) null else state.track.nextContinuationTrack()

        _uiState.update {
            it.copy(
                isCompleted = true,
                isPaused = false,
                pausedByInterruption = false,
                totalSessionSeconds = durationSec,
                pendingNextTrack = nextTrack,
                nextTrackCountdownSec = if (nextTrack != null) 5 else 0,
                nextTrackCountdownTotalSec = 5,
                qazoDecrementedOnCompletion = isQazoSession
            )
        }

        viewModelScope.launch {
            historyRepo.recordCompletion(
                prayerTypeId = state.track.parentPrayer.id,
                trackId = state.track.id,
                rakatCount = state.track.rakatCount,
                coachMode = state.mode.name,
                cityId = state.settings.selectedCityId,
                durationSeconds = durationSec,
                markDailyCompleted = !isQazoSession
            )
            if (activeQazoType != null) {
                qazoRepo.decrement(activeQazoType)
            }
        }

        if (nextTrack != null) {
            if (audioCoach.shouldAvoidForeignRobotTts(state.settings.preferOfflineMp3)) {
                if (state.settings.acousticChimeEnabled) {
                    audioCoach.playStepConfirmedChime()
                }
            } else {
                val announcement = when (nextTrack) {
                    PrayerTrack.VITR_VOJIB -> "Xufton sunnati tugadi. 5 soniyada Vitr namozi boshlanadi."
                    PrayerTrack.PESHIN_SUNNAT_2,
                    PrayerTrack.SHOM_SUNNAT,
                    PrayerTrack.XUFTON_SUNNAT -> "Farz namozi tugadi. 5 soniyada 2 rakat sunnat boshlanadi."
                    else -> "Sunnat namozi tugadi. 5 soniyada Farz namozi boshlanadi."
                }
                audioCoach.speak(
                    text = announcement,
                    gender = state.settings.gender,
                    preferOfflineMp3 = false,
                    speed = state.settings.voiceSpeed
                )
            }
            launchContinuationCountdown(fromSeconds = 5)
        }
    }

    private fun launchContinuationCountdown(fromSeconds: Int) {
        nextTrackCountdownJob?.cancel()
        nextTrackCountdownJob = viewModelScope.launch {
            for (sec in fromSeconds downTo 1) {
                if (!isActive || !_uiState.value.isCompleted || _uiState.value.pendingNextTrack == null) {
                    return@launch
                }
                _uiState.update { it.copy(nextTrackCountdownSec = sec) }
                delay(1000L)
            }
            if (isActive && _uiState.value.isCompleted && _uiState.value.pendingNextTrack != null) {
                startNextContinuationTrack()
            }
        }
    }

    fun cleanup() {
        timerJob?.cancel()
        postCueDelayJob?.cancel()
        startSessionJob?.cancel()
        nextTrackCountdownJob?.cancel()
        audioCoach.release()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
