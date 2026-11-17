package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.LocalRuntimePermissions
import com.example.audio.AudioCoachStatus
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PoseAnalysisSnapshot
import com.example.domain.model.lText
import com.example.ui.components.CameraPosePreview
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
    onSwitchToAudioOnly: () -> Unit,
    onSwitchToCamera: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val script = uiState.settings.script

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (uiState.isCompleted) {
        PrayerCompletedView(
            uiState = uiState,
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
    val isPoseAligned = detectedPose == step.expectedPose
    val hasLiveSkeleton = uiState.poseSnapshot.landmarks.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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

            // Progress bar across steps
            val totalSteps = uiState.steps.size.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { (uiState.currentStepIndex + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = NightEmeraldPrimary,
                trackColor = ObsidianSurfaceVariant
            )

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

            // Main Content Area: PiP Camera + Synchronized Responsive Recitation Text
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isCompactHeight = maxHeight < 360.dp
                val isNarrowWidth = maxWidth < 340.dp
                val panelWidth: Dp = if (isNarrowWidth) 120.dp else 138.dp
                val previewHeight: Dp = if (isCompactHeight) 156.dp else 196.dp

                val arabicFontSize = if (isCompactHeight) 19.sp else 23.sp
                val arabicLineHeight = if (isCompactHeight) 29.sp else 34.sp
                val translitFontSize = if (isCompactHeight) 16.sp else 19.sp
                val translitLineHeight = if (isCompactHeight) 23.sp else 28.sp

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Mini Camera + Skeleton OR Audio Pulse Panel
                    Column(
                        modifier = Modifier
                            .width(panelWidth)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.mode == CoachMode.CAMERA_AI && hasCameraPermission) {
                            Box(
                                modifier = Modifier
                                    .size(width = panelWidth, height = previewHeight)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black)
                            ) {
                                CameraPosePreview(
                                    darkLuminanceThreshold = uiState.settings.darkRoomLuminanceThreshold,
                                    onPoseResult = onPoseResult,
                                    poseSnapshot = uiState.poseSnapshot,
                                    showSkeleton = true,
                                    isMatched = isPoseAligned,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Skeleton Overlay status badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = if (hasLiveSkeleton) {
                                            lText("Skelet faol", "Скелет фаол").get(script)
                                        } else {
                                            lText("Tana kutilmoqda", "Тана кутилмоқда").get(script)
                                        },
                                        fontSize = 10.sp,
                                        color = if (hasLiveSkeleton) PoseMatchedGreen else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Live angle telemetry chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = detectedPose.title.get(script),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPoseAligned) PoseMatchedGreen else NightGold,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "θt: ${uiState.poseSnapshot.torsoAngle.roundToInt()}° • θk: ${uiState.poseSnapshot.kneeAngle.roundToInt()}°",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onSwitchToAudioOnly,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("active_switch_audio_button"),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = lText("Audio", "Аудио").get(script),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            AudioPulseCard(
                                width = panelWidth,
                                height = previewHeight,
                                remainingSec = uiState.audioOnlyRemainingSec,
                                isPaused = uiState.isPaused,
                                script = script
                            )
                        }
                    }

                    // Right: Responsive Scrollable Surah & Audio-Synchronized Words
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, NightEmeraldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = step.recitationTitle.get(script),
                                style = MaterialTheme.typography.labelLarge,
                                color = NightGold,
                                letterSpacing = 0.8.sp
                            )

                            Text(
                                text = step.arabicText,
                                fontSize = arabicFontSize,
                                lineHeight = arabicLineHeight,
                                textAlign = TextAlign.End,
                                color = NightEmeraldPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Audio-synchronized word-by-word coloring
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
                                            fontWeight = if (isSpoken) FontWeight.Bold else FontWeight.Medium
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
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = lText("Ustoz ko‘rsatmasi:", "Устоз кўрсатмаси:").get(script),
                        style = MaterialTheme.typography.labelMedium,
                        color = NightGold
                    )
                    Text(
                        text = step.coachInstruction.get(script),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }

            // Bottom Pause / Next Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
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
                        .height(52.dp)
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
private fun PrayerCompletedView(
    uiState: PrayerCoachUiState,
    onFinish: () -> Unit
) {
    val script = uiState.settings.script
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
                    color = NightGold
                )
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
