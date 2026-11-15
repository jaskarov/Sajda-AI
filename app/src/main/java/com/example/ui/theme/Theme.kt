package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

val SajdaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SajdaAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = SajdaShapes,
        content = content
    )
}
