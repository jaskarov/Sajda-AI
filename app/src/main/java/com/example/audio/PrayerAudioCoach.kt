package com.example.audio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.Gender
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

sealed interface AudioCoachStatus {
    data object Idle : AudioCoachStatus
    data object SynthesizingGeminiTts : AudioCoachStatus
    data class PlayingGeminiTts(val modelUsed: String) : AudioCoachStatus
    data class PlayingOfflineTts(val notice: String? = null) : AudioCoachStatus
    data class Error(val message: String) : AudioCoachStatus
}

/**
 * Prayer Audio Coach powered by Firebase AI Logic Gemini TTS (`gemini-3.8-flash-tts`)
 * and Media3 ExoPlayer, with automatic disk WAV caching and Android TextToSpeech fallback
 * for offline prayer rooms or depleted Gemini API billing credits.
 */
class PrayerAudioCoach(private val context: Context) {

    companion object {
        private const val TAG = "PrayerAudioCoach"
        private const val PRIMARY_TTS_MODEL = "gemini-3.8-flash-tts"
        private const val FALLBACK_TTS_MODEL = "gemini-3.1-flash-tts-preview"
        private const val TTS_TIMEOUT_MS = 30_000L

        @Volatile
        private var sessionGeminiBlockedNotice: String? = null

        fun clearSessionBlock() {
            sessionGeminiBlockedNotice = null
        }
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
    private var activeFallbackNotice: String? = null
    private var activeText: String = ""
    private var activeSpeed: Float = 1.0f
    private var isPaused: Boolean = false

    private val cacheDir: File by lazy {
        File(context.cacheDir, "gemini_tts_wav").apply { mkdirs() }
    }

    init {
        initExoPlayer()
        initLocalTts()
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
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
                        Locale("uz", "UZ"),
                        Locale("tr", "TR"),
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
        useGeminiTts: Boolean,
        speed: Float = 1.0f,
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
            val blockedNotice = sessionGeminiBlockedNotice
            if (useGeminiTts && blockedNotice == null) {
                try {
                    _status.value = AudioCoachStatus.SynthesizingGeminiTts
                    val voiceName = if (gender == Gender.FEMALE) "Sulafat" else "Charon"
                    val cachedWav = getOrSynthesizeWav(text, voiceName)
                    if (!isActive) return@launch
                    playWavFile(cachedWav, speed, PRIMARY_TTS_MODEL)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val friendlyReason = mapFriendlyError(e)
                    if (isBillingOrQuotaError(e)) {
                        sessionGeminiBlockedNotice = friendlyReason
                        Log.w(TAG, "Gemini TTS billing/quota unavailable; switching session to device TTS: $friendlyReason")
                    } else {
                        Log.w(TAG, "Gemini TTS failed, falling back to device TTS: ${e.localizedMessage}")
                    }
                    speakWithLocalTts(text, speed, notice = friendlyReason)
                }
            } else {
                speakWithLocalTts(text, speed, notice = if (useGeminiTts) blockedNotice else null)
            }
        }
    }

    private suspend fun getOrSynthesizeWav(promptText: String, voiceName: String): File {
        val cacheKey = sha256("${PRIMARY_TTS_MODEL}_${voiceName}_$promptText")
        val targetFile = File(cacheDir, "$cacheKey.wav")
        if (targetFile.exists() && targetFile.length() > 44L) {
            return targetFile
        }

        val pcmBytes = try {
            requestGeminiTtsBytes(PRIMARY_TTS_MODEL, promptText, voiceName)
        } catch (primaryErr: Exception) {
            if (primaryErr is CancellationException) throw primaryErr
            if (isBillingOrQuotaError(primaryErr)) throw primaryErr
            Log.w(TAG, "Primary model $PRIMARY_TTS_MODEL failed, trying $FALLBACK_TTS_MODEL")
            requestGeminiTtsBytes(FALLBACK_TTS_MODEL, promptText, voiceName)
        }

        return withContext(Dispatchers.IO) {
            val wavBytes = wrapPcmAsWav(pcmBytes, sampleRate = 24000, channels = 1, bitsPerSample = 16)
            FileOutputStream(targetFile).use { it.write(wavBytes) }
            targetFile
        }
    }

    @OptIn(PublicPreviewAPI::class)
    private suspend fun requestGeminiTtsBytes(
        modelName: String,
        promptText: String,
        voiceName: String
    ): ByteArray {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO)
            speechConfig = SpeechConfig(
                voice = Voice(voiceName)
            )
        }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = modelName,
                generationConfig = config
            )

        val styledPrompt =
            "Read calmly, reverently, and clearly at a measured prayer teaching pace: $promptText"

        val response = withTimeout(TTS_TIMEOUT_MS) {
            model.generateContent(styledPrompt)
        }

        val audioStream = ByteArrayOutputStream()
        val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        for (part in parts) {
            if (part is InlineDataPart) {
                audioStream.write(part.inlineData)
            }
        }
        val result = audioStream.toByteArray()
        if (result.isEmpty()) {
            throw IllegalStateException("Gemini TTS bo‘sh audio qaytardi.")
        }
        return result
    }

    private fun playWavFile(wavFile: File, speed: Float, modelLabel: String) {
        val player = exoPlayer ?: return
        _status.value = AudioCoachStatus.PlayingGeminiTts(modelLabel)
        player.playbackParameters = PlaybackParameters(speed)
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(wavFile)))
        player.prepare()
        player.play()
    }

    private fun speakWithLocalTts(text: String, speed: Float, notice: String?) {
        activeFallbackNotice = notice
        _status.value = AudioCoachStatus.PlayingOfflineTts(notice)

        var ttsStarted = false
        if (isLocalTtsReady) {
            val utteranceId = UUID.randomUUID().toString()
            activeUtteranceId = utteranceId
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
            speakWithLocalTts(activeText, activeSpeed, activeFallbackNotice)
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
                if (duration > 0L) {
                    _playbackProgress.value =
                        (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
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

    private fun isBillingOrQuotaError(error: Throwable): Boolean {
        val msg = (error.message ?: "") + " " + (error.cause?.message ?: "")
        val lower = msg.lowercase(Locale.US)
        return lower.contains("prepayment") ||
            lower.contains("credits are depleted") ||
            lower.contains("billing") ||
            lower.contains("quota") ||
            lower.contains("resource_exhausted")
    }

    private fun mapFriendlyError(error: Throwable): String {
        val lower = (error.message ?: "").lowercase(Locale.US)
        return when {
            lower.contains("prepayment") || lower.contains("credits are depleted") || lower.contains("billing") ->
                "Gemini API krediti tugagan"
            lower.contains("quota") || lower.contains("429") ->
                "Gemini so‘rov limiti tugadi"
            else ->
                "Oflayn ovozli rejim faol"
        }
    }

    private fun wrapPcmAsWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1.toShort())
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, (channels * bitsPerSample / 8).toShort())
        writeShortLE(header, 34, bitsPerSample.toShort())
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcmData.size)

        return header + pcmData
    }

    private fun writeIntLE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buffer: ByteArray, offset: Int, value: Short) {
        buffer[offset] = (value.toInt() and 0xFF).toByte()
        buffer[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }
}
