package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QazoPrayerEntity
import com.example.domain.model.PrayerType
import com.example.domain.model.QazoPrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.WeeklyChartMode
import com.example.domain.model.WeeklyPrayerAnalytics
import com.example.domain.model.WeeklyPrayerSummary
import com.example.domain.model.lText
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.SaffronGold
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

private data class RechartsBarItem(
    val id: String,
    val xAxisLabel: String,
    val tooltipTitle: String,
    val percentage: Float,
    val countDetail: String,
    val barColor: Color,
    val barHighlightColor: Color,
    val subBadges: List<Pair<String, Boolean>>
)

@Composable
fun WeeklyProgressScreen(
    uiState: MainUiState,
    onShiftWeek: (Int) -> Unit,
    onTogglePrayerForDay: (String, PrayerType) -> Unit,
    onIncrementQazo: (QazoPrayerType) -> Unit = {},
    onDecrementQazo: (QazoPrayerType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val script = uiState.settings.script
    val summary = uiState.weeklySummary ?: return
    var chartMode by remember { mutableStateOf(WeeklyChartMode.BY_PRAYER) }
    var selectedBarIndex by remember(chartMode, summary.weekOffset) {
        mutableIntStateOf(0)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val barItems = remember(summary, chartMode, script, primaryColor, tertiaryColor) {
        when (chartMode) {
            WeeklyChartMode.BY_PRAYER -> {
                summary.prayerBars.map { metric ->
                    val isPerfect = metric.completedDays == metric.totalDays
                    val fill = when {
                        isPerfect -> PoseMatchedGreen
                        metric.percentage >= 50f -> primaryColor
                        else -> SaffronGold
                    }
                    RechartsBarItem(
                        id = metric.prayerType.id,
                        xAxisLabel = metric.prayerType.title.get(script),
                        tooltipTitle = metric.prayerType.title.get(script),
                        percentage = metric.percentage,
                        countDetail = lText(
                            "${metric.completedDays} / ${metric.totalDays} kun",
                            "${metric.completedDays} / ${metric.totalDays} кун"
                        ).get(script),
                        barColor = fill,
                        barHighlightColor = fill.copy(alpha = 0.85f),
                        subBadges = summary.dailyBars.mapIndexed { idx, day ->
                            day.shortDayLabel.get(script) to metric.dayStatuses[idx]
                        }
                    )
                }
            }
            WeeklyChartMode.BY_DAY -> {
                summary.dailyBars.map { day ->
                    val isPerfect = day.completedCount == day.totalPrayers
                    val fill = when {
                        isPerfect -> PoseMatchedGreen
                        day.completedCount >= 3 -> primaryColor
                        day.completedCount > 0 -> SaffronGold
                        else -> tertiaryColor.copy(alpha = 0.65f)
                    }
                    RechartsBarItem(
                        id = day.dayKey,
                        xAxisLabel = day.shortDayLabel.get(script),
                        tooltipTitle = "${day.fullDayLabel.get(script)} (${day.shortDate})",
                        percentage = day.percentage,
                        countDetail = lText(
                            "${day.completedCount} / ${day.totalPrayers} namoz",
                            "${day.completedCount} / ${day.totalPrayers} намоз"
                        ).get(script),
                        barColor = fill,
                        barHighlightColor = fill.copy(alpha = 0.85f),
                        subBadges = WeeklyPrayerAnalytics.fiveDailyPrayers.map { type ->
                            type.title.get(script).take(3) to day.completedTypes.contains(type)
                        }
                    )
                }
            }
        }
    }

    val safeSelectedIndex = selectedBarIndex.coerceIn(0, (barItems.size - 1).coerceAtLeast(0))

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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header + Week Navigator
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lText(
                                    "Haftalik statistika",
                                    "Ҳафталик статистика"
                                ).get(script),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = lText(
                                    "Besh vaqt namozning haftalik bajarilish foizi",
                                    "Беш вақт намознинг ҳафталик бажарилиш фоизи"
                                ).get(script),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${summary.overallRoundedPercent}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Week Switcher Bar
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onShiftWeek(-1) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("week_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = lText(
                                        "Oldingi hafta",
                                        "Олдинги ҳафта"
                                    ).get(script)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = summary.rangeLabel.get(script),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (summary.weekOffset == 0) {
                                        lText("Shu hafta", "Шу ҳафта").get(script)
                                    } else {
                                        val weeksAgo = -summary.weekOffset
                                        lText(
                                            "$weeksAgo hafta oldin",
                                            "$weeksAgo ҳафта олдин"
                                        ).get(script)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { onShiftWeek(1) },
                                enabled = summary.weekOffset < 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("week_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = lText(
                                        "Keyingi hafta",
                                        "Кейинги ҳафта"
                                    ).get(script)
                                )
                            }
                        }
                    }
                }
            }

            // Qazo Namozlari Daftari (6 prayer counters backed by Room DB)
            item {
                QazoLedgerCard(
                    qazoProgress = uiState.qazoProgress,
                    script = script,
                    onIncrement = onIncrementQazo,
                    onDecrement = onDecrementQazo
                )
            }

            // Recharts-style BarChart Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_bar_chart_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mode Switcher Pills (5 Prayers vs 7 Weekdays)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = chartMode == WeeklyChartMode.BY_PRAYER,
                                onClick = { chartMode = WeeklyChartMode.BY_PRAYER },
                                label = {
                                    Text(
                                        text = lText(
                                            "5 namoz (%)",
                                            "5 намоз (%)"
                                        ).get(script),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chart_mode_by_prayer")
                            )
                            FilterChip(
                                selected = chartMode == WeeklyChartMode.BY_DAY,
                                onClick = { chartMode = WeeklyChartMode.BY_DAY },
                                label = {
                                    Text(
                                        text = lText(
                                            "7 kun (%)",
                                            "7 кун (%)"
                                        ).get(script),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chart_mode_by_day")
                            )
                        }

                        // Recharts Floating <Tooltip /> Box synced to selected/dragged bar
                        val activeItem = barItems.getOrNull(safeSelectedIndex)
                        AnimatedVisibility(
                            visible = activeItem != null,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(150))
                        ) {
                            if (activeItem != null) {
                                RechartsTooltipCard(
                                    item = activeItem,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Interactive Recharts BarChart Canvas (CartesianGrid + YAxis + XAxis + ReferenceLine + Bars)
                        RechartsBarChartCanvas(
                            items = barItems,
                            selectedIndex = safeSelectedIndex,
                            averagePercentage = summary.overallPercentage,
                            averageLabel = lText(
                                "O‘rtacha ${summary.overallRoundedPercent}%",
                                "Ўртача ${summary.overallRoundedPercent}%"
                            ).get(script),
                            onSelectBar = { idx -> selectedBarIndex = idx },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .testTag("recharts_bar_canvas")
                        )

                        // Recharts Bottom <Legend />
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(
                                color = PoseMatchedGreen,
                                label = lText("100% to‘liq", "100% тўлиқ").get(script)
                            )
                            Spacer(Modifier.width(14.dp))
                            LegendDot(
                                color = MaterialTheme.colorScheme.primary,
                                label = lText("Bajarilgan %", "Бажарилган %").get(script)
                            )
                            Spacer(Modifier.width(14.dp))
                            LegendDot(
                                color = SaffronGold,
                                label = lText("O‘rtacha chiziq", "Ўртача чизиқ").get(script),
                                isDashed = true
                            )
                        }

                        if (summary.totalCompleted == 0) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = lText(
                                            "Ushbu hafta uchun hali namoz belgilanmagan. Quyidagi jadvalda yoki Asosiy sahifada namozlarni belgilang.",
                                            "Ушбу ҳафта учун ҳали намоз белгиланмаган. Қуйидаги жадвалда ёки Асосий саҳифада намозларни белгиланг."
                                        ).get(script),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary KPI Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryKpiCard(
                        title = lText("Jami namoz", "Жами намоз").get(script),
                        value = "${summary.totalCompleted}/${summary.totalPossible}",
                        subtitle = "${summary.overallRoundedPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryKpiCard(
                        title = lText("5/5 kunlar", "5/5 кунлар").get(script),
                        value = "${summary.perfectDaysCount}/7",
                        subtitle = lText("to‘liq kun", "тўлиқ кун").get(script),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryKpiCard(
                        title = lText("Eng faol", "Энг фаол").get(script),
                        value = summary.bestPrayer?.title?.get(script) ?: "—",
                        subtitle = if (summary.bestPrayer != null) {
                            val bestMetric = summary.prayerBars.first {
                                it.prayerType == summary.bestPrayer
                            }
                            "${bestMetric.roundedPercent}%"
                        } else {
                            "0%"
                        },
                        highlightIcon = summary.bestPrayer != null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive 7x5 Weekly Matrix backed by Room
            item {
                WeeklyPrayerMatrixCard(
                    summary = summary,
                    script = script,
                    onTogglePrayerForDay = onTogglePrayerForDay
                )
            }
        }
    }
}

@Composable
private fun RechartsTooltipCard(
    item: RechartsBarItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier.testTag("recharts_tooltip")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(item.barColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.tooltipTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.countDetail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.percentage.roundToInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = item.barColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.subBadges.forEach { (label, completed) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (completed) PoseMatchedGreen.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completed) PoseMatchedGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RechartsBarChartCanvas(
    items: List<RechartsBarItem>,
    selectedIndex: Int,
    averagePercentage: Float,
    averageLabel: String,
    onSelectBar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedFractions = items.mapIndexed { index, item ->
        val animated by animateFloatAsState(
            targetValue = (item.percentage / 100f).coerceIn(0f, 1f),
            animationSpec = tween(
                durationMillis = 480 + index * 40,
                easing = FastOutSlowInEasing
            ),
            label = "bar_fraction_${item.id}"
        )
        animated
    }
    val animatedAverage by animateFloatAsState(
        targetValue = (averagePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "avg_line"
    )

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) }

    Canvas(
        modifier = modifier
            .pointerInput(items.size) {
                detectTapGestures { offset ->
                    val leftMargin = 38.dp.toPx()
                    val rightMargin = 8.dp.toPx()
                    val plotWidth = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                    val relX = (offset.x - leftMargin).coerceIn(0f, plotWidth - 1f)
                    val slotWidth = plotWidth / items.size.coerceAtLeast(1)
                    val idx = (relX / slotWidth).toInt().coerceIn(0, items.lastIndex)
                    onSelectBar(idx)
                }
            }
            .pointerInput(items.size) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val leftMargin = 38.dp.toPx()
                    val rightMargin = 8.dp.toPx()
                    val plotWidth = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                    val relX = (change.position.x - leftMargin).coerceIn(0f, plotWidth - 1f)
                    val slotWidth = plotWidth / items.size.coerceAtLeast(1)
                    val idx = (relX / slotWidth).toInt().coerceIn(0, items.lastIndex)
                    onSelectBar(idx)
                }
            }
    ) {
        if (items.isEmpty()) return@Canvas

        val leftMargin = 38.dp.toPx()
        val rightMargin = 8.dp.toPx()
        val topMargin = 24.dp.toPx()
        val bottomMargin = 30.dp.toPx()

        val plotLeft = leftMargin
        val plotRight = size.width - rightMargin
        val plotTop = topMargin
        val plotBottom = size.height - bottomMargin
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

        // 1. Recharts <CartesianGrid /> + <YAxis /> ticks (0%, 25%, 50%, 75%, 100%)
        val ticks = listOf(0, 25, 50, 75, 100)
        ticks.forEach { pct ->
            val y = plotBottom - (pct / 100f) * plotHeight
            drawLine(
                color = if (pct == 0) outlineColor.copy(alpha = 0.7f)
                else outlineColor.copy(alpha = 0.3f),
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = if (pct == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (pct == 0) null else dashEffect
            )

            val yLabel = textMeasurer.measure(
                text = "$pct%",
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = onSurfaceVariantColor
                )
            )
            drawText(
                textLayoutResult = yLabel,
                topLeft = Offset(
                    (plotLeft - yLabel.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                    y - yLabel.size.height / 2f
                )
            )
        }

        val slotWidth = plotWidth / items.size
        val barWidth = (slotWidth * 0.52f).coerceAtMost(44.dp.toPx())
        val topCornerRadius = 8.dp.toPx()

        // 2. Bars, cursor band, top value labels, and <XAxis /> labels
        items.forEachIndexed { idx, item ->
            val slotLeft = plotLeft + idx * slotWidth
            val barCenterX = slotLeft + slotWidth / 2f
            val barLeft = barCenterX - barWidth / 2f
            val barRight = barCenterX + barWidth / 2f
            val isSelected = idx == selectedIndex

            // Recharts hover/active cursor band behind column
            if (isSelected) {
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.08f),
                    topLeft = Offset(slotLeft + 3.dp.toPx(), plotTop),
                    size = Size((slotWidth - 6.dp.toPx()).coerceAtLeast(4f), plotHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // Full-height subtle track behind bar
            val trackPath = topRoundedBarPath(
                left = barLeft,
                top = plotTop,
                right = barRight,
                bottom = plotBottom,
                cornerRadius = topCornerRadius
            )
            drawPath(
                path = trackPath,
                color = surfaceVariantColor.copy(alpha = 0.28f)
            )

            // Animated bar fill with rounded top corners [8, 8, 0, 0]
            val fraction = animatedFractions[idx]
            val barHeight = (fraction * plotHeight).coerceAtLeast(0f)
            val barTop = plotBottom - barHeight

            if (barHeight > 1f) {
                val r = topCornerRadius.coerceAtMost(barHeight / 2f).coerceAtMost(barWidth / 2f)
                val barPath = topRoundedBarPath(
                    left = barLeft,
                    top = barTop,
                    right = barRight,
                    bottom = plotBottom,
                    cornerRadius = r
                )
                drawPath(
                    path = barPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            item.barColor,
                            item.barColor.copy(alpha = if (isSelected) 0.92f else 0.72f)
                        ),
                        startY = barTop,
                        endY = plotBottom
                    )
                )
            }

            // Percentage label right above the bar
            val pctText = "${item.percentage.roundToInt()}%"
            val pctLayout = textMeasurer.measure(
                text = pctText,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) item.barColor else onSurfaceVariantColor
                )
            )
            drawText(
                textLayoutResult = pctLayout,
                topLeft = Offset(
                    barCenterX - pctLayout.size.width / 2f,
                    (barTop - pctLayout.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                )
            )

            // XAxis tick label
            val xLayout = textMeasurer.measure(
                text = item.xAxisLabel,
                style = TextStyle(
                    fontSize = if (items.size <= 5) 11.sp else 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) onSurfaceColor else onSurfaceVariantColor
                ),
                maxLines = 1
            )
            drawText(
                textLayoutResult = xLayout,
                topLeft = Offset(
                    (barCenterX - xLayout.size.width / 2f).coerceIn(
                        0f,
                        (size.width - xLayout.size.width).coerceAtLeast(0f)
                    ),
                    plotBottom + 8.dp.toPx()
                )
            )
        }

        // 3. Recharts <ReferenceLine /> for weekly average
        if (averagePercentage > 0f) {
            val avgY = plotBottom - animatedAverage * plotHeight
            drawLine(
                color = SaffronGold,
                start = Offset(plotLeft, avgY),
                end = Offset(plotRight, avgY),
                strokeWidth = 1.75.dp.toPx(),
                pathEffect = dashEffect,
                cap = StrokeCap.Round
            )
            val avgLayout = textMeasurer.measure(
                text = averageLabel,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1400)
                )
            )
            val pillW = avgLayout.size.width + 10.dp.toPx()
            val pillH = avgLayout.size.height + 4.dp.toPx()
            val pillX = (plotRight - pillW - 4.dp.toPx()).coerceAtLeast(plotLeft)
            val pillY = (avgY - pillH - 3.dp.toPx()).coerceAtLeast(plotTop)
            drawRoundRect(
                color = SaffronGold,
                topLeft = Offset(pillX, pillY),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawText(
                textLayoutResult = avgLayout,
                topLeft = Offset(pillX + 5.dp.toPx(), pillY + 2.dp.toPx())
            )
        }
    }
}

