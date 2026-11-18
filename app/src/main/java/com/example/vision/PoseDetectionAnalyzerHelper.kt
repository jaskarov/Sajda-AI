package com.example.vision

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.NormalizedLandmark
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.io.Closeable
import java.util.concurrent.Executor

data class ExtractedPoseLandmark(
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val normalizedX: Float,
    val normalizedY: Float,
    val inFrameLikelihood: Float
)

data class PoseFrameLandmarks(
    val landmarks: List<ExtractedPoseLandmark>,
    val frameWidth: Int,
    val frameHeight: Int,
    val rotationDegrees: Int,
    val timestampMs: Long
) {
    private val byType: Map<Int, ExtractedPoseLandmark> by lazy(LazyThreadSafetyMode.NONE) {
        landmarks.associateBy { it.type }
    }

    fun landmark(type: Int): ExtractedPoseLandmark? = byType[type]

    fun toNormalizedLandmarks(): List<NormalizedLandmark> = landmarks.map { lm ->
        NormalizedLandmark(
            type = lm.type,
            x = lm.normalizedX,
            y = lm.normalizedY,
            inFrameLikelihood = lm.inFrameLikelihood
        )
    }
}

/**
 * Helper class that wraps/cooperates with a CameraX [ImageAnalysis.Analyzer] and integrates
 * ML Kit's [PoseDetector] to extract pixel and normalized pose landmark coordinates from frames.
 *
 * Ensures safe CameraX [ImageProxy] ownership: frames stay in memory only during ML Kit processing
 * and are then forwarded to [delegateAnalyzer] (if supplied) or closed immediately.
 */
class PoseDetectionAnalyzerHelper(
    private val delegateAnalyzer: ImageAnalysis.Analyzer? = null,
    private val minFrameIntervalMs: Long = DEFAULT_MIN_FRAME_INTERVAL_MS,
    private val minLandmarkLikelihood: Float = 0.0f,
    private val detectorProvider: () -> PoseDetector = { createAccurateStreamDetector() },
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val onPreInspectFrame: ((ImageProxy) -> Unit)? = null,
    private val onError: (Exception) -> Unit = { error ->
        Log.e(TAG, "ML Kit pose detection error", error)
    },
    private val onLandmarksExtracted: (PoseFrameLandmarks) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val detector: PoseDetector by lazy { detectorProvider() }

    @Volatile
    private var lastProcessedTimeMs = 0L

    @Volatile
    private var isBusy = false

    fun attachTo(imageAnalysis: ImageAnalysis, executor: Executor) {
        imageAnalysis.setAnalyzer(executor, this)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = clockMs()
        if (isBusy || (now - lastProcessedTimeMs) < minFrameIntervalMs) {
            finishImageProxy(imageProxy)
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            finishImageProxy(imageProxy)
            return
        }

        isBusy = true
        lastProcessedTimeMs = now

        runCatching { onPreInspectFrame?.invoke(imageProxy) }
            .onFailure { err -> Log.w(TAG, "Frame pre-inspection failed", err) }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val (uprightWidth, uprightHeight) = computeUprightDimensions(
            width = imageProxy.width,
            height = imageProxy.height,
            rotationDegrees = rotationDegrees
        )

        val inputImage = try {
            InputImage.fromMediaImage(mediaImage, rotationDegrees)
        } catch (t: Throwable) {
            isBusy = false
            Log.e(TAG, "Failed to build InputImage from CameraX frame", t)
            finishImageProxy(imageProxy)
            return
        }

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                val extracted = extractFromPose(
                    pose = pose,
                    frameWidth = uprightWidth,
                    frameHeight = uprightHeight,
                    rotationDegrees = rotationDegrees,
                    timestampMs = clockMs()
                )
                onLandmarksExtracted(extracted)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
            .addOnCompleteListener {
                isBusy = false
                finishImageProxy(imageProxy)
            }
    }

    fun extractFromPose(
        pose: Pose,
        frameWidth: Int,
        frameHeight: Int,
        rotationDegrees: Int = 0,
        timestampMs: Long = clockMs()
    ): PoseFrameLandmarks {
        val safeW = frameWidth.coerceAtLeast(1)
        val safeH = frameHeight.coerceAtLeast(1)
        val coordinates = pose.allPoseLandmarks
            .asSequence()
            .filter { it.inFrameLikelihood >= minLandmarkLikelihood }
            .map { lm ->
                val pos3d = lm.position3D
                createCoordinate(
                    type = lm.landmarkType,
                    x = pos3d.x,
                    y = pos3d.y,
                    z = pos3d.z,
                    inFrameLikelihood = lm.inFrameLikelihood,
                    frameWidth = safeW,
                    frameHeight = safeH
                )
            }
            .toList()

        return PoseFrameLandmarks(
            landmarks = coordinates,
            frameWidth = safeW,
            frameHeight = safeH,
            rotationDegrees = rotationDegrees,
            timestampMs = timestampMs
        )
    }

    private fun finishImageProxy(imageProxy: ImageProxy) {
        val delegate = delegateAnalyzer
        if (delegate != null) {
            try {
                delegate.analyze(imageProxy)
            } catch (t: Throwable) {
                Log.w(TAG, "Delegate analyzer threw during analyze; closing ImageProxy", t)
                runCatching { imageProxy.close() }
            }
        } else {
            runCatching { imageProxy.close() }
        }
    }

    override fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing pose detector", e)
        }
    }

    companion object {
        private const val TAG = "PoseDetectionHelper"
        const val DEFAULT_MIN_FRAME_INTERVAL_MS = 66L

        fun createAccurateStreamDetector(): PoseDetector {
            val options = AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .build()
            return PoseDetection.getClient(options)
        }

        fun wrap(
            analyzer: ImageAnalysis.Analyzer,
            minFrameIntervalMs: Long = DEFAULT_MIN_FRAME_INTERVAL_MS,
            minLandmarkLikelihood: Float = 0.0f,
            onLandmarksExtracted: (PoseFrameLandmarks) -> Unit
        ): PoseDetectionAnalyzerHelper = PoseDetectionAnalyzerHelper(
            delegateAnalyzer = analyzer,
            minFrameIntervalMs = minFrameIntervalMs,
            minLandmarkLikelihood = minLandmarkLikelihood,
            onLandmarksExtracted = onLandmarksExtracted
        )

        fun computeUprightDimensions(
            width: Int,
            height: Int,
            rotationDegrees: Int
        ): Pair<Int, Int> {
            val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
            return if (normalizedRotation == 90 || normalizedRotation == 270) {
                height.coerceAtLeast(1) to width.coerceAtLeast(1)
            } else {
                width.coerceAtLeast(1) to height.coerceAtLeast(1)
            }
        }

        fun createCoordinate(
            type: Int,
            x: Float,
            y: Float,
            z: Float,
            inFrameLikelihood: Float,
            frameWidth: Int,
            frameHeight: Int
        ): ExtractedPoseLandmark {
            val safeW = frameWidth.coerceAtLeast(1).toFloat()
            val safeH = frameHeight.coerceAtLeast(1).toFloat()
            return ExtractedPoseLandmark(
                type = type,
                x = x,
                y = y,
                z = z,
                normalizedX = (x / safeW).coerceIn(0f, 1f),
                normalizedY = (y / safeH).coerceIn(0f, 1f),
                inFrameLikelihood = inFrameLikelihood
            )
        }
    }
}
