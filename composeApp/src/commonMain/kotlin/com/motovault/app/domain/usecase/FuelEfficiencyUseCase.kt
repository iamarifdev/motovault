package com.motovault.app.domain.usecase

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Serializable
data class FuelLogEntry(
    val id: String,
    val vehicleId: String,
    val odometerKm: Double,
    val liters: Double,
    val totalCost: Double,
    val isFullTank: Boolean,
    val epochMillis: Long
)

data class FuelEfficiencyResult(
    val calculatedEconomyKmPerLiter: Double?,
    val costPerKm: Double?,
    val deltaDistanceKm: Double?,
    val fuelConsumedLiters: Double?,
    val isReliable: Boolean,
    val statusMessage: String
)

class FuelEfficiencyUseCase {

    /**
     * Calculates fuel economy using the automotive standard "Full-to-Full" tank method.
     *
     * Rules:
     * 1. Requires previous full-tank baseline.
     * 2. If current fill is not full-tank, efficiency cannot be conclusively calculated until next full tank.
     * 3. Prevents invalid calculations (zero/negative liters, odometer rollback).
     */
    fun calculate(
        previousEntry: FuelLogEntry?,
        currentEntry: FuelLogEntry
    ): FuelEfficiencyResult {
        if (previousEntry == null) {
            return FuelEfficiencyResult(
                calculatedEconomyKmPerLiter = null,
                costPerKm = null,
                deltaDistanceKm = null,
                fuelConsumedLiters = currentEntry.liters,
                isReliable = false,
                statusMessage = "Initial fill-up recorded as baseline."
            )
        }

        val deltaDistance = currentEntry.odometerKm - previousEntry.odometerKm
        if (deltaDistance <= 0.0) {
            return FuelEfficiencyResult(
                calculatedEconomyKmPerLiter = null,
                costPerKm = null,
                deltaDistanceKm = deltaDistance,
                fuelConsumedLiters = currentEntry.liters,
                isReliable = false,
                statusMessage = "Invalid odometer delta (must be greater than previous odometer)."
            )
        }

        if (currentEntry.liters <= 0.0) {
            return FuelEfficiencyResult(
                calculatedEconomyKmPerLiter = null,
                costPerKm = null,
                deltaDistanceKm = deltaDistance,
                fuelConsumedLiters = currentEntry.liters,
                isReliable = false,
                statusMessage = "Fuel volume must be positive."
            )
        }

        if (!previousEntry.isFullTank || !currentEntry.isFullTank) {
            return FuelEfficiencyResult(
                calculatedEconomyKmPerLiter = null,
                costPerKm = if (deltaDistance > 0) currentEntry.totalCost / deltaDistance else null,
                deltaDistanceKm = deltaDistance,
                fuelConsumedLiters = currentEntry.liters,
                isReliable = false,
                statusMessage = "Partial fill-up recorded. Exact economy requires full-to-full fill-up."
            )
        }

        val economy = deltaDistance / currentEntry.liters
        val costPerKm = currentEntry.totalCost / deltaDistance

        return FuelEfficiencyResult(
            calculatedEconomyKmPerLiter = (economy * 100.0).roundToLong() / 100.0,
            costPerKm = (costPerKm * 100.0).roundToLong() / 100.0,
            deltaDistanceKm = deltaDistance,
            fuelConsumedLiters = currentEntry.liters,
            isReliable = true,
            statusMessage = "Success"
        )
    }
}
