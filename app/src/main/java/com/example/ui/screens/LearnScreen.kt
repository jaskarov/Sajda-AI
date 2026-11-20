package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.PrayerHistoryEntity
import com.example.domain.engine.PrayerContentCatalog
import com.example.domain.model.Gender
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.PurificationCategory
import com.example.domain.model.PurificationGuideItem
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    uiState: MainUiState,
    onPlayAudio: (String, String) -> Unit,
    onStartTrack: (PrayerTrack) -> Unit,
    initialTab: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    onToggleTahoratAutoPlay: (List<PurificationGuideItem>) -> Unit = {},
    onOpenPrayerGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val script = uiState.settings.script
    val gender = uiState.settings.gender
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var showAiDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 3)
    }

    val tabs = listOf(
        lText("Harakat", "Ҳаракат").get(script),
        lText("Tahorat", "Таҳорат").get(script),
        lText("Suralar", "Суралар").get(script),
        lText("Tarix", "Тарих").get(script)
    )

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
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = lText("O‘rganish markazi", "Ўрганиш маркази").get(script),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = lText(
                        "Tahorat, namoz ruknlari va suralarni ovozli tinglab mashq qiling",
                        "Таҳорат, намоз рукнлари ва сураларни овозли тинглаб машқ қилинг"
                    ).get(script),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = {
                            selectedTab = idx
                            onTabChanged(idx)
                        },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (idx) {
                                    0 -> Icons.Default.AccessibilityNew
                                    1 -> Icons.Default.WaterDrop
                                    2 -> Icons.Default.MenuBook
                                    else -> Icons.Default.History
                                },
                                contentDescription = title
                            )
                        },
                        modifier = Modifier.testTag("learn_tab_$idx")
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "learn_tab_content"
            ) { activeTab ->
            when (activeTab) {
                0 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            onClick = onOpenPrayerGuide,
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learn_open_prayer_guide_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = lText(
                                            "Interaktiv namoz qo‘llanmasi",
                                            "Интерактив намоз қўлланмаси"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = lText(
                                            "6 ta bosqichni jonli siluet va ovoz bilan ko‘rib chiqing",
                                            "6 та босқични жонли силуэт ва овоз билан кўриб чиқинг"
                                        ).get(script),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(onClick = onOpenPrayerGuide) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(lText("Boshlash", "Бошлаш").get(script))
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lText("Namoz ruknlari", "Намоз рукнлари").get(script),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            FilterChip(
                                selected = showAiDiagnostics,
                                onClick = { showAiDiagnostics = !showAiDiagnostics },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = lText("AI diagnostika", "AI диагностика").get(script),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("toggle_ai_diagnostics")
                            )
                        }
                    }

                    items(PrayerContentCatalog.postureGuides, key = { it.id }) { guide ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${guide.pose.emoji} ${guide.title.get(script)}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (showAiDiagnostics) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = guide.angleSpec,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = guide.description.get(script),
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                ) {
                                    val genderNote = if (gender == Gender.MALE) {
                                        guide.maleNote.get(script)
                                    } else {
                                        guide.femaleNote.get(script)
                                    }
                                    Text(
                                        text = "${gender.label.get(script)}: $genderNote",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledTonalButton(
                                        onClick = { onPlayAudio(guide.ttsPrompt, "") },
                                        modifier = Modifier.testTag("listen_${guide.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(lText("Tinglash", "Тинглаш").get(script))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lText(
                                            "Amaliy mashg‘ulot",
                                            "Амалий машғулот"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = lText(
                                            "2 rakat Bomdod bilan AI-ustozni sinab ko‘ring",
                                            "2 ракат Бомдод билан AI-устозни синаб кўринг"
                                        ).get(script),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onStartTrack(PrayerTrack.BOMDOD_SUNNAT) },
                                    modifier = Modifier.testTag("learn_practice_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(lText("Mashq", "Машқ").get(script))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val wuduSteps = PrayerContentCatalog.purificationGuides.filter {
                        it.category == PurificationCategory.WUDU
                    }
                    val breakers = PrayerContentCatalog.purificationGuides.filter {
                        it.category == PurificationCategory.WUDU_BREAKERS
                    }
                    val ghuslAndTayammum = PrayerContentCatalog.purificationGuides.filter {
                        it.category == PurificationCategory.GHUSL_TAYAMMUM
                    }

                    LazyColumn(
                        modifier = Modifier.testTag("learn_tahorat_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    0.8.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wudu_farz_summary_card")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(148.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_wudu_art_1795154345207),
                                            contentDescription = lText(
                                                "Tahorat poklik ramzi",
                                                "Таҳорат поклик рамзи"
                                            ).get(script),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.58f)
                                                        )
                                                    )
                                                )
                                        )
                                        Text(
                                            text = lText(
                                                "Tahoratning 4 ta farzi (Hanafiy)",
                                                "Таҳоратнинг 4 та фарзи (Ҳанафий)"
                                            ).get(script),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        )
                                    }
                                    Text(
                                        text = lText(
                                            "1. Yuzni yuvish • 2. Qo‘llarni tirsakkacha yuvish • 3. Boshning 1/4 qismiga mas’h tortish • 4. Oyoqlarni to‘piqgacha yuvish.",
                                            "1. Юзни ювиш • 2. Қўлларни тирсаккача ювиш • 3. Бошнинг 1/4 қисмига масъҳ тортиш • 4. Оёқларни тўпиқгача ювиш."
                                        ).get(script),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.tahoratAutoPlayActive) {
                                        PoseMatchedGreen.copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lText(
                                                "Qo‘lsiz ovozli mashq",
                                                "Қўлсиз овозли машқ"
                                            ).get(script),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = lText(
                                                "Suv oldida telefonni ushlamasdan 8 qadamni ketma-ket tinglang.",
                                                "Сув олдида телефонни ушламасдан 8 қадамни кетма-кет тингланг."
                                            ).get(script),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { onToggleTahoratAutoPlay(wuduSteps) },
                                        modifier = Modifier.testTag("tahorat_autoplay_button")
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.tahoratAutoPlayActive) Icons.Default.Stop
                                            else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.tahoratAutoPlayActive) {
                                                lText("To‘xtatish", "Тўхтатиш").get(script)
                                            } else {
                                                lText("Ketma-ket", "Кетма-кет").get(script)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = PurificationCategory.WUDU.title.get(script),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(wuduSteps, key = { it.id }) { item ->
                            PurificationGuideCard(
                                item = item,
                                script = script,
                                isCurrentlyPlaying = uiState.activeTahoratStepId == item.id,
                                onPlayAudio = { onPlayAudio(item.ttsPrompt, "") }
                            )
                        }

                        item {
                            Text(
                                text = PurificationCategory.WUDU_BREAKERS.title.get(script),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(breakers, key = { it.id }) { item ->
                            PurificationGuideCard(
                                item = item,
                                script = script,
                                modifier = Modifier.testTag("wudu_breakers_card"),
                                onPlayAudio = { onPlayAudio(item.ttsPrompt, "") }
                            )
                        }

                        item {
                            Text(
                                text = PurificationCategory.GHUSL_TAYAMMUM.title.get(script),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(ghuslAndTayammum, key = { it.id }) { item ->
                            PurificationGuideCard(
                                item = item,
                                script = script,
                                onPlayAudio = { onPlayAudio(item.ttsPrompt, "") }
                            )
                        }
                    }
                }

                2 -> {
                    var surahQuery by remember { mutableStateOf("") }
                    var playingSurahId by remember { mutableStateOf<String?>(null) }
                    val filteredSurahs = remember(surahQuery, script) {
                        val q = surahQuery.trim().lowercase(Locale.ROOT)
                        if (q.isEmpty()) {
                            PrayerContentCatalog.surahs
                        } else {
                            PrayerContentCatalog.surahs.filter { s ->
                                s.title.latin.lowercase(Locale.ROOT).contains(q) ||
                                    s.title.cyrillic.lowercase(Locale.ROOT).contains(q) ||
                                    s.subtitle.latin.lowercase(Locale.ROOT).contains(q) ||
                                    s.translitLatin.lowercase(Locale.ROOT).contains(q) ||
                                    s.meaning.latin.lowercase(Locale.ROOT).contains(q)
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Gemini-1 Style Header + Search Bar
                        item(key = "surah_search_header") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = lText("Suralar va Duolar", "Суралар ва Дуолар").get(script),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                OutlinedTextField(
                                    value = surahQuery,
                                    onValueChange = { surahQuery = it },
                                    singleLine = true,
                                    shape = CircleShape,
                                    placeholder = {
                                        Text(
                                            text = lText(
                                                "Suralarni qidirish...",
                                                "Сураларни қидириш..."
                                            ).get(script)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (surahQuery.isNotEmpty()) {
                                            IconButton(onClick = { surahQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = lText("Tozalash", "Тозалаш").get(script)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("surah_search_input")
                                )
                            }
                        }

                        itemsIndexed(filteredSurahs, key = { _, it -> it.id }) { index, surah ->
                            val isPlaying = playingSurahId == surah.id
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isPlaying) 1.4.dp else 0.9.dp,
                                    color = if (isPlaying) LuxuryGold.copy(alpha = 0.72f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Title + Gold Rule Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${surah.title.get(script)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = LuxuryGold.copy(alpha = 0.18f),
                                                border = BorderStroke(0.7.dp, LuxuryGold.copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = surah.subtitle.get(script),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LuxuryGold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Gemini-1 Style Waveform Audio Player Strip
                                    SurahWaveformPlayerRow(
                                        isPlaying = isPlaying,
                                        script = script,
                                        onPlayClick = {
                                            playingSurahId = surah.id
                                            onPlayAudio(surah.ttsPrompt, surah.audioAssetId)
                                        },
                                        modifier = Modifier.testTag("listen_surah_${surah.id}")
                                    )

                                    // Gold-Trimmed Mushaf Arabic Panel (Amiri Classic Naskh)
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = LuxuryGold.copy(alpha = 0.11f),
                                        border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.42f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = surah.arabicText,
                                            fontFamily = AmiriFontFamily,
                                            fontSize = 24.sp,
                                            lineHeight = 42.sp,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = if (script == TextScript.LATIN) surah.translitLatin
                                        else surah.translitCyrillic,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = surah.meaning.get(script),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (uiState.dailyProgressHistory.isEmpty() && uiState.history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = lText(
                                        "Hali o‘qilgan namozlar tarixi yo‘q",
                                        "Ҳали ўқилган намозлар тарихи йўқ"
                                    ).get(script),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = lText(
                                        "AI-ustoz bilan namoz o‘qing yoki bosh sahifada kunlik namozlarni belgilang.",
                                        "AI-устоз билан намоз ўқинг ёки бош саҳифада кунлик намозларни белгиланг."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (uiState.dailyProgressHistory.isNotEmpty()) {
                                item {
                                    Text(
                                        text = lText(
                                            "Kunlik 5 vaqt namoz natijalari",
                                            "Кунлик 5 вақт намоз натижалари"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                items(uiState.dailyProgressHistory, key = { "day_${it.dayKey}" }) { day ->
                                    DailyProgressCard(day, script)
                                }
                            }
                            if (uiState.history.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = lText(
                                            "AI-ustoz mashg‘ulotlari",
                                            "AI-устоз машғулотлари"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                items(uiState.history, key = { it.id }) { item ->
                                    HistoryItemCard(item, script)
                                }
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
private fun SurahWaveformPlayerRow(
    isPlaying: Boolean,
    script: TextScript,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(0.35f, 0.72f, 0.48f, 0.95f, 0.62f, 1.0f, 0.54f, 0.85f, 0.42f, 0.76f, 0.58f, 0.38f)
    }
    val wavePhase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "surah_card_wave")
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(520),
                repeatMode = RepeatMode.Reverse
            ),
            label = "surah_wave_phase"
        ).value
    } else {
        1f
    }

    Surface(
        onClick = onPlayClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaying) LuxuryGold.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            0.8.dp,
            if (isPlaying) LuxuryGold.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = LuxuryGold,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = lText("Tinglash", "Тинглаш").get(script),
                        tint = Color(0xFF1A1200),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPlaying) {
                        lText("Qori qiroati ijro etilmoqda", "Қори қироати ижро этилмоқда").get(script)
                    } else {
                        lText("Oflayn Qori qiroatini tinglash", "Офлайн Қори қироатини тинглаш").get(script)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                barHeights.forEachIndexed { idx, base ->
                    val factor = when {
                        !isPlaying -> base * 0.72f
                        idx % 2 == 0 -> (base * wavePhase).coerceIn(0.25f, 1f)
                        else -> (base * (1.35f - wavePhase)).coerceIn(0.25f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((22f * factor).dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) LuxuryGold
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: PrayerHistoryEntity, script: TextScript) {
    val track = PrayerTrack.fromId(item.trackId)
    val dateStr = remember(item.completedAtMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(item.completedAtMillis))
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = track.title.get(script),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$dateStr • ${item.durationSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = if (item.coachMode == "CAMERA_AI") "AI Kamera" else "Faqat Audio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyProgressCard(day: DailyPrayerProgressEntity, script: TextScript) {
    val prayers = listOf(
        PrayerType.BOMDOD,
        PrayerType.PESHIN,
        PrayerType.ASR,
        PrayerType.SHOM,
        PrayerType.XUFTON
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dayKey,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = CircleShape,
                    color = if (day.completedCount() == 5) PoseMatchedGreen.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${day.completedCount()} / 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.completedCount() == 5) PoseMatchedGreen
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prayers.forEach { prayer ->
                    val done = day.isCompleted(prayer)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (done) PoseMatchedGreen.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = prayer.title.get(script),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) PoseMatchedGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurificationGuideCard(
    item: PurificationGuideItem,
    script: TextScript,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentlyPlaying: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) {
                PoseMatchedGreen.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrentlyPlaying || item.isFarz) 1.5.dp else 1.dp,
            color = when {
                isCurrentlyPlaying -> PoseMatchedGreen
                item.isFarz -> PoseMatchedGreen.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heading = if (item.stepNumber > 0) {
                    "${item.stepNumber}. ${item.title.get(script)}"
                } else {
                    item.title.get(script)
                }
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (item.isFarz) PoseMatchedGreen.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = item.ruleBadge.get(script),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isFarz) PoseMatchedGreen
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = item.description.get(script),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.testTag("listen_wudu_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(lText("Tinglash", "Тинглаш").get(script))
                }
            }
        }
    }
}

