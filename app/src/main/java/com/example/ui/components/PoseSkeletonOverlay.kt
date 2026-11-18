package com.example.ui.components

import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.NormalizedLandmark
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.PoseWarningAmber
import com.example.vision.PrayerPoseAnalyzer
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.concurrent.Executors
import kotlin.math.max

private const val TAG_CAMERA = "CameraPosePreview"

@Composable
fun CameraPosePreview(
    darkLuminanceThreshold: Float,
    onPoseResult: (PoseAnalysisSnapshot) -> Unit,
    modifier: Modifier = Modifier,
    poseSnapshot: PoseAnalysisSnapshot = PoseAnalysisSnapshot(),
    showSkeleton: Boolean = true,
    isMatched: Boolean = false,
    enabled: Boolean = true,
    seatedMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember(darkLuminanceThreshold) {
        PrayerPoseAnalyzer(
            isFrontCamera = true,
            darkLuminanceThreshold = darkLuminanceThreshold,
            seatedMode = seatedMode,
            onPoseResult = onPoseResult
        )
    }
    analyzer.seatedMode = seatedMode

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, analyzer, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var boundProvider: ProcessCameraProvider? = null
            var boundUseCases: Array<UseCase> = emptyArray()
            var isDisposed = false
            val mainExecutor = ContextCompat.getMainExecutor(context)

            fun bindCamera(retryOnFailure: Boolean) {
                if (isDisposed) return
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    if (isDisposed) return@runCatching
                    boundProvider = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(480, 640),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor, analyzer)
                        }

                    val useFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    analyzer.isFrontCamera = useFront
                    val cameraSelector = if (useFront) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    boundUseCases = arrayOf(preview, imageAnalysis)
                }.onFailure { err ->
                    Log.w(TAG_CAMERA, "Camera bind failed (retry=$retryOnFailure)", err)
                    if (retryOnFailure && !isDisposed) {
                        previewView.postDelayed({ bindCamera(retryOnFailure = false) }, 180L)
                    }
                }
            }

            cameraProviderFuture.addListener({ bindCamera(retryOnFailure = true) }, mainExecutor)

            onDispose {
                isDisposed = true
                // Unbind ONLY this preview's own UseCases so transitioning screens never
                // kills the incoming screen's newly bound camera session.
                if (boundUseCases.isNotEmpty()) {
                    runCatching { boundProvider?.unbind(*boundUseCases) }
                }
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if (showSkeleton) {
            PoseSkeletonCanvas(
                landmarks = poseSnapshot.landmarks,
                imageWidth = poseSnapshot.imageWidth,
                imageHeight = poseSnapshot.imageHeight,
                isFrontCamera = poseSnapshot.isFrontCamera,
                boneColor = if (isMatched) PoseMatchedGreen else PoseWarningAmber,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val SKELETON_CONNECTIONS = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
    PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
    PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
    PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
    PoseLandmark.NOSE to PoseLandmark.LEFT_SHOULDER,
    PoseLandmark.NOSE to PoseLandmark.RIGHT_SHOULDER
)

private val SKELETON_JOINT_TYPES = setOf(
    PoseLandmark.NOSE,
    PoseLandmark.LEFT_SHOULDER,
    PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_ELBOW,
    PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_KNEE,
    PoseLandmark.RIGHT_KNEE,
    PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_ANKLE
)

@Composable
fun PoseSkeletonCanvas(
    landmarks: List<NormalizedLandmark>,
    imageWidth: Int = 480,
    imageHeight: Int = 640,
    isFrontCamera: Boolean = true,
    boneColor: Color = PoseMatchedGreen,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (landmarks.isEmpty()) {
            val cx = size.width * 0.5f
            val h = size.height
            val w = size.width
            val guideColor = PoseMatchedGreen.copy(alpha = 0.62f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

            val head = Offset(cx, h * 0.20f)
            val neck = Offset(cx, h * 0.28f)
            val lShoulder = Offset(cx - w * 0.17f, h * 0.31f)
            val rShoulder = Offset(cx + w * 0.17f, h * 0.31f)
            val lElbow = Offset(cx - w * 0.22f, h * 0.44f)
            val rElbow = Offset(cx + w * 0.22f, h * 0.44f)
            val lHip = Offset(cx - w * 0.11f, h * 0.54f)
            val rHip = Offset(cx + w * 0.11f, h * 0.54f)
            val lKnee = Offset(cx - w * 0.12f, h * 0.72f)
            val rKnee = Offset(cx + w * 0.12f, h * 0.72f)
            val lAnkle = Offset(cx - w * 0.12f, h * 0.88f)
            val rAnkle = Offset(cx + w * 0.12f, h * 0.88f)

            drawCircle(
                color = guideColor,
                radius = w * 0.08f,
                center = head,
                style = Stroke(width = 3f, pathEffect = dash)
            )
            val guideBones = listOf(
                neck to lShoulder,
                neck to rShoulder,
                lShoulder to rShoulder,
                lShoulder to lElbow,
                rShoulder to rElbow,
                lShoulder to lHip,
                rShoulder to rHip,
                lHip to rHip,
                lHip to lKnee,
                lKnee to lAnkle,
                rHip to rKnee,
                rKnee to rAnkle
            )
            guideBones.forEach { (start, end) ->
                drawLine(
                    color = guideColor,
                    start = start,
                    end = end,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = dash
                )
            }
            listOf(lShoulder, rShoulder, lHip, rHip, lKnee, rKnee).forEach { pt ->
                drawCircle(color = guideColor, radius = 4.5f, center = pt)
            }
            return@Canvas
        }

        // Aspect-ratio-aware PreviewView.ScaleType.FILL_CENTER coordinate mapping
        val srcW = imageWidth.coerceAtLeast(1).toFloat()
        val srcH = imageHeight.coerceAtLeast(1).toFloat()
        val scale = max(size.width / srcW, size.height / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val dx = (size.width - scaledW) / 2f
        val dy = (size.height - scaledH) / 2f

        val map = landmarks.associateBy { it.type }

        fun toOffset(lm: NormalizedLandmark): Offset {
            val nx = if (isFrontCamera) 1f - lm.x else lm.x
            return Offset(dx + nx * scaledW, dy + lm.y * scaledH)
        }

        for ((startType, endType) in SKELETON_CONNECTIONS) {
            val a = map[startType]
            val b = map[endType]
            if (a != null && b != null && a.inFrameLikelihood > 0.35f && b.inFrameLikelihood > 0.35f) {
                val p1 = toOffset(a)
                val p2 = toOffset(b)
                drawLine(
                    color = boneColor.copy(alpha = 0.35f),
                    start = p1,
                    end = p2,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = boneColor.copy(alpha = 0.95f),
                    start = p1,
                    end = p2,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }

        landmarks.forEach { lm ->
            if (lm.type in SKELETON_JOINT_TYPES && lm.inFrameLikelihood > 0.40f) {
                val center = toOffset(lm)
                drawCircle(
                    color = Color.White,
                    radius = 7.5f,
                    center = center
                )
                drawCircle(
                    color = boneColor,
                    radius = 4.8f,
                    center = center
                )
            }
        }
    }
}
