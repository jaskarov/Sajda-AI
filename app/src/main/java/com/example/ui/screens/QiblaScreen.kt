package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.lText
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.PoseWarningAmber
import com.example.ui.theme.SaffronGold
import com.example.ui.viewmodel.CompassUiState
import com.example.ui.viewmodel.MainUiState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    uiState: MainUiState,
    compassState: CompassUiState = CompassUiState(),
    onDetectNearestCity: () -> String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val script = uiState.settings.script
    val schedule = uiState.schedule ?: return

    val qiblaBearing = schedule.qiblaBearingDegrees.toFloat()
    val azimuth = compassState.compassAzimuth
    val displayQiblaDeg = ((qiblaBearing.roundToInt() % 360) + 360) % 360
    val displayAzimuthDeg = ((azimuth.roundToInt() % 360) + 360) % 360
    val kaabaDistanceKm = remember(schedule.city.latitude, schedule.city.longitude) {
        UzbekistanCities.calculateDistanceToKaabaKm(
            schedule.city.latitude,
            schedule.city.longitude
        )
    }

    var locationMessage by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val detected = onDetectNearestCity()
            locationMessage = if (detected != null) {
                lText("Eng yaqin shahar: $detected", "Энг яқин шаҳар: $detected").get(script)
            } else {
                lText(
                    "Joylashuv topilmadi, shaharni ro‘yxatdan tanlang",
                    "Жойлашув топилмади, шаҳарни рўйхатдан танланг"
                ).get(script)
            }
        } else {
            locationMessage = lText(
                "Joylashuv ruxsati berilmadi",
                "Жойлашув рухсати берилмади"
            ).get(script)
        }
    }

    // Shortest signed angle difference from current device heading to Qibla in [-180, +180]
    val diff = ((qiblaBearing - azimuth + 540f) % 360f) - 180f
    val isAligned = abs(diff) <= 5f

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    // Continuous shortest-path angle tracking prevents 359° -> 0° spin-around glitches
    var continuousDialTarget by remember { mutableFloatStateOf(-azimuth) }
    LaunchedEffect(azimuth) {
        val desired = -azimuth
        var delta = (desired - continuousDialTarget) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        continuousDialTarget += delta
    }

    var continuousQiblaTarget by remember { mutableFloatStateOf(qiblaBearing - azimuth) }
    LaunchedEffect(azimuth, qiblaBearing) {
        val desired = qiblaBearing - azimuth
        var delta = (desired - continuousQiblaTarget) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        continuousQiblaTarget += delta
    }

    val animatedDialAngle by animateFloatAsState(
        targetValue = continuousDialTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dial_rotation"
    )
    val animatedQiblaAngle by animateFloatAsState(
        targetValue = continuousQiblaTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "qibla_rotation"
    )
    val animatedTiltX by animateFloatAsState(
        targetValue = compassState.tiltX.coerceIn(-1f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tilt_x"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = compassState.tiltY.coerceIn(-1f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tilt_y"
    )

    val turnGuidance = remember(diff, isAligned, script) {
        val absDeg = abs(diff).roundToInt()
        when {
            isAligned -> lText(
                "Qibla yo‘nalishi topildi!",
                "Қибла йўналиши топилди!"
            ).get(script)
            diff > 0f -> lText(
                "O‘ngga $absDeg° buriling",
                "Ўнгга $absDeg° бурилинг"
            ).get(script)
            else -> lText(
                "Chapga $absDeg° buriling",
                "Чапга $absDeg° бурилинг"
            ).get(script)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lText("Qibla kompasi", "Қибла компаси").get(script),
                    style = MaterialTheme.typography.headlineMedium
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = schedule.city.name.get(script),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isAligned) PoseMatchedGreen.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    1.dp,
                    if (isAligned) PoseMatchedGreen.copy(alpha = 0.55f)
                    else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Explore,
                        contentDescription = null,
                        tint = if (isAligned) PoseMatchedGreen else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = turnGuidance,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAligned) PoseMatchedGreen
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!compassState.isLevel) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PoseWarningAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PoseWarningAmber.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_level_hint_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenLockRotation,
                            contentDescription = null,
                            tint = PoseWarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = lText(
                                "Aniq natija uchun telefonni yer bilan tekis (gorizontal) tuting",
                                "Аниқ натижа учун телефонни ер билан текис (горизонтал) тутинг"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Compass Dial Canvas
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(4.dp)
                    .testTag("qibla_compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                val outlineColor = MaterialTheme.colorScheme.outline
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                val activeRingColor = if (isAligned) PoseMatchedGreen else primaryColor
                val needleAccent = if (isAligned) PoseMatchedGreen else SaffronGold

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val outerRadius = size.minDimension / 2f - 12f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Fixed top phone heading lubber triangle at 12 o'clock
                    val lubberTipY = center.y - outerRadius - 8f
                    val lubberPath = Path().apply {
                        moveTo(center.x, lubberTipY + 14f)
                        lineTo(center.x - 10f, lubberTipY)
                        lineTo(center.x + 10f, lubberTipY)
                        close()
                    }
                    drawPath(
                        path = lubberPath,
                        color = if (isAligned) PoseMatchedGreen else primaryColor
                    )

                    // Dial background with subtle depth gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                surfaceColor,
                                surfaceVariantColor.copy(alpha = 0.45f)
                            ),
                            center = center,
                            radius = outerRadius
                        ),
                        radius = outerRadius,
                        center = center
                    )

                    // Inner crosshairs, concentric guide ring & accelerometer bubble level target
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.25f),
                        radius = outerRadius * 0.62f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    val levelZoneRadius = outerRadius * 0.24f
                    val levelColor = if (compassState.isLevel) PoseMatchedGreen else PoseWarningAmber
                    drawCircle(
                        color = levelColor.copy(alpha = 0.28f),
                        radius = levelZoneRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(center.x - outerRadius * 0.52f, center.y),
                        end = Offset(center.x + outerRadius * 0.52f, center.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(center.x, center.y - outerRadius * 0.52f),
                        end = Offset(center.x, center.y + outerRadius * 0.52f),
                        strokeWidth = 2f
                    )
                    val bubbleCenter = Offset(
                        x = center.x + animatedTiltX * (levelZoneRadius * 0.85f),
                        y = center.y + animatedTiltY * (levelZoneRadius * 0.85f)
                    )
                    drawCircle(
                        color = levelColor.copy(alpha = 0.26f),
                        radius = 12f,
                        center = bubbleCenter
                    )
                    drawCircle(
                        color = levelColor.copy(alpha = 0.78f),
                        radius = 6f,
                        center = bubbleCenter
                    )

                    // Outer bezel ring
                    drawCircle(
                        color = activeRingColor.copy(alpha = if (isAligned) 0.95f else 0.7f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = if (isAligned) 8f else 4.5f)
                    )

                    // Rotating Dial: degree ticks + cardinal markers (N / E / S / W)
                    rotate(degrees = animatedDialAngle, pivot = center) {
                        for (deg in 0 until 360 step 5) {
                            val isCardinal = deg % 90 == 0
                            val isMajor = deg % 30 == 0
                            val isMedium = deg % 15 == 0
                            val rad = Math.toRadians(deg.toDouble() - 90.0)
                            val tickLen = when {
                                isCardinal -> 26f
                                isMajor -> 20f
                                isMedium -> 14f
                                else -> 8f
                            }
                            val inner = outerRadius - tickLen
                            val outer = outerRadius - 4f
                            drawLine(
                                color = when {
                                    deg == 0 -> Color(0xFFD94A4A)
                                    isCardinal -> primaryColor
                                    isMajor -> onSurfaceColor.copy(alpha = 0.65f)
                                    else -> outlineColor.copy(alpha = 0.45f)
                                },
                                start = Offset(
                                    center.x + (inner * cos(rad)).toFloat(),
                                    center.y + (inner * sin(rad)).toFloat()
                                ),
                                end = Offset(
                                    center.x + (outer * cos(rad)).toFloat(),
                                    center.y + (outer * sin(rad)).toFloat()
                                ),
                                strokeWidth = when {
                                    isCardinal -> 5f
                                    isMajor -> 3f
                                    else -> 1.5f
                                },
                                cap = StrokeCap.Round
                            )
                        }

                        val cardinals = listOf(
                            0 to "N",
                            90 to "E",
                            180 to "S",
                            270 to "W"
                        )
                        val labelRadius = outerRadius - 50f
                        cardinals.forEach { (deg, label) ->
                            val rad = Math.toRadians(deg.toDouble() - 90.0)
                            val lx = center.x + (labelRadius * cos(rad)).toFloat()
                            val ly = center.y + (labelRadius * sin(rad)).toFloat()
                            val layoutResult = textMeasurer.measure(
                                text = label,
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (deg == 0) 17.sp else 15.sp,
                                    color = if (deg == 0) Color(0xFFD94A4A) else onSurfaceVariantColor
                                )
                            )
                            // Counter-rotate label text so letters remain upright
                            rotate(degrees = -animatedDialAngle, pivot = Offset(lx, ly)) {
                                drawText(
                                    textLayoutResult = layoutResult,
                                    topLeft = Offset(
                                        lx - layoutResult.size.width / 2f,
                                        ly - layoutResult.size.height / 2f
                                    )
                                )
                            }
                        }
                    }

                    // Rotating Qibla Needle + High-Contrast Kaaba Emblem
                    rotate(degrees = animatedQiblaAngle, pivot = center) {
                        // Target alignment arc on the dial rim
                        drawArc(
                            color = needleAccent.copy(alpha = if (isAligned) 0.45f else 0.25f),
                            startAngle = -102f,
                            sweepAngle = 24f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - outerRadius + 2f,
                                center.y - outerRadius + 2f
                            ),
                            size = Size((outerRadius - 2f) * 2f, (outerRadius - 2f) * 2f),
                            style = Stroke(width = 10f, cap = StrokeCap.Round)
                        )

                        // Back tail
                        val tailPath = Path().apply {
                            moveTo(center.x, center.y + outerRadius * 0.42f)
                            lineTo(center.x - 10f, center.y)
                            lineTo(center.x + 10f, center.y)
                            close()
                        }
                        drawPath(
                            path = tailPath,
                            color = outlineColor.copy(alpha = 0.5f)
                        )

                        // Front Qibla pointer
                        val tipY = center.y - outerRadius + 56f
                        val needlePath = Path().apply {
                            moveTo(center.x, tipY)
                            lineTo(center.x - 15f, center.y)
                            lineTo(center.x, center.y - 12f)
                            lineTo(center.x + 15f, center.y)
                            close()
                        }
                        drawPath(
                            path = needlePath,
                            color = needleAccent
                        )

                        // High-contrast Kaaba icon at Qibla direction on the ring
                        val kaabaSize = 28f
                        val kaabaTopLeft = Offset(
                            center.x - kaabaSize / 2f,
                            center.y - outerRadius + 22f
                        )
                        drawRoundRect(
                            color = Color(0xFF141816),
                            topLeft = kaabaTopLeft,
                            size = Size(kaabaSize, kaabaSize),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                        drawRoundRect(
                            color = if (isAligned) PoseMatchedGreen else SaffronGold,
                            topLeft = kaabaTopLeft,
                            size = Size(kaabaSize, kaabaSize),
                            cornerRadius = CornerRadius(5f, 5f),
                            style = Stroke(width = 3f)
                        )
                        // Gold Kiswah band across upper third of Kaaba emblem
                        drawLine(
                            color = SaffronGold,
                            start = Offset(kaabaTopLeft.x + 3f, kaabaTopLeft.y + 9f),
                            end = Offset(kaabaTopLeft.x + kaabaSize - 3f, kaabaTopLeft.y + 9f),
                            strokeWidth = 3f
                        )

                        // Center pivot cap
                        drawCircle(
                            color = primaryColor,
                            radius = 15f,
                            center = center
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 6f,
                            center = center
                        )
                    }
                }
            }

            // Telemetry card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lText("Qibla burchagi", "Қибла бурчаги").get(script),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$displayQiblaDeg°",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lText("Joriy yo‘nalish", "Жорий йўналиш").get(script),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$displayAzimuthDeg°",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.testTag("compass_azimuth_value")
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lText("Ka’bagacha", "Каъбагача").get(script),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$kaabaDistanceKm km",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SaffronGold,
                                modifier = Modifier.testTag("qibla_kaaba_distance")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    val accuracyLabel = when (compassState.compassAccuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                            lText("Yuqori", "Юқори").get(script)
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                            lText("O‘rta", "Ўрта").get(script)
                        else -> lText("Past", "Паст").get(script)
                    }
                    val levelLabel = if (compassState.isLevel) {
                        lText("Tekis ✓", "Текис ✓").get(script)
                    } else {
                        lText(
                            "${compassState.tiltDegrees.roundToInt()}° egilgan",
                            "${compassState.tiltDegrees.roundToInt()}° эгилган"
                        ).get(script)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lText(
                                "Magnetometr + Akselerometr • $accuracyLabel",
                                "Магнитометр + Акселерометр • $accuracyLabel"
                            ).get(script),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = levelLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (compassState.isLevel) PoseMatchedGreen else PoseWarningAmber
                        )
                    }
                }
            }

            if (compassState.hasCompassSensor &&
                compassState.compassAccuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PoseWarningAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PoseWarningAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = PoseWarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = lText(
                                "Kompas aniqligi past — telefonni havoda 8 shaklida aylantiring.",
                                "Компас аниқлиги паст — телефонни ҳавода 8 шаклида айлантиринг."
                            ).get(script),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (fineGranted || coarseGranted) {
                        val detected = onDetectNearestCity()
                        locationMessage = if (detected != null) {
                            lText("Shahar yangilandi: $detected", "Шаҳар янгиланди: $detected")
                                .get(script)
                        } else {
                            lText("GPS signali topilmadi", "GPS сигнали топилмади").get(script)
                        }
                    } else {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("detect_city_location_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    lText(
                        "Joylashuv bo‘yicha shaharni aniqlash",
                        "Жойлашув бўйича шаҳарни аниқлаш"
                    ).get(script)
                )
            }

            if (locationMessage != null) {
                Text(
                    text = locationMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            if (!compassState.hasCompassSensor) {
                Text(
                    text = lText(
                        "Ushbu qurilmada magnitometr sensori aniqlanmadi. Yuqoridagi Qibla burchagidan foydalaning.",
                        "Ушбу қурилмада магнитометр сенсори аниқланмади. Юқоридаги Қибла бурчагидан фойдаланинг."
                    ).get(script),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
