package com.motovault.app.ui.garage.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.theme.*

@Composable
fun PersistentQuickActionDock(
    accentColor: Color,
    onLubeChainClick: () -> Unit,
    onFuelUpClick: () -> Unit,
    onPoliceCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceElevatedDark.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            label = "Lube Chain",
            subtitle = "1-Tap 500km",
            accentColor = accentColor,
            onClick = onLubeChainClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Fuel Up",
            subtitle = "Log Liters",
            accentColor = Color(0xFF38BDF8),
            onClick = onFuelUpClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Police Check",
            subtitle = "Max Light",
            accentColor = StatusOverdueRed,
            isEmergency = true,
            onClick = onPoliceCheckClick,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEmergency: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)
    )

    val bgColor = if (isEmergency) StatusOverdueRed.copy(alpha = 0.15f) else SurfaceCardDark
    val borderColor = if (isEmergency) StatusOverdueRed.copy(alpha = 0.4f) else BorderSubtleDark

    Column(
        modifier = modifier
            .scale(animatedScale)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MotoVaultTypography.titleMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEmergency) StatusOverdueRed else TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MotoVaultTypography.bodyMedium.copy(fontSize = 10.sp, color = TextMuted)
        )
    }
}
