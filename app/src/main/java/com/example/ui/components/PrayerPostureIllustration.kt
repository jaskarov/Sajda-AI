package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.Gender
import com.example.domain.model.PrayerPose
import com.example.ui.theme.NightEmeraldPrimary
import com.example.ui.theme.NightGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class PostureKeypoints(
    val headX: Float,
    val headY: Float,
    val shoulderX: Float,
    val shoulderY: Float,
    val hipX: Float,
    val hipY: Float,
    val kneeX: Float,
    val kneeY: Float,
    val footX: Float,
    val footY: Float,
    val elbowX: Float,
    val elbowY: Float,
    val handX: Float,
    val handY: Float
)

/**
 * Interactive animated silhouette illustration for each Hanafi prayer rukn.
 * Smoothly morphs body posture when switching steps, Gender (Male/Female), or Seated mode.
 */
@Composable
fun PrayerPostureIllustration(
    pose: PrayerPose,
    gender: Gender,
    seatedMode: Boolean,
    modifier: Modifier = Modifier
) {
    val target = keypointsFor(pose = pose, gender = gender, seatedMode = seatedMode)
    val animSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val headX by animateFloatAsState(target.headX, animSpec, label = "headX")
    val headY by animateFloatAsState(target.headY, animSpec, label = "headY")
    val shoulderX by animateFloatAsState(target.shoulderX, animSpec, label = "shoulderX")
    val shoulderY by animateFloatAsState(target.shoulderY, animSpec, label = "shoulderY")
    val hipX by animateFloatAsState(target.hipX, animSpec, label = "hipX")
    val hipY by animateFloatAsState(target.hipY, animSpec, label = "hipY")
    val kneeX by animateFloatAsState(target.kneeX, animSpec, label = "kneeX")
    val kneeY by animateFloatAsState(target.kneeY, animSpec, label = "kneeY")
    val footX by animateFloatAsState(target.footX, animSpec, label = "footX")
    val footY by animateFloatAsState(target.footY, animSpec, label = "footY")
    val elbowX by animateFloatAsState(target.elbowX, animSpec, label = "elbowX")
    val elbowY by animateFloatAsState(target.elbowY, animSpec, label = "elbowY")
    val handX by animateFloatAsState(target.handX, animSpec, label = "handX")
    val handY by animateFloatAsState(target.handY, animSpec, label = "handY")

    val infinite = rememberInfiniteTransition(label = "posture_ambient")
    val breathPhase by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_phase"
    )
    val salamTurn by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "salam_turn"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Decorative mihrab arch & soft sanctuary glow
        drawMihrabBackdrop(w = w, h = h)

        // Prayer mat (joynamoz) baseline
        val matY = h * 0.86f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    NightGold.copy(alpha = 0.25f),
                    NightGold.copy(alpha = 0.72f),
                    NightGold.copy(alpha = 0.25f)
                )
            ),
            topLeft = Offset(w * 0.14f, matY),
            size = Size(w * 0.72f, h * 0.032f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Chair silhouette if seatedMode is active
        if (seatedMode) {
            drawChairSilhouette(w = w, h = h, matY = matY)
        }

        val subtleBreath = breathPhase * h * 0.006f
        val headCenter = Offset(
            x = w * headX + if (pose == PrayerPose.SALAM) salamTurn * w * 0.022f else 0f,
            y = h * headY + subtleBreath
        )
        val shoulder = Offset(w * shoulderX, h * shoulderY + subtleBreath)
        val hip = Offset(w * hipX, h * hipY)
        val knee = Offset(w * kneeX, h * kneeY)
        val foot = Offset(w * footX, h * footY)
        val elbow = Offset(w * elbowX, h * elbowY + subtleBreath)
        val hand = Offset(w * handX, h * handY + subtleBreath)

        val limbWidth = (w.coerceAtMost(h) * 0.048f).coerceIn(7f, 16f)
        val torsoWidth = limbWidth * 1.45f
        val headRadius = (w.coerceAtMost(h) * 0.074f).coerceIn(13f, 26f)

        val primaryFigure = Color(0xFFEAFBF4)
        val accentHighlight = NightGold
        val jointGlow = NightEmeraldPrimary

        // Legs (Hip -> Knee -> Foot)
        drawLine(
            color = primaryFigure.copy(alpha = 0.85f),
            start = hip,
            end = knee,
            strokeWidth = limbWidth * 1.15f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryFigure.copy(alpha = 0.78f),
            start = knee,
            end = foot,
            strokeWidth = limbWidth,
            cap = StrokeCap.Round
        )

        // Female modest robe drape indicator for standing/bowing poses
        if (gender == Gender.FEMALE && !seatedMode && (pose == PrayerPose.TAKBIR || pose == PrayerPose.QIYOM)) {
            val robePath = Path().apply {
                moveTo(shoulder.x - limbWidth * 0.8f, shoulder.y)
                lineTo(shoulder.x + limbWidth * 0.8f, shoulder.y)
                lineTo(foot.x + w * 0.055f, foot.y)
                lineTo(foot.x - w * 0.055f, foot.y)
                close()
            }
            drawPath(
                path = robePath,
                color = NightEmeraldPrimary.copy(alpha = 0.24f)
            )
        }

        // Torso (Hip -> Shoulder)
        drawLine(
            color = primaryFigure,
            start = hip,
            end = shoulder,
            strokeWidth = torsoWidth,
            cap = StrokeCap.Round
        )

        // Arm (Shoulder -> Elbow -> Hand) highlighted softly in saffron gold to teach hand placement
        drawLine(
            color = accentHighlight.copy(alpha = 0.92f),
            start = shoulder,
            end = elbow,
            strokeWidth = limbWidth * 0.88f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentHighlight,
            start = elbow,
            end = hand,
            strokeWidth = limbWidth * 0.82f,
            cap = StrokeCap.Round
        )

        // Hand focus halo
        drawCircle(
            color = accentHighlight.copy(alpha = 0.30f),
            radius = limbWidth * 1.1f,
            center = hand
        )
        drawCircle(
            color = accentHighlight,
            radius = limbWidth * 0.45f,
            center = hand
        )

        // Head
        drawCircle(
            color = primaryFigure,
            radius = headRadius,
            center = headCenter
        )
        // Subtle headscarf curve for Female mode
        if (gender == Gender.FEMALE) {
            drawArc(
                color = NightEmeraldPrimary.copy(alpha = 0.72f),
                startAngle = 155f,
                sweepAngle = 210f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 1.18f, headCenter.y - headRadius * 1.18f),
                size = Size(headRadius * 2.36f, headRadius * 2.36f),
                style = Stroke(width = limbWidth * 0.42f, cap = StrokeCap.Round)
            )
        }

        // Pose-specific guidance overlays (gaze line, bow angle arc, or Salam turn arrows)
        when (pose) {
            PrayerPose.QIYOM -> {
                // Soft dashed gaze line toward sajda spot on the mat
                drawLine(
                    color = jointGlow.copy(alpha = 0.55f),
                    start = Offset(headCenter.x + headRadius * 0.8f, headCenter.y + headRadius * 0.3f),
                    end = Offset(w * 0.68f, matY - 6f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawCircle(
                    color = jointGlow.copy(alpha = 0.65f),
                    radius = 6f,
                    center = Offset(w * 0.68f, matY - 6f)
                )
            }
            PrayerPose.RUKU, PrayerPose.SAJDA -> {
                // Highlighted posture alignment arc at hip
                val arcRadius = w.coerceAtMost(h) * 0.14f
                drawArc(
                    color = jointGlow.copy(alpha = 0.7f),
                    startAngle = -90f,
                    sweepAngle = if (pose == PrayerPose.RUKU) {
                        if (seatedMode) 28f else 85f
                    } else {
                        if (seatedMode) 56f else 115f
                    },
                    useCenter = false,
                    topLeft = Offset(hip.x - arcRadius, hip.y - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            PrayerPose.SALAM -> {
                // Animated left & right shoulder turn arcs around head
                val turnR = headRadius * 2.0f
                val rightActive = salamTurn >= 0f
                drawArc(
                    color = if (rightActive) accentHighlight else Color.White.copy(alpha = 0.32f),
                    startAngle = -35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(headCenter.x - turnR, headCenter.y - turnR),
                    size = Size(turnR * 2f, turnR * 2f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (!rightActive) accentHighlight else Color.White.copy(alpha = 0.32f),
                    startAngle = 145f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(headCenter.x - turnR, headCenter.y - turnR),
                    size = Size(turnR * 2f, turnR * 2f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
            else -> Unit
        }
    }
}

private fun DrawScope.drawMihrabBackdrop(w: Float, h: Float) {
    val archPath = Path().apply {
        moveTo(w * 0.18f, h * 0.88f)
        lineTo(w * 0.18f, h * 0.44f)
        quadraticTo(w * 0.50f, h * 0.04f, w * 0.82f, h * 0.44f)
        lineTo(w * 0.82f, h * 0.88f)
    }
    drawPath(
        path = archPath,
        color = Color.White.copy(alpha = 0.07f)
    )
    drawPath(
        path = archPath,
        color = NightGold.copy(alpha = 0.22f),
        style = Stroke(width = 2.5f, join = StrokeJoin.Round)
    )
    // Subtle crescent emblem at apex
    val apex = Offset(w * 0.50f, h * 0.14f)
    val r = (w.coerceAtMost(h) * 0.045f).coerceIn(7f, 14f)
    drawCircle(
        color = NightGold.copy(alpha = 0.25f),
        radius = r,
        center = apex
    )
    drawCircle(
        color = Color(0xFF0C523E),
        radius = r * 0.82f,
        center = Offset(apex.x + r * 0.36f, apex.y - r * 0.14f)
    )
}

private fun DrawScope.drawChairSilhouette(w: Float, h: Float, matY: Float) {
    val chairColor = Color.White.copy(alpha = 0.28f)
    val stroke = (w.coerceAtMost(h) * 0.022f).coerceIn(4f, 8f)
    val seatY = h * 0.62f
    val backX = w * 0.34f
    val frontX = w * 0.54f
    // Backrest + rear leg
    drawLine(chairColor, Offset(backX, h * 0.34f), Offset(backX, matY), strokeWidth = stroke, cap = StrokeCap.Round)
    // Seat
    drawLine(chairColor, Offset(backX, seatY), Offset(frontX, seatY), strokeWidth = stroke * 1.15f, cap = StrokeCap.Round)
    // Front leg
    drawLine(chairColor, Offset(frontX, seatY), Offset(frontX, matY), strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun keypointsFor(
    pose: PrayerPose,
    gender: Gender,
    seatedMode: Boolean
): PostureKeypoints {
    if (seatedMode) {
        return seatedKeypointsFor(pose = pose, gender = gender)
    }
    val isFemale = gender == Gender.FEMALE
    return when (pose) {
        PrayerPose.TAKBIR -> PostureKeypoints(
            headX = 0.48f, headY = 0.21f,
            shoulderX = 0.48f, shoulderY = 0.32f,
            hipX = 0.48f, hipY = 0.55f,
            kneeX = 0.49f, kneeY = 0.70f,
            footX = 0.49f, footY = 0.84f,
            elbowX = 0.58f, elbowY = if (isFemale) 0.38f else 0.33f,
            // Male: earlobes, Female: shoulder height
            handX = if (isFemale) 0.57f else 0.54f,
            handY = if (isFemale) 0.30f else 0.22f
        )
        PrayerPose.QIYOM, PrayerPose.QAVMA -> PostureKeypoints(
            headX = 0.48f, headY = 0.21f,
            shoulderX = 0.48f, shoulderY = 0.32f,
            hipX = 0.48f, hipY = 0.55f,
            kneeX = 0.49f, kneeY = 0.70f,
            footX = 0.49f, footY = 0.84f,
            elbowX = 0.55f, elbowY = if (isFemale) 0.42f else 0.47f,
            // Male: below navel, Female: on chest
            handX = 0.52f,
            handY = if (isFemale) 0.39f else 0.51f
        )
        PrayerPose.RUKU -> PostureKeypoints(
            headX = if (isFemale) 0.65f else 0.69f,
            headY = if (isFemale) 0.44f else 0.49f,
            shoulderX = if (isFemale) 0.58f else 0.61f,
            shoulderY = if (isFemale) 0.46f else 0.50f,
            hipX = 0.41f, hipY = 0.52f,
            kneeX = 0.44f, kneeY = 0.68f,
            footX = 0.43f, footY = 0.84f,
            elbowX = 0.54f, elbowY = 0.59f,
            handX = 0.47f, handY = 0.67f
        )
        PrayerPose.SAJDA -> PostureKeypoints(
            headX = 0.68f, headY = 0.79f,
            shoulderX = 0.59f, shoulderY = 0.71f,
            hipX = 0.41f, hipY = 0.59f,
            kneeX = 0.45f, kneeY = 0.81f,
            footX = 0.30f, footY = 0.83f,
            elbowX = if (isFemale) 0.57f else 0.60f,
            elbowY = if (isFemale) 0.79f else 0.76f,
            handX = 0.65f, handY = 0.83f
        )
        PrayerPose.QADA, PrayerPose.JALSA -> PostureKeypoints(
            headX = 0.45f, headY = 0.45f,
            shoulderX = 0.45f, shoulderY = 0.56f,
            hipX = 0.44f, hipY = 0.76f,
            kneeX = 0.59f, kneeY = 0.82f,
            footX = if (isFemale) 0.37f else 0.33f,
            footY = 0.83f,
            elbowX = 0.50f, elbowY = 0.68f,
            handX = 0.57f, handY = 0.77f
        )
        PrayerPose.SALAM -> PostureKeypoints(
            headX = 0.50f, headY = 0.43f,
            shoulderX = 0.50f, shoulderY = 0.55f,
            hipX = 0.50f, hipY = 0.75f,
            kneeX = 0.59f, kneeY = 0.82f,
            footX = 0.41f, footY = 0.83f,
            elbowX = 0.58f, elbowY = 0.67f,
            handX = 0.56f, handY = 0.76f
        )
        PrayerPose.UNKNOWN -> PostureKeypoints(
            headX = 0.48f, headY = 0.21f,
            shoulderX = 0.48f, shoulderY = 0.32f,
            hipX = 0.48f, hipY = 0.55f,
            kneeX = 0.49f, kneeY = 0.70f,
            footX = 0.49f, footY = 0.84f,
            elbowX = 0.55f, elbowY = 0.47f,
            handX = 0.52f, handY = 0.51f
        )
    }
}

private fun seatedKeypointsFor(pose: PrayerPose, gender: Gender): PostureKeypoints {
    val isFemale = gender == Gender.FEMALE
    val hipX = 0.42f
    val hipY = 0.59f
    val kneeX = 0.59f
    val kneeY = 0.61f
    val footX = 0.60f
    val footY = 0.84f

    fun leanedTorso(leanDeg: Float): Pair<Offset, Offset> {
        val rad = (leanDeg * PI / 180.0).toFloat()
        val torsoLen = 0.23f
        val neckLen = 0.10f
        val sx = hipX + sin(rad) * torsoLen
        val sy = hipY - cos(rad) * torsoLen
        val hx = sx + sin(rad) * neckLen
        val hy = sy - cos(rad) * neckLen
        return Offset(sx, sy) to Offset(hx, hy)
    }

    return when (pose) {
        PrayerPose.TAKBIR -> {
            val (s, h) = leanedTorso(0f)
            PostureKeypoints(
                headX = h.x, headY = h.y,
                shoulderX = s.x, shoulderY = s.y,
                hipX = hipX, hipY = hipY,
                kneeX = kneeX, kneeY = kneeY,
                footX = footX, footY = footY,
                elbowX = 0.52f, elbowY = 0.41f,
                handX = 0.50f, handY = if (isFemale) 0.34f else 0.27f
            )
        }
        PrayerPose.RUKU -> {
            // 24 deg gentle bow on chair
            val (s, h) = leanedTorso(24f)
            PostureKeypoints(
                headX = h.x, headY = h.y,
                shoulderX = s.x, shoulderY = s.y,
                hipX = hipX, hipY = hipY,
                kneeX = kneeX, kneeY = kneeY,
                footX = footX, footY = footY,
                elbowX = 0.55f, elbowY = 0.50f,
                handX = 0.59f, handY = 0.59f
            )
        }
        PrayerPose.SAJDA -> {
            // 54 deg deeper bow on chair (deeper than Ruku per Hanafi fiqh)
            val (s, h) = leanedTorso(54f)
            PostureKeypoints(
                headX = h.x, headY = h.y,
                shoulderX = s.x, shoulderY = s.y,
                hipX = hipX, hipY = hipY,
                kneeX = kneeX, kneeY = kneeY,
                footX = footX, footY = footY,
                elbowX = 0.60f, elbowY = 0.56f,
                handX = 0.62f, handY = 0.63f
            )
        }
        PrayerPose.SALAM -> {
            val (s, h) = leanedTorso(0f)
            PostureKeypoints(
                headX = h.x + 0.04f, headY = h.y,
                shoulderX = s.x + 0.04f, shoulderY = s.y,
                hipX = hipX + 0.04f, hipY = hipY,
                kneeX = kneeX, kneeY = kneeY,
                footX = footX, footY = footY,
                elbowX = 0.53f, elbowY = 0.50f,
                handX = 0.57f, handY = 0.58f
            )
        }
        else -> {
            val (s, h) = leanedTorso(0f)
            PostureKeypoints(
                headX = h.x, headY = h.y,
                shoulderX = s.x, shoulderY = s.y,
                hipX = hipX, hipY = hipY,
                kneeX = kneeX, kneeY = kneeY,
                footX = footX, footY = footY,
                elbowX = 0.50f, elbowY = if (isFemale) 0.45f else 0.49f,
                handX = if (pose == PrayerPose.QIYOM) 0.47f else 0.56f,
                handY = when {
                    pose == PrayerPose.QIYOM && isFemale -> 0.42f
                    pose == PrayerPose.QIYOM -> 0.52f
                    else -> 0.58f
                }
            )
        }
    }
}
