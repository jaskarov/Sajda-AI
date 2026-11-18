package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PrayerStep
import com.example.domain.model.PrayerTrack
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NightEmeraldPrimary
import com.example.ui.theme.NightGold
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.PoseMatchedGreen

private enum class RakatUnitState {
    COMPLETED,
    CURRENT,
    NEXT,
    UPCOMING
}

@Composable
fun RakatProgressIndicator(
    currentRakat: Int,
    totalRakats: Int,
    currentStepIndex: Int,
    steps: List<PrayerStep>,
    nextContinuationTrack: PrayerTrack?,
    isPaused: Boolean,
    script: TextScript,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalRakats.coerceAtLeast(1)
    val safeCurrent = currentRakat.coerceIn(1, safeTotal)
    val currentStep = steps.getOrNull(currentStepIndex)

    val infiniteTransition = rememberInfiniteTransition(label = "current_rakat_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rakat_badge_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rakat_progress_indicator"),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary row: which rakat is active right now & which rakat/track comes next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NightGold)
                    )
                    Text(
                        text = lText(
                            "$safeCurrent-rakat bajarilmoqda",
                            "$safeCurrent-ракат бажарилмоқда"
                        ).get(script),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NightGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("rakat_current_summary")
                    )
                }

                val hasNextRakat = safeCurrent < safeTotal
                val nextPillColor = if (hasNextRakat || nextContinuationTrack != null) {
                    NightEmeraldPrimary
                } else {
                    PoseMatchedGreen
                }
                val nextSummaryLabel = when {
                    hasNextRakat -> lText(
                        "Keyingi: ${safeCurrent + 1}-rakat",
                        "Кейинги: ${safeCurrent + 1}-ракат"
                    ).get(script)
                    nextContinuationTrack == PrayerTrack.VITR_VOJIB -> lText(
                        "Keyingi: Vitr",
                        "Кейинги: Витр"
                    ).get(script)
                    nextContinuationTrack != null -> lText(
                        "Keyingi: Farz",
                        "Кейинги: Фарз"
                    ).get(script)
                    else -> lText("Yakuniy rakat", "Якуний ракат").get(script)
                }

                Surface(
                    shape = CircleShape,
                    color = nextPillColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, nextPillColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasNextRakat || nextContinuationTrack != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = nextPillColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = nextSummaryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = nextPillColor,
                            maxLines = 1,
                            modifier = Modifier.testTag("rakat_next_summary")
                        )
                    }
                }
            }

            // Segmented individual rakat units row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (rakat in 1..safeTotal) {
                    val unitState = when {
                        rakat < safeCurrent -> RakatUnitState.COMPLETED
                        rakat == safeCurrent -> RakatUnitState.CURRENT
                        rakat == safeCurrent + 1 -> RakatUnitState.NEXT
                        else -> RakatUnitState.UPCOMING
                    }

                    val rakatSteps = steps.filter { it.rakatNumber == rakat }
                    val intraRakatProgress = when (unitState) {
                        RakatUnitState.COMPLETED -> 1f
                        RakatUnitState.CURRENT -> {
                            val idxInRakat = if (currentStep != null) {
                                rakatSteps.indexOfFirst { it.index == currentStep.index }
                            } else {
                                -1
                            }
                            if (rakatSteps.isNotEmpty() && idxInRakat >= 0) {
                                ((idxInRakat + 1).toFloat() / rakatSteps.size.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.35f
                            }
                        }
                        RakatUnitState.NEXT, RakatUnitState.UPCOMING -> 0f
                    }

                    RakatUnitChip(
                        rakatNumber = rakat,
                        state = unitState,
                        progress = intraRakatProgress,
                        pulseScale = if (unitState == RakatUnitState.CURRENT && !isPaused) pulseScale else 1f,
                        script = script,
                        modifier = Modifier.weight(1f)
                    )

                    if (rakat < safeTotal) {
                        val connectorColor = when {
                            rakat < safeCurrent -> PoseMatchedGreen
                            rakat == safeCurrent -> NightGold
                            else -> Color.White.copy(alpha = 0.22f)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = connectorColor,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RakatUnitChip(
    rakatNumber: Int,
    state: RakatUnitState,
    progress: Float,
    pulseScale: Float,
    script: TextScript,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "rakat_${rakatNumber}_progress"
    )

    val containerColor = when (state) {
        RakatUnitState.COMPLETED -> PoseMatchedGreen.copy(alpha = 0.14f)
        RakatUnitState.CURRENT -> NightGold.copy(alpha = 0.18f)
        RakatUnitState.NEXT -> NightEmeraldPrimary.copy(alpha = 0.11f)
        RakatUnitState.UPCOMING -> ObsidianSurfaceVariant.copy(alpha = 0.65f)
    }
    val borderStroke = when (state) {
        RakatUnitState.COMPLETED -> BorderStroke(1.dp, PoseMatchedGreen.copy(alpha = 0.55f))
        RakatUnitState.CURRENT -> BorderStroke(1.5.dp, NightGold)
        RakatUnitState.NEXT -> BorderStroke(1.dp, NightEmeraldPrimary.copy(alpha = 0.6f))
        RakatUnitState.UPCOMING -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }
    val statusText = when (state) {
        RakatUnitState.COMPLETED -> lText("Tayyor", "Тайёр").get(script)
        RakatUnitState.CURRENT -> lText("Hozir", "Ҳозир").get(script)
        RakatUnitState.NEXT -> lText("Keyingi", "Кейинги").get(script)
        RakatUnitState.UPCOMING -> lText("Navbatda", "Навбатда").get(script)
    }
    val statusColor = when (state) {
        RakatUnitState.COMPLETED -> PoseMatchedGreen
        RakatUnitState.CURRENT -> NightGold
        RakatUnitState.NEXT -> NightEmeraldPrimary
        RakatUnitState.UPCOMING -> Color.White.copy(alpha = 0.55f)
    }
    val stateAccessibility = when (state) {
        RakatUnitState.COMPLETED -> lText("$rakatNumber-rakat bajarildi", "$rakatNumber-ракат бажарилди").get(script)
        RakatUnitState.CURRENT -> lText("$rakatNumber-rakat hozir bajarilmoqda", "$rakatNumber-ракат ҳозир бажарилмоқда").get(script)
        RakatUnitState.NEXT -> lText("$rakatNumber-rakat keyingi", "$rakatNumber-ракат кейинги").get(script)
        RakatUnitState.UPCOMING -> lText("$rakatNumber-rakat navbatda", "$rakatNumber-ракат навбатда").get(script)
    }

    Surface(
        modifier = modifier
            .testTag("rakat_unit_$rakatNumber")
            .semantics(mergeDescendants = true) {
                contentDescription = stateAccessibility
                stateDescription = statusText
            },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                RakatUnitState.COMPLETED -> PoseMatchedGreen
                                RakatUnitState.CURRENT -> NightGold
                                RakatUnitState.NEXT -> NightEmeraldPrimary.copy(alpha = 0.22f)
                                RakatUnitState.UPCOMING -> Color.White.copy(alpha = 0.1f)
                            }
                        )
                        .then(
                            if (state == RakatUnitState.NEXT) {
                                Modifier.border(1.dp, NightEmeraldPrimary, CircleShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state == RakatUnitState.COMPLETED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00291D),
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Text(
                            text = rakatNumber.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (state) {
                                RakatUnitState.CURRENT -> Color(0xFF1F1600)
                                RakatUnitState.NEXT -> NightEmeraldPrimary
                                else -> Color.White.copy(alpha = 0.72f)
                            }
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = if (state == RakatUnitState.CURRENT) FontWeight.Bold else FontWeight.SemiBold,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mini intra-rakat step progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(CircleShape)
                            .background(
                                if (state == RakatUnitState.COMPLETED) PoseMatchedGreen else NightGold
                            )
                    )
                }
            }
        }
    }
}
