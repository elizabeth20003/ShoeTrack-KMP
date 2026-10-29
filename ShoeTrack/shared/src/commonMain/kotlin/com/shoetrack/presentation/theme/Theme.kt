package com.shoetrack.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalShoeTrackColors = staticCompositionLocalOf { LightShoeTrackColors }
private val LocalMoneyStyles = staticCompositionLocalOf<MoneyStyles> { error("ShoeTrackTheme is not applied") }

/** Accessors for what the Material theme has no slot for. */
object ShoeTrackTheme {
    val colors: ShoeTrackColors
        @Composable @ReadOnlyComposable get() = LocalShoeTrackColors.current

    val money: MoneyStyles
        @Composable @ReadOnlyComposable get() = LocalMoneyStyles.current
}

private val ShoeTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ShoeTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val display = displayFontFamily()
    val body = bodyFontFamily()
    val typography = shoeTrackTypography(display, body)
    val money = remember(display) { moneyStyles(display) }

    CompositionLocalProvider(
        LocalShoeTrackColors provides if (darkTheme) DarkShoeTrackColors else LightShoeTrackColors,
        LocalMoneyStyles provides money,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = typography,
            shapes = ShoeTrackShapes,
            content = content,
        )
    }
}
