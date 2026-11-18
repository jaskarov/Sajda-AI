package com.example

import com.example.domain.model.PrayerPose
import com.example.vision.ExtractedPoseLandmark
import com.example.vision.PoseFrameLandmarks
import com.example.vision.PrayerPoseAnalyzer
import com.google.mlkit.vision.pose.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerPoseAnalyzerTest {

    private fun lm(
        type: Int,
        normX: Float,
        normY: Float,
        z: Float = 0f,
        w: Int = 480,
        h: Int = 640
    ): ExtractedPoseLandmark = ExtractedPoseLandmark(
        type = type,
        x = normX * w,
        y = normY * h,
        z = z,
        normalizedX = normX,
        normalizedY = normY,
        inFrameLikelihood = 0.95f
    )

    private fun standingQiyomFrame(timestampMs: Long): PoseFrameLandmarks = PoseFrameLandmarks(
        landmarks = listOf(
            lm(PoseLandmark.NOSE, 0.50f, 0.14f),
            lm(PoseLandmark.LEFT_EAR, 0.46f, 0.14f),
            lm(PoseLandmark.RIGHT_EAR, 0.54f, 0.14f),
            lm(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.25f),
            lm(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.25f),
            lm(PoseLandmark.LEFT_HIP, 0.44f, 0.55f),
            lm(PoseLandmark.RIGHT_HIP, 0.56f, 0.55f),
            lm(PoseLandmark.LEFT_KNEE, 0.45f, 0.74f),
            lm(PoseLandmark.RIGHT_KNEE, 0.55f, 0.74f),
            lm(PoseLandmark.LEFT_ANKLE, 0.45f, 0.93f),
            lm(PoseLandmark.RIGHT_ANKLE, 0.55f, 0.93f),
            lm(PoseLandmark.LEFT_WRIST, 0.48f, 0.48f),
            lm(PoseLandmark.RIGHT_WRIST, 0.52f, 0.48f)
        ),
        frameWidth = 480,
        frameHeight = 640,
        rotationDegrees = 0,
        timestampMs = timestampMs
    )

    @Test
    fun calculateAngle3D_detectsForwardBendInZAxis() {
        // Facing camera: shoulder (0, -200, -180) leaned forward in -Z relative to hip (0, 0, 0) & knee (0, 200, 0)
        val shoulder = ExtractedPoseLandmark(PoseLandmark.LEFT_SHOULDER, 240f, 340f, -200f, 0.5f, 0.53f, 0.95f)
        val hip = ExtractedPoseLandmark(PoseLandmark.LEFT_HIP, 240f, 350f, 0f, 0.5f, 0.55f, 0.95f)
        val knee = ExtractedPoseLandmark(PoseLandmark.LEFT_KNEE, 240f, 475f, 0f, 0.5f, 0.74f, 0.95f)

        val angle3d = PrayerPoseAnalyzer.calculateAngle3D(shoulder, hip, knee)
        assertTrue("Expected ~90° 3D bend toward camera, was $angle3d", angle3d in 75f..105f)
    }

    @Test
    fun frontalRuku_isDetectedAfterQiyomBaselineWithStraightKneesAnd45PercentDrop() {
        val analyzer = PrayerPoseAnalyzer(onPoseResult = {})

        // Calibrate standing Qiyom baseline across 400ms+
        analyzer.evaluateFrame(standingQiyomFrame(timestampMs = 0L))
        val qiyom = analyzer.evaluateFrame(standingQiyomFrame(timestampMs = 450L))
        assertEquals(PrayerPose.QIYOM, qiyom.confirmedPose)

        // Frontal Ruku: knees stay straight (~180°), shoulders & nose drop ~45-50% toward hip Y (0.55)
        // Notice 2D coordinates are collinear along X=0.5, so 2D atan2 would fail (~180°),
        // yet frontal baseline height drop detects RUKU cleanly.
        val frontalRukuFrame = PoseFrameLandmarks(
            landmarks = listOf(
                lm(PoseLandmark.NOSE, 0.50f, 0.51f, z = 0f),
                lm(PoseLandmark.LEFT_EAR, 0.46f, 0.51f, z = 0f),
                lm(PoseLandmark.RIGHT_EAR, 0.54f, 0.51f, z = 0f),
                lm(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.48f, z = 0f),
                lm(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.48f, z = 0f),
                lm(PoseLandmark.LEFT_HIP, 0.44f, 0.55f, z = 0f),
                lm(PoseLandmark.RIGHT_HIP, 0.56f, 0.55f, z = 0f),
                lm(PoseLandmark.LEFT_KNEE, 0.45f, 0.74f, z = 0f),
                lm(PoseLandmark.RIGHT_KNEE, 0.55f, 0.74f, z = 0f),
                lm(PoseLandmark.LEFT_ANKLE, 0.45f, 0.93f, z = 0f),
                lm(PoseLandmark.RIGHT_ANKLE, 0.55f, 0.93f, z = 0f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 1000L
        )
        val firstRuku = analyzer.evaluateFrame(frontalRukuFrame)
        assertEquals(PrayerPose.RUKU, firstRuku.rawPose)
        val confirmedRuku = analyzer.evaluateFrame(frontalRukuFrame.copy(timestampMs = 1450L))
        assertEquals(PrayerPose.RUKU, confirmedRuku.confirmedPose)
    }

    @Test
    fun frontalSajda_detectsLowNoseAndShouldersWithBentKnees() {
        val analyzer = PrayerPoseAnalyzer(onPoseResult = {})
        val frontalSajda = PoseFrameLandmarks(
            landmarks = listOf(
                lm(PoseLandmark.NOSE, 0.50f, 0.84f),
                lm(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.78f),
                lm(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.78f),
                lm(PoseLandmark.LEFT_HIP, 0.43f, 0.66f),
                lm(PoseLandmark.RIGHT_HIP, 0.57f, 0.66f),
                lm(PoseLandmark.LEFT_KNEE, 0.40f, 0.82f),
                lm(PoseLandmark.RIGHT_KNEE, 0.60f, 0.82f),
                lm(PoseLandmark.LEFT_ANKLE, 0.46f, 0.80f),
                lm(PoseLandmark.RIGHT_ANKLE, 0.54f, 0.80f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 0L
        )
        val r1 = analyzer.evaluateFrame(frontalSajda)
        assertEquals(PrayerPose.SAJDA, r1.rawPose)
        val r2 = analyzer.evaluateFrame(frontalSajda.copy(timestampMs = 450L))
        assertEquals(PrayerPose.SAJDA, r2.confirmedPose)
    }

    @Test
    fun salam_detectsHeadTurnRightAndLeftWhileSeated() {
        val analyzer = PrayerPoseAnalyzer(onPoseResult = {})

        // Seated Qada looking straight ahead -> nose halfway between left and right ear
        val centeredQada = PoseFrameLandmarks(
            landmarks = listOf(
                lm(PoseLandmark.NOSE, 0.50f, 0.42f),
                lm(PoseLandmark.LEFT_EAR, 0.45f, 0.42f),
                lm(PoseLandmark.RIGHT_EAR, 0.55f, 0.42f),
                lm(PoseLandmark.LEFT_SHOULDER, 0.41f, 0.52f),
                lm(PoseLandmark.RIGHT_SHOULDER, 0.59f, 0.52f),
                lm(PoseLandmark.LEFT_HIP, 0.43f, 0.72f),
                lm(PoseLandmark.RIGHT_HIP, 0.57f, 0.72f),
                lm(PoseLandmark.LEFT_KNEE, 0.35f, 0.80f),
                lm(PoseLandmark.RIGHT_KNEE, 0.65f, 0.80f),
                lm(PoseLandmark.LEFT_ANKLE, 0.48f, 0.85f),
                lm(PoseLandmark.RIGHT_ANKLE, 0.52f, 0.85f)
            ),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 0,
            timestampMs = 0L
        )
        assertEquals(PrayerPose.QADA, analyzer.evaluateFrame(centeredQada).rawPose)

        // Turn head right for Salam: nose moves close to right ear
        val salamRight = centeredQada.copy(
            landmarks = centeredQada.landmarks.map {
                if (it.type == PoseLandmark.NOSE) lm(PoseLandmark.NOSE, 0.54f, 0.42f) else it
            },
            timestampMs = 500L
        )
        val salamSnap1 = analyzer.evaluateFrame(salamRight)
        assertEquals(PrayerPose.SALAM, salamSnap1.rawPose)
        val salamSnap2 = analyzer.evaluateFrame(salamRight.copy(timestampMs = 950L))
        assertEquals(PrayerPose.SALAM, salamSnap2.confirmedPose)

        // Turn head left for Salam: nose moves close to left ear
        val salamLeft = centeredQada.copy(
            landmarks = centeredQada.landmarks.map {
                if (it.type == PoseLandmark.NOSE) lm(PoseLandmark.NOSE, 0.46f, 0.42f) else it
            },
            timestampMs = 1400L
        )
        assertEquals(PrayerPose.SALAM, analyzer.evaluateFrame(salamLeft).rawPose)
    }

    @Test
    fun seatedMode_detectsQiyomRuku15To30_Sajda45To60_andSalamOnChair() {
        val analyzer = PrayerPoseAnalyzer(seatedMode = true, onPoseResult = {})

        fun seatedFrame(
            timestampMs: Long,
            leanDegrees: Float,
            noseOffsetX: Float = 0f
        ): PoseFrameLandmarks {
            val rad = Math.toRadians(leanDegrees.toDouble())
            val torsoLenPx = 180.0
            val headLenPx = 250.0
            val hipX = 240f
            val hipY = 420f
            val shoulderX = hipX + (torsoLenPx * kotlin.math.sin(rad)).toFloat()
            val shoulderY = hipY - (torsoLenPx * kotlin.math.cos(rad)).toFloat()
            val headCenterX = hipX + (headLenPx * kotlin.math.sin(rad)).toFloat()
            val noseX = headCenterX + noseOffsetX
            val noseY = hipY - (headLenPx * kotlin.math.cos(rad)).toFloat()

            return PoseFrameLandmarks(
                landmarks = listOf(
                    ExtractedPoseLandmark(PoseLandmark.NOSE, noseX, noseY, 0f, noseX / 480f, noseY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.LEFT_EAR, headCenterX - 18f, noseY, 0f, (headCenterX - 18f) / 480f, noseY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.RIGHT_EAR, headCenterX + 18f, noseY, 0f, (headCenterX + 18f) / 480f, noseY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.LEFT_SHOULDER, shoulderX, shoulderY, 0f, shoulderX / 480f, shoulderY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.RIGHT_SHOULDER, shoulderX + 20f, shoulderY, 0f, (shoulderX + 20f) / 480f, shoulderY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.LEFT_HIP, hipX, hipY, 0f, hipX / 480f, hipY / 640f, 0.95f),
                    ExtractedPoseLandmark(PoseLandmark.RIGHT_HIP, hipX + 16f, hipY, 0f, (hipX + 16f) / 480f, hipY / 640f, 0.95f)
                    // Notice: ankles/knees omitted (e.g. cropped below table), yet seated calibration succeeds!
                ),
                frameWidth = 480,
                frameHeight = 640,
                rotationDegrees = 0,
                timestampMs = timestampMs
            )
        }

        // 1. Upright sitting on chair -> full body calibrated & QIYOM confirmed
        val upright1 = analyzer.evaluateFrame(seatedFrame(timestampMs = 0L, leanDegrees = 2f))
        assertTrue(upright1.isFullBodyVisible)
        assertEquals(PrayerPose.QIYOM, upright1.rawPose)
        val uprightConfirmed = analyzer.evaluateFrame(seatedFrame(timestampMs = 450L, leanDegrees = 2f))
        assertEquals(PrayerPose.QIYOM, uprightConfirmed.confirmedPose)

        // 2. Slight forward bow (22°, inside 15°-30°) -> RUKU
        val rukuRaw = analyzer.evaluateFrame(seatedFrame(timestampMs = 900L, leanDegrees = 22f))
        assertEquals(PrayerPose.RUKU, rukuRaw.rawPose)
        val rukuConfirmed = analyzer.evaluateFrame(seatedFrame(timestampMs = 1350L, leanDegrees = 22f))
        assertEquals(PrayerPose.RUKU, rukuConfirmed.confirmedPose)

        // 3. Return upright -> QIYOM (accepted as Qavma)
        val qavmaConfirmed = analyzer.evaluateFrame(seatedFrame(timestampMs = 1800L, leanDegrees = 3f))
        assertEquals(PrayerPose.QIYOM, qavmaConfirmed.rawPose)

        // 4. Deeper forward bow (52°, inside 45°-60°) -> SAJDA
        val sajdaRaw = analyzer.evaluateFrame(seatedFrame(timestampMs = 2200L, leanDegrees = 52f))
        assertEquals(PrayerPose.SAJDA, sajdaRaw.rawPose)
        val sajdaConfirmed = analyzer.evaluateFrame(seatedFrame(timestampMs = 2650L, leanDegrees = 52f))
        assertEquals(PrayerPose.SAJDA, sajdaConfirmed.confirmedPose)

        // 5. Turn head right while sitting upright -> SALAM
        val salamRaw = analyzer.evaluateFrame(seatedFrame(timestampMs = 3100L, leanDegrees = 2f, noseOffsetX = 14f))
        assertEquals(PrayerPose.SALAM, salamRaw.rawPose)
        val salamConfirmed = analyzer.evaluateFrame(seatedFrame(timestampMs = 3550L, leanDegrees = 2f, noseOffsetX = 14f))
        assertEquals(PrayerPose.SALAM, salamConfirmed.confirmedPose)
    }
}
