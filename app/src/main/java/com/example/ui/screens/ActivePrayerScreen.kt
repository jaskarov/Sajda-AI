package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.LocalRuntimePermissions
import com.example.audio.AudioCoachStatus
import com.example.audio.PrayerAudioCoach
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.PrayerTrack
import com.example.domain.model.lText
import com.example.ui.components.CameraPosePreview
import com.example.ui.components.RakatProgressIndicator
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NightEmeraldPrimary
import com.example.ui.theme.NightGold
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.PoseAlertCoral
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.PoseWarningAmber
import com.example.ui.viewmodel.PrayerCoachUiState
import kotlin.math.roundToInt

@Composable
fun ActivePrayerScreen(
    uiState: PrayerCoachUiState,
    onPoseResult: (PoseAnalysisSnapshot) -> Unit,
    onTogglePause: () -> Unit,
    onNextStep: () -> Unit,
    onStartNextTrack: () -> Unit = {},
    onExtendContinuation: () -> Unit = {},
    onSwitchToAudioOnly: () -> Unit,
    onSwitchToCamera: () -> Unit,
    onToggleCameraFacing: () -> Unit = {},
    onLifecyclePause: () -> Unit = {},
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val script = uiState.settings.script
    var savedOriginalVolume by remember { mutableStateOf<Int?>(null) }

    val shouldBoostVolume = uiState.settingsLoaded &&
        uiState.settings.autoBoostPrayerVolume &&
        uiState.fallbackReason != FallbackReason.LOW_LIGHT_DARK_ROOM

    // Auto-pause active prayer timer & audio when app enters background or phone call takes focus
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        onLifecyclePause()
    }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && savedOriginalVolume != null) {
                PrayerAudioCoach.restorePrayerStreamVolume(audioManager, savedOriginalVolume)
                savedOriginalVolume = null
            }
        }
    }

    // Only boost volume AFTER DataStore settings are confirmed loaded, and never when disabled or in dark room
    LaunchedEffect(uiState.settingsLoaded, shouldBoostVolume, uiState.isCompleted, uiState.pendingNextTrack) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@LaunchedEffect
        val shouldRestoreNow = (uiState.isCompleted && uiState.pendingNextTrack == null) || !shouldBoostVolume
        if (shouldRestoreNow) {
            if (savedOriginalVolume != null) {
                PrayerAudioCoach.restorePrayerStreamVolume(audioManager, savedOriginalVolume)
                savedOriginalVolume = null
            }
        } else if (savedOriginalVolume == null) {
            savedOriginalVolume = PrayerAudioCoach.boostPrayerStreamVolume(
                audioManager = audioManager,
                targetFraction = PrayerAudioCoach.PRAYER_VOLUME_TARGET_FRACTION
            )
        }
    }

    // Tactile vibration when a prayer rukn is confirmed and advances to the next movement
    LaunchedEffect(uiState.hapticFeedbackPulse) {
        if (uiState.hapticFeedbackPulse > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    if (uiState.isCompleted) {
        PrayerCompletedView(
            uiState = uiState,
            onStartNextTrack = onStartNextTrack,
            onExtendContinuation = onExtendContinuation,
            onFinish = onExit
        )
        return
    }

    val step = uiState.currentStep ?: return
    val runtimePermissions = LocalRuntimePermissions.current
    val hasCameraPermission = runtimePermissions.cameraGranted ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    val detectedPose = uiState.poseSnapshot.confirmedPose
    val isSeatedMode = uiState.settings.seatedPrayerMode
    val isPoseAligned = if (isSeatedMode) {
        when (step.expectedPose) {
            com.example.domain.model.PrayerPose.QIYOM,
            com.example.domain.model.PrayerPose.QAVMA,
            com.example.domain.model.PrayerPose.JALSA,
            com.example.domain.model.PrayerPose.QADA -> detectedPose == com.example.domain.model.PrayerPose.QIYOM ||
                detectedPose == com.example.domain.model.PrayerPose.QAVMA ||
                detectedPose == com.example.domain.model.PrayerPose.JALSA ||
                detectedPose == com.example.domain.model.PrayerPose.QADA
            else -> detectedPose == step.expectedPose
        }
    } else {
        when (step.expectedPose) {
            com.example.domain.model.PrayerPose.QAVMA ->
                detectedPose == com.example.domain.model.PrayerPose.QIYOM || detectedPose == com.example.domain.model.PrayerPose.QAVMA
            com.example.domain.model.PrayerPose.JALSA ->
                detectedPose == com.example.domain.model.PrayerPose.QADA || detectedPose == com.example.domain.model.PrayerPose.JALSA
            else -> detectedPose == step.expectedPose
        }
    }
    val hasLiveSkeleton = uiState.poseSnapshot.landmarks.isNotEmpty()
    // 2-Meter Teleprompter (Katta matn) mode defaults ON for large-text, seated/senior, and Audio-Only users,
    // and can be toggled anytime during Camera AI mode without stopping pose analysis.
    var isTeleprompterMode by rememberSaveable(
        uiState.settings.largeTextMode,
        uiState.settings.seatedPrayerMode,
        uiState.mode
    ) {
        mutableStateOf(
            uiState.settings.largeTextMode ||
                uiState.settings.seatedPrayerMode ||
                uiState.mode == CoachMode.AUDIO_ONLY
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isShortScreen = maxHeight < 580.dp
        val verticalGap = if (isShortScreen) 5.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = if (isShortScreen) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(verticalGap)
        ) {
            // Top Header: Rakat | Expected Pose | Exit
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = if (isShortScreen) 2.dp else 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("active_prayer_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = lText("Chiqish", "Чиқиш").get(script),
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${step.rakatNumber}-${
                                lText("Rakat", "Ракат").get(script)
                            } / ${step.totalRakats}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NightGold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isPoseAligned) PoseMatchedGreen.copy(alpha = 0.25f)
                        else ObsidianSurfaceVariant
                    ) {
                        Text(
                            text = "${lText("Holat:", "Ҳолат:").get(script)} ${
                                step.expectedPose.title.get(script)
                            } ${step.expectedPose.emoji}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPoseAligned) PoseMatchedGreen else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Visual Rakat-by-Rakat Progress Indicator (Current vs Next Rakat)
            RakatProgressIndicator(
                currentRakat = step.rakatNumber,
                totalRakats = step.totalRakats,
                currentStepIndex = uiState.currentStepIndex,
                steps = uiState.steps,
                nextContinuationTrack = uiState.track.nextContinuationTrack(),
                isPaused = uiState.isPaused,
                script = script
            )

            val activeQazoType = com.example.domain.model.QazoPrayerType.fromId(uiState.qazoTypeId)
            if (activeQazoType != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NightGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, NightGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_qazo_badge")
                ) {
                    Text(
                        text = lText(
                            "Qazo namozi: ${activeQazoType.title.latin} • Yakunlangach qazo daftaridan ayiriladi",
                            "Қазо намози: ${activeQazoType.title.cyrillic} • Якунлангач қазо дафтаридан айирилади"
                        ).get(script),
                        style = MaterialTheme.typography.labelMedium,
                        color = NightGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (uiState.isPaused && uiState.pausedByInterruption) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PoseAlertCoral.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, PoseAlertCoral.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interruption_pause_banner")
                ) {
                    Text(
                        text = lText(
                            "Qo‘ng‘iroq yoki uzilish sababli pauza qilindi • Davom etish uchun «Davom» tugmasini bosing",
                            "Қўнғироқ ёки узилиш сабабли пауза қилинди • Давом этиш учун «Давом» тугмасини босинг"
                        ).get(script),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFD6CE),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Fallback Banner if in Audio-Only mode
            if (uiState.mode == CoachMode.AUDIO_ONLY) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PoseWarningAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, PoseWarningAmber.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = PoseWarningAmber
                            )
                            Spacer(Modifier.width(8.dp))
                            val reasonText = when (uiState.fallbackReason) {
                                FallbackReason.LOW_LIGHT_DARK_ROOM -> lText(
                                    "Xona qorong‘i • Faqat Audio + Taymer rejimi",
                                    "Хона қоронғи • Фақат Аудио + Таймер режими"
                                ).get(script)
                                FallbackReason.CAMERA_PERMISSION_DENIED -> lText(
                                    "Kamera ruxsati yo‘q • Faqat Audio rejimi",
                                    "Камера рухсати йўқ • Фақат Аудио режими"
                                ).get(script)
                                else -> lText(
                                    "Faqat Audio + Taymer rejimi faol",
                                    "Фақат Аудио + Таймер режими фаол"
                                ).get(script)
                            }
                            Text(
                                text = reasonText,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFE4A8)
                            )
                        }

                        if (hasCameraPermission) {
                            OutlinedButton(
                                onClick = onSwitchToCamera,
                                border = BorderStroke(1.dp, NightEmeraldPrimary),
                                modifier = Modifier.testTag("resume_camera_button")
                            ) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = NightEmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    lText("Kamera", "Камера").get(script),
                                    color = NightEmeraldPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Stuck-Pose Auto-Rescue Notice in Camera AI mode
            if (uiState.mode == CoachMode.CAMERA_AI && uiState.stuckPoseRescueCountdownSec > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PoseWarningAmber.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, PoseWarningAmber.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stuck_pose_rescue_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = if (isShortScreen) 4.dp else 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lText(
                                "Holat aniqlanmadi • ${uiState.stuckPoseRescueCountdownSec} soniyada avtomatik davom etadi",
                                "Ҳолат аниқланмади • ${uiState.stuckPoseRescueCountdownSec} сонияда автоматик давом этади"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFE7A8),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = if (isShortScreen) 1 else 2,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.consecutiveStuckRescues >= 1) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onSwitchToAudioOnly,
                                border = BorderStroke(1.dp, PoseWarningAmber),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(if (isShortScreen) 32.dp else 38.dp)
                            ) {
                                Text(
                                    text = lText("Audio", "Аудио").get(script),
                                    color = Color(0xFFFFE7A8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area: Full-Width 2-Meter Teleprompter + Compact Collapsible Camera Strip
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val contentMaxHeight = maxHeight
                val isCompactHeight = contentMaxHeight < 360.dp
                val isVeryShort = contentMaxHeight < 250.dp
                val isCameraLive = uiState.mode == CoachMode.CAMERA_AI && hasCameraPermission

                val arabicFontSize = when {
                    isVeryShort -> 20.sp
                    isTeleprompterMode -> if (uiState.settings.largeTextMode || isSeatedMode) 30.sp else 27.sp
                    isCompactHeight -> 21.sp
                    else -> 24.sp
                }
                val arabicLineHeight = (arabicFontSize.value * 1.42f).sp

                val translitFontSize = when {
                    isVeryShort -> 18.sp
                    isTeleprompterMode -> if (uiState.settings.largeTextMode || isSeatedMode) 30.sp else 26.sp
                    isCompactHeight -> 19.sp
                    else -> 22.sp
                }
                val translitLineHeight = (translitFontSize.value * 1.38f).sp

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 5.dp else 8.dp)
                ) {
                    // Compact Top Utility Strip: Pose Status + Teleprompter Toggle + Camera Controls
                    if (isCameraLive) {
                        if (isTeleprompterMode) {
                            // Keep CameraX Pose Analysis mounted silently (1dp) while showing huge full-width text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(1.dp)) {
                                    CameraPosePreview(
                                        darkLuminanceThreshold = uiState.settings.darkRoomLuminanceThreshold,
                                        onPoseResult = onPoseResult,
                                        poseSnapshot = uiState.poseSnapshot,
                                        showSkeleton = false,
                                        isMatched = isPoseAligned,
                                        seatedMode = isSeatedMode,
                                        lowPowerMode = uiState.settings.lowPowerCameraMode,
                                        useFrontCamera = uiState.useFrontCamera,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPoseAligned) PoseMatchedGreen.copy(alpha = 0.2f)
                                    else ObsidianSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPoseAligned) PoseMatchedGreen.copy(alpha = 0.65f)
                                        else Color.White.copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("active_pose_status_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${detectedPose.title.get(script)} • ${
                                                when {
                                                    step.expectedPose == com.example.domain.model.PrayerPose.SALAM &&
                                                        uiState.salamProgressStage == 1 -> lText(
                                                        "1-yelka ✓",
                                                        "1-елка ✓"
                                                    ).get(script)
                                                    isPoseAligned -> lText("Tasdiqlandi ✓", "Тасдиқланди ✓").get(script)
                                                    isSeatedMode -> lText("Stulda", "Стулда").get(script)
                                                    else -> lText("AI faol", "AI фаол").get(script)
                                                }
                                            }",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isPoseAligned) PoseMatchedGreen else NightGold,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.testTag("active_pose_status_text")
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { isTeleprompterMode = false },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianSurfaceVariant,
                                    border = BorderStroke(1.dp, NightEmeraldPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier.testTag("active_teleprompter_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = NightEmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = lText("Kamera", "Камера").get(script),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NightEmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onSwitchToAudioOnly,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("active_switch_audio_button"),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = lText("Audio", "Аудио").get(script),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Compact Top Camera PiP Strip (scales gracefully on compact/split-screen heights)
                            val pipHeight: Dp = when {
                                isShortScreen || contentMaxHeight < 210.dp -> 62.dp
                                isCompactHeight -> 86.dp
                                else -> 112.dp
                            }
                            val pipWidth: Dp = when {
                                isShortScreen || contentMaxHeight < 210.dp -> 58.dp
                                isCompactHeight -> 78.dp
                                else -> 96.dp
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = pipWidth, height = pipHeight)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black)
                                ) {
                                    CameraPosePreview(
                                        darkLuminanceThreshold = uiState.settings.darkRoomLuminanceThreshold,
                                        onPoseResult = onPoseResult,
                                        poseSnapshot = uiState.poseSnapshot,
                                        showSkeleton = true,
                                        isMatched = isPoseAligned,
                                        seatedMode = isSeatedMode,
                                        lowPowerMode = uiState.settings.lowPowerCameraMode,
                                        useFrontCamera = uiState.useFrontCamera,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(3.dp)
                                    ) {
                                        Text(
                                            text = if (hasLiveSkeleton) {
                                                lText("Skelet", "Скелет").get(script)
                                            } else {
                                                lText("Kutilmoqda", "Кутилмоқда").get(script)
                                            },
                                            fontSize = 9.sp,
                                            color = if (hasLiveSkeleton) PoseMatchedGreen else Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 4.dp else 6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isPoseAligned) PoseMatchedGreen.copy(alpha = 0.18f)
                                        else ObsidianSurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPoseAligned) PoseMatchedGreen.copy(alpha = 0.6f)
                                            else Color.White.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("active_pose_status_chip")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = if (isShortScreen) 3.dp else 6.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = detectedPose.title.get(script),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isPoseAligned) PoseMatchedGreen else NightGold,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            if (!isShortScreen) {
                                                Text(
                                                    text = when {
                                                        step.expectedPose == com.example.domain.model.PrayerPose.SALAM &&
                                                            uiState.salamProgressStage == 1 -> lText(
                                                            "1-yelka ✓ • Narigi buring",
                                                            "1-елка ✓ • Нариги буринг"
                                                        ).get(script)
                                                        isPoseAligned -> lText("Tasdiqlandi ✓", "Тасдиқланди ✓").get(script)
                                                        isSeatedMode -> lText("Stulda • Imo-ishora", "Стулда • Имо-ишора").get(script)
                                                        else -> lText("Holat kutilmoqda", "Ҳолат кутилмоқда").get(script)
                                                    },
                                                    fontSize = 11.sp,
                                                    color = if (isPoseAligned) PoseMatchedGreen else Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = { isTeleprompterMode = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = NightGold.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, NightGold.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("active_teleprompter_toggle")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FormatSize,
                                                    contentDescription = null,
                                                    tint = NightGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = lText("2m Matn", "2м Матн").get(script),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NightGold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = onToggleCameraFacing,
                                            shape = RoundedCornerShape(10.dp),
                                            color = ObsidianSurfaceVariant,
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                            modifier = Modifier.testTag("active_flip_camera_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cameraswitch,
                                                contentDescription = lText("Kamerani almashtirish", "Камерани алмаштириш").get(script),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(18.dp)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = onSwitchToAudioOnly,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(if (isShortScreen) 32.dp else 36.dp)
                                                .testTag("active_switch_audio_button"),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = lText("Audio", "Аудио").get(script),
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Compact Audio-Only Timer Strip (keeps recitation card 100% full width)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, NightEmeraldPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = NightEmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (uiState.isPaused) {
                                            lText("Pauzada", "Паузада").get(script)
                                        } else {
                                            lText(
                                                "2m Katta matn • Audio taymer",
                                                "2м Катта матн • Аудио таймер"
                                            ).get(script)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${uiState.audioOnlyRemainingSec} s",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NightGold
                                )
                            }
                        }
                    }

                    // Full-Width High-Contrast Recitation Card (readable from 2 meters on joynamoz / chair)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("active_teleprompter_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(
                            width = if (isTeleprompterMode) 1.5.dp else 1.dp,
                            color = if (isTeleprompterMode) NightGold.copy(alpha = 0.55f)
                            else NightEmeraldPrimary.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = if (isShortScreen) 8.dp else 14.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 6.dp else 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = step.recitationTitle.get(script),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NightGold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isTeleprompterMode) {
                                    Surface(
                                        shape = CircleShape,
                                        color = NightGold.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = lText("2m Katta matn", "2м Катта матн").get(script),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NightGold,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            SurahRecitationVisualizer(
                                progress = uiState.playbackProgress,
                                isPaused = uiState.isPaused,
                                isAudioFinished = uiState.stepAudioDone || uiState.isPostRecitationCueActive,
                                isPostCueActive = uiState.isPostRecitationCueActive,
                                postCueText = step.postRecitationCue?.get(script),
                                script = script
                            )

                            Text(
                                text = step.arabicText,
                                fontSize = arabicFontSize,
                                lineHeight = arabicLineHeight,
                                textAlign = TextAlign.End,
                                color = NightEmeraldPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Audio-synchronized word-by-word high-contrast coloring
                            val words = step.getWords(script)
                            val highlightedCount = (words.size * uiState.playbackProgress)
                                .roundToInt()
                                .coerceIn(0, words.size)

                            val annotated = buildAnnotatedString {
                                words.forEachIndexed { idx, word ->
                                    val isSpoken = idx < highlightedCount
                                    withStyle(
                                        SpanStyle(
                                            color = if (isSpoken) NightGold else Color.White,
                                            fontWeight = if (isSpoken || isTeleprompterMode) FontWeight.Bold else FontWeight.Medium
                                        )
                                    ) {
                                        append(word)
                                    }
                                    if (idx < words.lastIndex) append(" ")
                                }
                            }

                            Text(
                                text = annotated,
                                fontSize = translitFontSize,
                                lineHeight = translitLineHeight
                            )

                            Text(
                                text = step.meaning.get(script),
                                style = if (isTeleprompterMode) MaterialTheme.typography.bodyMedium
                                else MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )

                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Transparent Audio Coach Status Line
            AudioStatusBanner(status = uiState.audioStatus)

            // Coach Instruction Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurfaceVariant,
                border = BorderStroke(1.dp, NightGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = if (isShortScreen) 6.dp else 10.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = lText("Ustoz ko‘rsatmasi:", "Устоз кўрсатмаси:").get(script),
                        style = MaterialTheme.typography.labelMedium,
                        color = NightGold
                    )
                    Text(
                        text = step.coachInstruction.get(script),
                        style = if (isShortScreen) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (isShortScreen) 1 else 2
                    )
                }
            }

            // Bottom Pause / Next Buttons
            val actionButtonHeight = if (isShortScreen) 48.dp else 52.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(actionButtonHeight)
                        .testTag("pause_resume_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Default.PlayArrow
                        else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isPaused) {
                            lText("Davom", "Давом").get(script)
                        } else {
                            lText("Pauza", "Пауза").get(script)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onNextStep,
                    modifier = Modifier
                        .weight(1f)
                        .height(actionButtonHeight)
                        .testTag("next_step_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightEmeraldPrimary,
                        contentColor = Color(0xFF00291D)
                    )
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = lText("Keyingi", "Кейинги").get(script),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPulseCard(
    width: Dp,
    height: Dp,
    remainingSec: Int,
    isPaused: Boolean,
    script: com.example.domain.model.TextScript
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier.size(width = width, height = height),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurfaceVariant,
        border = BorderStroke(1.5.dp, NightGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = NightGold,
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (isPaused) 1f else pulse)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${remainingSec}s",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = lText("Taymer", "Таймер").get(script),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AudioStatusBanner(status: AudioCoachStatus) {
    val (text, color) = when (status) {
        AudioCoachStatus.Idle -> return
        is AudioCoachStatus.PlayingOfflineAudio ->
            "Oflayn Qori MP3 (ExoPlayer) • ${status.assetLabel}" to PoseMatchedGreen
        is AudioCoachStatus.PlayingOfflineTts -> {
            val detail = if (!status.notice.isNullOrBlank()) " • ${status.notice}" else ""
            "Oflayn ovozli yo‘riqnoma (TTS)$detail" to NightEmeraldPrimary
        }
        is AudioCoachStatus.Error ->
            status.message to PoseAlertCoral
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 2
        )
    }
}

@Composable
private fun SurahRecitationVisualizer(
    progress: Float,
    isPaused: Boolean,
    isAudioFinished: Boolean,
    isPostCueActive: Boolean,
    postCueText: String?,
    script: com.example.domain.model.TextScript
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percent = (clampedProgress * 100f).roundToInt()
    val infiniteTransition = rememberInfiniteTransition(label = "surah_wave")
    val barMultipliers = listOf(0.45f, 0.85f, 0.6f, 1.0f, 0.7f, 0.9f, 0.5f)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_phase"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isAudioFinished) PoseMatchedGreen.copy(alpha = 0.14f)
        else ObsidianSurfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (isAudioFinished) PoseMatchedGreen.copy(alpha = 0.55f)
            else NightEmeraldPrimary.copy(alpha = 0.32f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_visualizer_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    barMultipliers.forEachIndexed { idx, base ->
                        val heightFraction = when {
                            isAudioFinished -> 0.35f
                            isPaused -> base * 0.45f
                            idx % 2 == 0 -> (base * phase).coerceIn(0.25f, 1f)
                            else -> (base * (1.35f - phase)).coerceIn(0.25f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((18f * heightFraction).dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAudioFinished) PoseMatchedGreen else NightGold
                                )
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isPostCueActive && !postCueText.isNullOrBlank() -> postCueText
                            isAudioFinished -> lText("Qiroat tugadi ✓", "Қироат тугади ✓").get(script)
                            else -> lText("Qiroat jarayoni", "Қироат жараёни").get(script)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAudioFinished) PoseMatchedGreen else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Text(
                    text = "$percent%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAudioFinished) PoseMatchedGreen else NightGold
                )
            }

            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .testTag("surah_playback_progress"),
                color = if (isAudioFinished) PoseMatchedGreen else NightGold,
                trackColor = Color.White.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun PrayerCompletedView(
    uiState: PrayerCoachUiState,
    onStartNextTrack: () -> Unit,
    onExtendContinuation: () -> Unit,
    onFinish: () -> Unit
) {
    val script = uiState.settings.script
    val nextTrack = uiState.pendingNextTrack
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.5.dp, PoseMatchedGreen.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PoseMatchedGreen,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = lText("Namozingiz qabul bo‘lsin!", "Намозингиз қабул бўлсин!").get(script),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${uiState.track.title.get(script)} • ${uiState.totalSessionSeconds}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = NightGold,
                    textAlign = TextAlign.Center
                )

                val completedQazoType = com.example.domain.model.QazoPrayerType.fromId(uiState.qazoTypeId)
                if (uiState.qazoDecrementedOnCompletion && completedQazoType != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PoseMatchedGreen.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, PoseMatchedGreen.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qazo_completed_badge")
                    ) {
                        Text(
                            text = lText(
                                "Qazo daftaridan 1 ta ${completedQazoType.title.latin} kamaytirildi ✓",
                                "Қазо дафтаридан 1 та ${completedQazoType.title.cyrillic} камайтирилди ✓"
                            ).get(script),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PoseMatchedGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                if (nextTrack != null) {
                    val isVitrNext = nextTrack == PrayerTrack.VITR_VOJIB
                    val isPostFarzSunnat = nextTrack == PrayerTrack.PESHIN_SUNNAT_2 ||
                        nextTrack == PrayerTrack.SHOM_SUNNAT ||
                        nextTrack == PrayerTrack.XUFTON_SUNNAT
                    val bannerTitleLat = when {
                        isVitrNext -> "Navbatda Vitr namozi. ${uiState.nextTrackCountdownSec} soniyada (${nextTrack.rakatCount} rakat) boshlanadi"
                        isPostFarzSunnat -> "Farz namozi tugadi. ${uiState.nextTrackCountdownSec} soniyada 2 rakat sunnat boshlanadi"
                        else -> "Sunnat namozi tugadi. ${uiState.nextTrackCountdownSec} soniyada Farz namozi (${nextTrack.rakatCount} rakat) boshlanadi"
                    }
                    val bannerTitleCyr = when {
                        isVitrNext -> "Навбатда Витр намози. ${uiState.nextTrackCountdownSec} сонияда (${nextTrack.rakatCount} ракат) бошланади"
                        isPostFarzSunnat -> "Фарз намози тугади. ${uiState.nextTrackCountdownSec} сонияда 2 ракат суннат бошланади"
                        else -> "Суннат намози тугади. ${uiState.nextTrackCountdownSec} сонияда Фарз намози (${nextTrack.rakatCount} ракат) бошланади"
                    }
                    val nextBtnLabel = when {
                        isVitrNext -> lText("Vitrni boshlash", "Витрни бошлаш").get(script)
                        isPostFarzSunnat -> lText("Sunnatni boshlash", "Суннатни бошлаш").get(script)
                        else -> lText("Farzni boshlash", "Фарзни бошлаш").get(script)
                    }

                    val totalSec = uiState.nextTrackCountdownTotalSec.coerceAtLeast(1)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NightEmeraldPrimary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, NightEmeraldPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sunnat_to_farz_continuation_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = lText(bannerTitleLat, bannerTitleCyr).get(script),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(
                                progress = {
                                    ((totalSec - uiState.nextTrackCountdownSec).coerceIn(0, totalSec)) / totalSec.toFloat()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = NightGold,
                                trackColor = Color.White.copy(alpha = 0.16f)
                            )
                            OutlinedButton(
                                onClick = onExtendContinuation,
                                modifier = Modifier.testTag("extend_continuation_button"),
                                border = BorderStroke(1.dp, NightGold.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NightGold)
                            ) {
                                Text(
                                    text = lText("+10 soniya kutish", "+10 сония кутиш").get(script),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onStartNextTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_next_track_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NightEmeraldPrimary,
                            contentColor = Color(0xFF00291D)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = nextBtnLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("prayer_completed_home_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = lText("Asosiy ekranga qaytish", "Асосий экранга қайтиш").get(script),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("prayer_completed_home_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NightEmeraldPrimary,
                            contentColor = Color(0xFF00291D)
                        )
                    ) {
                        Text(
                            text = lText("Asosiy ekranga qaytish", "Асосий экранга қайтиш").get(script),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
