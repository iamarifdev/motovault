package com.motovault.app.domain.workshop

import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.RepairCase
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.model.WorkshopContact
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Preserves explicit contacts (including duplicate names) and appends unmigrated free-text workshop names. */
fun VaultState.migratedWorkshopContacts(): List<WorkshopContact> {
    val existingNames = workshopContacts.map { it.name.trim().lowercase() }.toSet()
    val logNames = (logs + garageBikes.flatMap { it.logs }).map { it.workshop.trim() }.filter { it.isNotBlank() }
    val legacyNames = (workshops + logNames).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val migrated = legacyNames
        .filterNot { it.lowercase() in existingNames }
        .map { name -> WorkshopContact(id = "ws-${name.lowercase().hashCode()}", name = name) }
    return workshopContacts + migrated
}

fun VaultState.upsertWorkshopContact(
    name: String,
    phone: String = "",
    existingId: String? = null
): Pair<VaultState, WorkshopContact?> {
    val cleanedName = name.trim()
    val cleanedPhone = phone.trim()
    if (cleanedName.isBlank()) return this to null
    val current = migratedWorkshopContacts()
    val target = existingId?.let { id -> current.firstOrNull { it.id == id } }
        ?: if (cleanedPhone.isNotBlank()) current.firstOrNull { it.name.equals(cleanedName, ignoreCase = true) && it.phone == cleanedPhone }
        else current.firstOrNull { it.name.equals(cleanedName, ignoreCase = true) }
    val resolved = target?.copy(
        name = cleanedName,
        phone = cleanedPhone.ifBlank { target.phone }
    ) ?: WorkshopContact(name = cleanedName, phone = cleanedPhone)
    val nextContacts = if (target != null) {
        current.map { if (it.id == resolved.id) resolved else it }
    } else {
        current + resolved
    }
    val nextWorkshops = (listOf(cleanedName) + workshops.filterNot { it == cleanedName }).take(6)
    return copy(workshopContacts = nextContacts, workshops = nextWorkshops) to resolved
}

/** Records a paid service visit or non-paid quotation/approved visit with optional repair case and warranties. */
fun VaultState.recordWorkshopVisit(
    log: RideLog,
    selectedRuleId: String?,
    visitState: VisitRecordState = VisitRecordState.PAID,
    caseTitle: String = "",
    existingCaseId: String? = null,
    warrantyMonths: Int = 0,
    warrantyKm: Double? = null,
    workshopPhone: String = "",
    saveQuickServiceFn: VaultState.(RideLog, String?) -> VaultState
): VaultState {
    val vehicleId = requireNotNull(ride).id
    val (afterWorkshop, contact) = upsertWorkshopContact(log.workshop, workshopPhone)
    val baseLog = log.copy(
        vehicleId = vehicleId,
        visitState = visitState.code,
        workshopId = contact?.id
    )
    val afterCase = linkRepairCase(afterWorkshop, baseLog.id, vehicleId, caseTitle, existingCaseId, log.date)
    val linkedLog = baseLog.copy(repairCaseId = afterCase.second)
    val afterWarranties = if (visitState == VisitRecordState.PAID && warrantyMonths > 0) {
        attachWarranties(afterCase.first, linkedLog, warrantyMonths, warrantyKm)
    } else {
        afterCase.first to linkedLog
    }
    val stateWithMeta = afterWarranties.first
    val finalLog = afterWarranties.second
    return if (visitState == VisitRecordState.PAID) {
        stateWithMeta.saveQuickServiceFn(finalLog, selectedRuleId)
    } else {
        stateWithMeta.copy(logs = listOf(finalLog) + stateWithMeta.logs)
    }
}

private fun linkRepairCase(
    state: VaultState,
    logId: String,
    vehicleId: String,
    caseTitle: String,
    existingCaseId: String?,
    date: String
): Pair<VaultState, String?> {
    val cleanedTitle = caseTitle.trim()
    val existing = existingCaseId?.let { id -> state.repairCases.firstOrNull { it.id == id } }
        ?: cleanedTitle.takeIf { it.isNotBlank() }?.let { t -> state.repairCases.firstOrNull { it.title.equals(t, ignoreCase = true) } }
    if (existing != null) {
        val updated = existing.copy(visitLogIds = (existing.visitLogIds + logId).distinct())
        return state.copy(repairCases = state.repairCases.map { if (it.id == updated.id) updated else it }) to updated.id
    }
    if (cleanedTitle.isBlank()) return state to null
    val created = RepairCase(vehicleId = vehicleId, title = cleanedTitle, createdDate = date, visitLogIds = listOf(logId))
    return state.copy(repairCases = listOf(created) + state.repairCases) to created.id
}

private fun attachWarranties(
    state: VaultState,
    log: RideLog,
    warrantyMonths: Int,
    warrantyKm: Double?
): Pair<VaultState, RideLog> {
    val startDate = LocalDate.parse(log.date)
    val expiryDate = startDate.plus(DatePeriod(months = warrantyMonths)).toString()
    val parts = if (log.serviceLines.isNotEmpty()) {
        log.serviceLines.map { it.catalogueId to it.displayName }
    } else {
        listOf(null to log.detail.ifBlank { log.title })
    }
    val created = parts.map { (catalogueId, name) ->
        PartWarranty(
            vehicleId = log.vehicleId,
            visitLogId = log.id,
            catalogueId = catalogueId,
            partName = name,
            startDate = log.date,
            expiryDate = expiryDate,
            mileageLimitKm = warrantyKm,
            startOdometerKm = log.odometer,
            workshopId = log.workshopId
        )
    }
    val nextLog = log.copy(warrantyIds = created.map { it.id })
    return state.copy(warranties = created + state.warranties) to nextLog
}

/** Transitions a quotation or approved visit to PAID (updating service baselines) or CANCELLED. */
fun VaultState.transitionVisitState(
    logId: String,
    nextState: VisitRecordState,
    saveQuickServiceFn: VaultState.(RideLog, String?) -> VaultState
): VaultState {
    val existing = logs.firstOrNull { it.id == logId } ?: return this
    if (existing.visitState == nextState.code) return this
    if (nextState == VisitRecordState.PAID) {
        val withoutDraft = copy(logs = logs.filterNot { it.id == logId })
        val paidLog = existing.copy(visitState = VisitRecordState.PAID.code)
        return withoutDraft.saveQuickServiceFn(paidLog, existing.scheduleIds.firstOrNull() ?: existing.componentId)
    }
    val updated = logs.map { if (it.id == logId) it.copy(visitState = nextState.code) else it }
    return copy(logs = updated)
}
