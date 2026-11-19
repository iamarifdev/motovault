package com.motovault.app.domain.workshop

import com.motovault.app.domain.model.BangladeshServiceCatalogue
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.RepairCase
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val MAX_ODOMETER_KM = 9_999_999.0
private const val MAX_LINE_QTY = 10_000.0

/**
 * Updates a service visit in place while preserving its identity and metadata (`visitState`,
 * `workshopId`, `repairCaseId`, `warrantyIds`), allowing custom items and engine oil without
 * mandatory `EngineOilMetadata`.
 */
fun VaultState.updateServiceVisit(
    existingId: String,
    updatedLog: RideLog,
    visitState: VisitRecordState,
    caseTitle: String = "",
    warrantyMonths: Int = 0,
    workshopPhone: String = "",
    saveQuickServiceFn: VaultState.(RideLog, String?) -> VaultState,
    today: LocalDate = com.motovault.app.domain.model.today()
): VaultState {
    val currentRide = requireNotNull(ride)
    val existing = logs.firstOrNull {
        it.id == existingId && !it.isFuel && !it.isDocumentFee && it.kind != "expense"
    } ?: error("This service record is no longer available.")
    val parsedDate = validateUpdatedServiceVisit(updatedLog, today)
    val vehicleId = existing.vehicleId.ifBlank { currentRide.id }
    val (afterWorkshop, resolvedWorkshopId) = updateVisitWorkshop(this, existing, updatedLog, workshopPhone)
    val (afterCase, resolvedCaseId) = updateVisitRepairCase(
        afterWorkshop, existing.id, vehicleId, existing.repairCaseId, caseTitle, updatedLog.date
    )
    val baseLog = updatedLog.copy(
        id = existing.id,
        kind = "service",
        vehicleId = vehicleId,
        visitState = visitState.code,
        workshopId = resolvedWorkshopId,
        repairCaseId = resolvedCaseId,
        notes = updatedLog.notes.ifBlank { existing.notes },
        location = updatedLog.location ?: existing.location,
        receiptBase64 = updatedLog.receiptBase64 ?: existing.receiptBase64,
        serviceMiscellaneousPoisha = updatedLog.serviceMiscellaneousPoisha ?: existing.serviceMiscellaneousPoisha
    )
    val (afterWarranties, resolvedWarrantyIds) = updateVisitWarranties(
        afterCase, existing, baseLog, visitState, warrantyMonths, parsedDate, resolvedWorkshopId
    )
    val finalLog = baseLog.copy(warrantyIds = resolvedWarrantyIds)
    val updatedPrices = mergeLastItemPrices(afterWarranties.lastItemPrices, finalLog)
    val wasPaid = existing.visitState.isBlank() || existing.visitState == VisitRecordState.PAID.code

    return when {
        visitState != VisitRecordState.PAID -> afterWarranties.copy(
            logs = afterWarranties.logs.map { if (it.id == existingId) finalLog else it },
            lastItemPrices = updatedPrices
        )
        !wasPaid -> {
            val withoutExisting = afterWarranties.copy(logs = afterWarranties.logs.filterNot { it.id == existingId })
            val preferredRuleId = existing.scheduleIds.firstOrNull() ?: existing.componentId
            val promoted = withoutExisting.saveQuickServiceFn(finalLog, preferredRuleId)
            val savedLog = promoted.logs.first()
            val restoredOrder = afterWarranties.logs.map { if (it.id == existingId) savedLog else it }
            promoted.copy(logs = restoredOrder, lastItemPrices = updatedPrices)
        }
        else -> applyEditedPaidBaselines(afterWarranties, existing, finalLog, parsedDate, updatedPrices)
    }
}

