package com.shoetrack.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.shoetrack.presentation.theme.Colorway
import com.shoetrack.presentation.theme.ShoeTrackTheme
import kotlin.math.abs

private const val DESIGN_WIDTH = 200f
private const val DESIGN_HEIGHT = 100f

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

/** A stylised sneaker drawn from paths, shown where a layaway has no photo yet. */
@Composable
fun SneakerIllustration(colorway: Colorway, modifier: Modifier = Modifier) {
    val sole = remember { path("M10 70 H196 V78 Q196 86 186 86 H20 Q10 86 10 78 Z") }
    val upper = remember {
        path("M14 70 C10 52 14 38 24 32 L42 28 C50 36 60 41 72 39 C86 33 94 21 108 21 C122 21 130 32 144 40 C164 47 190 50 195 70 Z")
    }
    val toeCap = remember { path("M146 41 C166 48 190 51 195 70 H146 Z") }
    val stripe = remember { path("M28 66 C40 56 58 52 78 54 C96 56 110 64 128 66") }
    val laces = remember { path("M96 26 L112 36 M108 24 L124 34 M118 28 L132 38") }

    Canvas(modifier = modifier.aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT)) {
        val scale = size.width / DESIGN_WIDTH
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawOval(Color.Black.copy(alpha = 0.14f), Offset(16f, 86f), Size(172f, 10f))
            drawPath(sole, colorway.sole)
            drawPath(upper, colorway.upper)
            drawPath(toeCap, colorway.panel)
            drawPath(stripe, colorway.panel, style = Stroke(width = 5f, cap = StrokeCap.Round))
            drawPath(laces, Color.White.copy(alpha = 0.9f), style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

/** Picks the same colorway for the same layaway everywhere, so a card and its detail match. */
@Composable
fun colorwayFor(id: String): Colorway {
    val colorways = ShoeTrackTheme.colors.colorways
    return colorways[abs(id.hashCode()) % colorways.size]
}
