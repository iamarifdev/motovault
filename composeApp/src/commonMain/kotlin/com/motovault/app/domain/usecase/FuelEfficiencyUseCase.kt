package com.motovault.app.domain.usecase

data class FuelFill(val odometerKm: Double, val liters: Double?, val isFullTank: Boolean, val missedFill: Boolean = false)
data class FuelConfidence(val economy: Double?, val completedCycles: Int, val openCycle: Boolean, val brokenCycle: Boolean)

class FuelEfficiencyUseCase {
    fun calculate(entries: List<FuelFill>): Double? = confidence(entries).economy

    fun confidence(entries: List<FuelFill>): FuelConfidence {
        var start: Double? = null
        var pendingLiters = 0.0
        var totalDistance = 0.0
        var totalLiters = 0.0
        var cycles = 0
        var broken = false
        var previous: Double? = null
        entries.sortedBy { it.odometerKm }.forEach { entry ->
            val liters = entry.liters
            val valid = entry.odometerKm.isFinite() && entry.odometerKm >= 0 && liters != null && liters.isFinite() && liters > 0
            if (!valid || entry.missedFill || previous?.let { entry.odometerKm <= it } == true) {
                start = null; pendingLiters = 0.0; broken = true
            }
            previous = entry.odometerKm
            if (!valid) return@forEach
            if (start == null) {
                if (entry.isFullTank) start = entry.odometerKm
            } else {
                pendingLiters += requireNotNull(liters)
                if (entry.isFullTank) {
                    val distance = entry.odometerKm - start!!
                    if (distance > 0 && pendingLiters > 0) { totalDistance += distance; totalLiters += pendingLiters; cycles++ }
                    start = entry.odometerKm
                    pendingLiters = 0.0
                }
            }
        }
        return FuelConfidence(if (totalLiters > 0) totalDistance / totalLiters else null, cycles,
            pendingLiters > 0 || (start != null && cycles == 0), broken)
    }
}
