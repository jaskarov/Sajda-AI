package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.PrayerTimeSlot
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.LuxuryGoldDeep
import com.example.ui.theme.LuxuryGoldLight
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.prayerSkyAtmosphere
import com.example.ui.viewmodel.MainUiState
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    onOpenPrayerGuide: () -> Unit = {},
    onToggleOledNightMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schedule = uiState.schedule
    val script = uiState.settings.script
    val oledMode = uiState.settings.oledNightMode
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

    // Dynamic 5-prayer time-of-day sky atmosphere
    val targetAtmosphere = remember(schedule.currentPrayer.id, oledMode) {
        prayerSkyAtmosphere(schedule.currentPrayer.id, oledMode)
    }
    val skyStart by animateColorAsState(
        targetValue = targetAtmosphere.startColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_sky_start"
    )
    val skyMid by animateColorAsState(
        targetValue = targetAtmosphere.midColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_sky_mid"
    )
    val skyEnd by animateColorAsState(
        targetValue = targetAtmosphere.endColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_sky_end"
    )
    val skyGlow by animateColorAsState(
        targetValue = targetAtmosphere.accentGlow,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_sky_glow"
    )

    val timeOfDayIcon = when (schedule.currentPrayer) {
        PrayerType.BOMDOD, PrayerType.SHOM -> Icons.Default.WbTwilight
        PrayerType.QUYOSH, PrayerType.PESHIN, PrayerType.ASR -> Icons.Default.WbSunny
        PrayerType.XUFTON -> Icons.Default.NightsStay
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .testTag("home_prayer_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Minimalist Editorial Header: Location Pill + OLED + Settings
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
                        tonalElevation = if (oledMode) 0.dp else 1.dp,
                        border = BorderStroke(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                        ),
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (oledMode) SaffronGold.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                0.8.dp,
                                if (oledMode) SaffronGold.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            IconButton(
                                onClick = { onToggleOledNightMode(!oledMode) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_oled_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (oledMode) Icons.Default.LightMode
                                    else Icons.Default.DarkMode,
                                    contentDescription = lText(
                                        "Bomdod OLED tungi rejim",
                                        "Бомдод OLED тунги режим"
                                    ).get(script),
                                    tint = if (oledMode) SaffronGold
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = lText("Sozlamalar", "Созламалар").get(script),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Dynamic 5-Prayer Sky Atmosphere + Samarkand Girih Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = skyStart,
                    border = if (oledMode) {
                        BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.35f))
                    } else {
                        BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.42f))
                    },
                    shadowElevation = if (oledMode) 0.dp else 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Layer 1: Dynamic Time-of-Day Sky Gradient
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(skyStart, skyMid, skyEnd),
                                        start = Offset(0f, 0f),
                                        end = Offset(900f, 900f)
                                    )
                                )
                        )

                        // Layer 2: Bespoke Samarkand Mosque & Girih Artwork subtly blended
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_mosque_1795154346302),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(if (oledMode) 0.14f else 0.26f)
                        )

                        // Layer 3: Legibility scrim + delicate geometric Girih star & horizon glow
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            skyStart.copy(alpha = if (oledMode) 0.72f else 0.45f),
                                            skyMid.copy(alpha = if (oledMode) 0.65f else 0.52f),
                                            skyStart.copy(alpha = if (oledMode) 0.90f else 0.78f)
                                        )
                                    )
                                )
                        )

                        Canvas(modifier = Modifier.matchParentSize()) {
                            // Subtle 8-point Samarkand Rub-el-Hizb girih star in top-right corner
                            val cx = size.width * 0.88f
                            val cy = size.height * 0.22f
                            val outerR = size.minDimension * 0.24f
                            val innerR = outerR * 0.62f
                            val starPath = Path()
                            for (i in 0 until 16) {
                                val angle = (i * 22.5) * PI / 180.0
                                val r = if (i % 2 == 0) outerR else innerR
                                val x = cx + (r * cos(angle)).toFloat()
                                val y = cy + (r * sin(angle)).toFloat()
                                if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                            }
                            starPath.close()
                            drawPath(
                                path = starPath,
                                color = skyGlow.copy(alpha = if (oledMode) 0.10f else 0.16f),
                                style = Stroke(width = 1.6f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        skyGlow.copy(alpha = if (oledMode) 0.08f else 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy),
                                    radius = outerR * 1.9f
                                ),
                                radius = outerR * 1.9f,
                                center = Offset(cx, cy)
                            )
                        }

                        // Layer 4: Foreground Typography & Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dynamic Prayer Time Atmosphere Pill
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.26f),
                                border = BorderStroke(1.dp, skyGlow.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = timeOfDayIcon,
                                        contentDescription = null,
                                        tint = targetAtmosphere.badgeTint,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${
                                            lText("KEYINGI:", "КЕЙИНГИ:").get(script)
                                        } ${schedule.nextPrayer.title.get(script).uppercase()} • ${schedule.nextPrayerTimeFormatted}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = targetAtmosphere.badgeTint,
                                        letterSpacing = 0.7.sp
                                    )
                                }
                            }

                            // Self-contained 1-second countdown (recomposes ONLY this text)
                            HomeCountdownText(
                                initialSeconds = schedule.countdownSeconds,
                                prayerKey = "${schedule.nextPrayer.id}_${schedule.nextPrayerTimeFormatted}"
                            )

                            Text(
                                text = "${schedule.hijriLabel.get(script)}  •  ${
                                    lText(
                                        "Hozir: ${schedule.currentPrayer.title.latin}",
                                        "Ҳозир: ${schedule.currentPrayer.title.cyrillic}"
                                    ).get(script)
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )

                            Spacer(Modifier.height(2.dp))

                            Button(
                                onClick = { onStartPrayer(selectedTrack) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_current_prayer_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF1A1200)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    LuxuryGoldLight,
                                                    LuxuryGold,
                                                    LuxuryGoldDeep
                                                )
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF1A1200),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = lText(
                                                "HOZIRGI NAMOZNI O‘QISH",
                                                "ҲОЗИРГИ НАМОЗНИ ЎҚИШ"
                                            ).get(script),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1200)
                                        )
                                    }
                                }
                            }

                            val chainedNextTrack = selectedTrack.nextContinuationTrack()
                            if (chainedNextTrack != null) {
                                Text(
                                    text = lText(
                                        "Uzluksiz oqim: ${selectedTrack.title.latin} → ${chainedNextTrack.title.latin}",
                                        "Узлуксиз оқим: ${selectedTrack.title.cyrillic} → ${chainedNextTrack.title.cyrillic}"
                                    ).get(script),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = targetAtmosphere.badgeTint.copy(alpha = 0.92f)
                                )
                            }

                            // Sleek Translucent Track Selector Chips
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
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        shape = CircleShape,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color.White.copy(alpha = 0.14f),
                                            labelColor = Color.White.copy(alpha = 0.92f),
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
            }

            // 3. Compact 2-Column Editorial Quick Action Tiles (Replaces noisy stacked banners)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Tile: Wudu / Purification with Bespoke Ceramic Water Vessel Art
                    Surface(
                        onClick = onOpenWuduGuide,
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (oledMode) 0.dp else 1.dp,
                        border = BorderStroke(
                            0.8.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("home_open_wudu_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_wudu_art_1795154345207),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = lText("Tahorat", "Таҳорат").get(script),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = lText("8 qadam • 4 farz", "8 қадам • 4 фарз").get(script),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right Tile: Interactive 6-Step Prayer Guide
                    Surface(
                        onClick = onOpenPrayerGuide,
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (oledMode) 0.dp else 1.dp,
                        border = BorderStroke(
                            0.8.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("home_open_prayer_guide_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                SaffronGold.copy(alpha = 0.22f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = lText("Namoz tartibi", "Намоз тартиби").get(script),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = lText("6 qadam • Jonli", "6 қадам • Жонли").get(script),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. Unified Schedule Header + Daily Progress Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lText("Bugungi vaqtlar", "Бугунги вақтлар").get(script),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        onClick = onOpenWeeklyStats,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("open_weekly_stats_badge")
                    ) {
                        val completedCount = uiState.todayProgress?.completedCount() ?: 0
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completedCount == 5) PoseMatchedGreen
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Text(
                                text = lText(
                                    "Bugun: $completedCount / 5",
                                    "Бугун: $completedCount / 5"
                                ).get(script),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 5. Unified Modern Prayer Schedule Table (Single Elegant Surface with Soft Glow Active Row)
            item(key = "unified_prayer_schedule") {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (oledMode) 0.dp else 1.dp,
                    border = BorderStroke(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        schedule.slots.forEachIndexed { index, slot ->
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
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                    onToggleAlarm(slot.type)
                                }
                            )
                            if (index < schedule.slots.lastIndex && !slot.isCurrent && !schedule.slots[index + 1].isCurrent) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                                )
                            }
                        }
                    }
                }
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
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
    val rowBg by animateColorAsState(
        targetValue = if (slot.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "prayer_row_bg_${slot.type.id}"
    )

    val rowShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (slot.isCurrent) 8.dp else 4.dp, vertical = 2.dp)
            .clip(rowShape)
            .background(rowBg)
            .clickable(enabled = slot.type.isPrayable, onClick = onStartTrack)
            .testTag("prayer_slot_${slot.type.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle Glowing Vertical Accent Indicator on Current Prayer
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(if (slot.isCurrent) 28.dp else 0.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

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
                        modifier = Modifier.size(23.dp)
                    )
                    slot.isCurrent -> Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = lText("Hozirgi", "Ҳозирги").get(script),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = lText("Belgilanmagan", "Белгиланмаган").get(script),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = slot.type.title.get(script),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (slot.isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (slot.isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (slot.isCurrent) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = lText("Hozir", "Ҳозир").get(script),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = slot.timeFormatted,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (slot.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 17.sp,
                color = if (slot.isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(4.dp))

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
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeCountdownText(
    initialSeconds: Long,
    prayerKey: String
) {
    var remainingSec by remember(prayerKey, initialSeconds) {
        mutableLongStateOf(initialSeconds.coerceAtLeast(0L))
    }

    LaunchedEffect(prayerKey, initialSeconds) {
        remainingSec = initialSeconds.coerceAtLeast(0L)
        while (remainingSec > 0L) {
            delay(1000L)
            remainingSec = (remainingSec - 1L).coerceAtLeast(0L)
        }
    }

    Text(
        text = formatCountdown(remainingSec),
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = 1.sp,
        color = Color.White,
        modifier = Modifier.testTag("home_countdown_text")
    )
}

private fun formatCountdown(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return String.format(Locale.US, "%02d : %02d : %02d", h, m, s)
}
