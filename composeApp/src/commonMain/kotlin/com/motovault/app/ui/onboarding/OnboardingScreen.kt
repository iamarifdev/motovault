package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.onboarding_back
import motovault.composeapp.generated.resources.onboarding_cancel
import motovault.composeapp.generated.resources.onboarding_step
import motovault.composeapp.generated.resources.onboarding_step_progress

@Composable
fun OnboardingScreen(
    onDismiss: (() -> Unit)? = null,
    onComplete: (OnboardingResult) -> Unit,
    onPreviewBrand: (String?) -> Unit = {}
) {
    val haptic = currentHaptic
    val capture = LocalOdometerCapture.current ?: rememberOdometerCaptureState()
    var step by remember { mutableIntStateOf(0) }
    var brandId by remember { mutableStateOf<String?>(null) }
    var familyId by remember { mutableStateOf<String?>(null) }
    var variantId by remember { mutableStateOf<String?>(null) }
    var customMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCc by remember { mutableStateOf("150") }
    var query by remember { mutableStateOf("") }
    var serviceTiming by remember { mutableStateOf(OnboardingServiceTiming.UNKNOWN) }
    val odoEntry = rememberNumberEntry(initialValues = mapOf("odo" to ""), firstField = "odo")
    val odoRaw = odoEntry.values["odo"].orEmpty()
    val odoVal = parseNum(odoRaw) ?: 0.0

    LaunchedEffect(step) { if (step != 2) capture.close() }
    DisposableEffect(Unit) { onDispose { onPreviewBrand(null); capture.close() } }
    PlatformBackHandler(enabled = step > 0 || onDismiss != null) {
        if (step > 0) step -= 1 else onDismiss?.invoke()
    }

    val state = rememberOnboardingStepState(
        step, brandId, familyId, variantId, customMode, customName, customCc, query, serviceTiming, odoRaw, odoVal,
        capture = capture,
        onBack = { step -= 1 },
        onDismiss = onDismiss,
        onSelectBrand = { id ->
            brandId = id; familyId = null; variantId = null; customMode = false; query = ""
            onPreviewBrand(id); step = 1
        },
        onQueryChange = { query = it },
        onSelectFamily = { fam ->
            customMode = false; familyId = fam.id; variantId = fam.variants.firstOrNull()?.modelId
        },
        onSelectVariant = { v -> variantId = v.modelId },
        onEnableCustom = { haptic.perform(HapticKind.TICK); customMode = true; familyId = null; variantId = null },
        onDisableCustom = { customMode = false },
        onCustomName = { customName = it },
        onCustomCc = { customCc = it },
        onContinueStep1 = { step = 2 },
        onSetOdo = { odoEntry.set("odo", it) },
        onKeyPress = odoEntry::press,
        onServiceTiming = { serviceTiming = it },
        onFinish = { finalOdo ->
            val brand = brandId ?: return@rememberOnboardingStepState
            val families = BikeCatalogueFamilies.forBrand(brand)
            val fam = families.firstOrNull { it.id == familyId }
            val chosenVar = fam?.variants?.firstOrNull { it.modelId == variantId } ?: fam?.variants?.firstOrNull()
            val result = buildOnboardingResult(brand, customMode, customName, customCc, chosenVar, finalOdo, serviceTiming, today())
                ?: return@rememberOnboardingStepState
            haptic.perform(HapticKind.SUCCESS)
            onPreviewBrand(null)
            onComplete(result)
        }
    )

    OnboardingScaffold(state = state)
}

private data class OnboardingStepState(
    val step: Int,
    val brandId: String?,
    val familyId: String?,
    val variantId: String?,
    val customMode: Boolean,
    val customName: String,
    val customCc: String,
    val query: String,
    val serviceTiming: OnboardingServiceTiming,
    val odoRaw: String,
    val odoVal: Double,
    val capture: OdometerCaptureState,
    val onBack: () -> Unit,
    val onDismiss: (() -> Unit)?,
    val onSelectBrand: (String) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onSelectFamily: (BikeFamily) -> Unit,
    val onSelectVariant: (BikeVariant) -> Unit,
    val onEnableCustom: () -> Unit,
    val onDisableCustom: () -> Unit,
    val onCustomName: (String) -> Unit,
    val onCustomCc: (String) -> Unit,
    val onContinueStep1: () -> Unit,
    val onSetOdo: (String) -> Unit,
    val onKeyPress: (String) -> Unit,
    val onServiceTiming: (OnboardingServiceTiming) -> Unit,
    val onFinish: (Double) -> Unit
)

