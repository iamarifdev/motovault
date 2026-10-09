package com.motovault.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.*
import com.motovault.app.domain.preset.KtmDuke125Preset
import com.motovault.app.theme.*

@Composable
fun VehicleSelectionScreen(
    onVehicleCreated: (Vehicle) -> Unit,
    modifier: Modifier = Modifier
) {
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasTftDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Welcome to MotoVault",
                    style = MotoVaultTypography.displayLarge.copy(fontSize = 28.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set up your motorcycle garage profile to load exact manufacturer service specs.",
                    style = MotoVaultTypography.bodyLarge,
                    color = TextSecondary
                )
            }
        }

        // STEP 1: SELECT BRAND
        item {
            SectionHeader(stepNumber = "1", title = "Select Brand")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Brand.entries.take(4).forEach { brand ->
                    BrandChip(
                        brand = brand,
                        isSelected = selectedBrand == brand,
                        onClick = { if (brand.isAvailable) selectedBrand = brand },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // STEP 2: SELECT MODEL
        item {
            SectionHeader(stepNumber = "2", title = "Select Model")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCardDark, RoundedCornerShape(16.dp))
                    .border(1.5.dp, KtmApexOrange, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KTM Duke 125",
                            style = MotoVaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "124.7cc DOHC • Liquid-Cooled • Single Cylinder",
                            style = MotoVaultTypography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "ACTIVE",
                        style = MotoVaultTypography.labelMedium.copy(color = KtmApexOrange)
                    )
                }
            }
        }

        // STEP 3: SELECT MARKET / EDITION
        item {
            SectionHeader(stepNumber = "3", title = "Select Regional Edition")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VehicleEdition.entries.forEach { edition ->
                    EditionCard(
                        edition = edition,
                        isSelected = selectedEdition == edition,
                        onClick = { selectedEdition = edition }
                    )
                }
            }
        }

        // STEP 4: SELECT EMISSION STANDARD (DEPENDENT ON EDITION)
        item {
            val availableEmissions = EmissionStandard.forEdition(selectedEdition)
            SectionHeader(
                stepNumber = "4",
                title = "Select Emission Standard (${selectedEdition.displayName})"
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableEmissions.forEach { emission ->
                    EmissionCard(
                        emission = emission,
                        isSelected = selectedEmission == emission,
                        onClick = {
                            selectedEmission = emission
                            selectedYear = emission.minYear.coerceAtLeast(selectedYear)
                        }
                    )
                }
            }
        }

        // STEP 5: RELEASE YEAR & ODOMETER
        item {
            SectionHeader(stepNumber = "5", title = "Model Year & Current Odometer")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Release Year",
                        style = MotoVaultTypography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCardDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSubtleDark, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = selectedYear.toString(),
                            style = MotoVaultTypography.labelMedium.copy(fontSize = 16.sp),
                            color = TextPrimary
                        )
                    }
                }

                // Odometer Input
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Current Odometer (km)",
                        style = MotoVaultTypography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCardDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSubtleDark, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "$odometerInput km",
                            style = MotoVaultTypography.labelMedium.copy(
                                fontSize = 16.sp,
                                color = KtmApexOrange
                            )
                        )
                    }
                }
            }
        }

        // CONFIRM & SETUP BUTTON
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val odo = odometerInput.toDoubleOrNull() ?: 0.0
                    val vehicle = KtmDuke125Preset.createVehicle(
                        id = "ktm_duke_125_01",
                        edition = selectedEdition,
                        emissionStandard = selectedEmission,
                        releaseYear = selectedYear,
                        initialOdometerKm = odo
                    )
                    onVehicleCreated(vehicle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KtmApexOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = "Launch MotoVault Dashboard",
                    style = MotoVaultTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeader(stepNumber: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(KtmApexOrange.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .border(1.dp, KtmApexOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MotoVaultTypography.labelMedium.copy(fontSize = 12.sp, color = KtmApexOrange)
            )
        }
        Text(
            text = title,
            style = MotoVaultTypography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}

@Composable
private fun BrandChip(
    brand: Brand,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) KtmApexOrange else BorderSubtleDark
    val bgColor = if (isSelected) KtmApexOrange.copy(alpha = 0.15f) else SurfaceCardDark
    val alpha = if (brand.isAvailable) 1f else 0.4f

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = brand.isAvailable, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = brand.displayName,
                style = MotoVaultTypography.titleMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = alpha)
                )
            )
            if (!brand.isAvailable) {
                Text(
                    text = "Soon",
                    style = MotoVaultTypography.bodyMedium.copy(fontSize = 9.sp, color = TextMuted)
                )
            }
        }
    }
}

@Composable
private fun EditionCard(
    edition: VehicleEdition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) KtmApexOrange else BorderSubtleDark
    val bgColor = if (isSelected) SurfaceElevatedDark else SurfaceCardDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = edition.displayName,
                style = MotoVaultTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = edition.subtitle,
                style = MotoVaultTypography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = KtmApexOrange)
        )
    }
}

@Composable
private fun EmissionCard(
    emission: EmissionStandard,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) KtmApexOrange else BorderSubtleDark
    val bgColor = if (isSelected) SurfaceElevatedDark else SurfaceCardDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = emission.displayName,
                style = MotoVaultTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Years ${emission.minYear} - ${emission.maxYear} • Tank: ${emission.fuelTankCapacityLiters}L",
                style = MotoVaultTypography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = KtmApexOrange)
        )
    }
}
