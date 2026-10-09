package com.motovault.app

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.motovault.app.domain.model.Vehicle
import com.motovault.app.theme.KtmApexOrange
import com.motovault.app.theme.MotoVaultTheme
import com.motovault.app.ui.garage.GarageScreen
import com.motovault.app.ui.onboarding.VehicleSelectionScreen
import com.motovault.app.ui.vault.DocumentVaultScreen

enum class AppDestination {
    ONBOARDING,
    GARAGE,
    DOCUMENT_VAULT
}

@Composable
fun App() {
    var activeVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var currentDestination by remember { mutableStateOf(AppDestination.ONBOARDING) }

    val dynamicAccentColor = activeVehicle?.let { Color(it.activeAccentColorHex) } ?: KtmApexOrange

    MotoVaultTheme(dynamicAccentColor = dynamicAccentColor) {
        when (currentDestination) {
            AppDestination.ONBOARDING -> {
                VehicleSelectionScreen(
                    onVehicleCreated = { vehicle ->
                        activeVehicle = vehicle
                        currentDestination = AppDestination.GARAGE
                    }
                )
            }
            AppDestination.GARAGE -> {
                activeVehicle?.let { vehicle ->
                    GarageScreen(
                        vehicle = vehicle,
                        onNavigateToVault = {
                            currentDestination = AppDestination.DOCUMENT_VAULT
                        }
                    )
                } ?: run {
                    currentDestination = AppDestination.ONBOARDING
                }
            }
            AppDestination.DOCUMENT_VAULT -> {
                DocumentVaultScreen(
                    onBackToGarage = {
                        currentDestination = AppDestination.GARAGE
                    }
                )
            }
        }
    }
}
