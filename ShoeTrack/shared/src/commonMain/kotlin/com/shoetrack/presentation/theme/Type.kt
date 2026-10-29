package com.shoetrack.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.bricolage_grotesque_bold
import shoetrack.shared.generated.resources.bricolage_grotesque_extrabold
import shoetrack.shared.generated.resources.bricolage_grotesque_semibold
import shoetrack.shared.generated.resources.dm_sans_bold
import shoetrack.shared.generated.resources.dm_sans_medium
import shoetrack.shared.generated.resources.dm_sans_regular
import shoetrack.shared.generated.resources.dm_sans_semibold

/** Styles for amounts of money, which are the biggest numbers on screen. */
@Immutable
data class MoneyStyles(
    val large: TextStyle,
    val medium: TextStyle,
    val small: TextStyle,
)

@Composable
internal fun displayFontFamily(): FontFamily = FontFamily(
    Font(Res.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(Res.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)

@Composable
internal fun bodyFontFamily(): FontFamily = FontFamily(
    Font(Res.font.dm_sans_regular, FontWeight.Normal),
    Font(Res.font.dm_sans_medium, FontWeight.Medium),
    Font(Res.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.dm_sans_bold, FontWeight.Bold),
)

internal fun shoeTrackTypography(display: FontFamily, body: FontFamily): Typography {
    val base = Typography()
    fun displayStyle(size: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(
        fontFamily = display,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * 1.1f).sp,
        letterSpacing = (-0.02f * size).sp,
    )
    return base.copy(
        displayLarge = displayStyle(44),
        displayMedium = displayStyle(40),
        displaySmall = displayStyle(32),
        headlineLarge = displayStyle(32),
        headlineMedium = displayStyle(26),
        headlineSmall = displayStyle(22),
        titleLarge = displayStyle(22),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

internal fun moneyStyles(display: FontFamily) = MoneyStyles(
    large = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.9f).sp),
    medium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.2f).sp),
    small = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
)
