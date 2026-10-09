package com.motovault.app.ui.garage.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.WearHealthStatus
import com.motovault.app.theme.*

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
            .background(SurfaceCardDark, RoundedCornerShape(18.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(88.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                // Background Track
                drawArc(
                    color = BorderSubtleDark,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Animated Progress Arc
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = (animatedPercentage / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$wearPercentage%",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = MotoVaultTypography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            color = TextPrimary
        )

        Text(
            text = "${remainingKm.toInt()} km left",
            style = MotoVaultTypography.bodyMedium.copy(fontSize = 11.sp),
            color = statusColor
        )
    }
}
