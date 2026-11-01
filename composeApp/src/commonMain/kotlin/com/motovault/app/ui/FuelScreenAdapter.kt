package com.motovault.app.ui

import com.motovault.app.domain.fuel.FuelTimelineEntry
import com.motovault.app.ui.fuel.FuelExistingEntry
import com.motovault.app.ui.fuel.FuelScreenInput
import kotlinx.datetime.LocalDate

/** Transitional mapping from the existing persisted UI model into fuel presentation data. */
internal fun VaultState.fuelScreenInput(today: LocalDate): FuelScreenInput? {
    val ride = ride ?: return null
    val fuelLogs = logs.filter { it.isFuel }
    val dates = fuelLogs.map { log -> runCatching { LocalDate.parse(log.date) }.getOrNull() }
    return FuelScreenInput(
        estimatedOdometer = WearCalculator.estimateOdometer(ride, logs, today),
        lastFuelOdometer = fuelLogs.maxOfOrNull { it.odometer } ?: 0.0,
        lastFuelDate = dates.filterNotNull().maxOrNull(),
        timeline = fuelLogs.zip(dates) { log, date -> date?.let { FuelTimelineEntry(log.id, log.odometer, it) } },
        lastPrices = lastPrices.toMap(),
        places = places.toList()
    )
}

internal fun RideLog.fuelExistingEntry() = FuelExistingEntry(
    id = id, cost = cost, fuelLiters = fuelLiters, fuelPrice = fuelPrice, odometer = odometer,
    date = date, fullTank = fullTank, fuelType = fuelType, locationLabel = locationLabel
)
