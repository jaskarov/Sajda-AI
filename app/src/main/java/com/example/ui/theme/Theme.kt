package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = SaffronGold,
    onSecondary = OnSaffronGold,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = TealAccent,
    onTertiary = OnTealAccent,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = AlabasterBackground,
    onBackground = AlabasterOnBackground,
    surface = IvorySurface,
    onSurface = AlabasterOnBackground,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = IvoryOnSurfaceVariant,
    outline = SubtleOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = NightEmeraldPrimary,
    onPrimary = NightEmeraldOnPrimary,
    primaryContainer = NightEmeraldContainer,
    onPrimaryContainer = NightEmeraldOnContainer,
    secondary = NightGold,
    onSecondary = NightOnGold,
    secondaryContainer = NightGoldContainer,
    onSecondaryContainer = NightOnGoldContainer,
    tertiary = NightTeal,
    onTertiary = NightOnTeal,
    background = ObsidianBackground,
    onBackground = ObsidianOnBackground,
    surface = ObsidianSurface,
    onSurface = ObsidianOnBackground,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    outline = ObsidianOutline
)

private val OledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BD6A8),
    onPrimary = Color(0xFF00261A),
    primaryContainer = Color(0xFF0A241B),
    onPrimaryContainer = Color(0xFFB3F2D8),
    secondary = Color(0xFFD9B66B),
    onSecondary = Color(0xFF241A04),
    secondaryContainer = Color(0xFF211A0A),
    onSecondaryContainer = Color(0xFFF5DFAA),
    tertiary = Color(0xFF6BBEC7),
    onTertiary = Color(0xFF032327),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE2EBE6),
    surface = Color(0xFF0A0E0D),
    onSurface = Color(0xFFE2EBE6),
    surfaceVariant = Color(0xFF131917),
    onSurfaceVariant = Color(0xFFA2B3AC),
    outline = Color(0xFF23302B)
)

val SajdaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SajdaAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    oledNightMode: Boolean = false,
    largeTextMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        oledNightMode -> OledDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val baseDensity = LocalDensity.current
    val effectiveDensity = if (largeTextMode) {
        Density(
            density = baseDensity.density,
            fontScale = (baseDensity.fontScale * 1.18f).coerceAtMost(1.6f)
        )
    } else {
        baseDensity
    }

    CompositionLocalProvider(LocalDensity provides effectiveDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = SajdaShapes,
            content = content
        )
    }
}
