package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.WearHealthStatus

data class ComponentWearCalculation(
    val componentId: String,
    val componentName: String,
    val wearPercentage: Int,
    val healthStatus: WearHealthStatus,
    val remainingDistanceKm: Double,
    val remainingDays: Int,
    val isDistanceExceeded: Boolean,
    val isTimeExceeded: Boolean
)

class CalculateWearUseCase {

    /**
     * Calculates wear percentage and health status using the dual-threshold rule:
     * Wear % = max( deltaDistance / maxDistance, deltaDays / maxDays ) * 100%
     *
     * Handles edge cases:
     * - Odometer rollback or typo (delta < 0 coerced to 0)
     * - Zero intervals (avoids division by zero)
     * - Cap percentage at realistic bounds (allows > 100% to represent overdue status)
     */
    fun execute(
        rule: ComponentRule,
        currentOdometerKm: Double,
        currentEpochDays: Long
    ): ComponentWearCalculation {
        val deltaDistance = (currentOdometerKm - rule.lastServicedOdometerKm).coerceAtLeast(0.0)
        val deltaDays = (currentEpochDays - rule.lastServicedEpochDays).coerceAtLeast(0L).toDouble()

        val distanceRatio = if (rule.intervalKm > 0) deltaDistance / rule.intervalKm else 0.0
        val timeRatio = if (rule.intervalDays > 0) deltaDays / rule.intervalDays else 0.0

        val maxRatio = maxOf(distanceRatio, timeRatio)
        val wearPercentage = (maxRatio * 100).toInt().coerceAtLeast(0)

        val healthStatus = when {
            wearPercentage >= 100 -> WearHealthStatus.OVERDUE
            wearPercentage >= 75 -> WearHealthStatus.SERVICE_SOON
            else -> WearHealthStatus.HEALTHY
        }

        val remainingDistance = (rule.intervalKm - deltaDistance).coerceAtLeast(0.0)
        val remainingDays = (rule.intervalDays - deltaDays.toInt()).coerceAtLeast(0)

        return ComponentWearCalculation(
            componentId = rule.id,
            componentName = rule.name,
            wearPercentage = wearPercentage,
            healthStatus = healthStatus,
            remainingDistanceKm = remainingDistance,
            remainingDays = remainingDays,
            isDistanceExceeded = deltaDistance >= rule.intervalKm && rule.intervalKm > 0,
            isTimeExceeded = deltaDays >= rule.intervalDays && rule.intervalDays > 0
        )
    }

    fun executeAll(
        rules: List<ComponentRule>,
        currentOdometerKm: Double,
        currentEpochDays: Long
    ): List<ComponentWearCalculation> {
        return rules.map { execute(it, currentOdometerKm, currentEpochDays) }
    }
}
