package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_quick_actions_desc
import motovault.composeapp.generated.resources.garage_quick_document
import motovault.composeapp.generated.resources.garage_quick_dot_desc
import motovault.composeapp.generated.resources.garage_quick_fuel
import motovault.composeapp.generated.resources.garage_quick_km
import motovault.composeapp.generated.resources.garage_quick_service

@Composable
internal fun GarageQuickActions(
    fuelDot: Boolean,
    docDot: Boolean,
    onAction: (String) -> Unit
) {
    val sectionDesc = appStringResource(Res.string.garage_quick_actions_desc)
    val dotDesc = appStringResource(Res.string.garage_quick_dot_desc)
    val tiles = listOf(
        Triple("fi_fuel.svg", appStringResource(Res.string.garage_quick_fuel), "fuel") to fuelDot,
        Triple("fi_tool.svg", appStringResource(Res.string.garage_quick_service), "service") to false,
        Triple("fi_gauge.svg", appStringResource(Res.string.garage_quick_km), "odometer") to false,
        Triple("fi_file-text.svg", appStringResource(Res.string.garage_quick_document), "docAdd") to docDot
    )

    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = sectionDesc },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tiles.forEach { (item, dot) ->
            val (icon, label, sheetName) = item
            QuickActionTile(
                icon = icon,
                label = label,
                dot = dot,
                dotDesc = dotDesc,
                onClick = { onAction(sheetName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: String,
    label: String,
    dot: Boolean,
    dotDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion),
        label = "quickTileScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor2)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(start = 4.dp, top = 12.dp, end = 4.dp, bottom = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(24.dp), tint = BrandColor)
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (dot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-5).dp, y = (-5).dp)
                    .size(11.dp)
                    .border(2.dp, SurfaceColor, CircleShape)
                    .clip(CircleShape)
                    .background(WarningColor)
                    .semantics { contentDescription = dotDesc }
            )
        }
    }
}
