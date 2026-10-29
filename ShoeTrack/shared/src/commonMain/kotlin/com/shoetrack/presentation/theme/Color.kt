package com.shoetrack.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF101437)
private val Royal = Color(0xFF3A4BFF)
private val RoyalSoft = Color(0xFF7B88FF)
private val Sun = Color(0xFFFFC53D)
private val Coral = Color(0xFFCF3A28)
private val CoralSoft = Color(0xFFFF7B6B)
private val Mint = Color(0xFF0F9A64)
private val MintSoft = Color(0xFF4FD6A0)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Royal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E8FF),
    onPrimaryContainer = Ink,
    secondary = Sun,
    onSecondary = Ink,
    tertiary = Mint,
    onTertiary = Color.White,
    background = Color(0xFFEDF0F6),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F4FA),
    onSurfaceVariant = Color(0xFF5B6180),
    outline = Color(0xFFC4CAE4),
    outlineVariant = Color(0xFFE3E7F2),
    error = Coral,
    onError = Color.White,
    errorContainer = Color(0xFFFFDDD6),
    onErrorContainer = Color(0xFF7A1F12),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = RoyalSoft,
    onPrimary = Ink,
    primaryContainer = Color(0xFF232A66),
    onPrimaryContainer = Color(0xFFE4E8FF),
    secondary = Sun,
    onSecondary = Ink,
    tertiary = MintSoft,
    onTertiary = Ink,
    background = Color(0xFF0B0E24),
    onBackground = Color(0xFFF2F4FF),
    surface = Color(0xFF121637),
    onSurface = Color(0xFFF2F4FF),
    surfaceVariant = Color(0xFF1C2150),
    onSurfaceVariant = Color(0xFFA3AAD6),
    outline = Color(0xFF3A4070),
    outlineVariant = Color(0xFF2B3160),
    error = CoralSoft,
    onError = Ink,
    errorContainer = Color(0xFF4A2420),
    onErrorContainer = Color(0xFFFFDDD6),
)

/** Colors of a shoe illustration and of the tinted backdrop it sits on. */
@Immutable
data class Colorway(
    val backdrop: Color,
    val upper: Color,
    val panel: Color,
    val sole: Color,
)

/** Colors the Material scheme has no slot for. */
@Immutable
data class ShoeTrackColors(
    val progressTrack: Color,
    val progressActive: Color,
    val progressOverdue: Color,
    val progressDone: Color,
    val hero: Color,
    val onHero: Color,
    val sun: Color,
    val onSun: Color,
    val tag: Color,
    val onTag: Color,
    val doneBadge: Color,
    val onDoneBadge: Color,
    val overdueBadge: Color,
    val onOverdueBadge: Color,
    val live: Color,
    val colorways: List<Colorway>,
)

internal val LightShoeTrackColors = ShoeTrackColors(
    progressTrack = Color(0xFFC4CAE4),
    progressActive = Royal,
    progressOverdue = Coral,
    progressDone = Mint,
    hero = Royal,
    onHero = Color.White,
    sun = Sun,
    onSun = Ink,
    tag = Ink,
    onTag = Color.White,
    doneBadge = MintSoft,
    onDoneBadge = Ink,
    overdueBadge = Coral,
    onOverdueBadge = Color.White,
    live = Color(0xFF12A36B),
    colorways = listOf(
        Colorway(Color(0xFFDCE2FF), Color.White, Royal, Ink),
        Colorway(Color(0xFFFFF0C7), Sun, Ink, Color.White),
        Colorway(Color(0xFFFFDDD6), Ink, Color(0xFFF2503F), Color.White),
        Colorway(Color(0xFFD3F3E5), Color(0xFF2BB47D), Color.White, Ink),
    ),
)

internal val DarkShoeTrackColors = ShoeTrackColors(
    progressTrack = Color(0xFF3A4070),
    progressActive = RoyalSoft,
    progressOverdue = CoralSoft,
    progressDone = MintSoft,
    hero = Royal,
    onHero = Color.White,
    sun = Sun,
    onSun = Ink,
    tag = Color(0xFFF2F4FF),
    onTag = Ink,
    doneBadge = MintSoft,
    onDoneBadge = Ink,
    overdueBadge = CoralSoft,
    onOverdueBadge = Ink,
    live = MintSoft,
    colorways = listOf(
        Colorway(Color(0xFF232A66), Color.White, RoyalSoft, Color(0xFFC9CFFF)),
        Colorway(Color(0xFF3D3416), Sun, Ink, Color.White),
        Colorway(Color(0xFF4A2420), Color(0xFF2A2F6B), CoralSoft, Color.White),
        Colorway(Color(0xFF163D31), Color(0xFF2BB47D), Color.White, Color(0xFFD3F3E5)),
    ),
)
