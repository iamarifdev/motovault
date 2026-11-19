package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

private const val WASH_FADE_MS = 350

val LocalWashActive = staticCompositionLocalOf { false }

fun Modifier.screenBrandWash(
    enabled: Boolean,
    palette: VaultPalette,
    reducedMotion: Boolean
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else WASH_FADE_MS, easing = VaultMotion.SkyCurve),
        label = "screenBrandWash"
    )
    val mid = colorMix(palette.wash, palette.canvas, 0.45f)
    drawBehind {
        drawRect(color = palette.canvas)
        val rx = size.width * 1.40f
        val ry = size.height * 0.48f
        if (rx > 0f && ry > 0f && alpha > 0.001f) {
            withTransform({
                translate(left = size.width / 2f, top = 0f)
                scale(scaleX = 1f, scaleY = ry / rx, pivot = Offset.Zero)
            }) {
                drawRect(
                    brush = Brush.radialGradient(
                        0f to palette.wash,
                        0.55f to mid,
                        1f to palette.canvas,
                        center = Offset.Zero,
                        radius = rx
                    ),
                    topLeft = Offset(-size.width / 2f, 0f),
                    size = Size(size.width, size.height * (rx / ry)),
                    alpha = alpha
                )
            }
        }
    }
}

fun Modifier.sheetBrandWash(
    enabled: Boolean,
    palette: VaultPalette
): Modifier {
    val mid = colorMix(palette.sheetWash, palette.surface, 0.45f)
    return drawBehind {
        drawRect(color = palette.surface)
        if (enabled) {
            val rx = size.width * 1.50f
            val ry = 300.dp.toPx()
            if (rx > 0f && ry > 0f) {
                withTransform({
                    translate(left = size.width / 2f, top = 0f)
                    scale(scaleX = 1f, scaleY = ry / rx, pivot = Offset.Zero)
                }) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to palette.sheetWash,
                            0.50f to mid,
                            1f to palette.surface,
                            center = Offset.Zero,
                            radius = rx
                        ),
                        topLeft = Offset(-size.width / 2f, 0f),
                        size = Size(size.width, size.height * (rx / ry))
                    )
                }
            }
        }
    }
}
