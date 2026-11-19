package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_close
import kotlin.math.roundToInt

enum class SheetDetent {
    CONTENT,
    TALL
}

val LocalSheetCompact = staticCompositionLocalOf { false }

private val SheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detent: SheetDetent = SheetDetent.CONTENT,
    compact: Boolean = false,
    dismissEnabled: Boolean = true,
    closeLabel: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVaultPalette.current
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val washActive = LocalWashActive.current
    val sheetToast = LocalSheetToast.current
    var closing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!closing && dismissEnabled) closing = true }, properties = sheetDialogProperties()) {
        SheetWindowPolicy()
        PlatformAppearance(palette.dark)
        SheetDialogContent(
            title = title,
            modifier = modifier,
            detent = detent,
            compact = compact,
            dismissEnabled = dismissEnabled,
            closeLabel = closeLabel ?: appStringResource(Res.string.action_close),
            closing = closing,
            onRequestClose = { if (!closing && dismissEnabled) closing = true },
            onDismiss = onDismiss,
            palette = palette,
            washActive = washActive,
            reducedMotion = reducedMotion,
            haptic = haptic,
            sheetToast = sheetToast,
            actions = actions,
            footer = footer,
            content = content
        )
    }
}

@Composable
private fun SheetDialogContent(
    title: String,
    modifier: Modifier,
    detent: SheetDetent,
    compact: Boolean,
    dismissEnabled: Boolean,
    closeLabel: String,
    closing: Boolean,
    onRequestClose: () -> Unit,
    onDismiss: () -> Unit,
    palette: VaultPalette,
    washActive: Boolean,
    reducedMotion: Boolean,
    haptic: VaultHaptic,
    sheetToast: SheetToastBinding,
    actions: (@Composable RowScope.() -> Unit)?,
    footer: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
        val safeTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
        val maxH = (maxHeight - safeTop - 60.dp).coerceAtLeast(160.dp)
        val exitDistancePx = with(density) { maxH.toPx() }
        val scrimAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
        val dragState = rememberSheetDragState(if (reducedMotion) 0f else exitDistancePx)

        LaunchedEffect(reducedMotion) {
            if (reducedMotion) {
                scrimAlpha.snapTo(1f)
                dragState.offsetYPx.snapTo(0f)
            } else {
                launch { scrimAlpha.animateTo(1f, VaultMotion.sheetInTween(false)) }
                launch { dragState.offsetYPx.animateTo(0f, VaultMotion.sheetSpring(false)) }
            }
        }
        LaunchedEffect(closing) {
            if (!closing) return@LaunchedEffect
            if (!reducedMotion) {
                launch { scrimAlpha.animateTo(0f, VaultMotion.sheetOutTween(false)) }
                dragState.offsetYPx.animateTo(exitDistancePx, tween(VaultMotion.SheetOutMs, easing = VaultMotion.SkyCurve))
            }
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = palette.scrim.alpha * scrimAlpha.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRequestClose)
                .excludeFromAccessibility()
        )
        SheetBodySurface(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = maxH)
                .then(if (detent == SheetDetent.TALL) Modifier.fillMaxHeight() else Modifier)
                .offset { IntOffset(0, dragState.offsetYPx.value.roundToInt()) },
            title = title,
            dismissEnabled = dismissEnabled,
            closeLabel = closeLabel,
            compact = compact,
            palette = palette,
            washActive = washActive,
            dragState = dragState,
            reducedMotion = reducedMotion,
            haptic = haptic,
            actions = actions,
            footer = footer,
            onClose = onRequestClose,
            content = content
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
        ) {
            ToastLayer(toast = sheetToast.toast, onDismiss = sheetToast.onDismiss)
        }
    }
}

@Composable
private fun SheetBodySurface(
    modifier: Modifier,
    title: String,
    dismissEnabled: Boolean,
    closeLabel: String,
    compact: Boolean,
    palette: VaultPalette,
    washActive: Boolean,
    dragState: SheetDragState,
    reducedMotion: Boolean,
    haptic: VaultHaptic,
    actions: (@Composable RowScope.() -> Unit)?,
    footer: (@Composable () -> Unit)?,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val bodyScroll = rememberScrollState()
    val bodyDragConnection = rememberSheetBodyDragConnection(dismissEnabled, bodyScroll, dragState, reducedMotion, haptic, onClose)
    Surface(modifier = modifier, color = SurfaceColor, shape = SheetTopShape, shadowElevation = 24.dp) {
        val safeBottom = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sheetBrandWash(washActive, palette)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            SheetHeader(
                title = title,
                dismissEnabled = dismissEnabled,
                closeLabel = closeLabel,
                actions = actions,
                onClose = onClose,
                modifier = Modifier.sheetHeaderDrag(dismissEnabled, dragState, reducedMotion, haptic, scope, onClose)
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(bodyDragConnection)
                    .verticalScroll(bodyScroll)
                    .padding(
                        start = 20.dp,
                        top = if (compact) 12.dp else 16.dp,
                        end = 20.dp,
                        bottom = (if (compact) 12.dp else 20.dp) + if (footer == null) safeBottom else 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                CompositionLocalProvider(LocalSheetCompact provides compact) { content() }
            }
            if (footer != null) {
                SheetFooter(compact = compact, footer = footer)
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    dismissEnabled: Boolean,
    closeLabel: String,
    actions: (@Composable RowScope.() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderStrongColor.copy(alpha = 0.6f))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                lineHeight = 32.4.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                actions?.invoke(this)
                if (dismissEnabled) {
                    VaultIconButton(icon = "fi_x.svg", label = closeLabel, onClick = onClose)
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
private fun SheetFooter(
    compact: Boolean,
    footer: @Composable () -> Unit
) {
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(
                start = 16.dp,
                top = if (compact) 8.dp else 10.dp,
                end = 16.dp,
                bottom = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        CompositionLocalProvider(LocalSheetCompact provides compact) {
            footer()
        }
    }
}
