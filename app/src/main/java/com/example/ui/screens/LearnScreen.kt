package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyPrayerProgressEntity
import com.example.data.local.PrayerHistoryEntity
import com.example.domain.engine.PrayerContentCatalog
import com.example.domain.model.Gender
import com.example.domain.model.PrayerTrack
import com.example.domain.model.PrayerType
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    uiState: MainUiState,
    onPlayAudio: (String) -> Unit,
    onStartTrack: (PrayerTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val script = uiState.settings.script
    val gender = uiState.settings.gender
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        lText("Harakatlar", "Ҳаракатлар").get(script),
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
                        "Har bir amal, burchaklar va suralarni ovozli tinglab mashq qiling",
                        "Ҳар бир амал, бурчаклар ва сураларни овозли тинглаб машқ қилинг"
                    ).get(script),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = when (idx) {
                                    0 -> Icons.Default.AccessibilityNew
                                    1 -> Icons.Default.MenuBook
                                    else -> Icons.Default.History
                                },
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.testTag("learn_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                        onClick = { onPlayAudio(guide.ttsPrompt) },
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

                1 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PrayerContentCatalog.surahs, key = { it.id }) { surah ->
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = surah.title.get(script),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = surah.subtitle.get(script),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onPlayAudio(surah.ttsPrompt) },
                                        modifier = Modifier.testTag("listen_surah_${surah.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(lText("Tinglash", "Тинглаш").get(script))
                                    }
                                }

                                Text(
                                    text = surah.arabicText,
                                    fontSize = 21.sp,
                                    lineHeight = 32.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = if (script == TextScript.LATIN) surah.translitLatin
                                    else surah.translitCyrillic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = surah.meaning.get(script),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

