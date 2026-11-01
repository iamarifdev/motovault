package com.motovault.app.storage

import com.motovault.app.application.fuel.FuelEntryBuilder
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.application.fuel.FuelEditRejected
import com.motovault.app.application.fuel.FuelEntryEditor
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.model.today
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.datetime.LocalDate

private const val LEGACY_FUEL_TITLE = "Fuel fill-up"
private const val LEGACY_FUEL_KIND = "fuel"
private const val MAX_RECENT_FUEL_PLACES = 6

/** Maps the fuel operation to the existing serialized vault without changing its format. */
internal object FuelEntryAdapter : FuelEntryBuilder<VaultState>, FuelEntryEditor<VaultState> {
    override fun build(previous: VaultState, request: FuelEntryRequest): VaultState {
        val ride = requireNotNull(previous.ride)
        val previousFuel = previous.logs.filter { it.isFuel }
        val lastOdometer = previousFuel.maxOfOrNull { it.odometer } ?: 0.0
        val lastDate = previousFuel.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.maxOrNull()
        require(FuelEntryRules.validateSaved(request.amount, request.litres, request.price, request.odometer, lastOdometer, request.date, today(), lastDate) == null)
        val place = request.place.trim()
        val log = RideLog(
            title = LEGACY_FUEL_TITLE, detail = request.legacyDetail, date = request.date.toString(),
            odometer = request.odometer, kind = LEGACY_FUEL_KIND, liters = request.litres,
            fullTank = request.fullTank, cost = request.amount, fuelType = request.fuelType,
            fuelPrice = request.price, locationLabel = place
        )
        val places = if (place.isNotBlank()) listOf(place) + previous.places.filterNot { it == place } else previous.places
        val logs = listOf(log) + previous.logs
        return previous.copy(
            ride = ride.confirmedMileage(request.odometer, request.date),
            logs = logs,
            lastFuelOdometer = logs.filter { it.isFuel }.maxOfOrNull { it.odometer },
            economy = VaultState.calculateEconomy(logs),
            places = places.take(MAX_RECENT_FUEL_PLACES),
            lastPrices = previous.lastPrices + (request.fuelType to request.price)
        )
    }

    override fun edit(previous: VaultState, request: FuelEditRequest): VaultState {
        val existing = previous.logs.singleOrNull { it.id == request.id }?.takeIf { it.isFuel }
            ?: throw FuelEditRejected(FuelEntryIssue.RECORD_MISSING)
        FuelEntryRules.validateEditedValues(request.amount, request.litres, request.price, request.odometer, request.date, today())
            ?.let { throw FuelEditRejected(it) }
        val timeline = previous.logs.filter { it.isFuel }.map { log ->
            val date = runCatching { LocalDate.parse(log.date) }.getOrNull()
                ?: throw FuelEditRejected(FuelEntryIssue.TIMELINE_CONFLICT)
            FuelTimelineEntry(log.id, log.odometer, date)
        }
        FuelEntryRules.validateEditOrder(FuelTimelineEntry(request.id, request.odometer, request.date), timeline)
            ?.let { throw FuelEditRejected(it) }
        val replacement = existing.copy(
            detail = request.updatedLegacyDetail ?: existing.detail,
            date = request.date.toString(), odometer = request.odometer, liters = request.litres,
            fullTank = request.fullTank, cost = request.amount, fuelType = request.fuelType,
            fuelPrice = request.price, locationLabel = request.place.trim()
        )
        val logs = previous.logs.map { if (it.id == existing.id) replacement else it }
        return previous.copy(
            ride = requireNotNull(previous.ride).confirmedMileage(request.odometer, request.date),
            logs = logs, lastFuelOdometer = logs.filter { it.isFuel }.maxOfOrNull { it.odometer },
            economy = VaultState.calculateEconomy(logs)
        )
    }
}
