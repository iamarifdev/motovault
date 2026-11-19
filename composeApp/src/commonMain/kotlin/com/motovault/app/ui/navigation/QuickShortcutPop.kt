package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.shortcut_fuel
import motovault.composeapp.generated.resources.shortcut_menu_desc
import motovault.composeapp.generated.resources.shortcut_odometer
import motovault.composeapp.generated.resources.shortcut_service

private data class ShortcutOption(val icon: String, val flow: String)

private val SHORTCUT_OPTIONS = listOf(
    ShortcutOption("fi_fuel.svg", "fuel"),
    ShortcutOption("fi_tool.svg", "service"),
    ShortcutOption("fi_gauge.svg", "odometer")
)

@Composable
internal fun QuickShortcutPop(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val haptic = currentHaptic
    val enterY = with(density) { 16.dp.roundToPx() }
    val exitY = with(density) { 12.dp.roundToPx() }

    PlatformBackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reducedMotion) 0 else VaultMotion.QuickMs)),
        exit = fadeOut(tween(if (reducedMotion) 0 else 140))
    ) {
        val menuLabel = appStringResource(Res.string.shortcut_menu_desc)
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .excludeFromAccessibility()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(start = 12.dp, end = 12.dp, bottom = 88.dp)
                    .animateEnterExit(
                        enter = fadeIn(VaultMotion.pressSpring(reducedMotion)) +
                            slideInVertically(VaultMotion.pressSpring(reducedMotion)) { enterY } +
                            scaleIn(VaultMotion.pressSpring(reducedMotion), initialScale = 0.9f),
                        exit = fadeOut(tween(if (reducedMotion) 0 else 140)) +
                            slideOutVertically(tween(if (reducedMotion) 0 else 140)) { exitY } +
                            scaleOut(tween(if (reducedMotion) 0 else 140), targetScale = 0.92f)
                    )
                    .shadow(8.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderStrongColor, RoundedCornerShape(999.dp))
                    .semantics { contentDescription = menuLabel }
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SHORTCUT_OPTIONS.forEach { option ->
                        val label = appStringResource(
                            when (option.flow) {
                                "fuel" -> Res.string.shortcut_fuel
                                "service" -> Res.string.shortcut_service
                                else -> Res.string.shortcut_odometer
                            }
                        )
                        ShortcutPillButton(
                            icon = option.icon,
                            label = label,
                            onClick = {
                                haptic.perform(HapticKind.TICK)
                                onDismiss()
                                onAction(option.flow)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutPillButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(BrandTintColor)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        V3Icon(
            path = "feather_icon/$icon",
            description = null,
            modifier = Modifier.size(18.dp),
            tint = BrandColor
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor,
            maxLines = 1
        )
    }
}
