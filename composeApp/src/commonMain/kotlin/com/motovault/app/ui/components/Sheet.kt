package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sky.design.tokens.SkyTokens
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

enum class SheetDetent {
    CONTENT,
    TALL
}

@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detent: SheetDetent = SheetDetent.CONTENT,
    dismissEnabled: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = currentHaptic
    val density = LocalDensity.current
    val reducedMotion = isReducedMotion
    var closing by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(if (reducedMotion) 0f else 600f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) offsetY.snapTo(0f)
        else {
            offsetY.animateTo(0f, VaultMotion.sheetSpring())
        }
    }

    fun dismiss() {
        if (closing || !dismissEnabled) return
        closing = true
        scope.launch {
            if (!reducedMotion) {
                offsetY.animateTo(600f, tween(VaultMotion.SheetOutMs, easing = VaultMotion.SkyCurve))
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = sheetDialogProperties()
    ) {
        SheetWindowPolicy()
        PlatformAppearance(LocalVaultPalette.current.dark)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val maxH = maxHeight - with(density) { WindowInsets.safeDrawing.getTop(this).toDp() } - 40.dp
            val sheetHeight = if (detent == SheetDetent.TALL) maxH else maxH

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { dismiss() }
                    .excludeFromAccessibility()
            )

            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetHeight)
                    .then(if (detent == SheetDetent.TALL) Modifier.fillMaxHeight(0.92f) else Modifier)
                    .offset(y = offsetY.value.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .pointerInput(dismissEnabled) {
                        detectDragGestures(
                            onDragEnd = {
                                if (!dismissEnabled) return@detectDragGestures
                                if (offsetY.value > 120f) {
                                    haptic.perform(HapticKind.TICK)
                                    dismiss()
                                } else {
                                    scope.launch { offsetY.animateTo(0f, VaultMotion.pressSpring(reducedMotion)) }
                                }
                            },
                            onDrag = { _, dragAmount ->
                                if (dismissEnabled && (dragAmount.y > 0 || offsetY.value > 0)) {
                                    scope.launch { offsetY.snapTo((offsetY.value + dragAmount.y * 0.7f).coerceAtLeast(0f)) }
                                }
                            }
                        )
                    },
                color = SurfaceColor,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                ) {
                    // Grab Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(BorderStrongColor.copy(alpha = 0.6f))
                        )
                    }

                    // Header
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
                        if (dismissEnabled) {
                            VaultIconButton(
                                icon = "fi_x.svg",
                                label = appStringResource(Res.string.action_close),
                                onClick = { dismiss() }
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )

                    // Pinned Footer
                    if (footer != null) {
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            footer()
                        }
                    }
                }
            }
        }
    }
}