@Composable
private fun rememberOnboardingStepState(
    step: Int, brandId: String?, familyId: String?, variantId: String?, customMode: Boolean,
    customName: String, customCc: String, query: String, serviceTiming: OnboardingServiceTiming,
    odoRaw: String, odoVal: Double, capture: OdometerCaptureState, onBack: () -> Unit,
    onDismiss: (() -> Unit)?, onSelectBrand: (String) -> Unit, onQueryChange: (String) -> Unit,
    onSelectFamily: (BikeFamily) -> Unit, onSelectVariant: (BikeVariant) -> Unit,
    onEnableCustom: () -> Unit, onDisableCustom: () -> Unit, onCustomName: (String) -> Unit,
    onCustomCc: (String) -> Unit, onContinueStep1: () -> Unit, onSetOdo: (String) -> Unit,
    onKeyPress: (String) -> Unit, onServiceTiming: (OnboardingServiceTiming) -> Unit,
    onFinish: (Double) -> Unit
) = OnboardingStepState(
    step, brandId, familyId, variantId, customMode, customName, customCc, query, serviceTiming,
    odoRaw, odoVal, capture, onBack, onDismiss, onSelectBrand, onQueryChange, onSelectFamily,
    onSelectVariant, onEnableCustom, onDisableCustom, onCustomName, onCustomCc, onContinueStep1,
    onSetOdo, onKeyPress, onServiceTiming, onFinish
)

@Composable
private fun OnboardingScaffold(state: OnboardingStepState) {
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val slidePx = with(density) { 28.dp.roundToPx() }
    val exitDuration = if (reducedMotion) 0 else 120

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OnboardingHeader(step = state.step, onBack = state.onBack, onDismiss = state.onDismiss)
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState >= initialState
                val enter = slideInHorizontally(VaultMotion.pageTween(reducedMotion)) { if (forward) slidePx else -slidePx } +
                    fadeIn(VaultMotion.pageTween(reducedMotion))
                val exit = slideOutHorizontally(tween(exitDuration)) { if (forward) -slidePx else slidePx } +
                    fadeOut(tween(exitDuration))
                enter togetherWith exit
            },
            label = "onboardingStep",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { currentStep ->
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (currentStep) {
                    0 -> OnboardingBrandStep(selectedBrandId = state.brandId, onSelectBrand = state.onSelectBrand)
                    1 -> {
                        val brand = worlds.firstOrNull { it.id == state.brandId } ?: return@Column
                        val families = remember(state.brandId, state.query) { BikeCatalogueFamilies.search(brand.id, state.query) }
                        OnboardingModelStep(
                            brand = brand, query = state.query, families = families,
                            selectedFamilyId = state.familyId, selectedVariantId = state.variantId,
                            customMode = state.customMode, customName = state.customName, customCc = state.customCc,
                            onQueryChange = state.onQueryChange, onSelectFamily = state.onSelectFamily,
                            onSelectVariant = state.onSelectVariant, onEnableCustom = state.onEnableCustom,
                            onDisableCustom = state.onDisableCustom, onCustomNameChange = state.onCustomName,
                            onCustomCcChange = state.onCustomCc, onContinue = state.onContinueStep1
                        )
                    }
                    else -> OnboardingOdometerStep(
                        odoRaw = state.odoRaw, odoVal = state.odoVal, serviceTiming = state.serviceTiming,
                        capture = state.capture, onSetOdo = state.onSetOdo, onKeyPress = state.onKeyPress,
                        onServiceTiming = state.onServiceTiming, onFinish = state.onFinish
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: Int, onBack: () -> Unit, onDismiss: (() -> Unit)?) {
    val f = currentFmt
    val dotsLabel = appStringResource(Res.string.onboarding_step, f.d((step + 1).toString()), f.d("3"))
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            step > 0 -> VaultIconButton("fi_arrow-left.svg", onClick = onBack, label = appStringResource(Res.string.onboarding_back))
            onDismiss != null -> VaultIconButton("fi_x.svg", onClick = onDismiss, label = appStringResource(Res.string.onboarding_cancel))
            else -> Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                V3Icon(path = "feather_icon/fi_bike.svg", description = null, modifier = Modifier.size(26.dp), tint = BrandColor)
            }
        }
        Row(
            modifier = Modifier.weight(1f).semantics { contentDescription = dotsLabel },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index <= step) BrandColor else BorderColor)
                )
            }
        }
        Text(
            text = appStringResource(Res.string.onboarding_step_progress, f.d((step + 1).toString()), f.d("3")),
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}
