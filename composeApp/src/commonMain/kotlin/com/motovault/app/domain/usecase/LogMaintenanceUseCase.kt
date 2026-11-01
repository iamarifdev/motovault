package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.BaselineCertainty

data class MaintenanceLogResult(
    val updatedRules: List<ComponentRule>,
    val servicedComponentIds: Set<String>,
    val resetCount: Int
)

class LogMaintenanceUseCase {

    /**
     * Resets only the schedules explicitly performed during this visit. A
     * one-time running-in schedule is removed after it is recorded; the ledger
     * entry keeps the history and the recurring schedules stay untouched.
     */
    fun recordService(
        existingRules: List<ComponentRule>,
        servicedComponentIds: Set<String>,
        serviceOdometerKm: Double,
        serviceEpochDays: Long
    ): MaintenanceLogResult {
        val startedRecurring = existingRules.filter { it.oneTime && it.id in servicedComponentIds }.flatMap { oneTime ->
            existingRules.filter { !it.oneTime && it.action == oneTime.action && it.catalogueId == oneTime.catalogueId }
                .map { it.id }
        }.toSet()
        val resetIds = servicedComponentIds + startedRecurring
        val updated = existingRules.mapNotNull { rule ->
            when {
                rule.id !in resetIds -> rule
                rule.oneTime -> null
                else -> rule.copy(
                    lastServicedOdometerKm = serviceOdometerKm,
                    lastServicedEpochDays = serviceEpochDays,
                    baselineCertainty = BaselineCertainty.CONFIRMED,
                    reminderSnoozedUntil = null
                )
            }
        }
        return MaintenanceLogResult(
            updatedRules = updated,
            servicedComponentIds = resetIds,
            resetCount = existingRules.count { it.id in resetIds }
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
        val chainRule = existingRules.firstOrNull {
            it.action == com.motovault.app.domain.maintenance.MaintenanceAction.LUBRICATE &&
                it.name.contains("chain", ignoreCase = true)
        } ?: existingRules.find { it.name.contains("Chain", ignoreCase = true) }
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
