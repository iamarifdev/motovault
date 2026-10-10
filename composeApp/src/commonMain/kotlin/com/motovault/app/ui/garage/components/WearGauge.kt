package com.motovault.app.ui.garage.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bosch TFT Inset Telemetry Dial
 * Features:
 * - Outer graduation tick ring (precision cockpit instrument feel)
 * - Inset track with specular depth
 * - Glowing progress sweep arc
 * - Centered monospace telemetry readout with status capsule badge
 */
@Composable
fun CircularWearGauge(
    title: String,
    specification: String,
    wearPercentage: Int,
    remainingKm: Double,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = wearPercentage.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 650)
    )

    val (statusColor, statusText) = when {
        wearPercentage >= 100 -> StatusOverdueRed to "OVERDUE"
        wearPercentage >= 75 -> StatusWarningYellow to "SERVICE SOON"
        else -> StatusHealthyGreen to "HEALTHY"
    }

    Column(
        modifier = modifier
            .background(SpecularCardBrush, RoundedCornerShape(24.dp))
            .border(1.dp, BorderSpecularDark, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(96.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.minDimension / 2f) - 2.dp.toPx()
                val trackRadius = outerRadius - 9.dp.toPx()

                // 1. Bosch TFT Outer Graduation Tick Ring (36 radial ticks)
                val numTicks = 36
                for (i in 0 until numTicks) {
                    val angleDeg = (i * (360f / numTicks)) - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val tickInner = outerRadius - 4.dp.toPx()
                    val startX = center.x + (tickInner * cos(angleRad)).toFloat()
                    val startY = center.y + (tickInner * sin(angleRad)).toFloat()
                    val endX = center.x + (outerRadius * cos(angleRad)).toFloat()
                    val endY = center.y + (outerRadius * sin(angleRad)).toFloat()

                    val isMajor = i % 9 == 0
                    val tickColor = if (isMajor) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                    val tickStroke = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickStroke
                    )
                }

                // 2. Inset Background Track
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = trackRadius,
                    center = center,
                    style = Stroke(width = 6.dp.toPx())
                )

                // 3. Glowing Progress Arc
                val sweepAngle = (animatedPercentage / 100f) * 360f
                if (sweepAngle > 0f) {
                    // Ambient glow shadow arc
                    drawArc(
                        color = statusColor.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Sharp foreground arc
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Centered Monospace Readout + Micro Status Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$wearPercentage%",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (wearPercentage >= 75) statusColor else TextPrimary
                    )
                )
                Text(
                    text = statusText,
                    style = MotoVaultTypography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = statusColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = MotoVaultTypography.titleMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${remainingKm.toInt()} km left",
            style = MotoVaultTypography.bodyMedium.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = statusColor
        )
    }
}

