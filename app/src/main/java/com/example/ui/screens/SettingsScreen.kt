package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.viewmodel.MainUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onGenderChange: (Gender) -> Unit,
    onScriptChange: (TextScript) -> Unit,
    onCityChange: (String) -> Unit,
    onGeminiTtsToggle: (Boolean) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onDarkThresholdChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val settings = uiState.settings
    val script = settings.script

    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
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

                // Gemini TTS & Audio Coach Settings
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
                            text = lText("AI Ovozli Ustoz", "AI Овозли Устоз").get(script),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini TTS (gemini-3.8-flash-tts)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = lText(
                                        "Tabiiy ovozda suralar va ko‘rsatmalarni o‘qish",
                                        "Табиий овозда суралар ва кўрсатмаларни ўқиш"
                                    ).get(script),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.useGeminiTts,
                                onCheckedChange = onGeminiTtsToggle,
                                modifier = Modifier.testTag("settings_gemini_tts_switch")
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

                // City Picker
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
                            text = lText("Shahar", "Шаҳар").get(script),
                            style = MaterialTheme.typography.titleMedium
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
                    }
                }

                // Notification Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                    FilledTonalButton(
                        onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            lText(
                                "Namoz vaqti bildirishnomalariga ruxsat berish",
                                "Намоз вақти билдиришномаларига рухсат бериш"
                            ).get(script)
                        )
                    }
                }
            }
        }
    }
}