private fun validateUpdatedServiceVisit(updatedLog: RideLog, today: LocalDate): LocalDate {
    val parsedDate = LocalDate.parse(updatedLog.date)
    require(parsedDate <= today && updatedLog.odometer.isFinite() && updatedLog.odometer in 0.0..MAX_ODOMETER_KM)
    require(updatedLog.serviceLines.isNotEmpty() && updatedLog.cost.isFinite() && updatedLog.cost >= 0.0)
    require(listOfNotNull(
        updatedLog.serviceLabourPoisha,
        updatedLog.serviceMiscellaneousPoisha,
        updatedLog.serviceDiscountPoisha
    ).all { it >= 0 })
    updatedLog.serviceLines.forEach { line ->
        require(line.catalogueId == null || BangladeshServiceCatalogue.find(line.catalogueId) != null)
        require(line.catalogueId != null || line.customName.isNotBlank())
        require(line.quantity.isFinite() && line.quantity > 0.0 && line.quantity <= MAX_LINE_QTY)
        require(line.unitPricePoisha == null || line.unitPricePoisha >= 0)
        require(line.unitPricePoisha == null || line.lineTotalPoisha != null)
    }
    return parsedDate
}

private fun updateVisitWorkshop(
    state: VaultState,
    existing: RideLog,
    updatedLog: RideLog,
    workshopPhone: String
): Pair<VaultState, String?> {
    val cleanWorkshop = updatedLog.workshop.trim()
    if (cleanWorkshop.isBlank()) return state to null
    val reusableId = existing.workshopId.takeIf { existing.workshop.trim().equals(cleanWorkshop, ignoreCase = true) }
    val (nextState, contact) = state.upsertWorkshopContact(cleanWorkshop, workshopPhone, existingId = reusableId)
    return nextState to contact?.id
}

private fun updateVisitRepairCase(
    state: VaultState,
    visitId: String,
    vehicleId: String,
    existingCaseId: String?,
    caseTitle: String,
    date: String
): Pair<VaultState, String?> {
    val cleanedTitle = caseTitle.trim()
    val currentLinked = existingCaseId?.let { id -> state.repairCases.firstOrNull { it.id == id } }
    if (cleanedTitle.isBlank()) {
        val detached = detachVisitFromCase(state, visitId, existingCaseId)
        return detached to null
    }
    if (currentLinked != null && currentLinked.title.equals(cleanedTitle, ignoreCase = true)) {
        val updated = if (visitId in currentLinked.visitLogIds) currentLinked
        else currentLinked.copy(visitLogIds = (currentLinked.visitLogIds + visitId).distinct())
        return state.copy(repairCases = state.repairCases.map { if (it.id == updated.id) updated else it }) to updated.id
    }
    val detached = detachVisitFromCase(state, visitId, existingCaseId)
    val matching = detached.repairCases.firstOrNull { it.title.equals(cleanedTitle, ignoreCase = true) }
    if (matching != null) {
        val updated = matching.copy(visitLogIds = (matching.visitLogIds + visitId).distinct())
        return detached.copy(repairCases = detached.repairCases.map { if (it.id == updated.id) updated else it }) to updated.id
    }
    val created = RepairCase(vehicleId = vehicleId, title = cleanedTitle, createdDate = date, visitLogIds = listOf(visitId))
    return detached.copy(repairCases = listOf(created) + detached.repairCases) to created.id
}

private fun detachVisitFromCase(state: VaultState, visitId: String, caseId: String?): VaultState {
    if (caseId == null) return state
    return state.copy(
        repairCases = state.repairCases.map { rc ->
            if (rc.id == caseId) rc.copy(visitLogIds = rc.visitLogIds.filterNot { it == visitId }) else rc
        }
    )
}

