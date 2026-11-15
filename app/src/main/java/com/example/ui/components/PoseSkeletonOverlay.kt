package com.example.ui.components

import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
fun CameraPosePreview(
    darkLuminanceThreshold: Float,
    onPoseResult: (PoseAnalysisSnapshot) -> Unit,
    modifier: Modifier = Modifier,
    poseSnapshot: PoseAnalysisSnapshot = PoseAnalysisSnapshot(),
    showSkeleton: Boolean = true,
    isMatched: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember(darkLuminanceThreshold) {
        PrayerPoseAnalyzer(
            isFrontCamera = true,
            darkLuminanceThreshold = darkLuminanceThreshold,
            onPoseResult = onPoseResult
        )
    }

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

    DisposableEffect(lifecycleOwner, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
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

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
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
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            boundProvider?.unbindAll()
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if (showSkeleton && poseSnapshot.landmarks.isNotEmpty()) {
            PoseSkeletonCanvas(
                landmarks = poseSnapshot.landmarks,
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

@Composable
fun PoseSkeletonCanvas(
    landmarks: List<NormalizedLandmark>,
    isFrontCamera: Boolean,
    boneColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val map = landmarks.associateBy { it.type }

        fun toOffset(lm: NormalizedLandmark): Offset {
            val nx = if (isFrontCamera) 1f - lm.x else lm.x
            return Offset(nx * size.width, lm.y * size.height)
        }

        for ((startType, endType) in SKELETON_CONNECTIONS) {
            val a = map[startType]
            val b = map[endType]
            if (a != null && b != null && a.inFrameLikelihood > 0.35f && b.inFrameLikelihood > 0.35f) {
                drawLine(
                    color = boneColor.copy(alpha = 0.85f),
                    start = toOffset(a),
                    end = toOffset(b),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        landmarks.forEach { lm ->
            if (lm.inFrameLikelihood > 0.45f) {
                val center = toOffset(lm)
                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = center
                )
                drawCircle(
                    color = boneColor,
                    radius = 4.5f,
                    center = center
                )
            }
        }
    }
}
