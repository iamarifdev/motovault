package com.motovault.app.ui.redesign

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.DOCUMENT_RENEWAL_WARNING_DAYS
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

expect fun loadVault(): String?
expect fun saveVault(value: String)

fun today() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun recordId() = Clock.System.now().toEpochMilliseconds().toString() + "-" + Random.nextLong().toString()
const val RENEWAL_WARNING_DAYS = DOCUMENT_RENEWAL_WARNING_DAYS

@Serializable
data class Ride(
    val brandId: String, val edition: String, val year: Int, val odometer: Double,
    val modelId: String? = null, val modelName: String = "", val registration: String = ""
) {
    val selectedModel: BikeModel? get() = BikeCatalogue.find(modelId)?.takeIf { it.brandId == brandId }
        ?: if (modelName.isBlank()) BikeCatalogue.legacyModel(brandId) else null
    val displayName: String get() = modelName.ifBlank { selectedModel?.name ?: "Your motorcycle" }
}

@Serializable
data class RideLog(
    val title: String, val detail: String, val date: String, val odometer: Double,
    val id: String = recordId(), val kind: String = "", val liters: Double? = null,
    val fullTank: Boolean = true, val cost: Double = 0.0,
    val workshop: String = "", val notes: String = "", val componentId: String? = null
) {
    val isFuel: Boolean get() = kind == "fuel" || title == "Fuel fill-up"
    val fuelLiters: Double? get() = liters ?: if (isFuel) detail.substringBefore(" L").toDoubleOrNull() else null
}

@Serializable
data class VaultDocument(
    val title: String, val number: String, val holder: String, val expiry: String,
    val id: String = recordId(), val issued: String = "",
    val frontPhoto: String? = null, val backPhoto: String? = null
) {
    val daysRemaining: Int? get() = expiry.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it).toEpochDays() - today().toEpochDays() }
    val expiryLabel: String get() = daysRemaining?.let { if (it <= 0) "Expired" else if (it <= RENEWAL_WARNING_DAYS) "$it days" else "Valid" } ?: "No expiry"
}

