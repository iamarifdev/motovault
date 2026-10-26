package com.motovault.app.ui

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.DOCUMENT_RENEWAL_WARNING_DAYS
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import com.motovault.app.domain.usecase.FuelEfficiencyUseCase
import com.motovault.app.domain.usecase.FuelFill
import com.motovault.app.domain.usecase.DocumentExpiryUseCase
import com.motovault.app.domain.model.DocumentExpiryStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

expect suspend fun loadVault(): String?
expect suspend fun saveVault(value: String)
typealias VaultSave = (VaultState, (Boolean) -> Unit) -> Unit

fun today() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun recordId() = Clock.System.now().toEpochMilliseconds().toString() + "-" + Random.nextLong().toString()
const val RENEWAL_WARNING_DAYS = DOCUMENT_RENEWAL_WARNING_DAYS

@Serializable
data class Ride(
    val brandId: String, val edition: String, val year: Int, val odometer: Double,
    val modelId: String? = null, val modelName: String = "", val registration: String = "",
    val market: RideMarket = RideMarket.UNKNOWN, val emission: RideEmission = RideEmission.UNKNOWN,
    val modelCode: String = "", val factoryProfileId: String? = null,
    val odometerConfirmedDate: String? = null
) {
    val selectedModel: BikeModel? get() = BikeCatalogue.find(modelId)?.takeIf { it.brandId == brandId }
        ?: if (modelName.isBlank()) BikeCatalogue.legacyModel(brandId) else null
    val displayName: String get() = modelName.ifBlank { selectedModel?.name ?: "Your motorcycle" }
    fun confirmedMileage(value: Double, date: LocalDate): Ride {
        require(value.isFinite() && value in 0.0..9999999.0 && date <= today())
        return if (value >= odometer) copy(odometer = value, odometerConfirmedDate = date.toString()) else this
    }
}

@Serializable
data class RideLog(
    val title: String, val detail: String, val date: String, val odometer: Double,
    val id: String = recordId(), val kind: String = "", val liters: Double? = null,
    val fullTank: Boolean = true, val cost: Double = 0.0,
    val workshop: String = "", val notes: String = "", val componentId: String? = null,
    val serviceLines: List<ServiceLineItem> = emptyList(),
    val serviceLabourPoisha: Int? = null, val serviceMiscellaneousPoisha: Int? = null,
    val serviceDiscountPoisha: Int? = null, val serviceTotalPoisha: Int? = null,
    val scheduleIds: List<String> = emptyList(), val location: GeoCapture? = null,
    val locationLabel: String = "", val receiptBase64: String? = null,
    val fuelType: String = "octane",
    val fuelPrice: Double? = null
) {
    val isFuel: Boolean get() = kind == "fuel" || title == "Fuel fill-up"
    val isDocumentFee: Boolean get() = kind == "document_fee"
    val fuelLiters: Double? get() = liters ?: if (isFuel) detail.substringBefore(" L").toDoubleOrNull() else null
}

