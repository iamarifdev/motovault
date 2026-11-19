package com.motovault.app.ui.garage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.BrandTintColor
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.ButtonSize
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.V3Icon
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.VaultMotion
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentHaptic
import com.motovault.app.ui.isReducedMotion
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.breakdown_open_action
import motovault.composeapp.generated.resources.seasonal_pill_desc
import motovault.composeapp.generated.resources.seasonal_pill_hint_closed
import motovault.composeapp.generated.resources.seasonal_pill_hint_open
import motovault.composeapp.generated.resources.seasonal_pill_tip
import motovault.composeapp.generated.resources.seasonal_pill_title

@Composable
internal fun SeasonalCareSection(onOpenBreakdown: () -> Unit) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else VaultMotion.ExpandMs, easing = VaultMotion.ExpandCurve),
        label = "seasonalChevron"
    )
    val sectionDesc = appStringResource(Res.string.seasonal_pill_desc)
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = sectionDesc },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    haptic.perform(HapticKind.TICK)
                    expanded = !expanded
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(BrandTintColor),
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/fi_cloud-rain.svg", description = null, modifier = Modifier.size(18.dp), tint = BrandColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appStringResource(Res.string.seasonal_pill_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = appStringResource(if (expanded) Res.string.seasonal_pill_hint_open else Res.string.seasonal_pill_hint_closed),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            V3Icon(
                path = "feather_icon/fi_chevron-down.svg",
                description = null,
                modifier = Modifier.size(18.dp).rotate(chevronAngle),
                tint = TextSecondary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(if (reducedMotion) 0 else VaultMotion.ExpandMs)) + fadeIn(tween(if (reducedMotion) 0 else VaultMotion.ExpandMs)),
            exit = shrinkVertically(tween(if (reducedMotion) 0 else VaultMotion.ExpandMs)) + fadeOut(tween(if (reducedMotion) 0 else 150))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = appStringResource(Res.string.seasonal_pill_tip),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary
                )
                VaultButton(
                    text = appStringResource(Res.string.breakdown_open_action),
                    size = ButtonSize.SM,
                    kind = ButtonKind.SECONDARY,
                    onClick = onOpenBreakdown
                )
            }
        }
    }
}
