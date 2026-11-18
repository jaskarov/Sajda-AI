package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onGenderChange: (Gender) -> Unit,
    onScriptChange: (TextScript) -> Unit,
    onCityChange: (String) -> Unit,
    onPreferOfflineMp3Toggle: (Boolean) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onDarkThresholdChange: (Float) -> Unit,
    onSeatedModeToggle: (Boolean) -> Unit = {},
    onSyncCalendarNow: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onTestAudio: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val settings = uiState.settings
    val script = settings.script

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(lText("Sozlamalar", "Созламалар").get(script)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = lText("Orqaga", "Орқага").get(script)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Script & Gender Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = lText("Shaxsiylashtirish", "Шахсийлаштириш").get(script),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = lText("Yozuv turi", "Ёзув тури").get(script),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = script == TextScript.LATIN,
                                onClick = { onScriptChange(TextScript.LATIN) },
                                label = { Text("Lotin") },
                                modifier = Modifier.testTag("settings_script_latin")
                            )
                            FilterChip(
                                selected = script == TextScript.CYRILLIC,
                                onClick = { onScriptChange(TextScript.CYRILLIC) },
                                label = { Text("Кирилл") },
                                modifier = Modifier.testTag("settings_script_cyrillic")
                            )
                        }

                        Text(
                            text = lText("Jins (Namoz qoidalari)", "Жинс (Намоз қоидалари)").get(script),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Gender.entries.forEach { g ->
                                FilterChip(
                                    selected = settings.gender == g,
                                    onClick = { onGenderChange(g) },
                                    label = { Text(g.label.get(script)) },
                                    modifier = Modifier.testTag("settings_gender_${g.id}")
                                )
                            }
                        }
                    }
                }

                // Chair / Seated Mode (Accessibility for Elderly & Mobility-Impaired)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (settings.seatedPrayerMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = lText(
                                        "Stulda / O‘tirib o‘qish (Imo-ishora)",
                                        "Стулда / Ўтириб ўқиш (Имо-ишора)"
                                    ).get(script),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = lText(
                                        "Keksalar va bel-tizzada og‘rig‘i borlar uchun: ruku' yengil (15°–30°), sajda chuqurroq (45°–60°) egilish orqali aniqlanadi.",
                                        "Кексалар ва бел-тиззада оғриғи борлар учун: рукуъ енгил (15°–30°), сажда чуқурроқ (45°–60°) эгилиш орқали аниқланади."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = settings.seatedPrayerMode,
                            onCheckedChange = onSeatedModeToggle,
                            modifier = Modifier.testTag("settings_seated_mode_switch")
                        )
                    }
                }

                // Offline Audio Coach (Bundled Qori MP3s via ExoPlayer + TTS)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = lText(
                                "Oflayn Qori va Ovozli Ustoz",
                                "Офлайн Қори ва Овозли Устоз"
                            ).get(script),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lText(
                                        "Oflayn Qori MP3 (ExoPlayer)",
                                        "Офлайн Қори MP3 (ExoPlayer)"
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = lText(
                                        "15 ta sura, duo va tasbeh MP3 fayllari ilova ichida saqlangan va internetsiz ijro etiladi.",
                                        "15 та сура, дуо ва тасбеҳ MP3 файллари илова ичида сақланган ва интернетсиз ижро этилади."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.preferOfflineMp3,
                                onCheckedChange = onPreferOfflineMp3Toggle,
                                modifier = Modifier.testTag("settings_offline_mp3_switch")
                            )
                        }

                        FilledTonalButton(
                            onClick = onTestAudio,
                            modifier = Modifier.testTag("settings_test_audio_button")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                lText(
                                    "Qiroatni sinab ko‘rish (Fotiha)",
                                    "Қироатни синаб кўриш (Фотиҳа)"
                                ).get(script)
                            )
                        }

                        Text(
                            text = "${lText("Ovoz tezligi:", "Овоз тезлиги:").get(script)} ${
                                String.format(Locale.US, "%.2fx", settings.voiceSpeed)
                            }",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = settings.voiceSpeed,
                            onValueChange = onVoiceSpeedChange,
                            valueRange = 0.75f..1.35f
                        )

                        Text(
                            text = "${
                                lText(
                                    "Qorong‘i xona sezgirligi:",
                                    "Қоронғи хона сезгирлиги:"
                                ).get(script)
                            } ${settings.darkRoomLuminanceThreshold.toInt()} lum",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = settings.darkRoomLuminanceThreshold,
                            onValueChange = onDarkThresholdChange,
                            valueRange = 12f..55f
                        )
                    }
                }

                // 365-Day Calendar DB & Online Sync + City Picker
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = lText(
                                "Hudud va 365 kunlik taqvim",
                                "Ҳудуд ва 365 кунлик тақвим"
                            ).get(script),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val sourceLabel = if (uiState.calendarSource == "aladhan_online" &&
                            uiState.lastCalendarSyncMillis > 0L
                        ) {
                            val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                .format(Date(uiState.lastCalendarSyncMillis))
                            lText(
                                "Manba: Onlayn sinxronlangan ($timeStr) • 14 hudud",
                                "Манба: Онлайн синхронланган ($timeStr) • 14 ҳудуд"
                            ).get(script)
                        } else {
                            lText(
                                "Manba: 365 kunlik oflayn Room bazasi • 14 hudud",
                                "Манба: 365 кунлик офлайн Room базаси • 14 ҳудуд"
                            ).get(script)
                        }
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UzbekistanCities.all.forEach { city ->
                                FilterChip(
                                    selected = settings.selectedCityId == city.id,
                                    onClick = { onCityChange(city.id) },
                                    label = { Text(city.name.get(script)) }
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onSyncCalendarNow,
                            enabled = !uiState.isCalendarSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_sync_calendar_button")
                        ) {
                            if (uiState.isCalendarSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                lText(
                                    "Taqvimni internet orqali yangilash",
                                    "Тақвимни интернет орқали янгилаш"
                                ).get(script)
                            )
                        }
                    }
                }

                // Runtime Permissions Center (Camera, Notifications, Exact Alarm)
                FilledTonalButton(
                    onClick = onOpenPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_permissions_button")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        lText(
                            "Ruxsatlar: Kamera, Bildirishnoma va Aniq Azon",
                            "Рухсатлар: Камера, Билдиришнома ва Аниқ Азон"
                        ).get(script)
                    )
                }
            }
        }
    }
}
