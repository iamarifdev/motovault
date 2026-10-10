package com.motovault.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.*
import com.motovault.app.domain.preset.KtmDuke125Preset
import com.motovault.app.theme.*

/**
 * Apple Setup Assistant 3-Step Paged Flow
 * Step 1: Brand & Model
 * Step 2: Regional Edition & Emission Standards
 * Step 3: Initial Odometer & Factory Spec Preview
 */
@Composable
fun VehicleSelectionScreen(
    onVehicleCreated: (Vehicle) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedBrand by remember { mutableStateOf(Brand.KTM) }
    var selectedModel by remember { mutableStateOf("Duke 125") }
    var selectedEdition by remember { mutableStateOf(VehicleEdition.INDIAN) }
    var selectedEmission by remember { mutableStateOf(EmissionStandard.BS6) }
    var selectedYear by remember { mutableStateOf(2021) }
    var odometerInput by remember { mutableStateOf("4200") }

    // When edition changes, ensure emission standard matches valid list
    LaunchedEffect(selectedEdition) {
        val validEmissions = EmissionStandard.forEdition(selectedEdition)
        if (selectedEmission !in validEmissions) {
            selectedEmission = validEmissions.first()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasTftDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // STEPPER PROGRESS HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VEHICLE PAIRING",
                            style = MotoVaultTypography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = KtmApexOrange
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Configure Your Ride",
                            style = MotoVaultTypography.displayLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )
                    }

                    // Apple-style Step Pill Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (step in 1..3) {
                            val isActive = currentStep == step
                            val isCompleted = currentStep > step
                            val pillWidth = if (isActive) 24.dp else 8.dp
                            val pillColor = when {
                                isActive -> KtmApexOrange
                                isCompleted -> KtmApexOrange.copy(alpha = 0.6f)
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = pillWidth, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(pillColor)
                            )
                        }
                    }
                }
            }

            // ANIMATED STEP CONTENT
            item {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(animationSpec = tween(350)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it } + fadeOut(animationSpec = tween(350)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(350)) { -it } + fadeIn(animationSpec = tween(350)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(350)))
                        }
                    }
                ) { step ->
                    when (step) {
                        1 -> Step1BrandAndModel(
                            selectedBrand = selectedBrand,
                            onBrandSelect = { selectedBrand = it },
                            selectedModel = selectedModel,
                            onNext = { currentStep = 2 }
                        )
                        2 -> Step2EditionAndEmission(
                            selectedEdition = selectedEdition,
                            onEditionSelect = { selectedEdition = it },
                            selectedEmission = selectedEmission,
                            onEmissionSelect = { selectedEmission = it },
                            onBack = { currentStep = 1 },
                            onNext = { currentStep = 3 }
                        )
                        3 -> Step3OdometerAndSpecs(
                            odometerInput = odometerInput,
                            onOdometerChange = { odometerInput = it },
                            edition = selectedEdition,
                            emission = selectedEmission,
                            onBack = { currentStep = 2 },
                            onLaunch = {
                                val odo = odometerInput.toDoubleOrNull() ?: 0.0
                                val vehicle = KtmDuke125Preset.createVehicle(
                                    id = "ktm_duke_125_01",
                                    edition = selectedEdition,
                                    emissionStandard = selectedEmission,
                                    releaseYear = selectedYear,
                                    initialOdometerKm = odo
                                )
                                onVehicleCreated(vehicle)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1BrandAndModel(
    selectedBrand: Brand,
    onBrandSelect: (Brand) -> Unit,
    selectedModel: String,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 1 of 3: Choose brand and model",
            style = MotoVaultTypography.bodyMedium.copy(color = TextSecondary)
        )

        // 2x2 Grid of Brand Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandCard(
                brand = Brand.KTM,
                isSelected = selectedBrand == Brand.KTM,
                onClick = { onBrandSelect(Brand.KTM) },
                modifier = Modifier.weight(1f)
            )
            BrandCard(
                brand = Brand.YAMAHA,
                isSelected = selectedBrand == Brand.YAMAHA,
                onClick = { onBrandSelect(Brand.YAMAHA) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandCard(
                brand = Brand.ATHER,
                isSelected = selectedBrand == Brand.ATHER,
                onClick = { onBrandSelect(Brand.ATHER) },
                modifier = Modifier.weight(1f)
            )
            BrandCard(
                brand = Brand.ROYAL_ENFIELD,
                isSelected = selectedBrand == Brand.ROYAL_ENFIELD,
                onClick = { onBrandSelect(Brand.ROYAL_ENFIELD) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hero Model Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpecularCardBrush, RoundedCornerShape(20.dp))
                .border(1.5.dp, KtmApexOrange, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "KTM Duke 125",
                            style = MotoVaultTypography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(KtmApexOrange.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(1.dp, KtmApexOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MotoVaultTypography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KtmApexOrange
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "124.7cc DOHC • Single Cylinder • Liquid-Cooled",
                        style = MotoVaultTypography.bodyMedium.copy(fontSize = 12.sp, color = TextSecondary)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(KtmApexOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = MotoVaultTypography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = KtmApexOrange),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(
                text = "Continue to Edition & Emission →",
                style = MotoVaultTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }
    }
}

@Composable
private fun BrandCard(
    brand: Brand,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) KtmApexOrange else BorderSpecularDark
    val bg = if (isSelected) KtmApexOrange.copy(alpha = 0.12f) else SurfaceCardDark
    val alpha = if (brand.isAvailable) 1f else 0.4f

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(18.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = brand.isAvailable, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isSelected) KtmApexOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) KtmApexOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = brand.name.take(3),
                    style = MotoVaultTypography.titleMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) KtmApexOrange else TextPrimary.copy(alpha = alpha)
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = brand.displayName,
                style = MotoVaultTypography.titleMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = alpha)
                )
            )
            Text(
                text = if (brand.isAvailable) "Active" else "Soon",
                style = MotoVaultTypography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (brand.isAvailable) KtmApexOrange else TextMuted
                )
            )
        }
    }
}

