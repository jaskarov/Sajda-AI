package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
import com.example.ui.theme.DeepHeroGradientEnd
import com.example.ui.theme.DeepHeroGradientStart
import com.example.ui.theme.SaffronGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialGender: Gender,
    initialScript: TextScript,
    initialCityId: String,
    initialSeatedMode: Boolean = false,
    initialLargeTextMode: Boolean = false,
    onAccessibilitySelected: (seatedMode: Boolean, largeTextMode: Boolean) -> Unit = { _, _ -> },
    onOpenPrayerGuide: (() -> Unit)? = null,
    onComplete: (Gender, TextScript, String) -> Unit
) {
    var selectedScript by remember { mutableStateOf(initialScript) }
    var selectedGender by remember { mutableStateOf(initialGender) }
    var selectedCityId by remember { mutableStateOf(initialCityId) }
    var seniorModeSelected by remember { mutableStateOf(initialSeatedMode || initialLargeTextMode) }

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
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(DeepHeroGradientStart, DeepHeroGradientEnd)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = SaffronGold.copy(alpha = 0.22f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = lText("NAMOZ USTOZ AI • VIRTUAL USTOZ", "НАМОЗ УСТОЗ AI • ВИРТУАЛ УСТОЗ")
                                .get(selectedScript),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFE7B3),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = lText(
                            "Assalomu alaykum! Keling, sozlab olaylik",
                            "Ассалому алайкум! Келинг, созлаб олайлик"
                        ).get(selectedScript),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = lText(
                            "Kamera va ovozli AI-ustoz sizning harakatlaringizga mos holda namozni bosqichma-bosqich o‘rgatadi.",
                            "Камера ва овозли AI-устоз сизнинг ҳаракатларингизга мос ҳолда намозни босқичма-босқич ўргатади."
                        ).get(selectedScript),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                    if (onOpenPrayerGuide != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            onClick = onOpenPrayerGuide,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, SaffronGold.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("onboarding_reopen_guide_button")
                        ) {
                            Text(
                                text = lText(
                                    "📖 Namoz tartibini (6 qadam) qayta ko‘rish",
                                    "📖 Намоз тартибини (6 қадам) қайта кўриш"
                                ).get(selectedScript),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFE7B3),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 1. Script Choice (Lotin / Kirill)
            SectionCard(
                icon = Icons.Default.Translate,
                title = lText("Yozuv turini tanlang", "Ёзув турини танланг").get(selectedScript)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChoiceTile(
                        label = "O‘zbekcha (Lotin)",
                        subtitle = "Alhamdulillahi...",
                        selected = selectedScript == TextScript.LATIN,
                        onClick = { selectedScript = TextScript.LATIN },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("script_latin_button")
                    )
                    ChoiceTile(
                        label = "Ўзбекча (Кирилл)",
                        subtitle = "Алҳамдулиллаҳи...",
                        selected = selectedScript == TextScript.CYRILLIC,
                        onClick = { selectedScript = TextScript.CYRILLIC },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("script_cyrillic_button")
                    )
                }
            }

            // 2. Gender Choice (Erkak / Ayol)
            SectionCard(
                icon = Icons.Default.Man,
                title = lText("Namoz qoidalari (Jins)", "Намоз қоидалари (Жинс)").get(selectedScript)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChoiceTile(
                        label = Gender.MALE.label.get(selectedScript),
                        subtitle = lText("Qo‘llar kindik ostida", "Қўллар киндик остида")
                            .get(selectedScript),
                        icon = Icons.Default.Man,
                        selected = selectedGender == Gender.MALE,
                        onClick = { selectedGender = Gender.MALE },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gender_male_button")
                    )
                    ChoiceTile(
                        label = Gender.FEMALE.label.get(selectedScript),
                        subtitle = lText("Qo‘llar ko‘krak ustida", "Қўллар кўкрак устида")
                            .get(selectedScript),
                        icon = Icons.Default.Woman,
                        selected = selectedGender == Gender.FEMALE,
                        onClick = { selectedGender = Gender.FEMALE },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gender_female_button")
                    )
                }
            }

            // 3. Comfort & Accessibility Mode (Standard vs Senior/Seated) - shown above city chips for elders
            SectionCard(
                icon = Icons.Default.Accessible,
                title = lText("Qanday qulaylik kerak?", "Қандай қулайлик керак?").get(selectedScript)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChoiceTile(
                        label = lText("Oddiy / Yoshlar", "Оддий / Ёшлар").get(selectedScript),
                        subtitle = lText(
                            "To‘liq harakatlar, standart shrift",
                            "Тўлиқ ҳаракатлар, стандарт шрифт"
                        ).get(selectedScript),
                        selected = !seniorModeSelected,
                        onClick = { seniorModeSelected = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_mode_standard")
                    )
                    ChoiceTile(
                        label = lText("Keksalar / Stulda", "Кексалар / Стулда").get(selectedScript),
                        subtitle = lText(
                            "Stulda imo-ishora + katta shrift",
                            "Стулда имо-ишора + катта шрифт"
                        ).get(selectedScript),
                        icon = Icons.Default.Accessible,
                        selected = seniorModeSelected,
                        onClick = { seniorModeSelected = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_mode_senior")
                    )
                }
            }

            // 4. City Choice
            SectionCard(
                icon = Icons.Default.LocationOn,
                title = lText("Shahringizni tanlang", "Шаҳрингизни танланг").get(selectedScript)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UzbekistanCities.all.forEach { city ->
                        val isSelected = city.id == selectedCityId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCityId = city.id },
                            label = { Text(city.name.get(selectedScript)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("city_chip_${city.id}")
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onAccessibilitySelected(seniorModeSelected, seniorModeSelected)
                    onComplete(selectedGender, selectedScript, selectedCityId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_continue_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = lText("Boshlash", "Бошлаш").get(selectedScript),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceTile(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