@Serializable
data class VaultState(
    val ride: Ride? = null,
    val rules: List<ComponentRule> = emptyList(),
    val logs: List<RideLog> = emptyList(),
    val documents: List<VaultDocument> = emptyList(),
    val lastFuelOdometer: Double? = null,
    val economy: Double? = null,
    val appearance: Appearance = Appearance.SYSTEM
) {
    fun logFuel(odometer: Double, liters: Double, cost: Double = 0.0, date: LocalDate = today(), fullTank: Boolean = true): VaultState {
        val currentRide = requireNotNull(ride)
        require(odometer.isFinite() && odometer in 0.0..9999999.0) { "Enter valid mileage." }
        require(liters.isFinite() && liters > 0 && liters <= 100) { "Enter litres between 0 and 100." }
        require(cost.isFinite() && cost >= 0) { "Cost cannot be negative." }
        require(date <= today()) { "The fill-up date cannot be in the future." }
        val fuel = logs.filter { it.isFuel }
        require(fuel.none { it.odometer >= odometer }) { "Use mileage greater than your latest fill-up." }
        require(fuel.none { LocalDate.parse(it.date) > date }) { "Use a date at or after your latest fill-up." }
        val updatedLogs = listOf(RideLog("Fuel fill-up", "$liters L · " + if (fullTank) "Full tank" else "Partial fill",
            date.toString(), odometer, kind = "fuel", liters = liters, fullTank = fullTank, cost = cost)) + logs
        return copy(ride = currentRide.copy(odometer = maxOf(currentRide.odometer, odometer)),
            lastFuelOdometer = odometer, economy = calculateEconomy(updatedLogs), logs = updatedLogs)
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

    fun saveSchedule(rule: ComponentRule): VaultState {
        val currentRide = requireNotNull(ride)
        require(rule.name.isNotBlank()) { "Enter a component name." }
        require(rule.intervalKm.isFinite() && rule.intervalKm in 0.0..1000000.0 && rule.intervalDays in 0..10000 &&
            (rule.intervalKm > 0 || rule.intervalDays > 0)) { "Set a distance or time interval." }
        require(rule.lastServicedOdometerKm.isFinite() && rule.lastServicedOdometerKm in 0.0..currentRide.odometer) { "Check the service baseline mileage." }
        require(rule.lastServicedEpochDays <= today().toEpochDays().toLong()) { "Baseline date cannot be in the future." }
        return copy(rules = if (rules.any { it.id == rule.id }) rules.map { if (it.id == rule.id) rule else it } else rules + rule)
    }

    fun deleteLog(log: RideLog): VaultState {
        if (!log.isFuel) {
            val rule = rules.firstOrNull { it.id == log.componentId || (log.componentId == null && it.name == log.title) }
            require(rule == null || rule.lastServicedOdometerKm != log.odometer || rule.lastServicedEpochDays != LocalDate.parse(log.date).toEpochDays().toLong()) {
                "This service sets the current baseline. Adjust its schedule baseline before deleting it."
            }
        }
        val remaining = logs.filterNot { it.id == log.id }
        return copy(logs = remaining, economy = calculateEconomy(remaining), lastFuelOdometer = remaining.filter { it.isFuel }.maxOfOrNull { it.odometer })
    }

    fun saveDocument(document: VaultDocument): VaultState {
        require(document.number.isNotBlank() && document.holder.isNotBlank()) { "Enter a document number and holder name." }
        val issued = document.issued.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val expiry = document.expiry.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        require(issued == null || expiry == null || expiry >= issued) { "Expiry must be on or after the issue date." }
        return copy(documents = if (documents.any { it.id == document.id }) documents.map { if (it.id == document.id) document else it } else documents + document)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun restore(): VaultState = loadVault()?.let { requireNotNull(decode(it)) { "Stored records could not be read. Existing storage has been preserved." } } ?: VaultState()
        fun decode(value: String): VaultState? = runCatching {
            val state = json.decodeFromString<VaultState>(value)
            state.copy(economy = calculateEconomy(state.logs), lastFuelOdometer = state.logs.filter { it.isFuel }.maxOfOrNull { it.odometer })
        }.getOrNull()
        fun encode(state: VaultState) = json.encodeToString(serializer(), state)

        fun create(ride: Ride, baseline: Double = ride.odometer, baselineDate: LocalDate = today()): VaultState {
            require(ride.odometer.isFinite() && ride.odometer in 0.0..9999999.0)
            require(baseline.isFinite() && baseline in 0.0..ride.odometer) { "Last service mileage cannot exceed current mileage." }
            require(baselineDate <= today()) { "Baseline date cannot be in the future." }
            require(ride.modelId == null || BikeCatalogue.find(ride.modelId)?.brandId == ride.brandId) { "Choose a model from the selected brand." }
            val examples = listOf(Triple("Chain", 500.0, 30), Triple("Engine oil", 3000.0, 180), Triple("Air filter", 5000.0, 180))
            return VaultState(ride = ride, rules = examples.map { (name, km, days) ->
                ComponentRule(recordId(), "primary", name, "Owner-defined example. Confirm against your manual.", km, days, baseline, baselineDate.toEpochDays().toLong())
            })
        }

        fun calculateEconomy(logs: List<RideLog>): Double? {
            var start: Double? = null
            var pendingLiters = 0.0
            var totalDistance = 0.0
            var totalLiters = 0.0
            logs.filter { it.isFuel }.sortedBy { it.odometer }.forEach { entry ->
                val liters = entry.fuelLiters ?: return@forEach
                if (start == null) {
                    if (entry.fullTank) start = entry.odometer
                } else {
                    pendingLiters += liters
                    if (entry.fullTank) {
                        val distance = entry.odometer - start!!
                        if (distance > 0 && pendingLiters > 0) { totalDistance += distance; totalLiters += pendingLiters }
                        start = entry.odometer
                        pendingLiters = 0.0
                    }
                }
            }
            return if (totalLiters > 0) totalDistance / totalLiters else null
        }
    }
}