private fun topRoundedBarPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float
): Path {
    val r = cornerRadius.coerceAtLeast(0f)
    return Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + r)
        if (r > 0f) {
            quadraticTo(left, top, left + r, top)
        } else {
            lineTo(left, top)
        }
        lineTo(right - r, top)
        if (r > 0f) {
            quadraticTo(right, top, right, top + r)
        } else {
            lineTo(right, top)
        }
        lineTo(right, bottom)
        close()
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(color, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryKpiCard(
    title: String,
    value: String,
    subtitle: String,
    highlightIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (highlightIcon) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SaffronGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeeklyPrayerMatrixCard(
    summary: WeeklyPrayerSummary,
    script: TextScript,
    onTogglePrayerForDay: (String, PrayerType) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_prayer_matrix")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lText("Haftalik jadval", "Ҳафталик жадвал").get(script),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = lText(
                        "Belgilash uchun bosing",
                        "Белгилаш учун босинг"
                    ).get(script),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Column headers for the 5 prayers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(64.dp))
                WeeklyPrayerAnalytics.fiveDailyPrayers.forEach { prayer ->
                    Text(
                        text = prayer.title.get(script).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7 weekday rows
            summary.dailyBars.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (day.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else Color.Transparent
                        )
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(60.dp)) {
                        Text(
                            text = day.shortDayLabel.get(script),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = day.shortDate,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    WeeklyPrayerAnalytics.fiveDailyPrayers.forEach { prayer ->
                        val completed = day.completedTypes.contains(prayer)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { onTogglePrayerForDay(day.dayKey, prayer) }
                                .testTag("matrix_${day.dayKey}_${prayer.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (completed) PoseMatchedGreen
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (completed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = prayer.title.get(script),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QazoLedgerCard(
    qazoProgress: QazoPrayerEntity,
    script: TextScript,
    onIncrement: (QazoPrayerType) -> Unit,
    onDecrement: (QazoPrayerType) -> Unit
) {
    val totalRemaining = qazoProgress.totalCount()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qazo_ledger_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lText(
                            "Qazo namozlari daftari",
                            "Қазо намозлари дафтари"
                        ).get(script),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lText(
                            "O‘qilgan har bir qazoni (-1) bilan kamaytiring",
                            "Ўқилган ҳар бир қазони (-1) билан камайтиринг"
                        ).get(script),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (totalRemaining == 0) PoseMatchedGreen.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = lText(
                            "Jami: $totalRemaining",
                            "Жами: $totalRemaining"
                        ).get(script),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (totalRemaining == 0) PoseMatchedGreen
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("qazo_total_badge")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            QazoPrayerType.entries.forEachIndexed { index, type ->
                val count = qazoProgress.countFor(type)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.title.get(script),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = type.rakatSummary.get(script),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onDecrement(type) },
                            enabled = count > 0,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = PoseMatchedGreen.copy(alpha = 0.16f),
                                contentColor = PoseMatchedGreen
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("qazo_dec_${type.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = lText(
                                    "${type.title.latin} qazosini kamaytirish",
                                    "${type.title.cyrillic} қазосини камайтириш"
                                ).get(script)
                            )
                        }

                        Text(
                            text = count.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            color = if (count > 0) MaterialTheme.colorScheme.onSurface
                            else PoseMatchedGreen,
                            modifier = Modifier
                                .widthIn(min = 38.dp)
                                .testTag("qazo_count_${type.id}")
                        )

                        FilledTonalIconButton(
                            onClick = { onIncrement(type) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("qazo_inc_${type.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = lText(
                                    "${type.title.latin} qazosini qo‘shish",
                                    "${type.title.cyrillic} қазосини қўшиш"
                                ).get(script)
                            )
                        }
                    }
                }
                if (index < QazoPrayerType.entries.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
        }
    }
}
