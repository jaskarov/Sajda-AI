package com.example

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import com.example.vision.PoseDetectionAnalyzerHelper
import com.example.vision.PoseFrameLandmarks
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PoseDetectionAnalyzerHelperTest {

    @Test
    fun `computeUprightDimensions swaps width and height for 90 and 270 degrees`() {
        assertEquals(640 to 480, PoseDetectionAnalyzerHelper.computeUprightDimensions(640, 480, 0))
        assertEquals(480 to 640, PoseDetectionAnalyzerHelper.computeUprightDimensions(640, 480, 90))
        assertEquals(640 to 480, PoseDetectionAnalyzerHelper.computeUprightDimensions(640, 480, 180))
        assertEquals(480 to 640, PoseDetectionAnalyzerHelper.computeUprightDimensions(640, 480, 270))
    }

    @Test
    fun `createCoordinate normalizes and clamps pixel coordinates accurately`() {
        val shoulder = PoseDetectionAnalyzerHelper.createCoordinate(
            type = PoseLandmark.LEFT_SHOULDER,
            x = 240f,
            y = 320f,
            z = -12.5f,
            inFrameLikelihood = 0.92f,
            frameWidth = 480,
            frameHeight = 640
        )

        assertEquals(PoseLandmark.LEFT_SHOULDER, shoulder.type)
        assertEquals(240f, shoulder.x, 0.001f)
        assertEquals(320f, shoulder.y, 0.001f)
        assertEquals(-12.5f, shoulder.z, 0.001f)
        assertEquals(0.5f, shoulder.normalizedX, 0.001f)
        assertEquals(0.5f, shoulder.normalizedY, 0.001f)

        val clamped = PoseDetectionAnalyzerHelper.createCoordinate(
            type = PoseLandmark.NOSE,
            x = -20f,
            y = 800f,
            z = 0f,
            inFrameLikelihood = 0.8f,
            frameWidth = 480,
            frameHeight = 640
        )
        assertEquals(0f, clamped.normalizedX, 0.001f)
        assertEquals(1f, clamped.normalizedY, 0.001f)

        val frame = PoseFrameLandmarks(
            landmarks = listOf(shoulder, clamped),
            frameWidth = 480,
            frameHeight = 640,
            rotationDegrees = 90,
            timestampMs = 1234L
        )
        assertNotNull(frame.landmark(PoseLandmark.LEFT_SHOULDER))
        val normalizedList = frame.toNormalizedLandmarks()
        assertEquals(2, normalizedList.size)
        assertEquals(0.5f, normalizedList.first().x, 0.001f)
    }

    @Test
    fun `helper delegates frame to wrapped analyzer and closes pose detector`() {
        val delegateCallCount = AtomicInteger(0)
        val detectorClosed = AtomicBoolean(false)
        val imageClosed = AtomicBoolean(false)

        val fakeDetector = Proxy.newProxyInstance(
            PoseDetector::class.java.classLoader,
            arrayOf(PoseDetector::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "close" -> {
                    detectorClosed.set(true)
                    null
                }
                "process" -> Tasks.forCanceled<Any>()
                else -> null
            }
        } as PoseDetector

        val wrappedAnalyzer = ImageAnalysis.Analyzer { proxy ->
            delegateCallCount.incrementAndGet()
            proxy.close()
        }

        val helper = PoseDetectionAnalyzerHelper(
            delegateAnalyzer = wrappedAnalyzer,
            detectorProvider = { fakeDetector },
            clockMs = { 1000L },
            onLandmarksExtracted = {}
        )

        val fakeImageInfo = Proxy.newProxyInstance(
            ImageInfo::class.java.classLoader,
            arrayOf(ImageInfo::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getRotationDegrees" -> 0
                "getTimestamp" -> 0L
                else -> null
            }
        } as ImageInfo

        val fakeImageProxy = Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getImage" -> null
                "getImageInfo" -> fakeImageInfo
                "getWidth" -> 480
                "getHeight" -> 640
                "close" -> {
                    imageClosed.set(true)
                    null
                }
                else -> null
            }
        } as ImageProxy

        helper.analyze(fakeImageProxy)
        assertEquals(1, delegateCallCount.get())
        assertTrue(imageClosed.get())

        helper.close()
        assertTrue(detectorClosed.get())
    }
}
