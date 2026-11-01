package com.motovault.app.ui.fuel

import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.datetime.LocalDate

/** Read-only information needed to render a fuel draft. */
internal data class FuelScreenInput(
    val estimatedOdometer: Double,
    val lastFuelOdometer: Double,
    val lastFuelDate: LocalDate?,
    val timeline: List<FuelTimelineEntry?>,
    val lastPrices: Map<String, Double>,
    val places: List<String>
)

internal data class FuelExistingEntry(
    val id: String,
    val cost: Double,
    val fuelLiters: Double?,
    val fuelPrice: Double?,
    val odometer: Double,
    val date: String,
    val fullTank: Boolean,
    val fuelType: String,
    val locationLabel: String?
)
