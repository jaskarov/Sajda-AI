package com.example.audio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.Gender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

sealed interface AudioCoachStatus {
    data object Idle : AudioCoachStatus
    data class PlayingOfflineAudio(val assetLabel: String) : AudioCoachStatus
    data class PlayingOfflineTts(val notice: String? = null) : AudioCoachStatus
    data class Error(val message: String) : AudioCoachStatus
}

/**
 * 100% Offline-capable Prayer Audio Coach:
 * 1) Plays bundled res/raw MP3 Qori recitations & duas via Media3 ExoPlayer
 *    (supporting comma-separated multi-clip queues such as "salovat,robbana").
 * 2) Plays Uzbek coach prompts via local MP3s (if supplied) or Android's built-in TextToSpeech.
 */
class PrayerAudioCoach(private val context: Context) {

    companion object {
        private const val TAG = "PrayerAudioCoach"
        private val SUPPORTED_EXTENSIONS = listOf("mp3", "m4a", "wav", "ogg")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _status = MutableStateFlow<AudioCoachStatus>(AudioCoachStatus.Idle)
    val status: StateFlow<AudioCoachStatus> = _status.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var localTts: TextToSpeech? = null
    private var isLocalTtsReady = false
    private var currentSpeechJob: Job? = null
    private var progressJob: Job? = null
    private var onPlaybackCompletedCallback: (() -> Unit)? = null
    private var activeUtteranceId: String? = null
    private var activeText: String = ""
    private var activeSpeed: Float = 1.0f
    private var isPaused: Boolean = false

    private val localRecitationsDir: File by lazy {
        File(context.filesDir, "recitations").apply { mkdirs() }
    }

    init {
        initExoPlayer()
        initLocalTts()
    }

    private fun initExoPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressPolling()
                    } else {
                        progressJob?.cancel()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        _playbackProgress.value = 1f
                        progressJob?.cancel()
                        onPlaybackCompletedCallback?.invoke()
                    }
                }
            })
        }
    }

    private fun initLocalTts() {
        localTts = TextToSpeech(context.applicationContext) { statusCode ->
            if (statusCode == TextToSpeech.SUCCESS) {
                val engine = localTts
                if (engine != null) {
                    val candidates = listOf(
                        Locale.forLanguageTag("uz-UZ"),
                        Locale.forLanguageTag("tr-TR"),
                        Locale.ENGLISH,
                        Locale.getDefault()
                    )
                    for (locale in candidates) {
                        val availability = engine.isLanguageAvailable(locale)
                        if (availability >= TextToSpeech.LANG_AVAILABLE) {
                            engine.language = locale
                            break
                        }
                    }
                    isLocalTtsReady = true
                }
            } else {
                Log.w(TAG, "Device TextToSpeech engine unavailable (code=$statusCode)")
            }
        }
        localTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    if (utteranceId == activeUtteranceId && !isPaused) {
                        activeUtteranceId = null
                        progressJob?.cancel()
                        _isPlaying.value = false
                        _playbackProgress.value = 1f
                        onPlaybackCompletedCallback?.invoke()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    if (utteranceId == activeUtteranceId) {
                        activeUtteranceId = null
                        _isPlaying.value = false
                    }
                }
            }
        })
    }

    fun speak(
        text: String,
        gender: Gender,
        preferOfflineMp3: Boolean = true,
        speed: Float = 1.0f,
        audioAssetId: String = "",
        onCompleted: (() -> Unit)? = null
    ) {
        currentSpeechJob?.cancel()
        progressJob?.cancel()
        stopInternal()
        isPaused = false
        activeText = text
        activeSpeed = speed
        onPlaybackCompletedCallback = onCompleted
        _playbackProgress.value = 0f

        currentSpeechJob = scope.launch {
            if (preferOfflineMp3 && audioAssetId.isNotBlank()) {
                val localUris = resolveOfflineAudioUris(audioAssetId)
                if (localUris.isNotEmpty() && playOfflineUris(localUris, speed, audioAssetId)) {
                    return@launch
                }
            }
            speakWithLocalTts(text, gender, speed)
        }
    }

    private fun resolveOfflineAudioUris(audioAssetSpec: String): List<Uri> {
        return audioAssetSpec.split(",")
            .mapNotNull { part ->
                val trimmed = part.trim()
                if (trimmed.isEmpty()) null else resolveSingleOfflineAudioUri(trimmed)
            }
    }

    private fun resolveSingleOfflineAudioUri(audioAssetId: String): Uri? {
        val safeId = audioAssetId.lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "_")

        // 1. Check app-private offline recitations folder (e.g. files/recitations/fotiha.mp3)
        for (ext in SUPPORTED_EXTENSIONS) {
            val file = File(localRecitationsDir, "$safeId.$ext")
            if (file.exists() && file.length() > 128L) {
                return Uri.fromFile(file)
            }
        }

        // 2. Check bundled res/raw/<audioAssetId> or res/raw/recitation_<audioAssetId>
        val rawCandidates = listOf(safeId, "recitation_$safeId")
        for (name in rawCandidates) {
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) {
                @Suppress("DEPRECATION")
                return RawResourceDataSource.buildRawResourceUri(resId)
            }
        }

        return null
    }

    private fun playOfflineUris(uris: List<Uri>, speed: Float, label: String): Boolean {
        val player = exoPlayer ?: return false
        return runCatching {
            val displayLabel = label.replace(",", " + ")
            _status.value = AudioCoachStatus.PlayingOfflineAudio(displayLabel)
            player.playbackParameters = PlaybackParameters(speed)
            player.setMediaItems(uris.map { MediaItem.fromUri(it) })
            player.prepare()
            player.play()
            true
        }.onFailure { err ->
            Log.w(TAG, "ExoPlayer failed for offline asset $label, falling back to TTS", err)
        }.getOrDefault(false)
    }

    private fun speakWithLocalTts(text: String, gender: Gender, speed: Float) {
        _status.value = AudioCoachStatus.PlayingOfflineTts()

        var ttsStarted = false
        if (isLocalTtsReady) {
            val utteranceId = UUID.randomUUID().toString()
            activeUtteranceId = utteranceId
            localTts?.setPitch(if (gender == Gender.FEMALE) 1.06f else 0.96f)
            localTts?.setSpeechRate(speed)
            val result = localTts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
            ttsStarted = result == TextToSpeech.SUCCESS
            if (!ttsStarted) {
                activeUtteranceId = null
            }
        }

        val estimatedMs = ((text.length * 65L) / speed).toLong().coerceIn(2500L, 16000L)
        startSimulatedProgress(
            estimatedDurationMs = estimatedMs,
            completeOnEnd = !ttsStarted
        )
    }

    fun pause() {
        isPaused = true
        exoPlayer?.pause()
        activeUtteranceId = null
        localTts?.stop()
        progressJob?.cancel()
        _isPlaying.value = false
    }

    fun resume() {
        if (!isPaused) return
        isPaused = false
        val player = exoPlayer
        if (player != null && player.playbackState == Player.STATE_READY) {
            player.play()
        } else if (activeText.isNotEmpty() && _playbackProgress.value < 1f) {
            speakWithLocalTts(activeText, Gender.MALE, activeSpeed)
        }
    }

    fun stop() {
        currentSpeechJob?.cancel()
        progressJob?.cancel()
        stopInternal()
        _status.value = AudioCoachStatus.Idle
        _playbackProgress.value = 0f
    }

    private fun stopInternal() {
        activeUtteranceId = null
        exoPlayer?.stop()
        localTts?.stop()
        _isPlaying.value = false
    }

    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
        localTts?.shutdown()
        localTts = null
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer ?: break
                val duration = player.duration
                val itemCount = player.mediaItemCount.coerceAtLeast(1)
                val currentIdx = player.currentMediaItemIndex.coerceIn(0, itemCount - 1)
                if (duration > 0L) {
                    val itemProgress = (player.currentPosition.toFloat() / duration.toFloat())
                        .coerceIn(0f, 1f)
                    _playbackProgress.value =
                        ((currentIdx + itemProgress) / itemCount.toFloat()).coerceIn(0f, 1f)
                }
                delay(80L)
            }
        }
    }

    private fun startSimulatedProgress(
        estimatedDurationMs: Long,
        completeOnEnd: Boolean
    ) {
        progressJob?.cancel()
        progressJob = scope.launch {
            val steps = 40
            val stepDelay = estimatedDurationMs / steps
            for (i in 1..steps) {
                if (!isActive || isPaused) return@launch
                _playbackProgress.value = i / steps.toFloat()
                delay(stepDelay)
            }
            if (completeOnEnd && isActive && !isPaused) {
                _isPlaying.value = false
                _playbackProgress.value = 1f
                onPlaybackCompletedCallback?.invoke()
            } else if (!completeOnEnd && isActive && !isPaused) {
                // Safety net if device TTS engine never fires onDone
                delay(2500L)
                if (activeUtteranceId != null && isActive && !isPaused) {
                    activeUtteranceId = null
                    _isPlaying.value = false
                    _playbackProgress.value = 1f
                    onPlaybackCompletedCallback?.invoke()
                }
            }
        }
    }
}
