package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.onboarding_cc_value
import motovault.composeapp.generated.resources.onboarding_choose_version
import motovault.composeapp.generated.resources.onboarding_versions_pill
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ModelFamilyRow(
    family: BikeFamily,
    selected: Boolean,
    activeVariant: BikeVariant,
    onSelectFamily: () -> Unit,
    onSelectVariant: (BikeVariant) -> Unit
) {
    val reducedMotion = isReducedMotion
    val shape = RoundedCornerShape(16.dp)
    val duration = if (reducedMotion) 0 else VaultMotion.ExpandMs

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.5.dp, if (selected) BrandColor else BorderColor, shape)
    ) {
        ModelFamilySummary(
            family = family,
            selected = selected,
            activeVariant = activeVariant,
            onClick = onSelectFamily
        )
        AnimatedVisibility(
            visible = selected && family.variants.size > 1,
            enter = expandVertically(tween(duration, easing = VaultMotion.ExpandCurve)) + fadeIn(tween(duration)),
            exit = shrinkVertically(tween(if (reducedMotion) 0 else 140)) + fadeOut(tween(if (reducedMotion) 0 else 140))
        ) {
            ModelVariantDrawer(
                variants = family.variants,
                activeVariant = activeVariant,
                onSelectVariant = onSelectVariant
            )
        }
    }
}

@Composable
private fun ModelFamilySummary(
    family: BikeFamily,
    selected: Boolean,
    activeVariant: BikeVariant,
    onClick: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val palette = LocalVaultPalette.current
    val previewPhoto = if (selected) activeVariant.photo else (family.leadModel?.photo ?: activeVariant.photo)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) BrandTintColor else Color.Transparent)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.radialGradient(listOf(palette.photoWellStart, palette.photoWellEnd))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(previewPhoto),
                contentDescription = null,
                modifier = Modifier.size(width = 96.dp, height = 62.dp),
                contentScale = ContentScale.Fit
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = family.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = appStringResource(Res.string.onboarding_cc_value, f.d(activeVariant.cc.toString())), fontSize = 12.sp, color = TextSecondary)
                if (family.variants.size > 1) {
                    Text(
                        text = appStringResource(Res.string.onboarding_versions_pill, f.d(family.variants.size.toString())),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (selected) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(BrandColor),
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/fi_check.svg", description = null, modifier = Modifier.size(18.dp), tint = OnBrandColor)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelVariantDrawer(
    variants: List<BikeVariant>,
    activeVariant: BikeVariant,
    onSelectVariant: (BikeVariant) -> Unit
) {
    val palette = LocalVaultPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.radialGradient(listOf(BrandTintColor, palette.photoWellEnd)))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(activeVariant.photo),
                    contentDescription = activeVariant.name,
                    modifier = Modifier.heightIn(max = 108.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(text = appStringResource(Res.string.onboarding_choose_version), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                variants.forEach { variant ->
                    VaultChip(
                        text = variant.label,
                        active = variant.modelId == activeVariant.modelId,
                        onClick = { onSelectVariant(variant) }
                    )
                }
            }
        }
    }
}
