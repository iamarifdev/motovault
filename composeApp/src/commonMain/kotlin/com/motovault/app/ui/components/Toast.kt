package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_undo
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TOAST_DURATION_MS = 5000
private const val TOAST_SWIPE_ELASTIC = 0.6f
private val TOAST_SWIPE_THRESHOLD = 80.dp

data class ToastData(
    val id: String = "",
    val message: String,
    val undoAction: (() -> Unit)? = null
)

@Composable
fun ToastLayer(
    toast: ToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val reducedMotion = isReducedMotion
    val enterY = with(density) { 24.dp.roundToPx() }
    val exitY = with(density) { 12.dp.roundToPx() }

    AnimatedContent(
        targetState = toast,
        contentKey = { it?.let { item -> item.id.ifBlank { item.message } } },
        transitionSpec = {
            if (targetState == null) {
                fadeIn(tween(0)) togetherWith
                    (fadeOut(VaultMotion.quickTween(reducedMotion)) +
                        slideOutVertically(VaultMotion.quickTween(reducedMotion)) { exitY })
            } else {
                (fadeIn(VaultMotion.sheetSpring(reducedMotion)) +
                    slideInVertically(VaultMotion.sheetSpring(reducedMotion)) { enterY } +
                    scaleIn(VaultMotion.sheetSpring(reducedMotion), initialScale = 0.96f)) togetherWith
                    (fadeOut(VaultMotion.quickTween(reducedMotion)) +
                        slideOutVertically(VaultMotion.quickTween(reducedMotion)) { exitY })
            }
        },
        modifier = modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
        label = "ToastLayer"
    ) { activeToast ->
        if (activeToast != null) {
            ToastCard(
                toast = activeToast,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun ToastLayer(
    message: String?,
    onUndo: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ToastLayer(
        toast = message?.let { ToastData(id = it, message = it, undoAction = onUndo) },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun ToastCard(
    toast: ToastData,
    onDismiss: () -> Unit
) {
    val haptic = currentHaptic
    val density = LocalDensity.current
    val reducedMotion = isReducedMotion
    val dark = LocalVaultPalette.current.dark
    val scope = rememberCoroutineScope()
    val toastBg = if (dark) Color(0xFFE9ECEA) else Color(0xFF181C1A)
    val toastText = if (dark) Color(0xFF181818) else Color(0xFFF1F2F3)
    val undoColor = if (dark) Color(0xFF1E47A8) else Color(0xFF8FB6FF)
    val swipeThresholdPx = with(density) { TOAST_SWIPE_THRESHOLD.toPx() }
    val toastKey = toast.id.ifBlank { toast.message }
    val progress = remember(toastKey) { Animatable(1f) }
    val dragX = remember(toastKey) { Animatable(0f) }
    var dismissed by remember(toastKey) { mutableStateOf(false) }

    LaunchedEffect(toastKey) {
        progress.snapTo(1f)
        progress.animateTo(0f, tween(TOAST_DURATION_MS, easing = LinearEasing))
        if (!dismissed) {
            dismissed = true
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .pointerInput(toastKey) {
                var fingerX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { fingerX = 0f },
                    onDragEnd = {
                        if (abs(fingerX) > swipeThresholdPx && !dismissed) {
                            dismissed = true
                            onDismiss()
                        } else {
                            scope.launch { dragX.animateTo(0f, VaultMotion.sheetSpring(reducedMotion)) }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dragX.animateTo(0f, VaultMotion.sheetSpring(reducedMotion)) }
                    },
                    onHorizontalDrag = { _, delta ->
                        fingerX += delta
                        scope.launch { dragX.snapTo(fingerX * TOAST_SWIPE_ELASTIC) }
                    }
                )
            }
            .offset { IntOffset(dragX.value.roundToInt(), 0) }
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(toastBg)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = toast.message,
                color = toastText,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            val undo = toast.undoAction
            if (undo != null) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            haptic.perform(HapticKind.WARNING)
                            dismissed = true
                            undo()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    V3Icon(
                        path = "feather_icon/fi_undo-2.svg",
                        description = null,
                        modifier = Modifier.size(16.dp),
                        tint = undoColor
                    )
                    Text(
                        text = appStringResource(Res.string.action_undo),
                        color = undoColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(toastText.copy(alpha = 0.4f))
            )
        }
    }
}
