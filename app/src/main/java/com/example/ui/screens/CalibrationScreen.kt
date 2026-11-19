package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.LocalRuntimePermissions
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.lText
import com.example.ui.components.CameraPosePreview
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.PoseWarningAmber
import com.example.ui.theme.SaffronGold
import com.example.ui.viewmodel.PrayerCoachUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen(
    uiState: PrayerCoachUiState,
    onPoseResult: (com.example.domain.model.PoseAnalysisSnapshot) -> Unit,
    onSpeakCalibrationPrompt: (String) -> Unit,
    onStartActiveSession: (CoachMode, FallbackReason) -> Unit,
    onSeatedModeToggle: (Boolean) -> Unit = {},
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val script = uiState.settings.script

    // Keep screen on during calibration & prayer
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val runtimePermissions = LocalRuntimePermissions.current
    var hasCameraPermission by remember(runtimePermissions.cameraGranted) {
        mutableStateOf(
            runtimePermissions.cameraGranted || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var releasingForTransition by remember { mutableStateOf(false) }

    fun navigateToSession(mode: CoachMode, reason: FallbackReason) {
        if (releasingForTransition) return
        releasingForTransition = true
        coroutineScope.launch {
            delay(80L)
            onStartActiveSession(mode, reason)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
        if (!granted) {
            navigateToSession(CoachMode.AUDIO_ONLY, FallbackReason.CAMERA_PERMISSION_DENIED)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Automatic fallback when room is too dark
    LaunchedEffect(uiState.poseSnapshot.isLowLight) {
        if (hasCameraPermission && uiState.poseSnapshot.isLowLight && !releasingForTransition) {
            delay(900L)
            navigateToSession(CoachMode.AUDIO_ONLY, FallbackReason.LOW_LIGHT_DARK_ROOM)
        }
    }

    // Voice confirmation when body fits inside calibration frame
    var voiceConfirmed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.poseSnapshot.isFullBodyVisible) {
        if (uiState.poseSnapshot.isFullBodyVisible && !voiceConfirmed) {
            voiceConfirmed = true
            onSpeakCalibrationPrompt(
                "Joyingiz qulay! Namozni boshlaymiz."
            )
            delay(1800L)
            navigateToSession(CoachMode.CAMERA_AI, FallbackReason.NONE)
        }
    }

    val isReady = uiState.poseSnapshot.isFullBodyVisible
    val frameColor by animateColorAsState(
        targetValue = when {
            uiState.poseSnapshot.isLowLight -> PoseWarningAmber
            isReady -> PoseMatchedGreen
            else -> Color.White.copy(alpha = 0.85f)
        },
        label = "calibration_frame_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (hasCameraPermission) {
            CameraPosePreview(
                darkLuminanceThreshold = uiState.settings.darkRoomLuminanceThreshold,
                onPoseResult = onPoseResult,
                poseSnapshot = uiState.poseSnapshot,
                showSkeleton = true,
                isMatched = isReady,
                enabled = !releasingForTransition,
                seatedMode = uiState.settings.seatedPrayerMode,
                lowPowerMode = uiState.settings.lowPowerCameraMode,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = SaffronGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = lText(
                            "Kamera ruxsati berilmadi — Faqat Audio rejimiga o‘tilmoqda...",
                            "Камера рухсати берилмади — Фақат Аудио режимига ўтилмоқда..."
                        ).get(script),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Silhouette Guide Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectW = size.width * 0.66f
            val rectH = size.height * 0.56f
            val left = (size.width - rectW) / 2f
            val top = size.height * 0.14f

            drawRoundRect(
                color = frameColor,
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(36f, 36f),
                style = Stroke(
                    width = if (isReady) 8f else 5f,
                    pathEffect = if (isReady) null
                    else PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
                )
            )

            // Stylized head & shoulders silhouette guide inside the frame
            val cx = size.width / 2f
            drawCircle(
                color = frameColor.copy(alpha = 0.4f),
                radius = rectW * 0.14f,
                center = Offset(cx, top + rectH * 0.16f),
                style = Stroke(width = 4f)
            )
            drawRoundRect(
                color = frameColor.copy(alpha = 0.35f),
                topLeft = Offset(cx - rectW * 0.22f, top + rectH * 0.28f),
                size = Size(rectW * 0.44f, rectH * 0.42f),
                cornerRadius = CornerRadius(40f, 40f),
                style = Stroke(width = 4f)
            )
        }

        // Top Bar + Compact Phone Placement Visual Guide
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calibration_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = lText("Chiqish", "Чиқиш").get(script),
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = uiState.track.title.get(script),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                Spacer(Modifier.size(48.dp))
            }

            val qazoType = com.example.domain.model.QazoPrayerType.fromId(uiState.qazoTypeId)
            if (qazoType != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SaffronGold.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calibration_qazo_badge")
                ) {
                    Text(
                        text = lText(
                            "Qazo namozi rejimi • Niyat: Zimmamdagi qazo ${qazoType.title.latin} farzini o‘qish",
                            "Қазо намози режими • Ният: Зиммамдаги қазо ${qazoType.title.cyrillic} фарзини ўқиш"
                        ).get(script),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFE7B3),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calibration_placement_guide_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SaffronGold.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = SaffronGold,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = lText(
                                "Joynamozdan 2 qadam (1.8 – 2.2 metr) masofaga qo‘ying",
                                "Жойнамоздан 2 қадам (1.8 – 2.2 метр) масофага қўйинг"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PoseMatchedGreen.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = null,
                                tint = PoseMatchedGreen,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = lText(
                                "Telefonni kitob yoki devorga biroz qiya (70°–80°) suyab qo‘ying",
                                "Телефонни китоб ёки деворга бироз қия (70°–80°) суяб қўйинг"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Seated / Chair Prayer Accessibility Toggle right before starting prayer
            val isSeatedMode = uiState.settings.seatedPrayerMode
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSeatedMode) PoseMatchedGreen.copy(alpha = 0.24f)
                else Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(
                    1.dp,
                    if (isSeatedMode) PoseMatchedGreen else Color.White.copy(alpha = 0.24f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calibration_seated_mode_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessible,
                            contentDescription = null,
                            tint = if (isSeatedMode) PoseMatchedGreen else SaffronGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = lText(
                                "Stulda / O‘tirib o‘qish (Imo-ishora)",
                                "Стулда / Ўтириб ўқиш (Имо-ишора)"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Switch(
                        checked = isSeatedMode,
                        onCheckedChange = onSeatedModeToggle,
                        modifier = Modifier.testTag("calibration_seated_mode_toggle")
                    )
                }
            }
        }

        // Bottom Guidance & Controls
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.poseSnapshot.isLowLight) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PoseWarningAmber.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = Color(0xFF1E1400)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = lText(
                                "Xona qorong‘i aniqlandi — Faqat Audio rejimiga o‘tilmoqda...",
                                "Хона қоронғи аниқланди — Фақат Аудио режимига ўтилмоқда..."
                            ).get(script),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF1E1400)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isReady) PoseMatchedGreen.copy(alpha = 0.92f)
                else Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, frameColor.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isReady) {
                            lText(
                                "🟢 Joyingiz qulay! Boshlaymiz:",
                                "🟢 Жойингиз қулай! Бошлаймиз:"
                            ).get(script)
                        } else {
                            uiState.poseSnapshot.calibrationHint.get(script)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = lText(
                            "\"Qo‘llaringizni ko‘tarib Allohu akbar deng\"",
                            "\"Қўлларингизни кўтариб Аллоҳу акбар денг\""
                        ).get(script),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        navigateToSession(CoachMode.AUDIO_ONLY, FallbackReason.USER_SELECTED)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("switch_audio_only_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = lText("Faqat Audio", "Фақат Аудио").get(script),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                val startBtnContainer by animateColorAsState(
                    targetValue = if (isReady || !hasCameraPermission) PoseMatchedGreen
                    else Color.White.copy(alpha = 0.2f),
                    label = "calibration_start_btn_color"
                )
                Button(
                    onClick = {
                        val mode = if (hasCameraPermission) CoachMode.CAMERA_AI else CoachMode.AUDIO_ONLY
                        val reason = if (hasCameraPermission) FallbackReason.NONE
                        else FallbackReason.CAMERA_PERMISSION_DENIED
                        navigateToSession(mode, reason)
                    },
                    enabled = isReady || !hasCameraPermission,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("calibration_start_now_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = startBtnContainer,
                        contentColor = Color(0xFF00291D),
                        disabledContainerColor = Color.White.copy(alpha = 0.16f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = lText("Boshlash", "Бошлаш").get(script),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
