package com.example.vision

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.NormalizedLandmark
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.lText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Real-time CameraX ImageAnalysis.Analyzer backed by ML Kit Pose Detection (Accurate).
 *
 * Implements:
 * - Zero-Storage Policy: frames stay in RAM and imageProxy.close() is always invoked.
 * - 15 FPS rate limiting (~66ms minimum frame interval) for thermal/battery efficiency.
 * - Y-plane luminance sampling to detect dark rooms and trigger 'Faqat Audio' fallback.
 * - Geometric calculation of torso angle (SHOULDER-HIP-KNEE), knee angle (HIP-KNEE-ANKLE),
 *   head height coefficient (NOSE.y - ANKLE.y ratio), and wrist elevation.
 * - 400ms debounce filter before confirming Qiyom, Takbir, Ruku, Sajda, or Qada states.
 */
class PrayerPoseAnalyzer(
    @Volatile var isFrontCamera: Boolean = true,
    private val darkLuminanceThreshold: Float = 28f,
    private val onPoseResult: (PoseAnalysisSnapshot) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PrayerPoseAnalyzer"
        private const val MIN_FRAME_INTERVAL_MS = 66L // ~15 FPS
        private const val DEBOUNCE_DURATION_MS = 400L
        private const val DARK_FRAMES_TRIGGER_COUNT = 25 // ~1.7-2.5s of dark frames
        private const val MIN_CONFIDENCE = 0.45f
    }

    private val detector: PoseDetector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    @Volatile
    private var lastProcessedTimeMs = 0L

    @Volatile
    private var isBusy = false

    private var candidatePose: PrayerPose = PrayerPose.UNKNOWN
    private var candidateSinceMs: Long = 0L
    private var confirmedPose: PrayerPose = PrayerPose.UNKNOWN
    private var consecutiveDarkFrames = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (isBusy || (now - lastProcessedTimeMs) < MIN_FRAME_INTERVAL_MS) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isBusy = true
        lastProcessedTimeMs = now

        val luminance = estimateLuminance(imageProxy)
        if (luminance < darkLuminanceThreshold) {
            consecutiveDarkFrames++
        } else {
            consecutiveDarkFrames = 0
        }
        val isRoomTooDark = consecutiveDarkFrames >= DARK_FRAMES_TRIGGER_COUNT

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val uprightWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.height
        } else {
            imageProxy.width
        }
        val uprightHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.width
        } else {
            imageProxy.height
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                val snapshot = evaluatePose(
                    pose = pose,
                    frameWidth = uprightWidth.coerceAtLeast(1),
                    frameHeight = uprightHeight.coerceAtLeast(1),
                    luminance = luminance,
                    isRoomTooDark = isRoomTooDark,
                    timestampMs = SystemClock.elapsedRealtime()
                )
                onPoseResult(snapshot)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "ML Kit pose detection error", error)
            }
            .addOnCompleteListener {
                isBusy = false
                imageProxy.close()
            }
    }

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing pose detector", e)
        }
    }

    private fun evaluatePose(
        pose: Pose,
        frameWidth: Int,
        frameHeight: Int,
        luminance: Float,
        isRoomTooDark: Boolean,
        timestampMs: Long
    ): PoseAnalysisSnapshot {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val shoulder = pickBestLandmark(lShoulder, rShoulder)
        val hip = pickBestLandmark(lHip, rHip)
        val knee = pickBestLandmark(lKnee, rKnee)
        val ankle = pickBestLandmark(lAnkle, rAnkle)
        val wrist = pickBestLandmark(lWrist, rWrist)

        // Check full-body visibility for calibration
        val hasHead = nose != null && nose.inFrameLikelihood >= MIN_CONFIDENCE
        val hasTorso = shoulder != null && shoulder.inFrameLikelihood >= MIN_CONFIDENCE &&
            hip != null && hip.inFrameLikelihood >= MIN_CONFIDENCE
        val hasLegs = knee != null && knee.inFrameLikelihood >= MIN_CONFIDENCE &&
            ankle != null && ankle.inFrameLikelihood >= MIN_CONFIDENCE

        val isFullBodyVisible = hasHead && hasTorso && hasLegs

        val calibrationHint = when {
            isRoomTooDark -> lText(
                "Xona qorong‘i! Yorug‘likni yoqing yoki Faqat Audio rejimiga o‘ting",
                "Хона қоронғи! Ёруғликни ёқинг ёки Фақат Аудио режимига ўтинг"
            )
            !hasHead -> lText(
                "Boshingiz kadrga to‘liq tushishi uchun telefon burchagini sozlang",
                "Бошингиз кадрга тўлиқ тушиши учун телефон бурчагини созланг"
            )
            !hasTorso -> lText(
                "Kameraga ro‘para turing, gavdangiz aniq ko‘rinishi kerak",
                "Камерага рўпара туринг, гавдангиз аниқ кўриниши керак"
            )
            !hasLegs -> lText(
                "Oyoq va tizzalaringiz ko‘rinishi uchun 1-2 qadam orqaga suriling",
                "Оёқ ва тиззаларингиз кўриниши учун 1-2 қадам орқага сурилинг"
            )
            else -> lText(
                "Joyingiz qulay! Boshlash uchun tayyorsiz",
                "Жойингиз қулай! Бошлаш учун тайёрсиз"
            )
        }

        // Calculate geometric angles
        val torsoAngle = computeSideAverageAngle(
            leftA = lShoulder, leftB = lHip, leftC = lKnee,
            rightA = rShoulder, rightB = rHip, rightC = rKnee
        ) ?: if (shoulder != null && hip != null && knee != null) {
            calculateAngle(shoulder, hip, knee)
        } else 0f

        val kneeAngle = computeSideAverageAngle(
            leftA = lHip, leftB = lKnee, leftC = lAnkle,
            rightA = rHip, rightB = rKnee, rightC = rAnkle
        ) ?: if (hip != null && knee != null && ankle != null) {
            calculateAngle(hip, knee, ankle)
        } else 0f

        val headHeightRatio = if (nose != null && ankle != null) {
            abs(ankle.position.y - nose.position.y) / frameHeight.toFloat()
        } else if (nose != null && hip != null) {
            abs(hip.position.y - nose.position.y) / frameHeight.toFloat()
        } else {
            0.5f
        }

        val noseNormalizedY = if (nose != null) nose.position.y / frameHeight.toFloat() else 0.5f

        val rawPose = classifyRawPose(
            hasTorso = hasTorso,
            torsoAngle = torsoAngle,
            kneeAngle = kneeAngle,
            headHeightRatio = headHeightRatio,
            noseNormalizedY = noseNormalizedY,
            shoulderY = shoulder?.position?.y,
            hipY = hip?.position?.y,
            wristY = wrist?.position?.y
        )

        // 400ms Debounce state filter
        if (rawPose != PrayerPose.UNKNOWN) {
            if (rawPose == candidatePose) {
                if ((timestampMs - candidateSinceMs) >= DEBOUNCE_DURATION_MS) {
                    confirmedPose = rawPose
                }
            } else {
                candidatePose = rawPose
                candidateSinceMs = timestampMs
            }
        }

        val normalizedLandmarks = pose.allPoseLandmarks.map { lm ->
            NormalizedLandmark(
                type = lm.landmarkType,
                x = (lm.position.x / frameWidth.toFloat()).coerceIn(0f, 1f),
                y = (lm.position.y / frameHeight.toFloat()).coerceIn(0f, 1f),
                inFrameLikelihood = lm.inFrameLikelihood
            )
        }

        return PoseAnalysisSnapshot(
            confirmedPose = confirmedPose,
            rawPose = rawPose,
            torsoAngle = torsoAngle,
            kneeAngle = kneeAngle,
            headHeightRatio = headHeightRatio,
            isFullBodyVisible = isFullBodyVisible,
            calibrationHint = calibrationHint,
            averageLuminance = luminance,
            isLowLight = isRoomTooDark,
            landmarks = normalizedLandmarks,
            imageWidth = frameWidth,
            imageHeight = frameHeight,
            isFrontCamera = isFrontCamera
        )
    }

    /**
     * Implements PRD 2.3 mathematical classification rules:
     * - QIYOM: theta_torso in [160°, 185°], theta_knee > 160°, wrists near chest/navel
     * - TAKBIR: upright + wrists elevated near/above shoulders
     * - RUKU: theta_torso in [70°, 120°], theta_knee > 155°
     * - SAJDA: minimal vertical nose-to-ankle distance (< 0.18 frame height) & theta_knee < 68°
     * - QADA: theta_torso in [70°, 115°], theta_knee < 58° & nose in upper/mid zone
     */
    private fun classifyRawPose(
        hasTorso: Boolean,
        torsoAngle: Float,
        kneeAngle: Float,
        headHeightRatio: Float,
        noseNormalizedY: Float,
        shoulderY: Float?,
        hipY: Float?,
        wristY: Float?
    ): PrayerPose {
        if (!hasTorso) return PrayerPose.UNKNOWN

        // 1. Sajda check: low head height relative to ankle & bent knees
        if (headHeightRatio < 0.20f && kneeAngle in 1f..68f) {
            return PrayerPose.SAJDA
        }
        // Fallback Sajda when knees occluded by loose garments: nose is very low in frame
        if (noseNormalizedY > 0.72f && torsoAngle in 1f..95f) {
            return PrayerPose.SAJDA
        }

        // 2. Qada / Sitting check: torso upright relative to thigh (70..115), knees deeply bent (<58)
        if (torsoAngle in 68f..118f && kneeAngle in 1f..58f && noseNormalizedY < 0.70f) {
            return PrayerPose.QADA
        }

        // 3. Ruku check: torso bent forward (55..125), knees extended (>= 140 to accommodate Hanafi female posture)
        if (torsoAngle in 55f..125f && kneeAngle >= 140f) {
            return PrayerPose.RUKU
        }

        // 4. Upright standing: Takbir vs Qiyom
        if (torsoAngle >= 155f && kneeAngle >= 155f) {
            if (wristY != null && shoulderY != null && hipY != null) {
                // In image coords, smaller Y means higher up
                val earOrShoulderZone = shoulderY + (hipY - shoulderY) * 0.15f
                if (wristY <= earOrShoulderZone) {
                    return PrayerPose.TAKBIR
                }
            }
            return PrayerPose.QIYOM
        }

        return PrayerPose.UNKNOWN
    }

    private fun computeSideAverageAngle(
        leftA: PoseLandmark?,
        leftB: PoseLandmark?,
        leftC: PoseLandmark?,
        rightA: PoseLandmark?,
        rightB: PoseLandmark?,
        rightC: PoseLandmark?
    ): Float? {
        val leftAngle = if (isReliable(leftA) && isReliable(leftB) && isReliable(leftC)) {
            calculateAngle(leftA!!, leftB!!, leftC!!)
        } else null

        val rightAngle = if (isReliable(rightA) && isReliable(rightB) && isReliable(rightC)) {
            calculateAngle(rightA!!, rightB!!, rightC!!)
        } else null

        return when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle != null -> leftAngle
            rightAngle != null -> rightAngle
            else -> null
        }
    }

    private fun isReliable(lm: PoseLandmark?): Boolean =
        lm != null && lm.inFrameLikelihood >= MIN_CONFIDENCE

    private fun pickBestLandmark(a: PoseLandmark?, b: PoseLandmark?): PoseLandmark? {
        if (a == null) return b
        if (b == null) return a
        return if (a.inFrameLikelihood >= b.inFrameLikelihood) a else b
    }

    fun calculateAngle(
        firstPoint: PoseLandmark,
        midPoint: PoseLandmark,
        lastPoint: PoseLandmark
    ): Float {
        val result = Math.toDegrees(
            atan2(
                (lastPoint.position.y - midPoint.position.y).toDouble(),
                (lastPoint.position.x - midPoint.position.x).toDouble()
            ) - atan2(
                (firstPoint.position.y - midPoint.position.y).toDouble(),
                (firstPoint.position.x - midPoint.position.x).toDouble()
            )
        )
        var angle = abs(result)
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle.toFloat()
    }

    /**
     * Fast Y-plane luminance estimation (0..255). Samples every 16th pixel for <0.5ms latency.
     */
    private fun estimateLuminance(imageProxy: ImageProxy): Float {
        val plane = imageProxy.planes.firstOrNull() ?: return 128f
        val buffer: ByteBuffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.rewind()
        buffer.get(data)
        if (data.isEmpty()) return 128f

        var sum = 0L
        var count = 0
        val step = 16
        var i = 0
        while (i < data.size) {
            sum += (data[i].toInt() and 0xFF)
            count++
            i += step
        }
        return if (count > 0) sum.toFloat() / count else 128f
    }
}
