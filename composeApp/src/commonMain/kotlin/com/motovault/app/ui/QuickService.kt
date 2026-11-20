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
        val catId = line.catalogueId ?: return@mapNotNull null
        val candidates = rules.filter { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, catId) }
        candidates.firstOrNull { it.id == selectedRuleId } ?: candidates.singleOrNull()
    }.distinctBy { it.id }
    val confirmed = confirmedRules().associateBy { it.id }
    // An older service is still saved (v4); it just does not move a newer confirmed reminder baseline.
    val resettable = linked.filterNot { rule ->
        confirmed[rule.id]?.baselineCertainty == BaselineCertainty.CONFIRMED &&
            (log.odometer < rule.lastServicedOdometerKm || date.toEpochDays() < rule.lastServicedEpochDays)
    }
    val ids = resettable.map { it.id }.toSet()
    val nextRules = LogMaintenanceUseCase().recordService(rules, ids, log.odometer, date.toEpochDays().toLong()).updatedRules
    val lines = log.serviceLines.map { line ->
        val rule = line.catalogueId?.let { catId ->
            resettable.firstOrNull { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, catId) }
        }
        line.copy(scheduleId = rule?.id, resetsScheduleBaseline = rule != null)
    }
    val nextRide = if (log.odometer >= ride.odometer) ride.confirmedMileage(log.odometer, date) else ride
    return copy(ride = nextRide, rules = nextRules,
        logs = listOf(log.copy(scheduleIds = ids.toList(), componentId = ids.firstOrNull(), serviceLines = lines)) + logs)
}
