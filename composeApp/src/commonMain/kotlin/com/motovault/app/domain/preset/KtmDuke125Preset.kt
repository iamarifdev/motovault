package com.motovault.app.domain.preset

import com.motovault.app.domain.model.*

object KtmDuke125Preset {

    fun createVehicle(
        id: String,
        edition: VehicleEdition,
        emissionStandard: EmissionStandard,
        releaseYear: Int,
        initialOdometerKm: Double = 0.0,
        registrationNumber: String = ""
    ): Vehicle {
        val tankCapacity = if (emissionStandard == EmissionStandard.BS4) 10.2 else 13.5
        return Vehicle(
            id = id,
            name = "KTM Duke 125",
            brand = Brand.KTM,
            model = "Duke 125",
            edition = edition,
            emissionStandard = emissionStandard,
            releaseYear = releaseYear,
            registrationNumber = registrationNumber,
            powertrainType = PowertrainType.ICE,
            category = VehicleCategory.MOTORCYCLE,
            currentOdometerKm = initialOdometerKm,
            rollingEfficiencyKmPerUnit = 38.5,
            fuelTankCapacityLiters = tankCapacity,
            engineDisplacementCc = 124.7,
            recommendedOilGrade = "Motul 7100 4T 10W-50 (1.5L)"
        )
    }

    fun defaultComponentRules(vehicleId: String, initialOdometerKm: Double = 0.0): List<ComponentRule> {
        return listOf(
            ComponentRule(
                id = "${vehicleId}_chain",
                vehicleId = vehicleId,
                name = "Drive Chain Clean & Lube",
                specification = "520 O-Ring / X-Ring • 5-7mm slack",
                intervalKm = 500.0,
                intervalDays = 30,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = true
            ),
            ComponentRule(
                id = "${vehicleId}_oil",
                vehicleId = vehicleId,
                name = "Engine Oil & Oil Filter",
                specification = "Motul 7100 10W-50 JASO MA2 (1.5L)",
                intervalKm = 5000.0,
                intervalDays = 120,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = true
            ),
            ComponentRule(
                id = "${vehicleId}_air_filter",
                vehicleId = vehicleId,
                name = "Air Filter",
                specification = "Paper element cartridge",
                intervalKm = 5000.0,
                intervalDays = 120,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = false
            ),
            ComponentRule(
                id = "${vehicleId}_spark_plug",
                vehicleId = vehicleId,
                name = "Spark Plug",
                specification = "NGK LKAR8A-9 (0.8-0.9mm gap)",
                intervalKm = 10000.0,
                intervalDays = 365,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = true
            ),
            ComponentRule(
                id = "${vehicleId}_brake_pads",
                vehicleId = vehicleId,
                name = "Brake Pads (Front & Rear)",
                specification = "ByBre Sintered / Organic",
                intervalKm = 7500.0,
                intervalDays = 180,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = true
            ),
            ComponentRule(
                id = "${vehicleId}_coolant",
                vehicleId = vehicleId,
                name = "Engine Coolant",
                specification = "Ethylene Glycol OAT Premix",
                intervalKm = 15000.0,
                intervalDays = 365,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = false
            ),
            ComponentRule(
                id = "${vehicleId}_brake_fluid",
                vehicleId = vehicleId,
                name = "Brake Fluid (DOT 4)",
                specification = "DOT 4 High Temp",
                intervalKm = 15000.0,
                intervalDays = 365,
                lastServicedOdometerKm = initialOdometerKm,
                isCritical = true
            )
        )
    }
}