private fun updateVisitWarranties(
    state: VaultState,
    existing: RideLog,
    baseLog: RideLog,
    visitState: VisitRecordState,
    warrantyMonths: Int,
    parsedDate: LocalDate,
    workshopId: String?
): Pair<VaultState, List<String>> {
    if (warrantyMonths <= 0) {
        return state.copy(warranties = state.warranties.filterNot { it.visitLogId == existing.id }) to emptyList()
    }
    if (visitState != VisitRecordState.PAID) {
        return state to existing.warrantyIds
    }
    val expiryDate = parsedDate.plus(DatePeriod(months = warrantyMonths)).toString()
    val expectedParts = baseLog.serviceLines.map { it.catalogueId to it.displayName }
    val existingForVisit = state.warranties.filter { it.visitLogId == existing.id }
    val unchanged = existingForVisit.isNotEmpty() &&
        existingForVisit.size == expectedParts.size &&
        existingForVisit.map { it.catalogueId to it.partName } == expectedParts &&
        existingForVisit.all {
            it.startDate == baseLog.date &&
                it.expiryDate == expiryDate &&
                it.startOdometerKm == baseLog.odometer &&
                it.workshopId == workshopId
        }
    if (unchanged) {
        return state to existing.warrantyIds.ifEmpty { existingForVisit.map { it.id } }
    }
    val created = expectedParts.map { (catalogueId, partName) ->
        PartWarranty(
            vehicleId = baseLog.vehicleId,
            visitLogId = baseLog.id,
            catalogueId = catalogueId,
            partName = partName,
            startDate = baseLog.date,
            expiryDate = expiryDate,
            startOdometerKm = baseLog.odometer,
            workshopId = workshopId
        )
    }
    val remaining = state.warranties.filterNot { it.visitLogId == existing.id }
    return state.copy(warranties = created + remaining) to created.map { it.id }
}

private fun applyEditedPaidBaselines(
    state: VaultState,
    existing: RideLog,
    finalLog: RideLog,
    parsedDate: LocalDate,
    updatedPrices: Map<String, Double>
): VaultState {
    val existingDay = runCatching { LocalDate.parse(existing.date).toEpochDays().toLong() }.getOrNull()
    val updatedDay = parsedDate.toEpochDays().toLong()
    val preferredIds = existing.scheduleIds.ifEmpty { listOfNotNull(existing.componentId) }
    val matchedRules = finalLog.serviceLines.mapNotNull { line ->
        if (line.action !in setOf("replace", "service")) return@mapNotNull null
        val catId = line.catalogueId ?: return@mapNotNull null
        val candidates = state.rules.filter { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, catId) }
        candidates.firstOrNull { it.id in preferredIds } ?: candidates.singleOrNull()
    }.distinctBy { it.id }

    val resetIds = matchedRules.filter { rule ->
        val setByExisting = existingDay != null &&
            rule.lastServicedOdometerKm == existing.odometer &&
            rule.lastServicedEpochDays == existingDay
        val notSuperseded = rule.baselineCertainty != BaselineCertainty.CONFIRMED ||
            (rule.lastServicedOdometerKm <= finalLog.odometer && rule.lastServicedEpochDays <= updatedDay)
        setByExisting || notSuperseded
    }.map { it.id }.toSet()

    val nextRules = if (resetIds.isEmpty()) state.rules else {
        LogMaintenanceUseCase().recordService(state.rules, resetIds, finalLog.odometer, updatedDay).updatedRules
    }
    val updatedLines = finalLog.serviceLines.map { line ->
        val matched = line.catalogueId?.let { catId ->
            matchedRules.firstOrNull { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, catId) }
        }
        line.copy(scheduleId = matched?.id, resetsScheduleBaseline = matched?.id in resetIds)
    }
    val ids = updatedLines.mapNotNull { it.scheduleId }.distinct()
    val savedLog = finalLog.copy(scheduleIds = ids, componentId = ids.firstOrNull(), serviceLines = updatedLines)
    val nextRide = requireNotNull(state.ride).let { ride ->
        if (finalLog.odometer >= ride.odometer) ride.confirmedMileage(finalLog.odometer, parsedDate) else ride
    }
    return state.copy(
        ride = nextRide,
        rules = nextRules,
        logs = state.logs.map { if (it.id == existing.id) savedLog else it },
        lastItemPrices = updatedPrices
    )
}

private fun mergeLastItemPrices(current: Map<String, Double>, log: RideLog): Map<String, Double> {
    val updated = current.toMutableMap()
    log.serviceLines.forEach { line ->
        val catId = line.catalogueId
        val unitPoisha = line.unitPricePoisha
        if (catId != null && unitPoisha != null) {
            updated[catId] = unitPoisha / 100.0
        }
    }
    return updated
}
