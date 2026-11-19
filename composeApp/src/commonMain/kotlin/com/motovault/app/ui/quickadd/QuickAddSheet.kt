package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.quickadd_document
import motovault.composeapp.generated.resources.quickadd_document_desc
import motovault.composeapp.generated.resources.quickadd_fuel
import motovault.composeapp.generated.resources.quickadd_fuel_desc
import motovault.composeapp.generated.resources.quickadd_odometer
import motovault.composeapp.generated.resources.quickadd_odometer_desc
import motovault.composeapp.generated.resources.quickadd_paste_desc
import motovault.composeapp.generated.resources.quickadd_paste_title
import motovault.composeapp.generated.resources.quickadd_service
import motovault.composeapp.generated.resources.quickadd_service_desc
import motovault.composeapp.generated.resources.quickadd_title

private data class QuickAddOption(
    val icon: String,
    val label: String,
    val subtitle: String,
    val flow: String
)

@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSelectFlow: (String) -> Unit
) {
    val haptic = currentHaptic
    val options = listOf(
        QuickAddOption("fi_fuel.svg", appStringResource(Res.string.quickadd_fuel), appStringResource(Res.string.quickadd_fuel_desc), "fuel"),
        QuickAddOption("fi_tool.svg", appStringResource(Res.string.quickadd_service), appStringResource(Res.string.quickadd_service_desc), "service"),
        QuickAddOption("fi_gauge.svg", appStringResource(Res.string.quickadd_odometer), appStringResource(Res.string.quickadd_odometer_desc), "odometer"),
        QuickAddOption("fi_file-text.svg", appStringResource(Res.string.quickadd_document), appStringResource(Res.string.quickadd_document_desc), "docAdd")
    )

    Sheet(title = appStringResource(Res.string.quickadd_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { colIndex, option ->
                        QuickAddCard(
                            option = option,
                            index = rowIndex * 2 + colIndex,
                            onClick = {
                                haptic.perform(HapticKind.TICK)
                                onSelectFlow(option.flow)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .v3DashedBorder(BrandLineColor, 16.dp)
                    .clickable(role = Role.Button) {
                        haptic.perform(HapticKind.TICK)
                        onSelectFlow("paste")
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                V3Icon(
                    path = "feather_icon/fi_mic.svg",
                    description = null,
                    modifier = Modifier.size(20.dp),
                    tint = BrandColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appStringResource(Res.string.quickadd_paste_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = appStringResource(Res.string.quickadd_paste_desc),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    option: QuickAddOption,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val startOffsetPx = with(density) { 14.dp.toPx() }
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val offsetYPx = remember { Animatable(if (reducedMotion) 0f else startOffsetPx) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            alpha.snapTo(1f)
            offsetYPx.snapTo(0f)
        } else {
            delay(80L + index * 40L)
            launch { alpha.animateTo(1f, VaultMotion.listSpring(false)) }
            launch { offsetYPx.animateTo(0f, VaultMotion.listSpring(false)) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 140.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetYPx.value
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor2)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandColor),
            contentAlignment = Alignment.Center
        ) {
            V3Icon(
                path = "feather_icon/${option.icon}",
                description = null,
                modifier = Modifier.size(28.dp),
                tint = OnBrandColor
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(text = option.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(text = option.subtitle, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}
