@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.motovault.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.capture_camera_denied
import motovault.composeapp.generated.resources.capture_camera_unavailable
import motovault.composeapp.generated.resources.capture_photo_failed
import org.jetbrains.compose.resources.decodeToImageBitmap

private val ScanCardBg = Color(0xFF0D1210)
private val ScanViewportBg = Color(0xFF18201C)
private val ScanReticleColor = Color(0xBF7EE2A7)
private val ScanLineColor = Color(0xFF7EE2A7)

@Composable
internal fun OcrScanCard(
    photo: OdometerPhoto?,
    scanning: Boolean,
    previewDescription: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ScanCardBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OcrScanViewport(photo = photo, scanning = scanning, previewDescription = previewDescription)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun OcrScanViewport(
    photo: OdometerPhoto?,
    scanning: Boolean,
    previewDescription: String
) {
    val reducedMotion = isReducedMotion
    val bitmap = remember(photo) {
        photo?.let { runCatching { it.preview.decodeToImageBitmap() }.getOrNull() }
    }
    val scanFraction = if (!scanning || reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "ocrScanLine")
        val animated by transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.82f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ocrScanLineY"
        )
        animated
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ScanViewportBg)
    ) {
        bitmap?.let { decoded ->
            Image(
                bitmap = decoded,
                contentDescription = previewDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val insetX = size.width * 0.12f
            val insetY = size.height * 0.24f
            val corner = CornerRadius(10.dp.toPx())
            drawRoundRect(
                color = ScanReticleColor,
                topLeft = Offset(insetX, insetY),
                size = Size(size.width - insetX * 2f, size.height - insetY * 2f),
                cornerRadius = corner,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            )
            if (scanning) {
                val barHeight = 3.dp.toPx()
                val left = size.width * 0.08f
                val width = size.width * 0.84f
                val top = (size.height * scanFraction - barHeight / 2f)
                    .coerceIn(0f, (size.height - barHeight).coerceAtLeast(0f))
                drawRoundRect(
                    color = ScanLineColor,
                    topLeft = Offset(left, top),
                    size = Size(width, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

@Composable
internal fun ocrCameraErrorText(error: OdometerCameraIssue): String = appStringResource(
    when (error) {
        OdometerCameraIssue.DENIED -> Res.string.capture_camera_denied
        OdometerCameraIssue.UNAVAILABLE -> Res.string.capture_camera_unavailable
        OdometerCameraIssue.FAILED, OdometerCameraIssue.CANCELLED -> Res.string.capture_photo_failed
    }
)
