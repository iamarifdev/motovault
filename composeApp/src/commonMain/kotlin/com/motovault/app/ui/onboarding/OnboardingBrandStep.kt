package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.onboarding_brand_subtitle
import motovault.composeapp.generated.resources.onboarding_brand_title
import motovault.composeapp.generated.resources.onboarding_model_count
import motovault.composeapp.generated.resources.onboarding_privacy_note
import org.jetbrains.compose.resources.painterResource

private const val BRAND_STAGGER_MS = 30L

@Composable
internal fun OnboardingBrandStep(
    selectedBrandId: String?,
    onSelectBrand: (String) -> Unit
) {
    val orderedBrands = remember {
        ONBOARDING_BRAND_ORDER.mapNotNull { id -> worlds.firstOrNull { it.id == id } }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = appStringResource(Res.string.onboarding_brand_title),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = appStringResource(Res.string.onboarding_brand_subtitle),
            fontSize = 16.sp,
            color = TextSecondary
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            orderedBrands.chunked(3).forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { colIndex, brand ->
                        OnboardingBrandTile(
                            brand = brand,
                            index = rowIndex * 3 + colIndex,
                            selected = selectedBrandId == brand.id,
                            familyCount = BikeCatalogueFamilies.forBrand(brand.id).size,
                            onClick = { onSelectBrand(brand.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text(
            text = appStringResource(Res.string.onboarding_privacy_note),
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun OnboardingBrandTile(
    brand: BrandWorld,
    index: Int,
    selected: Boolean,
    familyCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(index * BRAND_STAGGER_MS)
            visible = true
        }
    }
    val enterOffsetPx = with(density) { 12.dp.toPx() }
    val enterAlpha by animateFloatAsState(if (visible) 1f else 0f, VaultMotion.listSpring(reducedMotion), label = "brandAlpha")
    val enterY by animateFloatAsState(if (visible) 0f else enterOffsetPx, VaultMotion.listSpring(reducedMotion), label = "brandY")

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (isPressed) 0.95f else 1f, VaultMotion.pressSpring(reducedMotion), label = "brandPress")
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .graphicsLayer { alpha = enterAlpha; translationY = enterY }
            .scale(pressScale)
            .clip(shape)
            .background(if (selected) BrandTintColor else SurfaceColor)
            .border(1.5.dp, if (selected) BrandColor else BorderColor, shape)
            .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(brand.logo()),
            contentDescription = brand.name,
            modifier = Modifier.width(52.dp).height(28.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(if (selected) BrandColor else TextPrimary)
        )
        Spacer(Modifier.height(2.dp))
        Text(text = brand.name, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
        Text(text = appStringResource(Res.string.onboarding_model_count, f.d(familyCount.toString())), fontSize = 12.sp, color = TextSecondary)
    }
}
