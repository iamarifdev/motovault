package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String,
    val name: String,
    val brand: Brand,
    val model: String,
    val edition: VehicleEdition,
    val emissionStandard: EmissionStandard,
    val releaseYear: Int,
    val registrationNumber: String = "",
    val powertrainType: PowertrainType = PowertrainType.ICE,
    val category: VehicleCategory = VehicleCategory.MOTORCYCLE,
    val currentOdometerKm: Double = 0.0,
    val rollingEfficiencyKmPerUnit: Double = 38.5, // km/L or km/kWh
    val fuelTankCapacityLiters: Double = 13.5,
    val engineDisplacementCc: Double = 124.7,
    val recommendedOilGrade: String = "10W-50",
    val photoUrl: String? = null,
    val customAccentHex: Long? = null
) {
    val activeAccentColorHex: Long
        get() = customAccentHex ?: brand.defaultAccentHex
}
