package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.ReminderSoundMode
import com.example.data.remote.RemoteUzbekistanPrayerTimesService
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.DeepHeroGradientEnd
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LuxuryGold
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
    onOledNightModeToggle: (Boolean) -> Unit = {},
    onReminderSoundModeChange: (ReminderSoundMode) -> Unit = {},
    onPreviewTakbirSound: () -> Unit = {},
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = lText("Sozlamalar", "Созламалар").get(script),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // GROUP 1: PERSONALIZATION
                SettingsSectionBlock(
                    title = lText("SHAXSIYLASHTIRISH", "ШАХСИЙЛАШТИРИШ").get(script)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = lText("Yozuv turi", "Ёзув тури").get(script),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = script == TextScript.LATIN,
                                onClick = { onScriptChange(TextScript.LATIN) },
                                shape = CircleShape,
                                label = { Text("Lotin") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("settings_script_latin")
                            )
                            FilterChip(
                                selected = script == TextScript.CYRILLIC,
                                onClick = { onScriptChange(TextScript.CYRILLIC) },
                                shape = CircleShape,
                                label = { Text("Кирилл") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("settings_script_cyrillic")
                            )
                        }

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        )

                        Text(
                            text = lText("Jins (Namoz qoidalari)", "Жинс (Намоз қоидалари)").get(script),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Gender.entries.forEach { g ->
                                FilterChip(
                                    selected = settings.gender == g,
                                    onClick = { onGenderChange(g) },
                                    shape = CircleShape,
                                    label = { Text(g.label.get(script)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("settings_gender_${g.id}")
                                )
                            }
                        }
                    }
                }

                // GROUP 2: ACCESSIBILITY & APPEARANCE (Single clean grouped list)
                SettingsSectionBlock(
                    title = lText("QULAYLIK VA KO‘RINISH", "ҚУЛАЙЛИК ВА КЎРИНИШ").get(script)
                ) {
                    SettingsSwitchRow(
                        icon = Icons.Default.Accessible,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Stulda / O‘tirib o‘qish (Imo-ishora)",
                            "Стулда / Ўтириб ўқиш (Имо-ишора)"
                        ).get(script),
                        subtitle = lText(
                            "Keksalar va bel-tizzada og‘rig‘i borlar uchun: ruku' (15°–30°), sajda (45°–60°) egilish.",
                            "Кексалар ва бел-тиззада оғриғи борлар учун: рукуъ (15°–30°), сажда (45°–60°) эгилиш."
                        ).get(script),
                        checked = settings.seatedPrayerMode,
                        onCheckedChange = onSeatedModeToggle,
                        switchTag = "settings_seated_mode_switch"
                    )
                    SettingsInsetDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.FormatSize,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Katta shrift rejimi (Keksalar uchun)",
                            "Катта шрифт режими (Кексалар учун)"
                        ).get(script),
                        subtitle = lText(
                            "Matn va tugmalarni o‘qishga qulay qilib kattalashtiradi.",
                            "Матн ва тугмаларни ўқишга қулай қилиб катталаштиради."
                        ).get(script),
                        checked = settings.largeTextMode,
                        onCheckedChange = onLargeTextModeToggle,
                        switchTag = "settings_large_text_switch"
                    )
                    SettingsInsetDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.BatterySaver,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Tejamkor kamera rejimi",
                            "Тежамкор камера режими"
                        ).get(script),
                        subtitle = lText(
                            "Byudjet telefonlarda qizish va quvvat sarfini kamaytiradi (8 FPS).",
                            "Бюджет телефонларда қизиш ва қувват сарфини камайтиради (8 FPS)."
                        ).get(script),
                        checked = settings.lowPowerCameraMode,
                        onCheckedChange = onLowPowerCameraToggle,
                        switchTag = "settings_low_power_camera_switch"
                    )
                    SettingsInsetDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = lText(
                            "Bomdod OLED tungi rejim",
                            "Бомдод OLED тунги режим"
                        ).get(script),
                        subtitle = lText(
                            "Soat 05:00 da qorong‘i xonada ko‘zni qamashtirmaydigan qop-qora ekran.",
                            "Соат 05:00 да қоронғи хонада кўзни қамаштирмайдиган қоп-қора экран."
                        ).get(script),
                        checked = settings.oledNightMode,
                        onCheckedChange = onOledNightModeToggle,
                        switchTag = "settings_oled_night_mode_switch"
                    )
                }

                // GROUP 3: REMINDER SOUND & VOICE COACH
                SettingsSectionBlock(
                    title = lText("ESLATMA VA OVOZLI USTOZ", "ЭСЛАТМА ВА ОВОЗЛИ УСТОЗ").get(script)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingsIconBadge(
                                icon = Icons.Default.NotificationsActive,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lText(
                                        "Namoz eslatmasi ovozi",
                                        "Намоз эслатмаси овози"
                                    ).get(script),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = lText(
                                        "Bomdodga uyg‘onish uchun Qori Takbiri yoki faqat tebranishni tanlang.",
                                        "Бомдодга уйғониш учун Қори Такбири ёки фақат тебранишни танланг."
                                    ).get(script),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReminderSoundMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.reminderSoundMode == mode,
                                    onClick = { onReminderSoundModeChange(mode) },
                                    shape = CircleShape,
                                    label = { Text(mode.label.get(script)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("settings_sound_${mode.id}")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onPreviewTakbirSound,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("settings_preview_takbir_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = lText(
                                    "Qori Takbirini tinglash",
                                    "Қори Такбирини тинглаш"
                                ).get(script)
                            )
                        }
                    }

                    SettingsInsetDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.GraphicEq,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Oflayn Qori MP3 (ExoPlayer)",
                            "Офлайн Қори MP3 (ExoPlayer)"
                        ).get(script),
                        subtitle = lText(
                            "15 ta sura, duo va tasbeh MP3 fayllari internetsiz ijro etiladi.",
                            "15 та сура, дуо ва тасбеҳ MP3 файллари интернетсиз ижро этилади."
                        ).get(script),
                        checked = settings.preferOfflineMp3,
                        onCheckedChange = onPreferOfflineMp3Toggle,
                        switchTag = "settings_offline_mp3_switch"
                    )
                    SettingsInsetDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.VolumeUp,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Namozda ovozni avtomatik ko‘tarish",
                            "Намозда овозни автоматик кўтариш"
                        ).get(script),
                        subtitle = lText(
                            "2 metr masofadan eshitish uchun media ovozini 85% ga ko‘taradi.",
                            "2 метр масофадан эшитиш учун медиа овозини 85% га кўтаради."
                        ).get(script),
                        checked = settings.autoBoostPrayerVolume,
                        onCheckedChange = onAutoBoostVolumeToggle,
                        switchTag = "settings_auto_volume_switch"
                    )
                    SettingsInsetDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = lText(
                            "Rukn tasdig‘i tovushi (Chime)",
                            "Рукн тасдиғи товуши (Chime)"
                        ).get(script),
                        subtitle = lText(
                            "Keyingi bosqichga o‘tganini bilish uchun yengil ovozli signal.",
                            "Кейинги босқичга ўтганини билиш учун енгил овозли сигнал."
                        ).get(script),
                        checked = settings.acousticChimeEnabled,
                        onCheckedChange = onAcousticChimeToggle,
                        switchTag = "settings_acoustic_chime_switch"
                    )

                    SettingsInsetDivider()

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onTestAudio,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_test_audio_button")
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Fotiha sinovi", "Фотиҳа синови").get(script), maxLines = 1)
                            }
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_tts_settings_button")
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Tizim TTS", "Тизим TTS").get(script), maxLines = 1)
                            }
                        }

                        val tts = uiState.ttsReadiness
                        val ttsSummary = when {
                            tts.hasUzbekLocale -> lText(
                                "Tizim ovozi (TTS): O‘zbek tili tayyor",
                                "Тизим овози (TTS): Ўзбек тили тайёр"
                            )
                            tts.hasFallbackLocale -> lText(
                                "Tizim ovozi (TTS): Yordamchi ovoz faol. Oflayn Qori MP3 ishlaydi.",
                                "Тизим овози (TTS): Ёрдамчи овоз фаол. Офлайн Қори MP3 ишлайди."
                            )
                            else -> lText(
                                "Oflayn Qori MP3 tayyor. Qo‘shimcha ko‘rsatmalar uchun tizim TTS sozlanishi mumkin.",
                                "Офлайн Қори MP3 тайёр. Қўшимча кўрсатмалар учун тизим TTS созланиши мумкин."
                            )
                        }.get(script)
                        Text(
                            text = ttsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lText("Ovoz tezligi", "Овоз тезлиги").get(script),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = String.format(Locale.US, "%.2fx", settings.voiceSpeed),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.voiceSpeed,
                            onValueChange = onVoiceSpeedChange,
                            valueRange = 0.75f..1.35f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lText("Qorong‘i xona sezgirligi", "Қоронғи хона сезгирлиги").get(script),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${settings.darkRoomLuminanceThreshold.toInt()} lum",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.darkRoomLuminanceThreshold,
                            onValueChange = onDarkThresholdChange,
                            valueRange = 12f..55f
                        )
                    }
                }

                // GROUP 4: 365-DAY CALENDAR & BACKUP
                SettingsSectionBlock(
                    title = lText("TAQVIM VA ZAXIRA", "ТАҚВИМ ВА ЗАХИРА").get(script)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    shape = CircleShape,
                                    label = { Text(city.name.get(script)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onSyncCalendarNow,
                                enabled = !uiState.isCalendarSyncing,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_sync_calendar_button")
                            ) {
                                if (uiState.isCalendarSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Yangilash", "Янгилаш").get(script), maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { showOfflineMonthSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_view_offline_calendar_button")
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Oylik jadval", "Ойлик жадвал").get(script), maxLines = 1)
                            }
                        }

                        uiState.calendarSyncStatusMessage?.let { statusMsg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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

                    SettingsInsetDivider()

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = lText(
                                "Zaxira nusxa (JSON • Saqlash va Tiklash)",
                                "Захира нусха (JSON • Сақлаш ва Тиклаш)"
                            ).get(script),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lText(
                                "Qazo namozlari, kundalik jadval va tarixni telefonga saqlang yoki tiklang.",
                                "Қазо намозлари, кундалик жадвал ва тарихни телефонга сақланг ёки тикланг."
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_export_backup_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_import_backup_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(lText("Tiklash", "Тиклаш").get(script))
                            }
                        }

                        uiState.backupStatusMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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

                // GROUP 5: PERMISSIONS, GUIDE & PRIVACY (Clean chevron action rows)
                SettingsSectionBlock(
                    title = lText("XAVFSIZLIK VA QO‘LLANMA", "ХАВФСИЗЛИК ВА ҚЎЛЛАНМА").get(script)
                ) {
                    SettingsActionRow(
                        icon = Icons.Default.Security,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Ruxsatlar markazi",
                            "Рухсатлар маркази"
                        ).get(script),
                        subtitle = lText(
                            "Kamera, Bildirishnoma va Aniq Azon",
                            "Камера, Билдиришнома ва Аниқ Азон"
                        ).get(script),
                        onClick = onOpenPermissions,
                        tag = "settings_open_permissions_button"
                    )
                    SettingsInsetDivider()
                    SettingsActionRow(
                        icon = Icons.Default.MenuBook,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = lText(
                            "Namoz tartibi (6 qadam)",
                            "Намоз тартиби (6 қадам)"
                        ).get(script),
                        subtitle = lText(
                            "Interaktiv qo‘llanmani qayta ko‘rish",
                            "Интерактив қўлланмани қайта кўриш"
                        ).get(script),
                        onClick = onOpenPrayerGuide,
                        tag = "settings_open_prayer_guide_button"
                    )
                    SettingsInsetDivider()
                    SettingsActionRow(
                        icon = Icons.Default.Policy,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = lText(
                            "Maxfiylik siyosati va Fiqhiy asos",
                            "Махфийлик сиёсати ва Фиқҳий асос"
                        ).get(script),
                        subtitle = lText(
                            "100% oflayn kamera va Hanafiy mezonlari",
                            "100% офлайн камера ва Ҳанафий мезонлари"
                        ).get(script),
                        onClick = { showPrivacyPolicyDialog = true },
                        tag = "settings_privacy_policy_button"
                    )
                }

                // Warm Islamic Closing Banner (4th-mockup touch)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeepHeroGradientStart,
                    border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_dua_banner")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_mosque_1795154346302),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.32f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            DeepHeroGradientStart.copy(alpha = 0.84f),
                                            DeepHeroGradientEnd.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = lText(
                                        "Alloh namozimizni qabul qilsin!",
                                        "Аллоҳ намозимизни қабул қилсин!"
                                    ).get(script),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = lText(
                                        "Namoz Ustoz AI • 100% oflayn va maxfiy",
                                        "Намоз Устоз AI • 100% офлайн ва махфий"
                                    ).get(script),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFE5A3)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = LuxuryGold.copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = LuxuryGold,
                                        modifier = Modifier.size(22.dp)
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

@Composable
private fun SettingsSectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(
                0.8.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    tint: Color
) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SettingsInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(icon = icon, tint = iconTint)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(switchTag)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(icon = icon, tint = iconTint)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}
