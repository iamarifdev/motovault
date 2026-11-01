package com.motovault.app.application.fuel

import kotlinx.datetime.LocalDate
import com.motovault.app.domain.fuel.FuelEntryIssue

/** Canonical values and the legacy display detail required by the current serialized record. */
internal data class FuelEntryRequest(
    val amount: Double,
    val litres: Double,
    val price: Double,
    val odometer: Double,
    val date: LocalDate,
    val fullTank: Boolean,
    val fuelType: String,
    val place: String,
    val legacyDetail: String
)

/** Transitional boundary until the persisted UI-owned vault model is migrated. */
internal fun interface FuelEntryBuilder<T> {
    fun build(previous: T, request: FuelEntryRequest): T
}

internal data class FuelEditRequest(
    val id: String,
    val amount: Double,
    val litres: Double,
    val price: Double?,
    val odometer: Double,
    val date: LocalDate,
    val fullTank: Boolean,
    val fuelType: String,
    val place: String,
    val updatedLegacyDetail: String?
)

internal class FuelEditRejected(val issue: FuelEntryIssue) : IllegalArgumentException()

internal fun interface FuelEntryEditor<T> {
    fun edit(previous: T, request: FuelEditRequest): T
}