@Composable
private fun Step2EditionAndEmission(
    selectedEdition: VehicleEdition,
    onEditionSelect: (VehicleEdition) -> Unit,
    selectedEmission: EmissionStandard,
    onEmissionSelect: (EmissionStandard) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 2 of 3: Regional edition & emissions",
            style = MotoVaultTypography.bodyMedium.copy(color = TextSecondary)
        )

        // Segmented Pill Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevatedDark, RoundedCornerShape(16.dp))
                .border(1.dp, BorderSpecularDark, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VehicleEdition.entries.forEach { edition ->
                val isSelected = selectedEdition == edition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) KtmApexOrange else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onEditionSelect(edition) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = edition.displayName,
                        style = MotoVaultTypography.titleMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "EMISSION SPECIFICATIONS (${selectedEdition.displayName.uppercase()})",
            style = MotoVaultTypography.labelMedium.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        )

        // Emission Cards filtered by edition
        val availableEmissions = EmissionStandard.forEdition(selectedEdition)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            availableEmissions.forEach { emission ->
                val isSelected = selectedEmission == emission
                val borderColor = if (isSelected) KtmApexOrange else BorderSpecularDark
                val bg = if (isSelected) SurfaceElevatedDark else SurfaceCardDark

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg, RoundedCornerShape(18.dp))
                        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
                        .clickable { onEmissionSelect(emission) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = emission.displayName,
                                style = MotoVaultTypography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Years ${emission.minYear}-${emission.maxYear} • Tank: ${emission.fuelTankCapacityLiters}L",
                                style = MotoVaultTypography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) KtmApexOrange else TextSecondary
                                )
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(KtmApexOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    style = MotoVaultTypography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(0.35f)
                    .height(54.dp)
                    .border(1.dp, BorderSpecularDark, RoundedCornerShape(18.dp))
            ) {
                Text(
                    text = "← Back",
                    style = MotoVaultTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = KtmApexOrange),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(0.65f).height(54.dp)
            ) {
                Text(
                    text = "Continue to Odometer →",
                    style = MotoVaultTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun Step3OdometerAndSpecs(
    odometerInput: String,
    onOdometerChange: (String) -> Unit,
    edition: VehicleEdition,
    emission: EmissionStandard,
    onBack: () -> Unit,
    onLaunch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 3 of 3: Initial odometer & specifications",
            style = MotoVaultTypography.bodyMedium.copy(color = TextSecondary)
        )

        // Odometer Readout Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpecularCardBrush, RoundedCornerShape(20.dp))
                .border(1.dp, BorderSpecularDark, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "CURRENT ODOMETER (KM)",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = onOdometerChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KtmApexOrange,
                        unfocusedBorderColor = BorderSpecularDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceElevatedDark,
                        unfocusedContainerColor = SurfaceElevatedDark
                    ),
                    suffix = {
                        Text(
                            text = "km",
                            style = MotoVaultTypography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = KtmApexOrange,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Increment Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1,000", "4,200", "10,000").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(1.dp, BorderSpecularDark, RoundedCornerShape(10.dp))
                                .clickable { onOdometerChange(preset.replace(",", "")) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$preset km",
                                style = MotoVaultTypography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Factory Specs Summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpecularCardBrush, RoundedCornerShape(20.dp))
                .border(1.dp, BorderSpecularDark, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FACTORY SPECS LOADED",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = StatusHealthyGreen
                    )
                )

                SpecRow(label = "Chassis & Frame", value = "Split Trellis Frame (White/Orange)")
                SpecRow(label = "Fuel Tank Capacity", value = "${emission.fuelTankCapacityLiters} Liters")
                SpecRow(label = "Engine Oil Spec", value = "10W-50 JASO MA2 (1.5L)")
                SpecRow(label = "Maintenance Rules", value = "7 Factory Wear Triggers Configured")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevatedDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(0.35f)
                    .height(54.dp)
                    .border(1.dp, BorderSpecularDark, RoundedCornerShape(18.dp))
            ) {
                Text(
                    text = "← Back",
                    style = MotoVaultTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = KtmApexOrange),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(0.65f).height(54.dp)
            ) {
                Text(
                    text = "Initialize Garage 🏍️",
                    style = MotoVaultTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MotoVaultTypography.bodyMedium.copy(fontSize = 12.sp, color = TextSecondary)
        )
        Text(
            text = value,
            style = MotoVaultTypography.bodyMedium.copy(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}