@Serializable
data class VaultDocument(
    val title: String, val number: String, val holder: String, val expiry: String,
    val id: String = recordId(), val issued: String = "",
    val frontPhoto: String? = null, val backPhoto: String? = null,
    val pages: List<DocumentPage> = emptyList(), val originalPdf: String? = null,
    val pdfPageCount: Int = 0, val details: Map<String, String> = emptyMap(),
    val detailsConfirmed: Boolean = false,
    val extractionCandidates: Map<String, String> = emptyMap(), val capturedAt: String = "",
    val reminder: Boolean = false,
    val kind: String = "",
    val reminderWarningDays: Int? = null,
    val reminderSnoozedUntil: String? = null
) {
    val daysRemaining: Int? get() = expiry.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it).toEpochDays() - today().toEpochDays() }.getOrNull() }
    val expiryStatus: DocumentExpiryStatus get() = DocumentExpiryUseCase().evaluate(daysRemaining)
    val expiryLabel: String get() {
        val days = daysRemaining
        return when (DocumentExpiryUseCase().evaluate(days)) {
            DocumentExpiryStatus.NO_EXPIRY -> "No expiry"
            DocumentExpiryStatus.EXPIRED -> "Expired"
            DocumentExpiryStatus.EXPIRING_SOON -> "$days days"
            DocumentExpiryStatus.VALID -> "Valid"
        }
    }
}

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
    val digits: Digits = Digits.BN,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val textScale: Float = 1.0f,
    val brandOverride: String? = null,
    val serviceReminders: Boolean = true,
    val docReminders: Boolean = true,
    val lastPrices: Map<String, Double> = mapOf("octane" to 120.0, "petrol" to 115.0),
    val lastItemPrices: Map<String, Double> = emptyMap(),
    val workshops: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val reminderPolicy: ReminderPolicy = ReminderPolicy(),
    val privacy: com.motovault.app.domain.privacy.VaultPrivacySettings = com.motovault.app.domain.privacy.VaultPrivacySettings()
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
            require(!line.resetsScheduleBaseline || line.action in setOf("replace", "service")) { "Inspection, cleaning, and adjustment do not reset a service baseline." }
            require(line.scheduleId == null || line.resetsScheduleBaseline) { "Link a schedule only when this item sets its baseline." }
        }
        val scheduleIds = lines.filter { it.resetsScheduleBaseline }.mapNotNull { it.scheduleId }.distinct()
        val linkedRules = rules.filter { it.id in scheduleIds }
        require(linkedRules.size == scheduleIds.size) { "One linked schedule no longer exists." }
        lines.filter { it.scheduleId != null }.forEach { line ->
            val linkedRule = linkedRules.first { it.id == line.scheduleId }
            require(line.catalogueId != null && BangladeshServiceCatalogue.scheduleNameMatchesItem(linkedRule.name, line.catalogueId)) {
                "Link a schedule to the matching service item."
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
        fun emptyData(): VaultState = VaultState()

        fun seedData(today: LocalDate = today()): VaultState {
            val d = { n: Int -> LocalDate.fromEpochDays(today.toEpochDays() + n).toString() }
            val fuel = { id: String, day: Int, odo: Double, liters: Double, full: Boolean, place: String ->
                val cost = kotlin.math.round((liters * 120.0) / 10.0) * 10.0
                RideLog(
                    id = id,
                    title = "Fuel fill-up",
                    detail = "$liters L · ${if (full) "Full tank" else "Partial fill"}",
                    date = d(day),
                    odometer = odo,
                    kind = "fuel",
                    liters = liters,
                    fullTank = full,
                    cost = cost,
                    fuelType = "octane",
                    fuelPrice = 120.0,
                    locationLabel = place
                )
            }
            val logs = listOf(
                fuel("f7", -11, 12340.0, 4.9, true, "পদ্মা পাম্প, মিরপুর"),
                fuel("f6", -17, 12120.0, 5.2, true, "পদ্মা পাম্প, মিরপুর"),
                fuel("f5", -27, 11890.0, 5.0, true, "যমুনা ফিলিং স্টেশন"),
                fuel("f4", -33, 11700.0, 3.8, false, "যমুনা ফিলিং স্টেশন"),
                fuel("f3", -41, 11500.0, 4.8, true, "পদ্মা পাম্প, মিরপুর"),
                fuel("f2", -52, 11290.0, 4.7, true, "পদ্মা পাম্প, মিরপুর"),
                fuel("f1", -64, 11080.0, 5.0, true, "যমুনা ফিলিং স্টেশন"),
                RideLog(
                    id = "s1",
                    title = "Service",
                    detail = "ইঞ্জিন অয়েল ও ফিল্টার",
                    date = d(-45),
                    odometer = 11350.0,
                    kind = "service",
                    workshop = "রহমান মোটরস, মিরপুর ১০",
                    serviceLabourPoisha = 20000,
                    serviceMiscellaneousPoisha = 0,
                    serviceDiscountPoisha = 0,
                    serviceTotalPoisha = 135000,
                    cost = 1350.0,
                    serviceLines = listOf(
                        ServiceLineItem(catalogueId = "engine_oil", customName = "ইঞ্জিন অয়েল", action = "replace", quantity = 1.0, unitPricePoisha = 90000, detail = "10W-40"),
                        ServiceLineItem(catalogueId = "oil_filter", customName = "অয়েল ফিল্টার", action = "replace", quantity = 1.0, unitPricePoisha = 25000)
                    ),
                    scheduleIds = listOf("r-oil")
                )
            )
            val rules = listOf(
                ComponentRule("r-oil", "primary", "ইঞ্জিন অয়েল ও ফিল্টার", "ইঞ্জিন অয়েল ও ফিল্টার", 3000.0, 180, 11350.0, today.toEpochDays().toLong() - 45),
                ComponentRule("r-chain", "primary", "চেইন পরিষ্কার ও লুব্রিকেন্ট", "চেইন পরিষ্কার ও লুব্রিকেন্ট", 500.0, 30, 11520.0, today.toEpochDays().toLong() - 41),
                ComponentRule("r-air", "primary", "এয়ার ফিল্টার", "এয়ার ফিল্টার", 5000.0, 180, 9900.0, today.toEpochDays().toLong() - 140),
                ComponentRule("r-spark", "primary", "স্পার্ক প্লাগ", "স্পার্ক প্লাগ", 8000.0, 365, 8000.0, today.toEpochDays().toLong() - 200),
                ComponentRule("r-brakes", "primary", "ব্রেক প্যাড", "ব্রেক প্যাড", 10000.0, 365, 7200.0, today.toEpochDays().toLong() - 300)
            ).map { it.copy(baselineCertainty = com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) }
            val docs = listOf(
                VaultDocument("রেজিস্ট্রেশন স্মার্ট কার্ড", "ঢাকা মেট্রো-ল ৬২-৪৫১৮", "মো. রাফিদ হাসান", d(400), "d1", reminder = true, kind = "registration"),
                VaultDocument("ড্রাইভিং লাইসেন্স", "DL-1987-654321", "মো. রাফিদ হাসান", d(24), "d2", reminder = false, kind = "licence"),
                VaultDocument("ট্যাক্স টোকেন", "TT-2025-88120", "মো. রাফিদ হাসান", d(-5), "d3", reminder = false, kind = "tax"),
                VaultDocument("বিমা", "INS-554-2291", "মো. রাফিদ হাসান", d(200), "d4", reminder = true, kind = "insurance")
            )
            return VaultState(
                ride = Ride("yamaha", "Official catalogue", 2023, 12340.0, "yamaha-r15-v4", "R15 V4"),
                rules = rules,
                logs = logs,
                documents = docs,
                lastPrices = mapOf("octane" to 120.0, "petrol" to 115.0),
                lastItemPrices = mapOf(
                    "engine_oil" to 900.0,
                    "oil_filter" to 250.0,
                    "chain_sprocket" to 350.0,
                    "air_filter" to 400.0,
                    "spark_plug" to 300.0,
                    "brake_pad" to 650.0
                ),
                workshops = listOf("রহমান মোটরস, মিরপুর ১০", "স্টার সার্ভিস সেন্টার"),
                places = listOf("পদ্মা পাম্প, মিরপুর", "যমুনা ফিলিং স্টেশন")
            )
        }

        private val json = Json { ignoreUnknownKeys = true }
        suspend fun restore(): VaultState = loadVault()?.let { decode(it) } ?: seedData()
        fun decode(value: String): VaultState? = runCatching {
            val state = json.decodeFromString<VaultState>(value)
            state.copy(economy = calculateEconomy(state.logs), lastFuelOdometer = state.logs.filter { it.isFuel }.maxOfOrNull { it.odometer })
        }.getOrNull()
        fun encode(state: VaultState) = json.encodeToString(serializer(), state)

        fun create(ride: Ride, baseline: Double = ride.odometer, baselineDate: LocalDate = today()): VaultState {
            FactoryProfiles.validate(ride)
            require(ride.odometer.isFinite() && ride.odometer in 0.0..9999999.0)
            require(baseline.isFinite() && baseline in 0.0..ride.odometer) { "Last service mileage cannot exceed current mileage." }
            require(baselineDate <= today()) { "Baseline date cannot be in the future." }
            require(ride.factoryProfileId == null || baseline >= 1000) { "Use the recurring manual schedule after the 1,000 km running-in service. Before that, set an owner reminder for the first service." }
            require(ride.modelId == null || BikeCatalogue.find(ride.modelId)?.brandId == ride.brandId) { "Choose a model from the selected brand." }
            val examples = listOf(Triple("Chain", 500.0, 30), Triple("Engine oil", 3000.0, 180), Triple("Air filter", 5000.0, 180))
            return VaultState(ride = ride, rules = FactoryProfiles.find(ride.factoryProfileId)?.rules(baseline, baselineDate) ?: examples.map { (name, km, days) ->
                ComponentRule(recordId(), "primary", name, "Owner-defined example. Confirm against your manual.", km, days, baseline, baselineDate.toEpochDays().toLong())
            })
        }

        fun calculateEconomy(logs: List<RideLog>): Double? = FuelEfficiencyUseCase().calculate(
            logs.filter { it.isFuel }.map { FuelFill(it.odometer, it.fuelLiters, it.fullTank) }
        )
    }
}
