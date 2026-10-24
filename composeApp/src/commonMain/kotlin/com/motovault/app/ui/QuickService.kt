package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import kotlinx.datetime.LocalDate

/** v3's reviewed visit establishes only unambiguous matching schedule baselines. */
internal fun VaultState.saveQuickService(log: RideLog, selectedRuleId: String?): VaultState {
    val ride = requireNotNull(ride)
    val date = LocalDate.parse(log.date)
    require(date <= today() && log.odometer.isFinite() && log.odometer in 0.0..9999999.0)
    require(log.serviceLines.isNotEmpty() && log.cost.isFinite() && log.cost >= 0)
    require(logs.none { it.id == log.id })
    val linked = log.serviceLines.mapNotNull { line ->
        if (line.action !in setOf("replace", "service")) return@mapNotNull null
        val candidates = rules.filter { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, line.catalogueId.orEmpty()) }
        candidates.firstOrNull { it.id == selectedRuleId } ?: candidates.singleOrNull()
    }.distinctBy { it.id }
    val confirmed = confirmedRules().associateBy { it.id }
    linked.forEach { rule ->
        if (confirmed[rule.id]?.baselineCertainty == BaselineCertainty.CONFIRMED) {
            require(log.odometer >= rule.lastServicedOdometerKm && date.toEpochDays() >= rule.lastServicedEpochDays) {
                "সার্ভিসের কিমি ও তারিখ আগের নিশ্চিত সার্ভিসের পরে দিন।"
            }
        }
    }
    val ids = linked.map { it.id }.toSet()
    val nextRules = LogMaintenanceUseCase().recordService(rules, ids, log.odometer, date.toEpochDays().toLong()).updatedRules
    val lines = log.serviceLines.map { line ->
        val rule = linked.firstOrNull { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, line.catalogueId.orEmpty()) }
        line.copy(scheduleId = rule?.id, resetsScheduleBaseline = rule != null)
    }
    return copy(ride = ride.confirmedMileage(log.odometer, date), rules = nextRules,
        logs = listOf(log.copy(scheduleIds = ids.toList(), componentId = ids.firstOrNull(), serviceLines = lines)) + logs)
}
