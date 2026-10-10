package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.motovault.app.ui.garage.components.CircularWearGauge
import com.motovault.app.ui.garage.components.PersistentQuickActionDock

/**
 * Rivian OS 3.0 + Bosch TFT Digital Cockpit Garage Screen
 * - Ambient radial halo vehicle hero stage
 * - Bosch TFT Inset Telemetry Dials
 * - Specular factory maintenance checklist
 * - Floating frosted glass quick action dock
 */
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
                .padding(top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // VEHICLE HERO STAGE
            item {
                VehicleHeroCard(
                    vehicle = vehicle,
                    currentOdo = currentOdo,
                    accentColor = accentColor
                )
            }

            // SECTION 1: BOSCH TFT WEAR DIALS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRECISION TELEMETRY INSTRUMENTS",
                        style = MotoVaultTypography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Live Cluster",
                        style = MotoVaultTypography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StatusHealthyGreen
                        )
                    )
                }

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
                            modifier = Modifier.weight(1f),
                            onClick = {
                                toastMessage = "${comp.name}: ${comp.specification}"
                            }
                        )
                    }
                }
            }

            // SECTION 2: FACTORY MAINTENANCE SCHEDULE
            item {
                Text(
                    text = "FACTORY MAINTENANCE SCHEDULE",
                    style = MotoVaultTypography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
            }

            items(components) { comp ->
                ComponentDetailRow(
                    component = comp,
                    currentOdo = currentOdo,
                    onItemClick = {
                        toastMessage = "${comp.name} interval: ${comp.intervalKm.toInt()} km"
                    }
                )
            }
        }

        // FLOATING FROSTED QUICK ACTION DOCK
        PersistentQuickActionDock(
            accentColor = accentColor,
            onLubeChainClick = {
                toastMessage = "Chain cleaned & lubed! 500 km wear interval reset."
            },
            onFuelUpClick = {
                currentOdo += 120.0
                toastMessage = "Fuel logged: +120 km added (${currentOdo.toInt()} km total)"
            },
            onPoliceCheckClick = onNavigateToVault,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // TOAST BANNER
        toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2600)
                toastMessage = null
            }
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSpecularDark),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = msg,
                    style = MotoVaultTypography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(SpecularCardBrush)
            .border(1.dp, BorderSpecularDark, RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "READY TO RACE",
                            style = MotoVaultTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = accentColor
                            )
                        )
                    }
                    Text(
                        text = "${vehicle.emissionStandard.code} • ${vehicle.edition.displayName}",
                        style = MotoVaultTypography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    )
                }

                // Status Online indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(StatusHealthyGreen, CircleShape)
                    )
                    Text(
                        text = "OK",
                        style = MotoVaultTypography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StatusHealthyGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = vehicle.name,
                style = MotoVaultTypography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Text(
                text = "124.7cc DOHC • Chakan Facility Assembly",
                style = MotoVaultTypography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Precision Telemetry Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSpecularDark, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ODOMETER",
                        style = MotoVaultTypography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%,d", currentOdo.toLong()),
                            style = MotoVaultTypography.displayLarge.copy(
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km",
                            style = MotoVaultTypography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Vertical hairline divider
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(BorderSpecularDark)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "FUEL ECONOMY",
                        style = MotoVaultTypography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${vehicle.rollingEfficiencyKmPerUnit}",
                            style = MotoVaultTypography.displayLarge.copy(
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            color = StatusHealthyGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km/L",
                            style = MotoVaultTypography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = StatusHealthyGreen.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentDetailRow(
    component: ComponentRule,
    currentOdo: Double,
    onItemClick: () -> Unit
) {
    val wear = component.calculateWearPercentage(currentOdo, 0L)
    val remaining = component.remainingKm(currentOdo)
    val statusColor = when {
        wear >= 100 -> StatusOverdueRed
        wear >= 75 -> StatusWarningYellow
        else -> StatusHealthyGreen
    }

    val icon = when {
        component.name.contains("Chain", ignoreCase = true) -> "⛓️"
        component.name.contains("Oil", ignoreCase = true) -> "🛢️"
        component.name.contains("Brake", ignoreCase = true) -> "🛑"
        component.name.contains("Spark", ignoreCase = true) -> "⚡"
        component.name.contains("Coolant", ignoreCase = true) -> "🧪"
        component.name.contains("Filter", ignoreCase = true) -> "💨"
        else -> "⚙️"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SpecularCardBrush)
            .border(1.dp, BorderSpecularDark, RoundedCornerShape(18.dp))
            .clickable(onClick = onItemClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSpecularDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 16.sp)
            }

            Column {
                Text(
                    text = component.name,
                    style = MotoVaultTypography.titleMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = component.specification,
                    style = MotoVaultTypography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    ),
                    maxLines = 1
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$wear%",
                style = MotoVaultTypography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            )
            Text(
                text = "${remaining.toInt()} km left",
                style = MotoVaultTypography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            )
        }
    }
}
