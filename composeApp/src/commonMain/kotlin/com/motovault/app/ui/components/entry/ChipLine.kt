package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChipLine(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val bleedPx = 20.dp.roundToPx()
                val placeable = if (constraints.hasBoundedWidth) {
                    val widened = constraints.maxWidth + bleedPx * 2
                    measurable.measure(constraints.copy(minWidth = widened, maxWidth = widened))
                } else {
                    measurable.measure(constraints)
                }
                val reportedWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                layout(reportedWidth, placeable.height) {
                    placeable.placeRelative(if (constraints.hasBoundedWidth) -bleedPx else 0, 0)
                }
            }
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val width = size.width
                if (width > 0f) {
                    val leftStop = (16.dp.toPx() / width).coerceIn(0f, 0.4f)
                    val rightStop = (1f - 24.dp.toPx() / width).coerceIn(leftStop + 0.01f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            leftStop to Color.Black,
                            rightStop to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun ChipLineDivider() {
    Box(modifier = Modifier.width(1.dp).height(22.dp).background(BorderColor))
}

@Composable
fun EntryChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    hint: String? = null,
    warnDot: Boolean = false
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(if (active) BrandTintColor else SurfaceColor)
            .border(1.5.dp, if (active) BrandColor else BorderColor, shape)
            .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .semantics { selected = active }
            .padding(horizontal = 13.dp, vertical = 6.dp),
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
        if (warnDot) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(WarningColor)
            )
        }
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (hint != null) {
            Text(
                text = hint,
                color = if (active) TextSecondary else TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
fun <T> SegmentedPill(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val haptic = currentHaptic
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .selectableGroup()
            .then(if (label.isNotEmpty()) Modifier.semantics { contentDescription = label } else Modifier)
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, optLabel) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .heightIn(min = 30.dp)
                    .clip(shape)
                    .background(if (isSelected) BrandColor else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        if (!isSelected) {
                            haptic.perform(HapticKind.TICK)
                            onSelect(value)
                        }
                    }
                    .padding(horizontal = 11.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optLabel,
                    color = if (isSelected) OnBrandColor else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
