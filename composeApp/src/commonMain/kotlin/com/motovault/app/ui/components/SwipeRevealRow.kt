package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_delete_short
import kotlin.math.roundToInt

private const val SWIPE_MAX_DP = 88f
private const val SWIPE_SNAP_DP = 60f

/** Reveals an explicit red delete button when dragged left while collapsed. */
@Composable
internal fun SwipeRevealRow(
    open: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalVaultPalette.current
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxRevealPx = with(density) { SWIPE_MAX_DP.dp.toPx() }
    val snapThresholdPx = with(density) { SWIPE_SNAP_DP.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var crossedWarning by remember { mutableStateOf(false) }

    LaunchedEffect(open) {
        if (open && offsetX.value != 0f) {
            offsetX.snapTo(0f)
            crossedWarning = false
        }
    }

    val revealAlpha = ((-offsetX.value) / maxRevealPx).coerceIn(0f, 1f)
    val dragState = rememberDraggableState { delta ->
        val next = (offsetX.value + delta).coerceIn(-maxRevealPx, 0f)
        if (!crossedWarning && next <= -snapThresholdPx) {
            crossedWarning = true
            haptic.perform(HapticKind.WARNING)
        } else if (next > -snapThresholdPx) {
            crossedWarning = false
        }
        scope.launch { offsetX.snapTo(next) }
    }

    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        if (!open && revealAlpha > 0.01f) {
            SwipeDeleteBehindButton(
                revealAlpha = revealAlpha,
                dark = palette.dark,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd).matchParentSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = !open,
                    onDragStopped = {
                        val target = if (offsetX.value <= -snapThresholdPx) -maxRevealPx else 0f
                        scope.launch { offsetX.animateTo(target, VaultMotion.listSpring(reducedMotion)) }
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeDeleteBehindButton(
    revealAlpha: Float,
    dark: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = appStringResource(Res.string.action_delete_short)
    val iconColor = if (dark) Color(0xFF101215) else Color.White
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(96.dp)
                .fillMaxHeight()
                .alpha(revealAlpha)
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(ErrorColor)
                .clickable(enabled = revealAlpha > 0.2f, role = Role.Button, onClick = onDelete)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/fi_trash-2.svg", description = null, modifier = Modifier.size(22.dp), tint = iconColor)
        }
    }
}
