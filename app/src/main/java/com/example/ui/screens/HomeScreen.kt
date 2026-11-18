package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerTimeSlot
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.DeepHeroGradientEnd
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.SaffronGold
import com.example.ui.viewmodel.MainUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onStartPrayer: (PrayerTrack) -> Unit,
    onToggleAlarm: (PrayerType) -> Unit,
    onToggleCompleted: (PrayerType) -> Unit,
    onCitySelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeeklyStats: () -> Unit = {},
    onOpenWuduGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schedule = uiState.schedule
    val script = uiState.settings.script
    var showCitySheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    if (schedule == null) return

    val quickTracks = remember(schedule.currentPrayer, uiState.quickTracks) {
        uiState.quickTracks.ifEmpty { PrayerTrack.tracksForPrayer(schedule.currentPrayer) }
    }
    val defaultTrack = remember(schedule.currentPrayer, quickTracks) {
        quickTracks.firstOrNull() ?: PrayerTrack.defaultForPrayer(schedule.currentPrayer)
    }
    var selectedTrack by remember(defaultTrack) { mutableStateOf(defaultTrack) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Header: Location & Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showCitySheet = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("city_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${schedule.city.name.get(script)}, ${
                                    lText("O‘zbekiston", "Ўзбекистон").get(script)
                                }",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = lText("Sozlamalar", "Созламалар").get(script),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Hero Countdown & CTA Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(DeepHeroGradientStart, DeepHeroGradientEnd)
                            )
                        )
                ) {
                    // Subtle decorative mihrab arches in background
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val archPath = Path().apply {
                            val w = size.width
                            val h = size.height
                            moveTo(w * 0.65f, h)
                            lineTo(w * 0.65f, h * 0.45f)
                            quadraticTo(w * 0.82f, h * 0.12f, w * 0.98f, h * 0.45f)
                            lineTo(w * 0.98f, h)
                        }
                        drawPath(
                            path = archPath,
                            color = Color.White.copy(alpha = 0.08f),
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = SaffronGold.copy(alpha = 0.12f),
                            radius = size.height * 0.35f,
                            center = Offset(size.width * 0.88f, size.height * 0.22f)
                        )
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${
                                lText("KEYINGI NAMOZ:", "КЕЙИНГИ НАМОЗ:").get(script)
                            } ${schedule.nextPrayer.title.get(script).uppercase()} (${schedule.nextPrayerTimeFormatted})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFE5A3),
                            letterSpacing = 0.8.sp
                        )

                        Text(
                            text = formatCountdown(schedule.countdownSeconds),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White
                        )

                        Text(
                            text = "${schedule.hijriLabel.get(script)} • ${
                                lText(
                                    "Hozirgi vaqt: ${schedule.currentPrayer.title.latin}",
                                    "Ҳозирги вақт: ${schedule.currentPrayer.title.cyrillic}"
                                ).get(script)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.84f)
                        )

                        Button(
                            onClick = { onStartPrayer(selectedTrack) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_current_prayer_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaffronGold,
                                contentColor = Color(0xFF1B1400)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = lText(
                                    "HOZIRGI NAMOZNI O‘QISH",
                                    "ҲОЗИРГИ НАМОЗНИ ЎҚИШ"
                                ).get(script),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val chainedNextTrack = selectedTrack.nextContinuationTrack()
                        if (chainedNextTrack != null) {
                            Text(
                                text = lText(
                                    "Uzluksiz oqim: ${selectedTrack.title.latin} → ${chainedNextTrack.title.latin}",
                                    "Узлуксиз оқим: ${selectedTrack.title.cyrillic} → ${chainedNextTrack.title.cyrillic}"
                                ).get(script),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFE5A3)
                            )
                        }

                        // Dynamic Quick Rakat / Track Chips for the current prayer time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("home_quick_tracks_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickTracks.forEach { track ->
                                val isSelected = track == selectedTrack
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTrack = track },
                                    label = {
                                        Text(
                                            text = track.title.get(script),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        labelColor = Color.White.copy(alpha = 0.9f),
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = DeepHeroGradientStart
                                    ),
                                    border = null,
                                    modifier = Modifier.testTag("track_chip_${track.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Direct Wudu / Ghusl / Tayammum shortcut for Beginners
            item {
                Card(
                    onClick = onOpenWuduGuide,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_open_wudu_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lText(
                                    "Tahorat, G‘usl va Tayammum tartibi",
                                    "Таҳорат, Ғусл ва Таяммум тартиби"
                                ).get(script),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lText(
                                    "Namozdan oldingi 8 qadam va 4 farzni ovozli o‘rganing",
                                    "Намоздан олдинги 8 қадам ва 4 фарзни овозли ўрганинг"
                                ).get(script),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = lText("Ochish", "Очиш").get(script),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Section Title + Streak Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lText("Bugungi vaqtlar:", "Бугунги вақтлар:").get(script),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        onClick = onOpenWeeklyStats,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("open_weekly_stats_badge")
                    ) {
                        val completedCount = uiState.todayProgress?.completedCount() ?: 0
                        Text(
                            text = lText(
                                "Bugun: $completedCount / 5 namoz",
                                "Бугун: $completedCount / 5 намоз"
                            ).get(script),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Prayer Time Rows
            items(schedule.slots, key = { it.type.id }) { slot ->
                PrayerTimeSlotRow(
                    slot = slot,
                    script = script,
                    onStartTrack = {
                        if (slot.type.isPrayable) {
                            onStartPrayer(PrayerTrack.defaultForPrayer(slot.type))
                        }
                    },
                    onToggleCompleted = { onToggleCompleted(slot.type) },
                    onToggleAlarm = {
                        if (!slot.alarmEnabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onToggleAlarm(slot.type)
                    }
                )
            }
        }
    }

    if (showCitySheet) {
        ModalBottomSheet(onDismissRequest = { showCitySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = lText("Shahar tanlang", "Шаҳар танланг").get(script),
                    style = MaterialTheme.typography.headlineMedium
                )
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(UzbekistanCities.all, key = { it.id }) { city ->
                        val active = city.id == schedule.city.id
                        Surface(
                            onClick = {
                                onCitySelected(city.id)
                                showCitySheet = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.name.get(script),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = city.region.get(script),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrayerTimeSlotRow(
    slot: PrayerTimeSlot,
    script: TextScript,
    onStartTrack: () -> Unit,
    onToggleCompleted: () -> Unit,
    onToggleAlarm: () -> Unit
) {
    val containerColor = when {
        slot.isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (slot.isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (slot.isCurrent) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = slot.type.isPrayable, onClick = onStartTrack)
            .testTag("prayer_slot_${slot.type.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleCompleted,
                enabled = slot.type.isPrayable,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("complete_toggle_${slot.type.id}")
            ) {
                when {
                    slot.isCompletedToday -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = lText("O‘qildi", "Ўқилди").get(script),
                        tint = PoseMatchedGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    slot.isCurrent -> Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = lText("Hozirgi", "Ҳозирги").get(script),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = lText("Belgilanmagan", "Белгиланмаган").get(script),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                val nowSuffix = if (slot.isCurrent) {
                    " (${lText("Hozir", "Ҳозир").get(script)})"
                } else ""
                Text(
                    text = "${slot.type.title.get(script)}$nowSuffix",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = slot.timeFormatted,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = if (slot.isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onToggleAlarm,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("alarm_toggle_${slot.type.id}")
            ) {
                Icon(
                    imageVector = if (slot.alarmEnabled) Icons.Default.NotificationsActive
                    else Icons.Default.NotificationsOff,
                    contentDescription = lText("Eslatma", "Эслатма").get(script),
                    tint = if (slot.alarmEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun formatCountdown(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return String.format(Locale.US, "%02d : %02d : %02d", h, m, s)
}
