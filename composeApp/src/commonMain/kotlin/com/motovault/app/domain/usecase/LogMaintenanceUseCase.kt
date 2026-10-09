package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.ComponentRule

data class MaintenanceLogResult(
    val updatedRules: List<ComponentRule>,
    val servicedComponentIds: Set<String>,
    val resetCount: Int
)

class LogMaintenanceUseCase {

    /**
     * Resets wear gauges for components serviced during a maintenance event.
     */
    fun recordService(
        existingRules: List<ComponentRule>,
        servicedComponentIds: Set<String>,
        serviceOdometerKm: Double,
        serviceEpochDays: Long
    ): MaintenanceLogResult {
        val updated = existingRules.map { rule ->
            if (rule.id in servicedComponentIds) {
                rule.copy(
                    lastServicedOdometerKm = serviceOdometerKm,
                    lastServicedEpochDays = serviceEpochDays
                )
            } else {
                rule
            }
        }
        val resetCount = updated.count { it.id in servicedComponentIds }
        return MaintenanceLogResult(
            updatedRules = updated,
            servicedComponentIds = servicedComponentIds,
            resetCount = resetCount
        )
    }

    /**
     * 1-Tap Quick Action: Resets only the Drive Chain wear gauge to 0 km elapsed.
     */
    fun resetChainLube(
        existingRules: List<ComponentRule>,
        currentOdometerKm: Double,
        currentEpochDays: Long
    ): MaintenanceLogResult {
        val chainRule = existingRules.find { it.name.contains("Chain", ignoreCase = true) }
        return if (chainRule != null) {
            recordService(
                existingRules = existingRules,
                servicedComponentIds = setOf(chainRule.id),
                serviceOdometerKm = currentOdometerKm,
                serviceEpochDays = currentEpochDays
            )
        } else {
            MaintenanceLogResult(
                updatedRules = existingRules,
                servicedComponentIds = emptySet(),
                resetCount = 0
            )
        }
    }
}
