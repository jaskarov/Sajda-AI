package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Palette - Warm Alabaster & Deep Emerald Pine
val EmeraldPrimary = Color(0xFF0D5C46)
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldContainer = Color(0xFFCDEEE0)
val EmeraldOnContainer = Color(0xFF062E22)

val SaffronGold = Color(0xFFC48B1C)
val OnSaffronGold = Color(0xFFFFFFFF)
val SaffronContainer = Color(0xFFFCEBC7)
val OnSaffronContainer = Color(0xFF3E2900)

val TealAccent = Color(0xFF1F5F6B)
val OnTealAccent = Color(0xFFFFFFFF)
val TealContainer = Color(0xFFC8EBF2)
val OnTealContainer = Color(0xFF062C33)

val AlabasterBackground = Color(0xFFF8F6F0)
val AlabasterOnBackground = Color(0xFF171E1B)
val IvorySurface = Color(0xFFFFFFFF)
val IvorySurfaceVariant = Color(0xFFECE9DF)
val IvoryOnSurfaceVariant = Color(0xFF47524D)
val SubtleOutline = Color(0xFFD0CCC0)

// Dark Palette - Deep Luxury Emerald (#071A12) & Warm Gold (#E8B954)
val NightEmeraldPrimary = Color(0xFF4DD9AC)
val NightEmeraldOnPrimary = Color(0xFF003828)
val NightEmeraldContainer = Color(0xFF0E3D2D)
val NightEmeraldOnContainer = Color(0xFFC5F7E3)

val LuxuryGold = Color(0xFFE8B954)
val LuxuryGoldLight = Color(0xFFF6D782)
val LuxuryGoldDeep = Color(0xFFC99126)

val NightGold = Color(0xFFE8B954)
val NightOnGold = Color(0xFF261900)
val NightGoldContainer = Color(0xFF3E2D08)
val NightOnGoldContainer = Color(0xFFFFE7B3)

val NightTeal = Color(0xFF83D2E3)
val NightOnTeal = Color(0xFF003640)

val ObsidianBackground = Color(0xFF071A12)
val ObsidianOnBackground = Color(0xFFECF4F0)
val ObsidianSurface = Color(0xFF0E251C)
val ObsidianSurfaceVariant = Color(0xFF153428)
val ObsidianOnSurfaceVariant = Color(0xFFB3C7BE)
val ObsidianOutline = Color(0xFF264D3E)

// Semantic Status Colors
val PoseMatchedGreen = Color(0xFF18A96B)
val PoseWarningAmber = Color(0xFFE59B1A)
val PoseAlertCoral = Color(0xFFD94A4A)
val DeepHeroGradientStart = Color(0xFF0A4736)
val DeepHeroGradientEnd = Color(0xFF136E54)

// Dynamic 5-Prayer Sky Atmosphere Tokens (2026 Editorial Hero)
data class PrayerSkyAtmosphere(
    val startColor: Color,
    val midColor: Color,
    val endColor: Color,
    val accentGlow: Color,
    val badgeTint: Color
)

fun prayerSkyAtmosphere(
    prayerId: String,
    oledNightMode: Boolean
): PrayerSkyAtmosphere {
    if (oledNightMode) {
        return PrayerSkyAtmosphere(
            startColor = Color(0xFF040E0A),
            midColor = Color(0xFF020705),
            endColor = Color(0xFF000000),
            accentGlow = Color(0xFFC48B1C),
            badgeTint = Color(0xFFE8C87A)
        )
    }
    return when (prayerId) {
        "bomdod" -> PrayerSkyAtmosphere(
            // Dawn indigo twilight melting into soft Fajr emerald
            startColor = Color(0xFF0E1D3B),
            midColor = Color(0xFF163A52),
            endColor = Color(0xFF1D5C54),
            accentGlow = Color(0xFFF5D08A),
            badgeTint = Color(0xFFFFE7B0)
        )
        "quyosh", "peshin" -> PrayerSkyAtmosphere(
            // Crisp Samarkand daylight emerald pine
            startColor = Color(0xFF0A4736),
            midColor = Color(0xFF11634D),
            endColor = Color(0xFF1B7D61),
            accentGlow = Color(0xFFF7D57E),
            badgeTint = Color(0xFFFFE5A3)
        )
        "asr" -> PrayerSkyAtmosphere(
            // Warm golden-hour olive emerald & honey saffron
            startColor = Color(0xFF1F3D31),
            midColor = Color(0xFF474E2B),
            endColor = Color(0xFF7B5D25),
            accentGlow = Color(0xFFFFDE96),
            badgeTint = Color(0xFFFFEAC0)
        )
        "shom" -> PrayerSkyAtmosphere(
            // Maghrib twilight plum-crimson & warm dusk amber
            startColor = Color(0xFF231635),
            midColor = Color(0xFF53243B),
            endColor = Color(0xFF84422B),
            accentGlow = Color(0xFFFFC994),
            badgeTint = Color(0xFFFFDEC0)
        )
        else -> PrayerSkyAtmosphere(
            // Xufton starlit sapphire-emerald night
            startColor = Color(0xFF081426),
            midColor = Color(0xFF0E253D),
            endColor = Color(0xFF123A46),
            accentGlow = Color(0xFFE4D2A0),
            badgeTint = Color(0xFFF2E3B8)
        )
    }
}

