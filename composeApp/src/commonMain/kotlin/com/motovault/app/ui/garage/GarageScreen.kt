package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.motovault.app.ui.garage.components.CircularWearGauge
import com.motovault.app.ui.garage.components.PersistentQuickActionDock

@Composable
fun GarageScreen(
    vehicle: Vehicle,
    onNavigateToVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentOdo by remember { mutableStateOf(vehicle.currentOdometerKm) }
    var components by remember {
        mutableStateOf(KtmDuke125Preset.defaultComponentRules(vehicle.id, currentOdo))
    }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val accentColor = Color(vehicle.activeAccentColorHex)

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
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP HEADER: VEHICLE SUMMARY
            item {
                VehicleHeroCard(vehicle = vehicle, currentOdo = currentOdo, accentColor = accentColor)
            }

            // SECTION 1: CIRCULAR WEAR GAUGES
            item {
                Text(
                    text = "COMPONENT WEAR GAUGES",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 12.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    components.take(3).forEach { comp ->
                        CircularWearGauge(
                            title = comp.name.replace("Drive ", "").replace(" & Oil Filter", ""),
                            specification = comp.specification,
                            wearPercentage = comp.calculateWearPercentage(currentOdo, 0L),
                            remainingKm = comp.remainingKm(currentOdo),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 2: DETAILED SERVICE CHECKLIST
            item {
                Text(
                    text = "MAINTENANCE SCHEDULE OVERVIEW",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 12.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
            }

            items(components) { comp ->
                ComponentDetailRow(component = comp, currentOdo = currentOdo)
            }
        }

        // PERSISTENT FLOATING QUICK ACTION DOCK AT BOTTOM
        PersistentQuickActionDock(
            accentColor = accentColor,
            onLubeChainClick = {
                toastMessage = "1-Tap: Chain cleaned & lubed! 500 km interval reset."
            },
            onFuelUpClick = {
                currentOdo += 120.0
                toastMessage = "Fuel logged: +120 km added to Odometer (${currentOdo.toInt()} km)"
            },
            onPoliceCheckClick = onNavigateToVault,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )

        // QUICK TOAST BANNER
        toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2800)
                toastMessage = null
            }
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = msg,
                    style = MotoVaultTypography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun VehicleHeroCard(
    vehicle: Vehicle,
    currentOdo: Double,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCardDark, RoundedCornerShape(22.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = vehicle.name,
                    style = MotoVaultTypography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "${vehicle.emissionStandard.code} • ${vehicle.edition.displayName}",
                    style = MotoVaultTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "READY TO RACE",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ODOMETER",
                    style = MotoVaultTypography.bodyMedium.copy(fontSize = 11.sp, color = TextMuted)
                )
                Text(
                    text = "${currentOdo.toInt()} km",
                    style = MotoVaultTypography.displayLarge.copy(fontSize = 24.sp),
                    color = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "FUEL EFFICIENCY",
                    style = MotoVaultTypography.bodyMedium.copy(fontSize = 11.sp, color = TextMuted)
                )
                Text(
                    text = "${vehicle.rollingEfficiencyKmPerUnit} km/L",
                    style = MotoVaultTypography.displayLarge.copy(
                        fontSize = 24.sp,
                        color = StatusHealthyGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun ComponentDetailRow(component: ComponentRule, currentOdo: Double) {
    val wear = component.calculateWearPercentage(currentOdo, 0L)
    val remaining = component.remainingKm(currentOdo)
    val statusColor = when {
        wear >= 100 -> StatusOverdueRed
        wear >= 75 -> StatusWarningYellow
        else -> StatusHealthyGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCardDark, RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.name,
                style = MotoVaultTypography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = component.specification,
                style = MotoVaultTypography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$wear%",
                style = MotoVaultTypography.labelMedium.copy(fontSize = 15.sp, color = statusColor)
            )
            Text(
                text = "${remaining.toInt()} km left",
                style = MotoVaultTypography.bodyMedium.copy(fontSize = 11.sp, color = TextMuted)
            )
        }
    }
}
