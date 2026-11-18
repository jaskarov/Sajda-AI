package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.DeepHeroGradientEnd
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.PoseMatchedGreen
import com.example.ui.theme.PoseWarningAmber
import com.example.ui.theme.SaffronGold

@Composable
fun PermissionsScreen(
    script: TextScript,
    isFromSettings: Boolean,
    onContinue: () -> Unit
) {
    BackHandler(enabled = isFromSettings, onBack = onContinue)

    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshTick++
    }

    val cameraGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val locationGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    val notificationsGranted = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val exactAlarmGranted = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val batteryOptimizationIgnored = remember(refreshTick) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    val runtimePermsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTick++
    }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTick++
    }

    val requestStandardPermissions = {
        val toRequest = buildList {
            if (!cameraGranted) add(Manifest.permission.CAMERA)
            if (!locationGranted) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (toRequest.isNotEmpty()) {
            runtimePermsLauncher.launch(toRequest.toTypedArray())
        }
    }

    val openExactAlarmSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
            runCatching {
                exactAlarmSettingsLauncher.launch(intent)
            }.onFailure {
                val fallback = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                exactAlarmSettingsLauncher.launch(fallback)
            }
        }
    }

    val openBatteryOptimizationSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        runCatching {
            exactAlarmSettingsLauncher.launch(intent)
        }.onFailure {
            val fallback = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            runCatching { exactAlarmSettingsLauncher.launch(fallback) }
        }
        Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isFromSettings) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onContinue,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("permissions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = lText("Orqaga", "Орқага").get(script)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = lText("Ruxsatlar markazi", "Рухсатлар маркази").get(script),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(DeepHeroGradientStart, DeepHeroGradientEnd)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SaffronGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = lText("XAVFSIZ RUXSATLAR", "ХАВФСИЗ РУХСАТЛАР").get(script),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFE5A3)
                        )
                    }
                    Text(
                        text = lText(
                            "To‘liq imkoniyatlar uchun ruxsat bering",
                            "Тўлиқ имкониятлар учун рухсат беринг"
                        ).get(script),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lText(
                            "Kamera tasvirlari saqlanmaydi va faqat qurilmaning o‘zida qayta ishlanadi. Ruxsat bermasangiz ham Audio rejimi ishlayveradi.",
                            "Камера тасвирлари сақланмайди ва фақат қурилманинг ўзида қайта ишланади. Рухсат бермасангиз ҳам Аудио режими ишлайверади."
                        ).get(script),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // 1. Camera permission
            PermissionCard(
                icon = Icons.Default.CameraAlt,
                title = lText("Kamera (AI holat tahlili)", "Камера (AI ҳолат таҳлили)").get(script),
                description = lText(
                    "Qiyom, Ruku va Sajda burchaklarini real vaqtda kuzatish uchun kerak.",
                    "Қиём, Руку ва Сажда бурчакларини реал вақтда кузатиш учун керак."
                ).get(script),
                granted = cameraGranted,
                actionLabel = lText("Kameraga ruxsat berish", "Камерага рухсат бериш").get(script),
                onAction = { runtimePermsLauncher.launch(arrayOf(Manifest.permission.CAMERA)) },
                script = script,
                testTag = "permission_camera_button"
            )

            // 2. Geolocation permission
            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = lText("Joylashuv (GPS / Shahar)", "Жойлашув (GPS / Шаҳар)").get(script),
                description = lText(
                    "Eng yaqin shahar namoz vaqtlarini va Qibla burchagini aniqlash uchun kerak.",
                    "Энг яқин шаҳар намоз вақтларини ва Қибла бурчагини аниқлаш учун керак."
                ).get(script),
                granted = locationGranted,
                actionLabel = lText("Joylashuvga ruxsat berish", "Жойлашувга рухсат бериш").get(script),
                onAction = {
                    runtimePermsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                script = script,
                testTag = "permission_location_button"
            )

            // 3. Notification permission (Android 13+)
            PermissionCard(
                icon = Icons.Default.NotificationsActive,
                title = lText("Bildirishnomalar (Android 13+)", "Билдиришномалар (Android 13+)").get(script),
                description = lText(
                    "5 vaqt namoz vaqti kirganda eslatma va azon bildirishnomasini ko‘rsatish uchun.",
                    "5 вақт намоз вақти кирганда эслатма ва азон билдиришномасини кўрсатиш учун."
                ).get(script),
                granted = notificationsGranted,
                actionLabel = lText("Bildirishnomaga ruxsat", "Билдиришномага рухсат").get(script),
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runtimePermsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                },
                script = script,
                testTag = "permission_notifications_button"
            )

            // 4. Exact Alarm permission (Android 12+)
            PermissionCard(
                icon = Icons.Default.Alarm,
                title = lText("Aniq vaqtdagi azon signali", "Аниқ вақтдаги азон сигнали").get(script),
                description = lText(
                    "Telefon uyqu rejimida bo‘lsa ham namoz vaqtidan kechikmasdan signal chalishi uchun (SCHEDULE_EXACT_ALARM).",
                    "Телефон уйқу режимида бўлса ҳам намоз вақтидан кечикмасдан сигнал чалиши учун (SCHEDULE_EXACT_ALARM)."
                ).get(script),
                granted = exactAlarmGranted,
                actionLabel = lText("Aniq signalni yoqish", "Аниқ сигнални ёқиш").get(script),
                onAction = openExactAlarmSettings,
                script = script,
                testTag = "permission_exact_alarm_button"
            )

            // 5. Battery Optimization (MIUI / Samsung background reliability)
            PermissionCard(
                icon = Icons.Default.BatteryChargingFull,
                title = lText(
                    "Batareya cheklovisiz azon (Xiaomi / Samsung)",
                    "Батарея чекловисиз азон (Xiaomi / Samsung)"
                ).get(script),
                description = lText(
                    "Telefon uxlaganda azon signali to‘xtab qolmasligi uchun tizim sozlamalarida Namoz Ustoz AI uchun \"Cheklanmagan\" (Unrestricted) rejimini tanlang.",
                    "Телефон ухлаганда азон сигнали тўхтаб қолмаслиги учун тизим созламаларида Namoz Ustoz AI учун «Чекланмаган» (Unrestricted) режимини танланг."
                ).get(script),
                granted = batteryOptimizationIgnored,
                actionLabel = lText("Batareya sozlamalarini ochish", "Батарея созламаларини очиш").get(script),
                onAction = openBatteryOptimizationSettings,
                script = script,
                testTag = "permission_battery_button"
            )

            if (!cameraGranted || !locationGranted || !notificationsGranted) {
                OutlinedButton(
                    onClick = requestStandardPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("permissions_request_all_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = lText(
                            "Barcha standart ruxsatlarni so‘rash",
                            "Барча стандарт рухсатларни сўраш"
                        ).get(script),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("permissions_continue_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isFromSettings) {
                        lText("Saqlash va qaytish", "Сақлаш ва қайтиш").get(script)
                    } else if (cameraGranted && locationGranted && notificationsGranted && exactAlarmGranted) {
                        lText("Ilovaga kirish", "Иловага кириш").get(script)
                    } else {
                        lText("Davom etish (Keyinroq ham sozlash mumkin)", "Давом этиш (Кейинроқ ҳам созлаш мумкин)")
                            .get(script)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    script: TextScript,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (granted) PoseMatchedGreen.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (granted) PoseMatchedGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (granted) PoseMatchedGreen.copy(alpha = 0.16f)
                    else PoseWarningAmber.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (granted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PoseMatchedGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = if (granted) {
                                lText("Ruxsat bor", "Рухсат бор").get(script)
                            } else {
                                lText("So‘ralmagan", "Сўралмаган").get(script)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (granted) PoseMatchedGreen else PoseWarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!granted) {
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
