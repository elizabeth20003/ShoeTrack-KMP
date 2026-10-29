package com.shoetrack.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Icons drawn on a 24-unit grid, so the app does not need the material-icons dependency. */
enum class IconShape { Plus, Search, Back, Check }

@Composable
fun AppIcon(icon: IconShape, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val unit = this.size.width / 24f
        val stroke = Stroke(width = 2.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        when (icon) {
            IconShape.Plus -> {
                drawLine(color, point(12f, 5f), point(12f, 19f), stroke.width, StrokeCap.Round)
                drawLine(color, point(5f, 12f), point(19f, 12f), stroke.width, StrokeCap.Round)
            }
            IconShape.Search -> {
                drawCircle(color, radius = 6.5f * unit, center = point(10.5f, 10.5f), style = stroke)
                drawLine(color, point(15.5f, 15.5f), point(20f, 20f), stroke.width, StrokeCap.Round)
            }
            IconShape.Back -> drawPath(
                Path().apply {
                    moveTo(15f * unit, 5f * unit)
                    lineTo(8f * unit, 12f * unit)
                    lineTo(15f * unit, 19f * unit)
                },
                color,
                style = stroke,
            )
            IconShape.Check -> drawPath(
                Path().apply {
                    moveTo(5f * unit, 12f * unit)
                    lineTo(10f * unit, 17f * unit)
                    lineTo(19f * unit, 7f * unit)
                },
                color,
                style = stroke,
            )
        }
    }
}
