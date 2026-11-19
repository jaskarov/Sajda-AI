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
import kotlin.math.atan2
import kotlin.math.max
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
    @Volatile var seatedMode: Boolean = false,
    @Volatile var lowPowerMode: Boolean = false,
    private val onPoseResult: (PoseAnalysisSnapshot) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val MIN_FRAME_INTERVAL_MS = 66L // ~15 FPS
        private const val LOW_POWER_FRAME_INTERVAL_MS = 125L // ~8 FPS for budget phones
        private const val DEBOUNCE_DURATION_MS = 400L
        private const val DARK_FRAMES_TRIGGER_COUNT = 25 // ~1.7-2.5s of dark frames
        private const val MIN_CONFIDENCE = 0.45f

        const val FRONTAL_RUKU_SPAN_RETENTION_MAX = 0.60f // >= 40% vertical drop vs Qiyom
        const val FRONTAL_RUKU_HEIGHT_MIN_RATIO = 0.30f
        const val FRONTAL_RUKU_HEIGHT_MAX_RATIO = 0.62f   // 38%..70% upper-body height drop
        const val FRONTAL_SAJDA_NOSE_Y_MIN = 0.75f
        const val FRONTAL_SAJDA_SHOULDER_Y_MIN = 0.70f
        const val SALAM_YAW_ASYMMETRY_THRESHOLD = 0.42f

        const val SEATED_RUKU_MIN_LEAN_DEG = 14f
        const val SEATED_RUKU_MAX_LEAN_DEG = 36f
        const val SEATED_SAJDA_MIN_LEAN_DEG = 41f

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
         * Detects signed left/right head turn direction for Salam:
         *  -1 = turned to one side (left), +1 = turned to opposite side (right), 0 = centered.
         * Supports ear-asymmetry, single-ear occlusion, AND hijab/scarf (ro'mol) fallback
         * using eye-nose asymmetry and nose offset from shoulder midpoint.
         */
        fun detectHeadYawDirection(
            nose: ExtractedPoseLandmark?,
            leftEar: ExtractedPoseLandmark?,
            rightEar: ExtractedPoseLandmark?,
            leftShoulder: ExtractedPoseLandmark? = null,
            rightShoulder: ExtractedPoseLandmark? = null,
            frameWidth: Int = 480,
            leftEye: ExtractedPoseLandmark? = null,
            rightEye: ExtractedPoseLandmark? = null
        ): Int {
            if (!isReliableStatic(nose)) return 0
            val n = nose!!
            val hasLeftEar = isReliableStatic(leftEar)
            val hasRightEar = isReliableStatic(rightEar)

            if (hasLeftEar && hasRightEar) {
                val dLeft = abs(n.x - leftEar!!.x)
                val dRight = abs(n.x - rightEar!!.x)
                val total = dLeft + dRight
                if (total > 1e-3f) {
                    val asymmetry = abs(dLeft - dRight) / total
                    if (asymmetry >= SALAM_YAW_ASYMMETRY_THRESHOLD) {
                        return if (dRight < dLeft) 1 else -1
                    }
                }
            } else if (hasLeftEar xor hasRightEar) {
                // One ear occluded by sharp turn or partial headscarf
                val visibleEar = (if (hasLeftEar) leftEar else rightEar)!!
                val shoulderSpan = if (isReliableStatic(leftShoulder) && isReliableStatic(rightShoulder)) {
                    abs(leftShoulder!!.x - rightShoulder!!.x)
                } else 0f
                if (shoulderSpan >= frameWidth * 0.14f) {
                    val noseEarDx = abs(n.x - visibleEar.x)
                    if (noseEarDx >= shoulderSpan * 0.18f) {
                        return if (n.x >= visibleEar.x) 1 else -1
                    }
                }
            }

            // Hijab / Scarf (ro'mol) fallback when ears are covered:
            // 1) Eye-to-nose horizontal asymmetry or single-eye visibility
            val hasLeftEye = isReliableStatic(leftEye)
            val hasRightEye = isReliableStatic(rightEye)
            val shoulderSpan = if (isReliableStatic(leftShoulder) && isReliableStatic(rightShoulder)) {
                abs(leftShoulder!!.x - rightShoulder!!.x)
            } else 0f

            if (hasLeftEye && hasRightEye) {
                val eLeft = abs(n.x - leftEye!!.x)
                val eRight = abs(n.x - rightEye!!.x)
                val totalEye = eLeft + eRight
                if (totalEye > 1e-3f) {
                    val eyeAsym = abs(eLeft - eRight) / totalEye
                    if (eyeAsym >= 0.36f) {
                        return if (eRight < eLeft) 1 else -1
                    }
                }
            } else if ((hasLeftEye xor hasRightEye) && shoulderSpan >= frameWidth * 0.14f) {
                val visibleEye = (if (hasLeftEye) leftEye else rightEye)!!
                if (abs(n.x - visibleEye.x) >= shoulderSpan * 0.08f) {
                    return if (n.x >= visibleEye.x) 1 else -1
                }
            }

            // 2) Shoulder-midpoint nose displacement when both ears are fully veiled
            if (shoulderSpan >= frameWidth * 0.14f) {
                val shoulderMidX = (leftShoulder!!.x + rightShoulder!!.x) / 2f
                val offset = n.x - shoulderMidX
                if (abs(offset) >= shoulderSpan * 0.22f) {
                    return if (offset >= 0f) 1 else -1
                }
            }
            return 0
        }

        /**
         * Detects left/right head turn (Salam) from horizontal asymmetry between NOSE and EARS,
         * with hijab/scarf fallback when ears are occluded.
         */
        fun isHeadTurnedForSalam(
            nose: ExtractedPoseLandmark?,
            leftEar: ExtractedPoseLandmark?,
            rightEar: ExtractedPoseLandmark?,
            leftShoulder: ExtractedPoseLandmark? = null,
            rightShoulder: ExtractedPoseLandmark? = null,
            frameWidth: Int = 480,
            leftEye: ExtractedPoseLandmark? = null,
            rightEye: ExtractedPoseLandmark? = null
        ): Boolean = detectHeadYawDirection(
            nose = nose,
            leftEar = leftEar,
            rightEar = rightEar,
            leftShoulder = leftShoulder,
            rightShoulder = rightShoulder,
            frameWidth = frameWidth,
            leftEye = leftEye,
            rightEye = rightEye
        ) != 0

        /**
         * Calculates forward lean angle (0..90+ degrees from upright vertical) for Seated/Chair mode
         * using 3D shoulder-hip and nose-hip vectors.
         */
        fun calculateForwardLeanDegrees(
            nose: ExtractedPoseLandmark?,
            shoulder: ExtractedPoseLandmark?,
            hip: ExtractedPoseLandmark?
        ): Float {
            val reliableHip = hip?.takeIf { isReliableStatic(it) } ?: return 0f
            val reliableShoulder = shoulder?.takeIf { isReliableStatic(it) }
            val reliableNose = nose?.takeIf { isReliableStatic(it) }

            var maxLean = 0f
            if (reliableShoulder != null) {
                val dx = (reliableShoulder.x - reliableHip.x).toDouble()
                val dz = (reliableShoulder.z - reliableHip.z).toDouble()
                val dy = (reliableHip.y - reliableShoulder.y).toDouble().coerceAtLeast(1.0)
                val horiz = sqrt(dx * dx + dz * dz)
                val shoulderLean = Math.toDegrees(atan2(horiz, dy)).toFloat()
                maxLean = max(maxLean, shoulderLean)
            }
            if (reliableNose != null) {
                val dx = (reliableNose.x - reliableHip.x).toDouble()
                val dz = (reliableNose.z - reliableHip.z).toDouble()
                val dy = (reliableHip.y - reliableNose.y).toDouble().coerceAtLeast(1.0)
                val horiz = sqrt(dx * dx + dz * dz)
                val noseLean = Math.toDegrees(atan2(horiz, dy)).toFloat()
                maxLean = max(maxLean, noseLean)
            }
            return maxLean
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
    private var baselineSeatedUpperSpanNorm: Float? = null

    private val poseHelper = PoseDetectionAnalyzerHelper(
        minFrameIntervalMs = MIN_FRAME_INTERVAL_MS,
        frameIntervalProvider = {
            if (lowPowerMode) LOW_POWER_FRAME_INTERVAL_MS else MIN_FRAME_INTERVAL_MS
        },
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
        val lEye = frameLandmarks.landmark(PoseLandmark.LEFT_EYE)
        val rEye = frameLandmarks.landmark(PoseLandmark.RIGHT_EYE)
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

        val isSeated = seatedMode
        // Head + torso suffices for reliable pose coaching (supports small rooms & long prayer dresses
        // where knees/ankles may be veiled or outside the lower frame edge).
        val isFullBodyVisible = hasHead && hasTorso

        val calibrationHint = when {
            isRoomTooDark -> lText(
                "Xona qorong‘i! Yorug‘likni yoqing yoki Faqat Audio rejimiga o‘ting",
                "Хона қоронғи! Ёруғликни ёқинг ёки Фақат Аудио режимига ўтинг"
            )
            !hasHead -> lText(
                if (isSeated) "Stulda o‘tiring — boshingiz kadrga to‘liq tushishi uchun kamerani sozlang"
                else "Boshingiz kadrga to‘liq tushishi uchun telefon burchagini sozlang",
                if (isSeated) "Стулда ўтиринг — бошингиз кадрга тўлиқ тушиши учун камерани созланг"
                else "Бошингиз кадрга тўлиқ тушиши учун телефон бурчагини созланг"
            )
            !hasTorso -> lText(
                if (isSeated) "Stulda kameraga qarab o‘tiring, yelkangiz va belingiz ko‘rinsin"
                else "Kameraga ro‘para turing, yelkangiz va belingiz ko‘rinishi yetarli",
                if (isSeated) "Стулда камерага қараб ўтиринг, елкангиз ва белингиз кўринсин"
                else "Камерага рўпара туринг, елкангиз ва белингиз кўриниши етарли"
            )
            else -> lText(
                if (isSeated) "Joyingiz qulay! Stulda tik o‘tirib boshlashingiz mumkin"
                else "Joyingiz qulay! Boshlash uchun tayyorsiz",
                if (isSeated) "Жойингиз қулай! Стулда тик ўтириб бошлашингиз мумкин"
                else "Жойингиз қулай! Бошлаш учун тайёрсиз"
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
            hasLegs = hasLegs,
            kneeAngle = kneeAngle,
            currentTorsoSpanNorm = currentTorsoSpanNorm,
            currentNoseAboveAnkleNorm = currentNoseAboveAnkleNorm,
            currentShoulderAboveAnkleNorm = currentShoulderAboveAnkleNorm,
            avgShoulderNormY = avgShoulderNormY,
            avgHipNormY = avgHipNormY,
            avgAnkleNormY = avgAnkleNormY
        )

        val headYawDirection = detectHeadYawDirection(
            nose = nose,
            leftEar = lEar,
            rightEar = rEar,
            leftShoulder = lShoulder,
            rightShoulder = rShoulder,
            frameWidth = frameLandmarks.frameWidth,
            leftEye = lEye,
            rightEye = rEye
        )
        val isHeadTurned = headYawDirection != 0

        val rawPose = if (isSeated) {
            val currentUpperSpanNorm = if (hasHead && hasTorso) {
                (avgHipNormY - noseNormalizedY).coerceAtLeast(0f)
            } else null

            val lean3dDeg = calculateForwardLeanDegrees(
                nose = nose,
                shoulder = shoulder,
                hip = hip
            )
            val baseSeated = baselineSeatedUpperSpanNorm
            val frontalBowDeg = if (currentUpperSpanNorm != null &&
                baseSeated != null &&
                baseSeated > 0.14f &&
                currentUpperSpanNorm < baseSeated * 0.97f
            ) {
                val cosRatio = (currentUpperSpanNorm / baseSeated).coerceIn(0f, 1f)
                Math.toDegrees(acos(cosRatio.toDouble())).toFloat()
            } else {
                0f
            }
            val effectiveLeanDeg = max(lean3dDeg, frontalBowDeg)

            if (effectiveLeanDeg < 12f && currentUpperSpanNorm != null && currentUpperSpanNorm >= 0.14f) {
                if (baseSeated == null || currentUpperSpanNorm >= baseSeated * 0.96f) {
                    baselineSeatedUpperSpanNorm = blendBaseline(baseSeated, currentUpperSpanNorm)
                }
            }

            classifySeatedRawPose(
                hasTorso = hasTorso,
                effectiveLeanDeg = effectiveLeanDeg,
                shoulderY = shoulder?.y,
                hipY = hip?.y,
                wristY = wrist?.y,
                isHeadTurnedForSalam = isHeadTurned
            )
        } else {
            val classified = classifyRawPose(
                hasTorso = hasTorso,
                hasLegs = hasLegs,
                torsoAngle = torsoAngle,
                kneeAngle = kneeAngle,
                headHeightRatio = headHeightRatio,
                noseNormalizedY = noseNormalizedY,
                shoulderNormalizedY = avgShoulderNormY,
                hipNormalizedY = avgHipNormY,
                currentTorsoSpanNorm = currentTorsoSpanNorm,
                shoulderY = shoulder?.y,
                hipY = hip?.y,
                wristY = wrist?.y,
                isFrontalRuku = isFrontalRuku,
                isHeadTurnedForSalam = isHeadTurned
            )

            // Calibrate upright Qiyom standing height baselines dynamically while user stands straight
            if (classified == PrayerPose.QIYOM || classified == PrayerPose.TAKBIR || classified == PrayerPose.QAVMA) {
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
            classified
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
            isFrontCamera = isFrontCamera,
            headYawDirection = headYawDirection
        )
    }

    private fun detectFrontalRuku(
        hasTorso: Boolean,
        hasLegs: Boolean,
        kneeAngle: Float,
        currentTorsoSpanNorm: Float?,
        currentNoseAboveAnkleNorm: Float?,
        currentShoulderAboveAnkleNorm: Float?,
        avgShoulderNormY: Float,
        avgHipNormY: Float,
        avgAnkleNormY: Float?
    ): Boolean {
        val kneesStraightOrCovered = if (hasLegs) kneeAngle >= 135f else true
        if (!hasTorso || !kneesStraightOrCovered) return false

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
        } else if (!hasLegs && currentTorsoSpanNorm != null && avgHipNormY < 0.68f && currentTorsoSpanNorm <= 0.11f) {
            return true
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
        currentTorsoSpanNorm: Float?,
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

        // 2. Salam check while seated (head turned right/left via nose-ear or headscarf fallback)
        val isSeatedPosture = (kneeAngle in 1f..115f && noseNormalizedY < 0.74f) ||
            (!hasLegs && hipNormalizedY >= 0.58f && noseNormalizedY < 0.74f)
        if (isSeatedPosture && isHeadTurnedForSalam) {
            return PrayerPose.SALAM
        }

        // 3. Qada / Sitting check: side-view (torso 65..120, knee < 62), frontal foreshortened Qa'da, or loose dress sitting
        val isFrontalQada = hipNormalizedY >= 0.60f &&
            noseNormalizedY < 0.68f &&
            torsoAngle in 65f..148f &&
            kneeAngle in 1f..115f
        val isLooseDressQada = !hasLegs &&
            hipNormalizedY >= 0.58f &&
            noseNormalizedY in 0.25f..0.72f &&
            shoulderNormalizedY >= 0.40f
        if ((torsoAngle in 65f..120f && kneeAngle in 1f..62f && noseNormalizedY < 0.72f) || isFrontalQada || isLooseDressQada) {
            return PrayerPose.QADA
        }

        // 4. Ruku check: 3D spatial torso bend OR frontal height-drop Ruku while knees remain straight
        if ((torsoAngle in 52f..125f && kneeAngle >= 135f) || isFrontalRuku) {
            return PrayerPose.RUKU
        }

        // 5. Upright standing: Takbir vs Qiyom (including loose long dress when knees are veiled)
        val isStandingLooseDress = !hasLegs &&
            hipNormalizedY < 0.65f &&
            (currentTorsoSpanNorm ?: 0f) >= 0.16f &&
            noseNormalizedY < 0.45f
        if ((torsoAngle >= 150f && kneeAngle >= 150f) || isStandingLooseDress) {
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

    /**
     * Chair / Seated (Imo-ishora) pose classification for elderly or mobility-impaired users:
     * - Upright seated: QIYOM (or TAKBIR if hands raised)
     * - Slight head+torso forward bow (15°-30°): RUKU
     * - Deeper head+torso forward bow (45°-60°+): SAJDA
     * - Head turned right/left while upright: SALAM
     */
    private fun classifySeatedRawPose(
        hasTorso: Boolean,
        effectiveLeanDeg: Float,
        shoulderY: Float?,
        hipY: Float?,
        wristY: Float?,
        isHeadTurnedForSalam: Boolean
    ): PrayerPose {
        if (!hasTorso) return PrayerPose.UNKNOWN

        if (effectiveLeanDeg < SEATED_RUKU_MAX_LEAN_DEG && isHeadTurnedForSalam) {
            return PrayerPose.SALAM
        }
        if (effectiveLeanDeg >= SEATED_SAJDA_MIN_LEAN_DEG) {
            return PrayerPose.SAJDA
        }
        if (effectiveLeanDeg in SEATED_RUKU_MIN_LEAN_DEG..SEATED_RUKU_MAX_LEAN_DEG) {
            return PrayerPose.RUKU
        }
        if (wristY != null && shoulderY != null && hipY != null) {
            val earOrShoulderZone = shoulderY + (hipY - shoulderY) * 0.15f
            if (wristY <= earOrShoulderZone) {
                return PrayerPose.TAKBIR
            }
        }
        return PrayerPose.QIYOM
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
