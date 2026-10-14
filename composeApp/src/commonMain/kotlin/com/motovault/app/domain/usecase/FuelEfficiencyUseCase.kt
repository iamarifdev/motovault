package com.motovault.app.domain.usecase

data class FuelFill(val odometerKm: Double, val liters: Double?, val isFullTank: Boolean)

class FuelEfficiencyUseCase {
    fun calculate(entries: List<FuelFill>): Double? {
        var start: Double? = null
        var pendingLiters = 0.0
        var totalDistance = 0.0
        var totalLiters = 0.0
        entries.sortedBy { it.odometerKm }.forEach { entry ->
            val liters = entry.liters ?: return@forEach
            if (start == null) {
                if (entry.isFullTank) start = entry.odometerKm
            } else {
                pendingLiters += liters
                if (entry.isFullTank) {
                    val distance = entry.odometerKm - start!!
                    if (distance > 0 && pendingLiters > 0) { totalDistance += distance; totalLiters += pendingLiters }
                    start = entry.odometerKm
                    pendingLiters = 0.0
                }
            }
        }
        return if (totalLiters > 0) totalDistance / totalLiters else null
    }
}
