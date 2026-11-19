package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val SHEET_ELASTIC_DOWN = 0.7f
private val SHEET_DISMISS_OFFSET = 110.dp
private val SHEET_DISMISS_VELOCITY = 650.dp
private val SHEET_BODY_DRAG_SLOP = 10.dp

@Stable
internal class SheetDragState(
    val offsetYPx: Animatable<Float, AnimationVector1D>,
    density: Density
) {
    val dismissOffsetPx: Float = with(density) { SHEET_DISMISS_OFFSET.toPx() }
    val dismissVelocityPx: Float = with(density) { SHEET_DISMISS_VELOCITY.toPx() }
    val bodyDragSlopPx: Float = with(density) { SHEET_BODY_DRAG_SLOP.toPx() }

    /** Finger travel since the drag started, in screen pixels (v4 compares the finger, not the sheet). */
    var fingerTravelPx: Float = 0f
        private set
    private var baseOffsetPx = 0f
    private var appliedOffsetPx = 0f
    private var lastStepPx = 0f

    fun begin() {
        baseOffsetPx = offsetYPx.value
        appliedOffsetPx = baseOffsetPx
        fingerTravelPx = 0f
        lastStepPx = 0f
    }

    /**
     * Pointer positions are read inside the sheet, which itself moves. The sheet's own last step is added back
     * so the sheet follows the finger at the v4 elastic factor. Returns the new sheet offset.
     */
    fun follow(localDeltaPx: Float): Float {
        fingerTravelPx += localDeltaPx + lastStepPx
        val target = (baseOffsetPx + fingerTravelPx * SHEET_ELASTIC_DOWN).coerceAtLeast(0f)
        lastStepPx = target - appliedOffsetPx
        appliedOffsetPx = target
        return target
    }

    /** Converts a velocity measured inside the moving sheet into finger velocity. */
    fun fingerVelocity(localVelocityY: Float): Float =
        if (localVelocityY > 0f) localVelocityY / (1f - SHEET_ELASTIC_DOWN) else localVelocityY

    fun shouldDismiss(fingerVelocityY: Float): Boolean =
        fingerTravelPx > dismissOffsetPx || fingerVelocityY > dismissVelocityPx
}

@Composable
internal fun rememberSheetDragState(initialOffsetPx: Float): SheetDragState {
    val density = LocalDensity.current
    val animatable = remember { Animatable(initialOffsetPx) }
    return remember(density, animatable) { SheetDragState(animatable, density) }
}

internal fun Modifier.sheetHeaderDrag(
    enabled: Boolean,
    dragState: SheetDragState,
    reducedMotion: Boolean,
    haptic: VaultHaptic,
    scope: CoroutineScope,
    onDismiss: () -> Unit
): Modifier = pointerInput(enabled, dragState, reducedMotion) {
    if (!enabled) return@pointerInput
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = { tracker.resetTracking(); dragState.begin() },
        onDragCancel = {
            scope.launch { dragState.offsetYPx.animateTo(0f, VaultMotion.sheetSpring(reducedMotion)) }
        },
        onDragEnd = {
            val velocityY = tracker.calculateVelocity().y
            if (dragState.shouldDismiss(velocityY)) {
                haptic.perform(HapticKind.TICK)
                onDismiss()
            } else {
                scope.launch { dragState.offsetYPx.animateTo(0f, VaultMotion.sheetSpring(reducedMotion)) }
            }
        },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            val next = dragState.follow(dragAmount)
            tracker.addPosition(change.uptimeMillis, Offset(0f, dragState.fingerTravelPx))
            scope.launch { dragState.offsetYPx.snapTo(next) }
        }
    )
}

@Composable
internal fun rememberSheetBodyDragConnection(
    enabled: Boolean,
    bodyScroll: ScrollState,
    dragState: SheetDragState,
    reducedMotion: Boolean,
    haptic: VaultHaptic,
    onDismiss: () -> Unit
): NestedScrollConnection {
    val scope = rememberCoroutineScope()
    var draggingBody by remember { mutableStateOf(false) }
    var pendingSlopPx by remember { mutableFloatStateOf(0f) }

    fun finishDrag(velocityY: Float): Boolean {
        val active = draggingBody || dragState.offsetYPx.value > 0f
        draggingBody = false
        pendingSlopPx = 0f
        if (!active) return false
        if (dragState.shouldDismiss(dragState.fingerVelocity(velocityY))) {
            haptic.perform(HapticKind.TICK)
            onDismiss()
        } else {
            scope.launch { dragState.offsetYPx.animateTo(0f, VaultMotion.sheetSpring(reducedMotion)) }
        }
        return true
    }

    return remember(enabled, bodyScroll, dragState, reducedMotion) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || !draggingBody) return Offset.Zero
                val current = dragState.offsetYPx.value
                val delta = available.y
                val next = dragState.follow(delta)
                scope.launch { dragState.offsetYPx.snapTo(next) }
                if (next <= 0f && delta < 0f) {
                    draggingBody = false
                    pendingSlopPx = 0f
                    val consumed = -current / SHEET_ELASTIC_DOWN
                    return Offset(0f, consumed)
                }
                return Offset(0f, delta)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || source != NestedScrollSource.UserInput || bodyScroll.value > 0) {
                    pendingSlopPx = 0f
                    return Offset.Zero
                }
                if (available.y <= 0f) {
                    pendingSlopPx = 0f
                    return Offset.Zero
                }
                pendingSlopPx += available.y
                if (!draggingBody && pendingSlopPx > dragState.bodyDragSlopPx) {
                    draggingBody = true
                    dragState.begin()
                    val next = dragState.follow(pendingSlopPx - dragState.bodyDragSlopPx)
                    scope.launch { dragState.offsetYPx.snapTo(next) }
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity =
                if (finishDrag(available.y)) available else Velocity.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (finishDrag(available.y)) available else Velocity.Zero
        }
    }
}
