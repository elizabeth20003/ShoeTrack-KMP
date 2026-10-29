package com.shoetrack.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shoetrack.presentation.theme.ShoeTrackTheme

private const val FILL_MILLIS = 700

/** A rounded bar showing how much of the price is paid. Changing [fraction] animates the fill. */
@Composable
fun PaymentProgress(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 8.dp,
    description: String? = null,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(FILL_MILLIS),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(CircleShape)
            .background(ShoeTrackTheme.colors.progressTrack)
            .semantics { description?.let { contentDescription = it } },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(CircleShape).background(color))
    }
}

/** The progress color that goes with a layaway's state. */
@Composable
fun progressColorFor(isOverdue: Boolean, isCompleted: Boolean): Color = when {
    isCompleted -> ShoeTrackTheme.colors.progressDone
    isOverdue -> ShoeTrackTheme.colors.progressOverdue
    else -> ShoeTrackTheme.colors.progressActive
}
