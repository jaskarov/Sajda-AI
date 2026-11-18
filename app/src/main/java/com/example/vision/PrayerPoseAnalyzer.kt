package com.example.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerPose
import com.example.domain.model.lText
import com.google.mlkit.vision.pose.PoseLandmark
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Real-time CameraX ImageAnalysis.Analyzer backed by [PoseDetectionAnalyzerHelper]
 * and ML Kit Pose Detection (Accurate).
 *
 * Implements:
 * - Zero-Storage Policy: frames stay in RAM and imageProxy.close() is always invoked.
 * - 15 FPS rate limiting (~66ms minimum frame interval) for thermal/battery efficiency.
 * - Y-plane luminance sampling to detect dark rooms and trigger 'Faqat Audio' fallback.
 * - 3D spatial vector dot-product angles (SHOULDER-HIP-KNEE, HIP-KNEE-ANKLE) plus frontal view rules
 *   for Ruku (40-55% height drop approaching hip level), Sajda (nose/shoulders Y > 0.75 + bent knees),
 *   and Salam (left/right head turn via NOSE, LEFT_EAR, RIGHT_EAR asymmetry).
 * - 400ms debounce filter before confirming Qiyom, Takbir, Ruku, Sajda, Qada, or Salam states.
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

        const val FRONTAL_RUKU_SPAN_RETENTION_MAX = 0.60f // >= 40% vertical drop vs Qiyom
        const val FRONTAL_RUKU_HEIGHT_MIN_RATIO = 0.30f
        const val FRONTAL_RUKU_HEIGHT_MAX_RATIO = 0.62f   // 38%..70% upper-body height drop
        const val FRONTAL_SAJDA_NOSE_Y_MIN = 0.75f
        const val FRONTAL_SAJDA_SHOULDER_Y_MIN = 0.70f
        const val SALAM_YAW_ASYMMETRY_THRESHOLD = 0.42f

        /**
         * Calculates the 3D angle (0..180 degrees) at [midPoint] between vectors
         * (firstPoint - midPoint) and (lastPoint - midPoint) via vector dot product.
         */
        fun calculateAngle3D(
            firstPoint: ExtractedPoseLandmark,
            midPoint: ExtractedPoseLandmark,
            lastPoint: ExtractedPoseLandmark
        ): Float {
            val v1x = (firstPoint.x - midPoint.x).toDouble()
            val v1y = (firstPoint.y - midPoint.y).toDouble()
            val v1z = (firstPoint.z - midPoint.z).toDouble()

            val v2x = (lastPoint.x - midPoint.x).toDouble()
            val v2y = (lastPoint.y - midPoint.y).toDouble()
            val v2z = (lastPoint.z - midPoint.z).toDouble()

            val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
            val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
            if (mag1 < 1e-4 || mag2 < 1e-4) return 0f

            val dot = v1x * v2x + v1y * v2y + v1z * v2z
            val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosTheta)).toFloat()
        }

        /**
         * Detects left/right head turn (Salam) from horizontal asymmetry between NOSE and EARS.
         */
        fun isHeadTurnedForSalam(
            nose: ExtractedPoseLandmark?,
            leftEar: ExtractedPoseLandmark?,
            rightEar: ExtractedPoseLandmark?,
            leftShoulder: ExtractedPoseLandmark? = null,
            rightShoulder: ExtractedPoseLandmark? = null,
            frameWidth: Int = 480
        ): Boolean {
            if (!isReliableStatic(nose)) return false
            val n = nose!!
            val hasLeft = isReliableStatic(leftEar)
            val hasRight = isReliableStatic(rightEar)

            if (hasLeft && hasRight) {
                val dLeft = abs(n.x - leftEar!!.x)
                val dRight = abs(n.x - rightEar!!.x)
                val total = dLeft + dRight
                if (total > 1e-3f) {
                    val asymmetry = abs(dLeft - dRight) / total
                    if (asymmetry >= SALAM_YAW_ASYMMETRY_THRESHOLD) return true
                }
            } else if (hasLeft xor hasRight) {
                // When turned sharply sideways, one ear is occluded while shoulders face camera
                val visibleEar = (if (hasLeft) leftEar else rightEar)!!
                val shoulderSpan = if (isReliableStatic(leftShoulder) && isReliableStatic(rightShoulder)) {
                    abs(leftShoulder!!.x - rightShoulder!!.x)
                } else 0f
                if (shoulderSpan >= frameWidth * 0.14f) {
                    val noseEarDx = abs(n.x - visibleEar.x)
                    if (noseEarDx >= shoulderSpan * 0.18f) return true
                }
            }
            return false
        }

        private fun isReliableStatic(lm: ExtractedPoseLandmark?): Boolean =
            lm != null && lm.inFrameLikelihood >= MIN_CONFIDENCE
    }

    @Volatile
    private var latestLuminance: Float = 128f

    @Volatile
    private var latestIsRoomTooDark: Boolean = false

    private var candidatePose: PrayerPose = PrayerPose.UNKNOWN
    private var candidateSinceMs: Long = 0L
    private var confirmedPose: PrayerPose = PrayerPose.UNKNOWN
    private var consecutiveDarkFrames = 0

    // Qiyom standing height baselines for frontal Ruku detection
    private var baselineTorsoSpanNorm: Float? = null
    private var baselineNoseAboveAnkleNorm: Float? = null
    private var baselineShoulderAboveAnkleNorm: Float? = null

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
            val snapshot = evaluateFrame(
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

    fun evaluateFrame(
        frameLandmarks: PoseFrameLandmarks,
        luminance: Float = 128f,
        isRoomTooDark: Boolean = false
    ): PoseAnalysisSnapshot {
        val nose = frameLandmarks.landmark(PoseLandmark.NOSE)
        val lEar = frameLandmarks.landmark(PoseLandmark.LEFT_EAR)
        val rEar = frameLandmarks.landmark(PoseLandmark.RIGHT_EAR)
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

        // Calculate 3D geometric angles using vector dot product (position3D x, y, z)
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

        val avgShoulderNormY = averageNormalizedY(lShoulder, rShoulder) ?: shoulder?.normalizedY ?: 0.5f
        val avgHipNormY = averageNormalizedY(lHip, rHip) ?: hip?.normalizedY ?: 0.6f
        val avgAnkleNormY = averageNormalizedY(lAnkle, rAnkle) ?: ankle?.normalizedY
        val noseNormalizedY = nose?.normalizedY ?: 0.5f

        val currentTorsoSpanNorm = if (hasTorso) abs(avgHipNormY - avgShoulderNormY) else null
        val currentNoseAboveAnkleNorm = if (nose != null && avgAnkleNormY != null) {
            (avgAnkleNormY - noseNormalizedY).coerceAtLeast(0f)
        } else null
        val currentShoulderAboveAnkleNorm = if (shoulder != null && avgAnkleNormY != null) {
            (avgAnkleNormY - avgShoulderNormY).coerceAtLeast(0f)
        } else null

        val isFrontalRuku = detectFrontalRuku(
            hasTorso = hasTorso,
            kneeAngle = kneeAngle,
            currentTorsoSpanNorm = currentTorsoSpanNorm,
            currentNoseAboveAnkleNorm = currentNoseAboveAnkleNorm,
            currentShoulderAboveAnkleNorm = currentShoulderAboveAnkleNorm,
            avgShoulderNormY = avgShoulderNormY,
            avgHipNormY = avgHipNormY,
            avgAnkleNormY = avgAnkleNormY
        )

        val isHeadTurned = isHeadTurnedForSalam(
            nose = nose,
            leftEar = lEar,
            rightEar = rEar,
            leftShoulder = lShoulder,
            rightShoulder = rShoulder,
            frameWidth = frameLandmarks.frameWidth
        )

        val rawPose = classifyRawPose(
            hasTorso = hasTorso,
            hasLegs = hasLegs,
            torsoAngle = torsoAngle,
            kneeAngle = kneeAngle,
            headHeightRatio = headHeightRatio,
            noseNormalizedY = noseNormalizedY,
            shoulderNormalizedY = avgShoulderNormY,
            hipNormalizedY = avgHipNormY,
            shoulderY = shoulder?.y,
            hipY = hip?.y,
            wristY = wrist?.y,
            isFrontalRuku = isFrontalRuku,
            isHeadTurnedForSalam = isHeadTurned
        )

        // Calibrate upright Qiyom standing height baselines dynamically while user stands straight
        if (rawPose == PrayerPose.QIYOM || rawPose == PrayerPose.TAKBIR || rawPose == PrayerPose.QAVMA) {
            if (currentTorsoSpanNorm != null && currentTorsoSpanNorm >= 0.14f) {
                baselineTorsoSpanNorm = blendBaseline(baselineTorsoSpanNorm, currentTorsoSpanNorm)
            }
            if (currentNoseAboveAnkleNorm != null && currentNoseAboveAnkleNorm >= 0.35f) {
                baselineNoseAboveAnkleNorm = blendBaseline(baselineNoseAboveAnkleNorm, currentNoseAboveAnkleNorm)
            }
            if (currentShoulderAboveAnkleNorm != null && currentShoulderAboveAnkleNorm >= 0.25f) {
                baselineShoulderAboveAnkleNorm = blendBaseline(baselineShoulderAboveAnkleNorm, currentShoulderAboveAnkleNorm)
            }
        }

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

    private fun detectFrontalRuku(
        hasTorso: Boolean,
        kneeAngle: Float,
        currentTorsoSpanNorm: Float?,
        currentNoseAboveAnkleNorm: Float?,
        currentShoulderAboveAnkleNorm: Float?,
        avgShoulderNormY: Float,
        avgHipNormY: Float,
        avgAnkleNormY: Float?
    ): Boolean {
        if (!hasTorso || kneeAngle < 135f) return false

        // 1. Compare current torso span & height against calibrated Qiyom standing baseline (40-55%+ drop)
        val baseTorso = baselineTorsoSpanNorm
        val shoulderApproachesHip = currentTorsoSpanNorm != null &&
            baseTorso != null &&
            baseTorso > 0.12f &&
            currentTorsoSpanNorm <= baseTorso * FRONTAL_RUKU_SPAN_RETENTION_MAX

        val baseNoseH = baselineNoseAboveAnkleNorm
        val noseDroppedBy40To55Pct = currentNoseAboveAnkleNorm != null &&
            baseNoseH != null &&
            baseNoseH > 0.25f &&
            (currentNoseAboveAnkleNorm / baseNoseH) in FRONTAL_RUKU_HEIGHT_MIN_RATIO..FRONTAL_RUKU_HEIGHT_MAX_RATIO

        val baseShoulderH = baselineShoulderAboveAnkleNorm
        val shoulderDroppedBy40To55Pct = currentShoulderAboveAnkleNorm != null &&
            baseShoulderH != null &&
            baseShoulderH > 0.20f &&
            (currentShoulderAboveAnkleNorm / baseShoulderH) in FRONTAL_RUKU_HEIGHT_MIN_RATIO..FRONTAL_RUKU_HEIGHT_MAX_RATIO

        if (shoulderApproachesHip || noseDroppedBy40To55Pct || shoulderDroppedBy40To55Pct) {
            return true
        }

        // 2. Baseline-free frontal fallback when user enters frame already bent in Ruku
        if (avgAnkleNormY != null && currentTorsoSpanNorm != null) {
            val legSpan = (avgAnkleNormY - avgHipNormY).coerceAtLeast(0f)
            if (legSpan >= 0.22f && currentTorsoSpanNorm <= legSpan * 0.42f && avgShoulderNormY <= avgHipNormY + 0.08f) {
                return true
            }
        }
        return false
    }

    private fun classifyRawPose(
        hasTorso: Boolean,
        hasLegs: Boolean,
        torsoAngle: Float,
        kneeAngle: Float,
        headHeightRatio: Float,
        noseNormalizedY: Float,
        shoulderNormalizedY: Float,
        hipNormalizedY: Float,
        shoulderY: Float?,
        hipY: Float?,
        wristY: Float?,
        isFrontalRuku: Boolean,
        isHeadTurnedForSalam: Boolean
    ): PrayerPose {
        if (!hasTorso) return PrayerPose.UNKNOWN

        // 1. Sajda check (side-view OR frontal view where nose & shoulders drop to bottom Y > 0.75 + knees bent)
        if (headHeightRatio < 0.20f && kneeAngle in 1f..68f) {
            return PrayerPose.SAJDA
        }
        if (noseNormalizedY > FRONTAL_SAJDA_NOSE_Y_MIN &&
            shoulderNormalizedY > FRONTAL_SAJDA_SHOULDER_Y_MIN &&
            (kneeAngle in 1f..130f || !hasLegs)
        ) {
            return PrayerPose.SAJDA
        }
        if (noseNormalizedY > 0.72f && torsoAngle in 1f..95f) {
            return PrayerPose.SAJDA
        }

        // 2. Salam check while seated (head turned right/left via nose-ear horizontal asymmetry)
        val isSeatedPosture = (kneeAngle in 1f..115f && noseNormalizedY < 0.74f) ||
            (!hasLegs && hipNormalizedY >= 0.58f && noseNormalizedY < 0.74f)
        if (isSeatedPosture && isHeadTurnedForSalam) {
            return PrayerPose.SALAM
        }

        // 3. Qada / Sitting check: side-view (torso 65..120, knee < 62) OR frontal foreshortened Qa'da
        val isFrontalQada = hipNormalizedY >= 0.60f &&
            noseNormalizedY < 0.68f &&
            torsoAngle in 65f..148f &&
            kneeAngle in 1f..115f
        if ((torsoAngle in 65f..120f && kneeAngle in 1f..62f && noseNormalizedY < 0.72f) || isFrontalQada) {
            return PrayerPose.QADA
        }

        // 4. Ruku check: 3D spatial torso bend OR frontal height-drop Ruku while knees remain straight
        if ((torsoAngle in 52f..125f && kneeAngle >= 135f) || isFrontalRuku) {
            return PrayerPose.RUKU
        }

        // 5. Upright standing: Takbir vs Qiyom
        if (torsoAngle >= 150f && kneeAngle >= 150f) {
            if (wristY != null && shoulderY != null && hipY != null) {
                val earOrShoulderZone = shoulderY + (hipY - shoulderY) * 0.15f
                if (wristY <= earOrShoulderZone) {
                    return PrayerPose.TAKBIR
                }
            }
            return PrayerPose.QIYOM
        }

        return PrayerPose.UNKNOWN
    }

    private fun averageNormalizedY(
        a: ExtractedPoseLandmark?,
        b: ExtractedPoseLandmark?
    ): Float? {
        val aOk = isReliable(a)
        val bOk = isReliable(b)
        return when {
            aOk && bOk -> (a!!.normalizedY + b!!.normalizedY) / 2f
            aOk -> a!!.normalizedY
            bOk -> b!!.normalizedY
            else -> null
        }
    }

    private fun blendBaseline(current: Float?, sample: Float): Float {
        return if (current == null) sample else (current * 0.85f + sample * 0.15f)
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
    ): Float = calculateAngle3D(firstPoint, midPoint, lastPoint)

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
