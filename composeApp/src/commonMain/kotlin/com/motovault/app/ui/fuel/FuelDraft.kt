package com.motovault.app.ui.fuel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.model.FuelDefaults
import kotlinx.datetime.LocalDate

internal enum class FuelDateSelection { TODAY, YESTERDAY, CUSTOM }

/** Transient draft belongs to the open fuel sheet, not durable vault storage. */
@Stable
internal class FuelDraft(existing: FuelExistingEntry?, today: LocalDate) {
    var fuelType by mutableStateOf(existing?.fuelType ?: FuelDefaults.OCTANE)
    var mode by mutableStateOf(FuelEntryMode.AMOUNT)
    var fullTank by mutableStateOf(existing?.fullTank ?: true)
    var dateSelection by mutableStateOf(if (existing == null) FuelDateSelection.TODAY else FuelDateSelection.CUSTOM)
    var customDate by mutableStateOf(existing?.date ?: today.toString())
    var place by mutableStateOf(existing?.locationLabel.orEmpty())
    var odometerTouched by mutableStateOf(existing != null)
    var priceTouched by mutableStateOf(false)
}
