package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

data class OnboardingResult(
    val brandId: String,
    val modelId: String,
    val modelName: String,
    val odometer: Double,
    val baselineKm: Double?,
    val baselineDate: LocalDate,
    val year: Int,
    val market: RideMarket,
    val modelCode: String,
    val factoryProfileId: String?,
    val baselineCertainty: com.motovault.app.domain.model.BaselineCertainty
)

@Composable
fun OnboardingScreen(
    onDismiss: (() -> Unit)? = null,
    onComplete: (OnboardingResult) -> Unit,
    initialStep: Int = 0,
    initialBrandId: String? = null,
    initialModelId: String? = null,
    initialModelCode: String = ""
) {
    val f = currentFmt
    val haptic = currentHaptic
    var step by remember { mutableStateOf(initialStep.coerceIn(0, 2)) } // 0: Brand, 1: Model, 2: Odometer
    var selectedBrandId by remember { mutableStateOf(initialBrandId) }
    var selectedModelId by remember { mutableStateOf(initialModelId) }
    var searchQuery by remember { mutableStateOf("") }
    var knowsLastService by remember { mutableStateOf(false) }

    val odoEntry = rememberNumberEntry(
        initialValues = mapOf("odo" to ""),
        firstField = "odo"
    )
    val odoVal = parseNum(odoEntry.values["odo"] ?: "") ?: 0.0

    val selectedBrand = worlds.firstOrNull { it.id == selectedBrandId }
    val brandModels = remember(selectedBrandId) {
        if (selectedBrandId != null) BikeCatalogue.forBrand(selectedBrandId!!) else emptyList()
    }
    val filteredModels = remember(brandModels, searchQuery) {
        if (searchQuery.isBlank()) brandModels
        else brandModels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 0) {
                VaultIconButton(
                    icon = "fi_arrow-left.svg",
                    label = "পেছনে",
                    onClick = { step -= 1 }
                )
            } else if (onDismiss != null) {
                VaultIconButton(
                    icon = "fi_arrow-left.svg",
                    label = "ফিরে যান",
                    onClick = onDismiss
                )
            } else {
                V3Icon(path = "feather_icon/fi_bike.svg", description = null, modifier = Modifier.size(26.dp), tint = BrandColor)
            }
            // Step dots
            Row(modifier = Modifier.weight(1f).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..2).forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) BrandColor else BorderColor)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "${f.d((step + 1).toString())} / ${f.d("3")}", fontSize = 13.sp, color = TextSecondary)
                if (onDismiss != null) {
                    VaultIconButton(
                        icon = "fi_x.svg",
                        label = "বন্ধ করুন",
                        onClick = onDismiss
                    )
                }
            }
        }

        when (step) {
            0 -> {
                // Step 0: "কোন বাইক চালান?"
                // EXCEPTION: uses bike brand logos aligning prototype!
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "কোন বাইক চালান?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "ব্র্যান্ড বেছে নিন। অ্যাপের রঙ আপনার বাইকের সঙ্গে মিলবে।", fontSize = 15.sp, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        worlds.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { w ->
                                    val isSelected = selectedBrandId == w.id
                                    val count = BikeCatalogue.forBrand(w.id).size
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) BrandTintColor else SurfaceColor)
                                            .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptic.perform(HapticKind.TICK)
                                                selectedBrandId = w.id
                                                step = 1
                                            }
                                            .padding(vertical = 14.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Brand vector logo (honoring user exception)
                                        Image(
                                            painter = painterResource(w.logo()),
                                            contentDescription = w.name,
                                            modifier = Modifier.width(52.dp).height(28.dp),
                                            contentScale = ContentScale.Fit,
                                            colorFilter = ColorFilter.tint(if (isSelected) BrandColor else TextPrimary)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(text = w.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text(text = "${f.d(count.toString())}টি মডেল", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    if (onDismiss != null) {
                        VaultButton(
                            text = "বাতিল করুন",
                            kind = ButtonKind.SECONDARY,
                            onClick = onDismiss,
                            full = true
                        )
                    }
                    Text(
                        text = "আপনার সব তথ্য এই ফোনেই থাকবে। কোনো অ্যাকাউন্ট লাগবে না।",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
            }
            1 -> {
                // Step 1: "{Brand} এর কোনটি?"
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "${selectedBrand?.name ?: ""} এর কোনটি?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Search box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceColor2)
                            .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            V3Icon(path = "feather_icon/fi_search.svg", description = null, modifier = Modifier.size(20.dp), tint = TextTertiary)
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("মডেলের নাম খুঁজুন", fontSize = 15.sp, color = TextTertiary)
                                    inner()
                                }
                            )
                        }
                    }

                    // Model list
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredModels.forEach { m ->
                            val isSelected = selectedModelId == m.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) BrandTintColor else SurfaceColor)
                                    .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.perform(HapticKind.TICK)
                                        selectedModelId = m.id
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 104.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(painter = painterResource(m.photo), contentDescription = null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                                }
                                Text(text = m.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape).background(BrandColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = OnBrandColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            selectedModelId = brandModels.firstOrNull()?.id
                            step = 2
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("আমার মডেল তালিকায় নেই", color = BrandColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VaultButton(
                        text = if (selectedModelId != null) "${brandModels.firstOrNull { it.id == selectedModelId }?.name ?: ""} নিয়ে এগিয়ে যান" else "মডেল বেছে নিন",
                        full = true,
                        enabled = selectedModelId != null,
                        onClick = { step = 2 }
                    )
                }
            }
            2 -> {
                var baselineKnown by remember { mutableStateOf(false) }
                var lastServiceKm by remember { mutableStateOf("") }
                var lastServiceDate by remember { mutableStateOf(today().toString()) }
                var year by remember { mutableStateOf("2023") }
                var market by remember { mutableStateOf(RideMarket.BANGLADESH) }
                var modelCode by remember { mutableStateOf(initialModelCode) }
                var useManual by remember { mutableStateOf(false) }
                val modelHasProfiles = selectedModelId != null && FactoryProfiles.profiles.any { it.modelId == selectedModelId }
                val profile = FactoryProfiles.available(selectedModelId, year.toIntOrNull(), market).firstOrNull {
                    modelCode.trim().uppercase() in it.modelCodes
                }
                LaunchedEffect(profile?.id) { useManual = profile != null }
                val lastKm = lastServiceKm.toDoubleOrNull()
                val baselineValid = !baselineKnown || (lastKm != null && lastKm >= 0.0 && lastKm <= odoVal)
                val baselineDateValue = runCatching { LocalDate.parse(lastServiceDate) }.getOrNull()
                val baselineDateValid = !baselineKnown || (baselineDateValue != null && baselineDateValue <= today())
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "এখন ওডোমিটারে কত?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val text = odoEntry.values["odo"].orEmpty()
                            Text(
                                text = if (text.isEmpty()) f.d("0") else f.d(formatLakh(text.toDoubleOrNull() ?: 0.0)),
                                fontSize = 44.sp, fontWeight = FontWeight.Bold,
                                color = if (text.isEmpty()) TextTertiary else TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = if (f.digits == Digits.BN) "কিমি" else "km", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VaultChip(text = appStringResource(Res.string.onboarding_baseline_unknown), active = !baselineKnown, onClick = { baselineKnown = false })
                            VaultChip(text = appStringResource(Res.string.onboarding_baseline_known), active = baselineKnown, onClick = { baselineKnown = true })
                        }
                        Text(
                            text = appStringResource(if (baselineKnown) Res.string.onboarding_baseline_known_hint else Res.string.onboarding_baseline_unknown_hint),
                            fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
                        )
                        if (baselineKnown) {
                            Field(appStringResource(Res.string.onboarding_last_service_km), lastServiceKm, { lastServiceKm = it }, numeric = true)
                            Field(appStringResource(Res.string.onboarding_last_service_date), lastServiceDate, { lastServiceDate = it })
                            if (!baselineValid) Text(appStringResource(Res.string.onboarding_baseline_invalid), color = WarningColor, fontSize = 12.sp)
                        } else {
                            Panel {
                                Text(appStringResource(Res.string.onboarding_first_inspection_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                listOf(
                                    Res.string.onboarding_first_inspection_tyres,
                                    Res.string.onboarding_first_inspection_brakes,
                                    Res.string.onboarding_first_inspection_chain,
                                    Res.string.onboarding_first_inspection_lights,
                                    Res.string.onboarding_first_inspection_leaks
                                ).forEach { item ->
                                    val line = "• ${appStringResource(item)}"
                                    Text(line, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        if (modelHasProfiles) {
                            Text(appStringResource(Res.string.onboarding_manual_section), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Field(appStringResource(Res.string.onboarding_model_year), year, { year = it }, numeric = true)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RideMarket.entries.forEach { option ->
                                    VaultChip(
                                        text = when (option) {
                                            RideMarket.UNKNOWN -> appStringResource(Res.string.onboarding_market) + "?"
                                            RideMarket.INDIA -> "India"
                                            RideMarket.EUROPE -> "Europe"
                                            RideMarket.BANGLADESH -> "Bangladesh"
                                        },
                                        active = market == option,
                                        onClick = { market = option }
                                    )
                                }
                            }
                            Field(appStringResource(Res.string.onboarding_model_code), modelCode, { modelCode = it.take(30) })
                            if (profile != null) {
                                val manualLine = "${profile.title} · ${profile.applicability}"
                                Text(manualLine, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                                val uri = LocalUriHandler.current
                                TextButton(onClick = { uri.openUri(profile.manualUrl) }) { Text(appStringResource(Res.string.schedule_manual_open), color = BrandColor) }
                                Row(Modifier.fillMaxWidth().clickable { useManual = !useManual }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(useManual, onCheckedChange = { useManual = it })
                                    Text(appStringResource(Res.string.onboarding_manual_toggle), fontSize = 12.sp)
                                }
                            } else {
                                Text(appStringResource(Res.string.onboarding_manual_unavailable), color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    VaultButton(
                        text = if (odoVal <= 0) appStringResource(Res.string.onboarding_odometer_required) else appStringResource(Res.string.onboarding_finish_btn),
                        full = true,
                        enabled = odoVal > 0 && baselineValid && baselineDateValid && (!useManual || profile != null),
                        onClick = {
                            haptic.perform(HapticKind.SUCCESS)
                            val chosenModel = brandModels.firstOrNull { it.id == selectedModelId }
                            onComplete(
                                OnboardingResult(
                                    brandId = selectedBrandId ?: "yamaha",
                                    modelId = selectedModelId ?: "yamaha-r15-v4",
                                    modelName = chosenModel?.name ?: "R15 V4",
                                    odometer = odoVal,
                                    baselineKm = if (baselineKnown) lastKm else null,
                                    baselineDate = baselineDateValue ?: today(),
                                    year = year.toIntOrNull() ?: 2023,
                                    market = market,
                                    modelCode = modelCode.trim().uppercase(),
                                    factoryProfileId = profile?.id?.takeIf { useManual },
                                    baselineCertainty = if (baselineKnown) com.motovault.app.domain.model.BaselineCertainty.OWNER_ESTIMATE else com.motovault.app.domain.model.BaselineCertainty.UNKNOWN
                                )
                            )
                        }
                    )
                    VaultKeypad(onKey = odoEntry::press, decimal = false)
                }
            }
        }
    }
}
