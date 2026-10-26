package com.motovault.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun Modifier.v3DashedBorder(color: Color, radius: Dp): Modifier = drawWithCache {
    val width = 1.5.dp.toPx()
    val inset = width / 2
    val stroke = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    onDrawWithContent {
        drawContent()
        drawRoundRect(color, Offset(inset, inset), Size(size.width - width, size.height - width),
            CornerRadius(radius.toPx() - inset), style = stroke)
    }
}
