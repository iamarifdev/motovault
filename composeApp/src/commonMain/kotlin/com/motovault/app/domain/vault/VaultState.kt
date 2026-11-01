package com.motovault.app.domain.vault

import com.motovault.app.domain.model.*
import com.motovault.app.domain.usecase.DocumentExpiryUseCase
import com.motovault.app.domain.usecase.FuelEfficiencyUseCase
import com.motovault.app.domain.usecase.FuelFill
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class VaultState(
    val ride: Ride? = null,
    val rules: List<ComponentRule> = emptyList(),
    val logs: List<RideLog> = emptyList(),
    val documents: List<VaultDocument> = emptyList(),
    val lastFuelOdometer: Double? = null,
    val economy: Double? = null,
    val appearance: Appearance = Appearance.SYSTEM,
    val autoLocation: Boolean = false,
    val language: AppLanguage = AppLanguage.BN,
    val digits: Digits = Digits.BN,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val textScale: Float = 1.0f,
    val brandOverride: String? = null,
    val serviceReminders: Boolean = true,
    val docReminders: Boolean = true,
    val lastPrices: Map<String, Double> = FuelDefaults.legacyPrices(),
    val lastItemPrices: Map<String, Double> = emptyMap(),
    val workshops: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val reminderPolicy: ReminderPolicy = ReminderPolicy(),
    val privacy: com.motovault.app.domain.privacy.VaultPrivacySettings = com.motovault.app.domain.privacy.VaultPrivacySettings(),
    val lastBackupEpochMillis: Long? = null
) {
    /** Only linked, matching recorded service can establish certainty for an older schedule. */
    fun confirmedRules() = rules.map { rule ->
        if (rule.baselineCertainty != com.motovault.app.domain.model.BaselineCertainty.UNKNOWN) rule
        else if (logs.any { log -> !log.isFuel && !log.isDocumentFee &&
            (rule.id in log.scheduleIds || log.componentId == rule.id) &&
            log.odometer == rule.lastServicedOdometerKm &&
            runCatching { LocalDate.parse(log.date).toEpochDays().toLong() }.getOrNull() == rule.lastServicedEpochDays
        }) rule.copy(baselineCertainty = com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) else rule
    }

    fun logFuel(odometer: Double, liters: Double, cost: Double = 0.0, date: LocalDate = today(), fullTank: Boolean = true, location: GeoCapture? = null, locationLabel: String = ""): VaultState {
        val currentRide = requireNotNull(ride)
        require(odometer.isFinite() && odometer in 0.0..9999999.0) { "Enter valid mileage." }
        require(liters.isFinite() && liters > 0 && liters <= 100) { "Enter litres between 0 and 100." }
        require(cost.isFinite() && cost >= 0) { "Cost cannot be negative." }
        require(date <= today()) { "The fill-up date cannot be in the future." }
        require(location == null || location.isValid()) { "The captured location is invalid." }
        val fuel = logs.filter { it.isFuel }
        require(fuel.none { it.odometer >= odometer }) { "Use mileage greater than your latest fill-up." }
        require(fuel.none { LocalDate.parse(it.date) > date }) { "Use a date at or after your latest fill-up." }
        val updatedLogs = listOf(RideLog("Fuel fill-up", "$liters L · " + if (fullTank) "Full tank" else "Partial fill",
            date.toString(), odometer, kind = "fuel", liters = liters, fullTank = fullTank, cost = cost, location = location, locationLabel = locationLabel.trim())) + logs
        return copy(ride = currentRide.confirmedMileage(odometer, date),
            lastFuelOdometer = odometer, economy = calculateEconomy(updatedLogs), logs = updatedLogs)
    }

    fun replaceFuelLog(existingId: String, odometer: Double, liters: Double, cost: Double, date: LocalDate,
        fullTank: Boolean, location: GeoCapture?, locationLabel: String): VaultState {
        val existing = logs.firstOrNull { it.id == existingId && it.isFuel } ?: error("This fuel record is no longer available.")
        require(odometer.isFinite() && odometer in 0.0..9999999.0) { "Enter valid mileage." }
        require(liters.isFinite() && liters > 0 && liters <= 100) { "Enter litres between 0 and 100." }
        require(cost.isFinite() && cost >= 0) { "Cost cannot be negative." }
        require(date <= today()) { "The fill-up date cannot be in the future." }
        require(location == null || location.isValid()) { "The captured location is invalid." }
        val replacement = existing.copy(title = "Fuel fill-up", detail = "$liters L · " + if (fullTank) "Full tank" else "Partial fill",
            date = date.toString(), odometer = odometer, kind = "fuel", liters = liters, fullTank = fullTank,
            cost = cost, location = location, locationLabel = locationLabel.trim())
        val updatedLogs = logs.map { if (it.id == existingId) replacement else it }
        val ordered = updatedLogs.filter { it.isFuel }.sortedBy { it.odometer }
        require(ordered.zipWithNext().all { (previous, next) ->
            next.odometer > previous.odometer && LocalDate.parse(next.date) >= LocalDate.parse(previous.date)
        }) { "Fuel mileage and dates must stay in order with the other fill-ups." }
        return copy(ride = requireNotNull(ride).confirmedMileage(odometer, date), logs = updatedLogs,
            economy = calculateEconomy(updatedLogs), lastFuelOdometer = ordered.lastOrNull()?.odometer)
    }

    fun logService(rule: ComponentRule, odometer: Double = requireNotNull(ride).odometer,
        date: LocalDate = today(), cost: Double = 0.0, workshop: String = "", notes: String = ""): VaultState {
        val currentRide = requireNotNull(ride)
        require(rules.any { it.id == rule.id }) { "Choose a component." }
        require(odometer.isFinite() && odometer in rule.lastServicedOdometerKm..currentRide.odometer) { "Service mileage must be at or after the baseline and no higher than your current reading." }
        require(date.toEpochDays().toLong() >= rule.lastServicedEpochDays && date <= today()) { "Service date must be at or after the baseline and not in the future." }
        require(cost.isFinite() && cost >= 0) { "Cost cannot be negative." }
        val result = LogMaintenanceUseCase().recordService(rules, setOf(rule.id), odometer, date.toEpochDays().toLong())
        return copy(rules = result.updatedRules, logs = listOf(RideLog(rule.name, "Service recorded", date.toString(), odometer,
            kind = "service", cost = cost, workshop = workshop.trim(), notes = notes.trim(), componentId = rule.id)) + logs)
    }

    /** Records one visit with several items. Schedule links must be explicitly attached to baseline-setting lines. */
    fun logServiceVisit(
        lines: List<ServiceLineItem>, odometer: Double = requireNotNull(ride).odometer,
        date: LocalDate = today(), labourPoisha: Int? = 0, miscellaneousPoisha: Int? = 0,
        discountPoisha: Int? = 0, workshop: String = "", notes: String = "", location: GeoCapture? = null,
        receiptBase64: String? = null
    ): VaultState {
        val currentRide = requireNotNull(ride)
        require(lines.isNotEmpty()) { "Add at least one service item." }
        require(odometer.isFinite() && odometer in 0.0..currentRide.odometer) { "Service mileage must be no higher than your current reading." }
        require(date <= today()) { "Service date cannot be in the future." }
        require(location == null || location.isValid()) { "The captured location is invalid." }
        require(receiptBase64 == null || receiptBase64.length <= 1_400_000) { "Receipt image must be 1 MB or smaller." }
        require(listOfNotNull(labourPoisha, miscellaneousPoisha, discountPoisha).all { it >= 0 }) { "Amounts cannot be negative." }
        lines.forEach { line ->
            require(line.catalogueId == null || BangladeshServiceCatalogue.find(line.catalogueId) != null) { "Choose a listed item or enter a custom item." }
            require(line.catalogueId != null || line.customName.isNotBlank()) { "Enter an item name." }
            require(line.quantity.isFinite() && line.quantity > 0 && line.quantity <= 10000) { "Enter a valid quantity." }
            require(line.unitPricePoisha == null || line.unitPricePoisha >= 0) { "Item price cannot be negative." }
            require(line.unitPricePoisha == null || line.lineTotalPoisha != null) { "This quantity and price are too large to calculate safely." }
            if (line.catalogueId == "engine_oil") require(line.oil?.isValid() == true) { "Choose an oil type and purchase basis, then check pack and quantity values." }
            else require(line.oil == null) { "Oil details can only be attached to an engine-oil item." }
            val linkedAction = com.motovault.app.domain.maintenance.MaintenanceAction.fromCode(line.action)
            require(!line.resetsScheduleBaseline || linkedAction != null) { "Choose a maintenance action before linking a schedule." }
            require(line.scheduleId == null || line.resetsScheduleBaseline) { "Link a schedule only when this item sets its baseline." }
        }
        val scheduleIds = lines.filter { it.resetsScheduleBaseline }.mapNotNull { it.scheduleId }.distinct()
        val linkedRules = rules.filter { it.id in scheduleIds }
        require(linkedRules.size == scheduleIds.size) { "One linked schedule no longer exists." }
        lines.filter { it.scheduleId != null }.forEach { line ->
            val linkedRule = linkedRules.first { it.id == line.scheduleId }
            require(line.catalogueId != null && BangladeshServiceCatalogue.scheduleMatchesItem(linkedRule.catalogueId, linkedRule.name, line.catalogueId)) {
                "Link a schedule to the matching service item."
            }
            require(com.motovault.app.domain.maintenance.MaintenanceAction.fromCode(line.action) == linkedRule.action) {
                "The linked schedule must use the same action as this service line."
            }
        }
        linkedRules.forEach { rule ->
            require(odometer >= rule.lastServicedOdometerKm && date.toEpochDays().toLong() >= rule.lastServicedEpochDays) {
                "Service mileage and date must be at or after every linked schedule baseline."
            }
        }
        val updatedRules = if (scheduleIds.isEmpty()) rules else
            LogMaintenanceUseCase().recordService(rules, scheduleIds.toSet(), odometer, date.toEpochDays().toLong()).updatedRules
        val total = serviceTotalPoisha(lines, labourPoisha, miscellaneousPoisha, discountPoisha)
        if (total == null && lines.all { it.lineTotalPoisha != null } && labourPoisha != null && miscellaneousPoisha != null && discountPoisha != null) {
            val beforeDiscount = lines.sumOf { it.lineTotalPoisha!!.toLong() } + labourPoisha + miscellaneousPoisha
            require(discountPoisha.toLong() <= beforeDiscount) { "Discount cannot be greater than the service amount." }
        }
        val title = lines.first().displayNameBn.let { first -> if (lines.size == 1) first else "$first +${lines.size - 1}" }
        val log = RideLog(
            title = title, detail = "পরিষেবার রেকর্ড", date = date.toString(), odometer = odometer,
            kind = "service", cost = total?.div(100.0) ?: 0.0, workshop = workshop.trim(), notes = notes.trim(),
            componentId = scheduleIds.firstOrNull(), serviceLines = lines, serviceLabourPoisha = labourPoisha,
            serviceMiscellaneousPoisha = miscellaneousPoisha, serviceDiscountPoisha = discountPoisha,
            serviceTotalPoisha = total, scheduleIds = scheduleIds, location = location, receiptBase64 = receiptBase64
        )
        return copy(rules = updatedRules, logs = listOf(log) + logs)
    }

    /** Replace an itemized service visit atomically while keeping its stable history ID. */
    fun replaceServiceVisit(
        existingId: String, lines: List<ServiceLineItem>, odometer: Double, date: LocalDate,
        labourPoisha: Int?, miscellaneousPoisha: Int?, discountPoisha: Int?, workshop: String,
        notes: String, location: GeoCapture?, receiptBase64: String?
    ): VaultState {
        val existing = logs.firstOrNull { it.id == existingId && !it.isFuel && it.serviceLines.isNotEmpty() }
            ?: error("This service record is no longer available.")
        val proposedSchedules = lines.mapNotNull { it.scheduleId }.toSet()
        val removedSchedules = existing.scheduleIds.filter { it !in proposedSchedules }
        val existingDay = LocalDate.parse(existing.date).toEpochDays().toLong()
        require(rules.filter { it.id in removedSchedules }.none {
            it.lastServicedEpochDays == existingDay && it.lastServicedOdometerKm == existing.odometer
        }) { "This service sets the current reminder baseline. Keep its linked item or adjust the reminder first." }
        val supersededSchedules = existing.scheduleIds.filter { id ->
            rules.firstOrNull { it.id == id }?.let { it.lastServicedEpochDays > existingDay || it.lastServicedOdometerKm > existing.odometer } == true
        }
        require(supersededSchedules.isEmpty() || (date == LocalDate.parse(existing.date) && odometer == existing.odometer && proposedSchedules.containsAll(supersededSchedules))) {
            "An earlier service cannot move a newer reminder baseline. Keep its date, mileage, and linked reminder."
        }
        val withoutExisting = copy(logs = logs.filterNot { it.id == existingId })
        val validationLines = lines.map { if (it.scheduleId in supersededSchedules) it.copy(scheduleId = null, resetsScheduleBaseline = false) else it }
        val added = withoutExisting.logServiceVisit(validationLines, odometer, date, labourPoisha,
            miscellaneousPoisha, discountPoisha, workshop, notes, location, receiptBase64)
        val newScheduleIds = lines.filter { it.resetsScheduleBaseline }.mapNotNull { it.scheduleId }.distinct()
        val replacement = added.logs.first().copy(id = existingId, serviceLines = lines, scheduleIds = newScheduleIds, componentId = newScheduleIds.firstOrNull())
        return added.copy(logs = listOf(replacement) + added.logs.drop(1))
    }

    fun saveSchedule(rule: ComponentRule): VaultState {
        val currentRide = requireNotNull(ride)
        require(rule.name.isNotBlank()) { "Enter a component name." }
        require(rule.intervalKm.isFinite() && rule.intervalKm in 0.0..1000000.0 && rule.intervalDays in 0..10000 &&
            (rule.intervalKm > 0 || rule.intervalDays > 0)) { "Set a distance or time interval." }
        require(rule.lastServicedOdometerKm.isFinite() && rule.lastServicedOdometerKm in 0.0..currentRide.odometer) { "Check the service baseline mileage." }
        require(rule.lastServicedEpochDays in 0..today().toEpochDays().toLong()) { "Check the baseline date. It cannot be in the future." }
        val old = rules.firstOrNull { it.id == rule.id }
        val saved = if (old != null && old.reminderRevision() != rule.reminderRevision()) rule.copy(reminderSnoozedUntil = null) else rule
        return copy(rules = if (old != null) rules.map { if (it.id == rule.id) saved else it } else rules + saved)
    }

    fun deleteLog(log: RideLog): VaultState {
        if (!log.isFuel && !log.isDocumentFee) {
            val logEpochDay = LocalDate.parse(log.date).toEpochDays().toLong()
            val linkedBaseline = rules.filter { it.id in log.scheduleIds }.any {
                it.lastServicedOdometerKm == log.odometer && it.lastServicedEpochDays == logEpochDay
            }
            val rule = if (log.scheduleIds.isEmpty() && log.serviceLines.isEmpty()) rules.firstOrNull { it.id == log.componentId || (log.componentId == null && it.name == log.title) } else null
            require(!linkedBaseline && (rule == null || rule.lastServicedOdometerKm != log.odometer || rule.lastServicedEpochDays != logEpochDay)) {
                "This service sets the current baseline. Adjust its schedule baseline before deleting it."
            }
        }
        val remaining = logs.filterNot { it.id == log.id }
        return copy(logs = remaining, economy = calculateEconomy(remaining), lastFuelOdometer = remaining.filter { it.isFuel }.maxOfOrNull { it.odometer })
    }

    /** Keeps each renewal payment as one ledger event; document details never hold a mutable spend total. */
    fun logDocumentFee(documentTitle: String, amountPoisha: Int, date: LocalDate = today(), notes: String = ""): VaultState {
        require(documents.any { it.title == documentTitle }) { "Choose a saved document first." }
        require(amountPoisha >= 0) { "Cost cannot be negative." }
        require(date <= today()) { "Payment date cannot be in the future." }
        val log = RideLog(
            title = "${documentTitle} renewal", detail = "Document renewal fee", date = date.toString(),
            odometer = requireNotNull(ride).odometer, kind = "document_fee", cost = amountPoisha / 100.0,
            notes = notes.trim()
        )
        return copy(logs = listOf(log) + logs)
    }

    fun saveDocument(document: VaultDocument): VaultState {
        require(document.reminderWarningDays == null || document.reminderWarningDays in 1..365)
        document.reminderSnoozedUntil?.let { kotlinx.datetime.LocalDateTime.parse(it) }
        require(document.pages.size <= documentPageLimit(document.title)) { "Too many document pages." }
        document.pages.forEach { validateDocumentAssetId(it.originalId); validateDocumentAssetId(it.previewId); it.acceptedId?.let(::validateDocumentAssetId); require(it.width > 0 && it.height > 0) }
        document.originalPdf?.let { validateDocumentAssetId(it) }
        require(document.number.isNotBlank() && document.holder.isNotBlank()) { "Enter a document number and holder name." }
        val issued = document.issued.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val expiry = document.expiry.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        require(issued == null || expiry == null || expiry >= issued) { "Expiry must be on or after the issue date." }
        val old = documents.firstOrNull { it.id == document.id }
        val saved = if (old != null && old.expiry != document.expiry) document.copy(reminderSnoozedUntil = null) else document
        return copy(documents = if (old != null) documents.map { if (it.id == document.id) saved else it } else documents + saved)
    }


    companion object {
        private val json = Json { ignoreUnknownKeys = true }
fun decode(value: String): VaultState? = runCatching {
            val state = json.decodeFromString<VaultState>(value)
            state.copy(economy = calculateEconomy(state.logs), lastFuelOdometer = state.logs.filter { it.isFuel }.maxOfOrNull { it.odometer })
        }.getOrNull()
        fun encode(state: VaultState) = json.encodeToString(serializer(), state)

        fun calculateEconomy(logs: List<RideLog>): Double? = FuelEfficiencyUseCase().calculate(
            logs.filter { it.isFuel }.map { FuelFill(it.odometer, it.fuelLiters, it.fullTank) }
        )
    }
}
