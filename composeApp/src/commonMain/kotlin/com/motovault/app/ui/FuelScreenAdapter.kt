package com.motovault.app.ui

import com.motovault.app.domain.fuel.FuelTimelineEntry
import com.motovault.app.ui.fuel.FuelExistingEntry
import com.motovault.app.ui.fuel.FuelScreenInput
import com.motovault.app.ui.fuel.RecentPump
import kotlinx.datetime.LocalDate

private const val MAX_RECENT_PUMPS = 5

/** Transitional mapping from the existing persisted UI model into fuel presentation data. */
internal fun VaultState.fuelScreenInput(today: LocalDate): FuelScreenInput? {
    val ride = ride ?: return null
    val fuelLogs = logs.filter { it.isFuel }
    val dates = fuelLogs.map { log -> runCatching { LocalDate.parse(log.date) }.getOrNull() }
    val recentPumps = buildList {
        val seen = mutableSetOf<String>()
        fuelLogs.sortedByDescending { it.date }.forEach { log ->
            val name = log.locationLabel.trim()
            if (name.isNotEmpty() && seen.add(name)) {
                add(RecentPump(name = name, lastDate = log.date.ifBlank { null }))
            }
        }
        places.forEach { place ->
            val name = place.trim()
            if (name.isNotEmpty() && seen.add(name)) {
                add(RecentPump(name = name, lastDate = null))
            }
        }
    }.take(MAX_RECENT_PUMPS)
    return FuelScreenInput(
        estimatedOdometer = WearCalculator.estimateOdometer(ride, logs, today),
        lastFuelOdometer = fuelLogs.maxOfOrNull { it.odometer } ?: 0.0,
        lastFuelDate = dates.filterNotNull().maxOrNull(),
        timeline = fuelLogs.zip(dates) { log, date -> date?.let { FuelTimelineEntry(log.id, log.odometer, it) } },
        lastPrices = lastPrices + ledger.confirmedPrices.mapValues { it.value.takaPerLitre },
        places = places.toList(),
        confirmedPrices = ledger.confirmedPrices,
        recentPumps = recentPumps
    )
}

internal fun RideLog.fuelExistingEntry() = FuelExistingEntry(
    id = id, cost = cost, fuelLiters = fuelLiters, fuelPrice = fuelPrice, odometer = odometer,
    date = date, fullTank = fullTank, fuelType = fuelType, locationLabel = locationLabel, missedFill = missedFuelFill
)
