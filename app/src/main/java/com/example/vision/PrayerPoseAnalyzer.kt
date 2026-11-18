package com.example.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.lText
import com.google.mlkit.vision.pose.PoseLandmark
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Real-time CameraX ImageAnalysis.Analyzer backed by [PoseDetectionAnalyzerHelper]
 * and ML Kit Pose Detection (Accurate).
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
        private const val MIN_FRAME_INTERVAL_MS = 66L // ~15 FPS
        private const val DEBOUNCE_DURATION_MS = 400L
        private const val DARK_FRAMES_TRIGGER_COUNT = 25 // ~1.7-2.5s of dark frames
        private const val MIN_CONFIDENCE = 0.45f
    }

    @Volatile
    private var latestLuminance: Float = 128f

    @Volatile
    private var latestIsRoomTooDark: Boolean = false

    private var candidatePose: PrayerPose = PrayerPose.UNKNOWN
    private var candidateSinceMs: Long = 0L
    private var confirmedPose: PrayerPose = PrayerPose.UNKNOWN
    private var consecutiveDarkFrames = 0

    private val poseHelper = PoseDetectionAnalyzerHelper(
        minFrameIntervalMs = MIN_FRAME_INTERVAL_MS,
        onPreInspectFrame = { imageProxy ->
            val luminance = estimateLuminance(imageProxy)
            if (luminance < darkLuminanceThreshold) {
                consecutiveDarkFrames++
            } else {
                consecutiveDarkFrames = 0
            }
            latestLuminance = luminance
            latestIsRoomTooDark = consecutiveDarkFrames >= DARK_FRAMES_TRIGGER_COUNT
        },
        onLandmarksExtracted = { frameLandmarks ->
            val snapshot = evaluatePose(
                frameLandmarks = frameLandmarks,
                luminance = latestLuminance,
                isRoomTooDark = latestIsRoomTooDark
            )
            onPoseResult(snapshot)
        }
    )

    override fun analyze(imageProxy: ImageProxy) {
        poseHelper.analyze(imageProxy)
    }

    fun close() {
        poseHelper.close()
    }

    private fun evaluatePose(
        frameLandmarks: PoseFrameLandmarks,
        luminance: Float,
        isRoomTooDark: Boolean
    ): PoseAnalysisSnapshot {
        val nose = frameLandmarks.landmark(PoseLandmark.NOSE)
        val lShoulder = frameLandmarks.landmark(PoseLandmark.LEFT_SHOULDER)
        val rShoulder = frameLandmarks.landmark(PoseLandmark.RIGHT_SHOULDER)
        val lHip = frameLandmarks.landmark(PoseLandmark.LEFT_HIP)
        val rHip = frameLandmarks.landmark(PoseLandmark.RIGHT_HIP)
        val lKnee = frameLandmarks.landmark(PoseLandmark.LEFT_KNEE)
        val rKnee = frameLandmarks.landmark(PoseLandmark.RIGHT_KNEE)
        val lAnkle = frameLandmarks.landmark(PoseLandmark.LEFT_ANKLE)
        val rAnkle = frameLandmarks.landmark(PoseLandmark.RIGHT_ANKLE)
        val lWrist = frameLandmarks.landmark(PoseLandmark.LEFT_WRIST)
        val rWrist = frameLandmarks.landmark(PoseLandmark.RIGHT_WRIST)

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

        val frameHeight = frameLandmarks.frameHeight.coerceAtLeast(1)
        val headHeightRatio = if (nose != null && ankle != null) {
            abs(ankle.y - nose.y) / frameHeight.toFloat()
        } else if (nose != null && hip != null) {
            abs(hip.y - nose.y) / frameHeight.toFloat()
        } else {
            0.5f
        }

        val noseNormalizedY = nose?.normalizedY ?: 0.5f

        val rawPose = classifyRawPose(
            hasTorso = hasTorso,
            torsoAngle = torsoAngle,
            kneeAngle = kneeAngle,
            headHeightRatio = headHeightRatio,
            noseNormalizedY = noseNormalizedY,
            shoulderY = shoulder?.y,
            hipY = hip?.y,
            wristY = wrist?.y
        )

        // 400ms Debounce state filter
        if (rawPose != PrayerPose.UNKNOWN) {
            if (rawPose == candidatePose) {
                if ((frameLandmarks.timestampMs - candidateSinceMs) >= DEBOUNCE_DURATION_MS) {
                    confirmedPose = rawPose
                }
            } else {
                candidatePose = rawPose
                candidateSinceMs = frameLandmarks.timestampMs
            }
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
            landmarks = frameLandmarks.toNormalizedLandmarks(),
            imageWidth = frameLandmarks.frameWidth,
            imageHeight = frameLandmarks.frameHeight,
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
        leftA: ExtractedPoseLandmark?,
        leftB: ExtractedPoseLandmark?,
        leftC: ExtractedPoseLandmark?,
        rightA: ExtractedPoseLandmark?,
        rightB: ExtractedPoseLandmark?,
        rightC: ExtractedPoseLandmark?
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

    private fun isReliable(lm: ExtractedPoseLandmark?): Boolean =
        lm != null && lm.inFrameLikelihood >= MIN_CONFIDENCE

    private fun pickBestLandmark(
        a: ExtractedPoseLandmark?,
        b: ExtractedPoseLandmark?
    ): ExtractedPoseLandmark? {
        if (a == null) return b
        if (b == null) return a
        return if (a.inFrameLikelihood >= b.inFrameLikelihood) a else b
    }

    fun calculateAngle(
        firstPoint: ExtractedPoseLandmark,
        midPoint: ExtractedPoseLandmark,
        lastPoint: ExtractedPoseLandmark
    ): Float {
        val result = Math.toDegrees(
            atan2(
                (lastPoint.y - midPoint.y).toDouble(),
                (lastPoint.x - midPoint.x).toDouble()
            ) - atan2(
                (firstPoint.y - midPoint.y).toDouble(),
                (firstPoint.x - midPoint.x).toDouble()
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
