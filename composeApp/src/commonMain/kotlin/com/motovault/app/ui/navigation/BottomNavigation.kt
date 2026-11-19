package com.motovault.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.nav_add_record
import motovault.composeapp.generated.resources.nav_garage
import motovault.composeapp.generated.resources.nav_profile
import motovault.composeapp.generated.resources.nav_records
import motovault.composeapp.generated.resources.nav_vault

private const val PLUS_LONG_PRESS_MS = 360L

@Composable
internal fun VaultBottomNavigation(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenShortcuts: () -> Unit = {}
) {
    val haptic = currentHaptic
    val brand = BrandColor
    val surface = SurfaceColor
    val line = BorderColor
    val reducedMotion = isReducedMotion

    Column(modifier = Modifier.fillMaxWidth().background(surface)) {
        HorizontalDivider(color = line, thickness = 0.5.dp)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            val slotWidth = maxWidth / 5
            val slot = when (selected) {
                Tab.GARAGE -> 0
                Tab.VAULT -> 1
                Tab.HISTORY -> 3
                Tab.ME -> 4
            }
            val markerX by animateDpAsState(
                targetValue = slotWidth * slot + (slotWidth - 28.dp) / 2,
                animationSpec = VaultMotion.markerSpring(reducedMotion),
                label = "tabMarker"
            )
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(Tab.GARAGE, selected == Tab.GARAGE, {
                    haptic.perform(HapticKind.TICK); onSelect(Tab.GARAGE)
                }, Modifier.weight(1f))
                BottomNavItem(Tab.VAULT, selected == Tab.VAULT, {
                    haptic.perform(HapticKind.TICK); onSelect(Tab.VAULT)
                }, Modifier.weight(1f))
                PlusCenterButton(
                    brand = brand,
                    onBrand = OnBrandColor,
                    surface = surface,
                    reducedMotion = reducedMotion,
                    haptic = haptic,
                    onQuickAdd = onQuickAdd,
                    onOpenShortcuts = onOpenShortcuts,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                BottomNavItem(Tab.HISTORY, selected == Tab.HISTORY, {
                    haptic.perform(HapticKind.TICK); onSelect(Tab.HISTORY)
                }, Modifier.weight(1f))
                BottomNavItem(Tab.ME, selected == Tab.ME, {
                    haptic.perform(HapticKind.TICK); onSelect(Tab.ME)
                }, Modifier.weight(1f))
            }
            Box(
                Modifier.align(Alignment.BottomStart).offset(x = markerX, y = (-5).dp)
                    .size(28.dp, 4.dp).background(brand, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
    }
}

@Composable
private fun PlusCenterButton(
    brand: Color,
    onBrand: Color,
    surface: Color,
    reducedMotion: Boolean,
    haptic: VaultHaptic,
    onQuickAdd: () -> Unit,
    onOpenShortcuts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addRecordDescription = appStringResource(Res.string.nav_add_record)
    val latestQuickAdd by rememberUpdatedState(onQuickAdd)
    val latestShortcuts by rememberUpdatedState(onOpenShortcuts)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion),
        label = "plusScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .offset(y = (-23).dp)
                .size(70.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(surface, CircleShape)
                .padding(5.dp)
                .shadow(8.dp, CircleShape, ambientColor = brand.copy(alpha = 0.45f), spotColor = brand.copy(alpha = 0.45f))
                .clip(CircleShape)
                .background(brand)
                .pointerInput(Unit) {
                    coroutineScope {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            var held = false
                            val holdJob = launch {
                                delay(PLUS_LONG_PRESS_MS)
                                held = true
                                haptic.perform(HapticKind.SUCCESS)
                                latestShortcuts()
                            }
                            val up = waitForUpOrCancellation()
                            holdJob.cancel()
                            pressed = false
                            if (up != null && !held) {
                                haptic.perform(HapticKind.SUCCESS)
                                latestQuickAdd()
                            }
                        }
                    }
                }
                .semantics {
                    role = Role.Button
                    contentDescription = addRecordDescription
                    onClick { haptic.perform(HapticKind.SUCCESS); latestQuickAdd(); true }
                    onLongClick { haptic.perform(HapticKind.SUCCESS); latestShortcuts(); true }
                },
            contentAlignment = Alignment.Center
        ) {
            V3Icon(
                path = "feather_icon/fi_plus.svg",
                description = null,
                modifier = Modifier.size(30.dp),
                tint = onBrand,
                strokeWidth = 2.4f
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) BrandColor else TextTertiary
    val title = appStringResource(when (tab) {
        Tab.GARAGE -> Res.string.nav_garage
        Tab.VAULT -> Res.string.nav_vault
        Tab.HISTORY -> Res.string.nav_records
        Tab.ME -> Res.string.nav_profile
    })
    Column(
        modifier = modifier
            .heightIn(min = 70.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .semantics { role = Role.Tab; this.selected = isSelected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        V3Icon(
            path = "feather_icon/${tab.icon}",
            description = title,
            modifier = Modifier.size(28.dp),
            tint = color,
            strokeWidth = if (isSelected) 2.4f else 2f
        )
        Spacer(Modifier.height(2.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, lineHeight = 20.sp)
    }
}
