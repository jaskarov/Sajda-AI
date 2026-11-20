package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.engine.UzbekistanCities
import com.example.domain.model.Gender
import com.example.domain.model.TextScript
import com.example.domain.model.lText
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Bespoke Samarkand Mihrab Artwork Hero Banner
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DeepHeroGradientStart,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 196.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_onboarding_art_1795154348749),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF061E17).copy(alpha = 0.42f),
                                        Color(0xFF061E17).copy(alpha = 0.65f),
                                        Color(0xFF041711).copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SaffronGold.copy(alpha = 0.24f),
                            border = BorderStroke(0.8.dp, SaffronGold.copy(alpha = 0.45f)),
                            shape = CircleShape
                        ) {
                            Text(
                                text = lText(
                                    "SO‘NGGI QADAM • TEZKOR SOZLASH",
                                    "СЎНГГИ ҚАДАМ • ТЕЗКОР СОЗЛАШ"
                                ).get(selectedScript),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE8B3),
                                letterSpacing = 0.7.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = lText(
                                "Assalomu alaykum! Shahringiz va qulaylikni tanlang",
                                "Ассалому алайкум! Шаҳрингиз ва қулайликни танланг"
                            ).get(selectedScript),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = lText(
                                "Taqvim va ovozli AI-ustoz sizga moslashadi. Istalgan payt sozlamalardan o‘zgartirish mumkin.",
                                "Тақвим ва овозли AI-устоз сизга мослашади. Исталган пайт созламалардан ўзгартириш мумкин."
                            ).get(selectedScript),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        if (onOpenPrayerGuide != null) {
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                onClick = onOpenPrayerGuide,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(0.8.dp, SaffronGold.copy(alpha = 0.55f)),
                                modifier = Modifier.testTag("onboarding_reopen_guide_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFFFFE8B3),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = lText(
                                            "Namoz tartibi (6 qadam)",
                                            "Намоз тартиби (6 қадам)"
                                        ).get(selectedScript),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFE8B3),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Compact Quick-Confirm Strip for Script & Gender (No duplicate bulky form cards!)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = lText("ASOSIY PROFIL", "АСОСИЙ ПРОФИЛ").get(selectedScript),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row A: Script Segmented Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactSegmentPill(
                                label = "Lotin",
                                selected = selectedScript == TextScript.LATIN,
                                onClick = { selectedScript = TextScript.LATIN },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("script_latin_button")
                            )
                            CompactSegmentPill(
                                label = "Кирилл",
                                selected = selectedScript == TextScript.CYRILLIC,
                                onClick = { selectedScript = TextScript.CYRILLIC },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("script_cyrillic_button")
                            )
                        }

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        )

                        // Row B: Gender Segmented Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactSegmentPill(
                                label = Gender.MALE.label.get(selectedScript),
                                icon = Icons.Default.Man,
                                selected = selectedGender == Gender.MALE,
                                onClick = { selectedGender = Gender.MALE },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gender_male_button")
                            )
                            CompactSegmentPill(
                                label = Gender.FEMALE.label.get(selectedScript),
                                icon = Icons.Default.Woman,
                                selected = selectedGender == Gender.FEMALE,
                                onClick = { selectedGender = Gender.FEMALE },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gender_female_button")
                            )
                        }
                    }
                }
            }

            // 3. Comfort & Accessibility Mode (Airy borderless editorial tiles)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = lText("QULAYLIK REJIMI", "ҚУЛАЙЛИК РЕЖИМИ").get(selectedScript),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EditorialChoiceTile(
                        label = lText("Oddiy / Yoshlar", "Оддий / Ёшлар").get(selectedScript),
                        subtitle = lText(
                            "To‘liq harakatlar, standart shrift",
                            "Тўлиқ ҳаракатлар, стандарт шрифт"
                        ).get(selectedScript),
                        icon = Icons.Default.Person,
                        selected = !seniorModeSelected,
                        onClick = { seniorModeSelected = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_mode_standard")
                    )
                    EditorialChoiceTile(
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

            // 4. City Selection (Clean Editorial Grouped Surface)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = lText("SHAHRINGIZ (TAQVIM UCHUN)", "ШАҲРИНГИЗ (ТАҚВИМ УЧУН)").get(selectedScript),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UzbekistanCities.all.forEach { city ->
                            val isSelected = city.id == selectedCityId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCityId = city.id },
                                shape = CircleShape,
                                label = {
                                    Text(
                                        text = city.name.get(selectedScript),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("city_chip_${city.id}")
                            )
                        }
                    }
                }
            }

            // 5. Primary Action Button
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
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompactSegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segment_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segment_fg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
            if (selected) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorialChoiceTile(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "choice_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "choice_border"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        tonalElevation = if (selected) 0.dp else 1.dp,
        border = BorderStroke(if (selected) 1.8.dp else 0.8.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
