package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VaultChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    hint: String? = null,
    icon: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val bgColor = if (active) BrandTintColor else SurfaceColor
    val borderColor = if (active) BrandColor else BorderColor
    val textColor = TextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .semantics { selected = active }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                V3Icon(
                    path = "feather_icon/$icon",
                    description = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (active) BrandColor else TextSecondary
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (hint != null) {
                Text(
                    text = hint,
                    color = if (active) TextSecondary else TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun <T> VaultSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true
) {
    val haptic = currentHaptic
    val palette = LocalVaultPalette.current
    Row(
        modifier = modifier
            .selectableGroup()
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, optLabel) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) palette.segPill else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) {
                        if (!isSelected) {
                            haptic.perform(HapticKind.TICK)
                            onSelect(value)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optLabel,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
