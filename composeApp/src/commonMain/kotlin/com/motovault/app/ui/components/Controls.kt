package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.LocalDate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.keypad_backspace
import motovault.composeapp.generated.resources.keypad_decimal
import motovault.composeapp.generated.resources.action_undo

private val VaultPosition = SemanticsPropertyKey<Rect>("VaultPosition")

/** Keep the native accessibility bridge informed when a button moves inside a scroll container. */

private fun Modifier.accessiblePosition(): Modifier = composed {
    var position by remember { mutableStateOf(Rect.Zero) }
    onGloballyPositioned { coordinates ->
        val next = coordinates.boundsInWindow()
        if (next != position) position = next
    }.semantics { this[VaultPosition] = position }
}

enum class ButtonKind {
    PRIMARY,
    SECONDARY,
    GHOST,
    DANGER
}

enum class ButtonSize {
    MD,
    SM
}

enum class IconTone {
    PLAIN,
    TINT
}

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    size: ButtonSize = ButtonSize.MD,
    icon: String? = null,
    full: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val minHeight = if (size == ButtonSize.SM) 40.dp else 52.dp
    val hPad = if (size == ButtonSize.SM) 14.dp else 20.dp
    val corner = if (size == ButtonSize.SM) 10.dp else 12.dp

    val bgColor = when {
        !enabled -> SurfaceColor2
        kind == ButtonKind.PRIMARY -> BrandColor
        kind == ButtonKind.SECONDARY -> SurfaceColor
        kind == ButtonKind.GHOST -> Color.Transparent
        kind == ButtonKind.DANGER -> Color.Transparent
        else -> SurfaceColor2
    }

    val contentColor = when {
        !enabled -> TextTertiary
        kind == ButtonKind.PRIMARY -> OnBrandColor
        kind == ButtonKind.SECONDARY -> TextPrimary
        kind == ButtonKind.GHOST -> BrandColor
        kind == ButtonKind.DANGER -> ErrorColor
        else -> TextPrimary
    }

    val borderModifier = when {
        !enabled -> Modifier
        kind == ButtonKind.SECONDARY -> Modifier.border(1.5.dp, BorderStrongColor, RoundedCornerShape(corner))
        kind == ButtonKind.DANGER -> Modifier.border(1.5.dp, ErrorColor, RoundedCornerShape(corner))
        else -> Modifier
    }

    val widthModifier = if (full) Modifier.fillMaxWidth() else Modifier

    Box(
        modifier = modifier
            .then(widthModifier)
            .accessiblePosition()
            .scale(scale)
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .then(borderModifier)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                V3Icon(
                    path = "feather_icon/$icon",
                    description = null,
                    modifier = Modifier.size(if (size == ButtonSize.SM) 16.dp else 20.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = if (size == ButtonSize.SM) 14.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VaultIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    tone: IconTone = IconTone.PLAIN
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val bgColor = if (tone == IconTone.TINT) BrandTintColor else Color.Transparent
    val iconColor = if (tone == IconTone.TINT) BrandColor else TextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .semantics {
                role = Role.Button
                if (label.isNotEmpty()) contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        V3Icon(
            path = "feather_icon/$icon",
            description = null,
            modifier = Modifier.size(22.dp),
            tint = iconColor
        )
    }
}

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
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) SurfaceColor else Color.Transparent)
                    .then(if (isSelected) Modifier.border(0.5.dp, BorderColor, RoundedCornerShape(10.dp)) else Modifier)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) {
                        if (!isSelected) {
                            haptic.perform(HapticKind.TICK)
                            onSelect(value)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optLabel,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VaultSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch) {
                haptic.perform(HapticKind.TICK)
                onCheckedChange(it)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) BrandColor else BorderStrongColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
fun CountUp(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified
) {
    val reducedMotion = isReducedMotion
    val anim = remember { Animatable(if (reducedMotion) value.toFloat() else 0f) }

    LaunchedEffect(value, reducedMotion) {
        if (reducedMotion) {
            anim.snapTo(value.toFloat())
        } else {
            anim.animateTo(value.toFloat(), tween(800, easing = VaultMotion.SkyCurve))
        }
    }

    Text(
        text = format(anim.value.toDouble()),
        style = style,
        color = color,
        modifier = modifier
    )
}
