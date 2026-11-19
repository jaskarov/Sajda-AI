package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.RemoteUzbekistanPrayerTimesService
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    onLargeTextModeToggle: (Boolean) -> Unit = {},
    onLowPowerCameraToggle: (Boolean) -> Unit = {},
    onAutoBoostVolumeToggle: (Boolean) -> Unit = {},
    onAcousticChimeToggle: (Boolean) -> Unit = {},
    onSyncCalendarNow: () -> Unit = {},
    onExportBackupUri: (Uri) -> Unit = {},
    onImportBackupUri: (Uri) -> Unit = {},
    onClearBackupMessage: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenPrayerGuide: () -> Unit = {},
    onTestAudio: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val settings = uiState.settings
    val script = settings.script
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showOfflineMonthSheet by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackupUri(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackupUri(uri)
        }
    }

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

                // Large Text Mode (Senior Mode) + Low-Power Camera (Budget Phones)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = lText(
                                            "Katta shrift rejimi (Keksalar uchun)",
                                            "Катта шрифт режими (Кексалар учун)"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = lText(
                                            "Matn va tugmalarni o‘qishga qulay qilib kattalashtiradi.",
                                            "Матн ва тугмаларни ўқишга қулай қилиб катталаштиради."
                                        ).get(script),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = settings.largeTextMode,
                                onCheckedChange = onLargeTextModeToggle,
                                modifier = Modifier.testTag("settings_large_text_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatterySaver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = lText(
                                            "Tejamkor kamera rejimi",
                                            "Тежамкор камера режими"
                                        ).get(script),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = lText(
                                            "Byudjet telefonlarda qizish va quvvat sarfini kamaytiradi (8 FPS).",
                                            "Бюджет телефонларда қизиш ва қувват сарфини камайтиради (8 FPS)."
                                        ).get(script),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = settings.lowPowerCameraMode,
                                onCheckedChange = onLowPowerCameraToggle,
                                modifier = Modifier.testTag("settings_low_power_camera_switch")
                            )
                        }
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lText(
                                        "Namozda ovozni avtomatik ko‘tarish",
                                        "Намозда овозни автоматик кўтариш"
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = lText(
                                        "2 metr masofadan eshitish uchun media ovozini 85% ga ko‘taradi (qorong‘ida o‘chiriladi).",
                                        "2 метр масофадан эшитиш учун медиа овозини 85% га кўтаради (қоронғида ўчирилади)."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoBoostPrayerVolume,
                                onCheckedChange = onAutoBoostVolumeToggle,
                                modifier = Modifier.testTag("settings_auto_volume_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lText(
                                        "Rukn tasdig‘i tovushi (Chime)",
                                        "Рукн тасдиғи товуши (Chime)"
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = lText(
                                        "Ekranga qaramasdan keyingi bosqichga o‘tganini bilish uchun yengil ovozli signal.",
                                        "Экранга қарамасдан кейинги босқичга ўтганини билиш учун енгил овозли сигнал."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.acousticChimeEnabled,
                                onCheckedChange = onAcousticChimeToggle,
                                modifier = Modifier.testTag("settings_acoustic_chime_switch")
                            )
                        }

                        FilledTonalButton(
                            onClick = onTestAudio,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("settings_test_audio_button")
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

                        // TTS Language Status & System Voice Helper
                        val tts = uiState.ttsReadiness
                        val ttsSummary = when {
                            tts.hasUzbekLocale -> lText(
                                "Tizim ovozi (TTS): O‘zbek tili tayyor",
                                "Тизим овози (TTS): Ўзбек тили тайёр"
                            )
                            tts.hasFallbackLocale -> lText(
                                "Tizim ovozi (TTS): O‘zbekcha paket o‘rnatilmagan (yordamchi ovoz faol). Oflayn Qori MP3 ishlaydi.",
                                "Тизим овози (TTS): Ўзбекча пакет ўрнатилмаган (ёрдамчи овоз фаол). Офлайн Қори MP3 ишлайди."
                            )
                            else -> lText(
                                "Oflayn Qori MP3 tayyor. Ko‘rsatmalar uchun tizim TTS ovozini sozlashingiz mumkin.",
                                "Офлайн Қори MP3 тайёр. Кўрсатмалар учун тизим TTS овозини созлашингиз мумкин."
                            )
                        }.get(script)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ttsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        val ttsIntent = Intent("com.android.settings.TTS_SETTINGS")
                                        runCatching { context.startActivity(ttsIntent) }
                                            .onFailure {
                                                runCatching {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                                }
                                            }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_tts_settings_button")
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        lText(
                                            "Tizim ovozi sozlamalari (TTS)",
                                            "Тизим овози созламалари (TTS)"
                                        ).get(script)
                                    )
                                }
                            }
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
                        val providerTitle = when (uiState.calendarSource) {
                            RemoteUzbekistanPrayerTimesService.PROVIDER_ISLOM_API -> "Islom.uz API"
                            RemoteUzbekistanPrayerTimesService.PROVIDER_ALADHAN -> "Aladhan API"
                            else -> lText("Oflayn Room bazasi", "Офлайн Room базаси").get(script)
                        }
                        val cachedCountLabel = if (uiState.cachedCalendarDaysCount > 0) {
                            lText(
                                "${uiState.cachedCalendarDaysCount} kun oflayn saqlangan",
                                "${uiState.cachedCalendarDaysCount} кун офлайн сақланган"
                            ).get(script)
                        } else {
                            lText("365 kunlik oflayn Room bazasi", "365 кунлик офлайн Room базаси").get(script)
                        }
                        val sourceLabel = if (uiState.lastCalendarSyncMillis > 0L) {
                            val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                .format(Date(uiState.lastCalendarSyncMillis))
                            lText(
                                "Manba: $providerTitle ($timeStr) • $cachedCountLabel",
                                "Манба: $providerTitle ($timeStr) • $cachedCountLabel"
                            ).get(script)
                        } else {
                            lText(
                                "Manba: $providerTitle • $cachedCountLabel • 14 hudud",
                                "Манба: $providerTitle • $cachedCountLabel • 14 ҳудуд"
                            ).get(script)
                        }
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings_calendar_source_label")
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
                                .heightIn(min = 48.dp)
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
                                    "O‘zbekiston vaqtlarini yangilash",
                                    "Ўзбекистон вақтларини янгилаш"
                                ).get(script)
                            )
                        }

                        OutlinedButton(
                            onClick = { showOfflineMonthSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_view_offline_calendar_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                lText(
                                    "Oylik oflayn taqvimni ko‘rish",
                                    "Ойлик офлайн тақвимни кўриш"
                                ).get(script)
                            )
                        }

                        uiState.calendarSyncStatusMessage?.let { statusMsg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PoseMatchedGreen.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_calendar_sync_status")
                            ) {
                                Text(
                                    text = statusMsg.get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Offline JSON Backup & Restore (SAF - Zero Storage Permission)
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
                                "Zaxira nusxa (Saqlash va Tiklash)",
                                "Захира нусха (Сақлаш ва Тиклаш)"
                            ).get(script),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = lText(
                                "Qazo namozlari, kundalik jadval va sozlamalarni telefonga JSON fayl qilib saqlang yoki yangi qurilmaga tiklang.",
                                "Қазо намозлари, кундалик жадвал ва созламаларни телефонга JSON файл қилиб сақланг ёки янги қурилмага тикланг."
                            ).get(script),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onClearBackupMessage()
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    exportBackupLauncher.launch("namoz_ustoz_ai_zaxira_$dateStr.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_export_backup_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Saqlash", "Сақлаш").get(script))
                            }

                            OutlinedButton(
                                onClick = {
                                    onClearBackupMessage()
                                    importBackupLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_import_backup_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Tiklash", "Тиклаш").get(script))
                            }
                        }

                        uiState.backupStatusMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PoseMatchedGreen.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_backup_status")
                            ) {
                                Text(
                                    text = msg.get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Runtime Permissions Center (Camera, Notifications, Exact Alarm, Battery)
                FilledTonalButton(
                    onClick = onOpenPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
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

                // Reopen Interactive Step-by-Step Prayer Guide (Onboarding)
                FilledTonalButton(
                    onClick = onOpenPrayerGuide,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_open_prayer_guide_button")
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        lText(
                            "Namoz tartibi (Interaktiv qo‘llanma)",
                            "Намоз тартиби (Интерактив қўлланма)"
                        ).get(script)
                    )
                }

                // Privacy Policy & Fiqh Calculation Reference
                OutlinedButton(
                    onClick = { showPrivacyPolicyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_privacy_policy_button")
                ) {
                    Icon(Icons.Default.Policy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        lText(
                            "Maxfiylik siyosati va Fiqhiy asos",
                            "Махфийлик сиёсати ва Фиқҳий асос"
                        ).get(script)
                    )
                }
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            modifier = Modifier.testTag("settings_privacy_policy_dialog"),
            icon = { Icon(Icons.Default.Policy, contentDescription = null) },
            title = {
                Text(
                    lText(
                        "Maxfiylik va Fiqhiy asos",
                        "Махфийлик ва Фиқҳий асос"
                    ).get(script)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = lText(
                            "1. Kamera xavfsizligi: Kamera tasvirlari hech qachon yozib olinmaydi, saqlanmaydi va internetga yuborilmaydi. AI tahlil faqat qurilmaning tezkor xotirasida (RAM) ishlaydi.",
                            "1. Камера хавфсизлиги: Камера тасвирлари ҳеч қачон ёзиб олинмайди, сақланмайди ва интернетга юборилмайди. AI таҳлил фақат қурилманинг тезкор хотирасида (RAM) ишлайди."
                        ).get(script),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = lText(
                            "2. Ma’lumotlar egasi sizsiz: Qazo daftari, haftalik jadval va sozlamalar faqat telefoningizda saqlanadi. Xohlagan paytda JSON zaxira nusxasini olishingiz mumkin.",
                            "2. Маълумотлар эгаси сизсиз: Қазо дафтари, ҳафталик жадвал ва созламалар фақат телефонингизда сақланади. Хоҳлаган пайтда JSON захира нусхасини олишингиз мумкин."
                        ).get(script),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = lText(
                            "3. Fiqhiy mezon: Taqvim va rakat tartibi Hanafiy mazhabi hamda O‘zbekiston musulmonlari idorasi mezonlariga (Bomdod/Xufton 15°, Asr 2x soya) asoslangan.",
                            "3. Фиқҳий мезон: Тақвим ва ракат тартиби Ҳанафий мазҳаби ҳамда Ўзбекистон мусулмонлари идораси мезонларига (Бомдод/Хуфтон 15°, Аср 2х соя) асосланган."
                        ).get(script),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPrivacyPolicyDialog = false },
                    modifier = Modifier.testTag("settings_privacy_policy_close")
                ) {
                    Text(lText("Tushunarli", "Тушунарли").get(script))
                }
            }
        )
    }

    if (showOfflineMonthSheet) {
        val selectedCity = UzbekistanCities.findById(settings.selectedCityId)
        val todayDayOfMonth = remember {
            Calendar.getInstance(TimeZone.getTimeZone("Asia/Tashkent")).get(Calendar.DAY_OF_MONTH)
        }
        ModalBottomSheet(onDismissRequest = { showOfflineMonthSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("offline_monthly_calendar_sheet"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = lText(
                        "${selectedCity.name.latin} — Oylik oflayn taqvim",
                        "${selectedCity.name.cyrillic} — Ойлик офлайн тақвим"
                    ).get(script),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lText(
                        "Mahalliy Room bazasida saqlangan (${uiState.monthlyCalendarDays.size} kun). Internetsiz ishlaydi.",
                        "Маҳаллий Room базасида сақланган (${uiState.monthlyCalendarDays.size} кун). Интернетсиз ишлайди."
                    ).get(script),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = lText("Kun", "Кун").get(script),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(34.dp)
                    )
                    listOf(
                        lText("Bomdod", "Бомдод"),
                        lText("Quyosh", "Қуёш"),
                        lText("Peshin", "Пешин"),
                        lText("Asr", "Аср"),
                        lText("Shom", "Шом"),
                        lText("Xufton", "Хуфтон")
                    ).forEach { col ->
                        Text(
                            text = col.get(script),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (uiState.monthlyCalendarDays.isEmpty()) {
                    Text(
                        text = lText(
                            "Oylik taqvim yuklanmoqda...",
                            "Ойлик тақвим юкланмоқда..."
                        ).get(script),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.monthlyCalendarDays, key = { "${it.year}-${it.month}-${it.day}" }) { row ->
                            val isToday = row.day == todayDayOfMonth
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isToday) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${row.day}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(34.dp)
                                    )
                                    listOf(
                                        row.bomdod,
                                        row.quyosh,
                                        row.peshin,
                                        row.asr,
                                        row.shom,
                                        row.xufton
                                    ).forEach { timeText ->
                                        Text(
                                            text = timeText,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
