package com.motovault.app.ui.fuel

import androidx.compose.runtime.Composable
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.ui.ChipLine
import com.motovault.app.ui.ChipLineDivider
import com.motovault.app.ui.DateChips
import com.motovault.app.ui.EntryChip
import com.motovault.app.ui.EntryDateKey
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_full_tank
import motovault.composeapp.generated.resources.fuel_missed_fill_chip
import motovault.composeapp.generated.resources.fuel_octane
import motovault.composeapp.generated.resources.fuel_options_desc
import motovault.composeapp.generated.resources.fuel_petrol
import motovault.composeapp.generated.resources.fuel_presets_desc
import kotlin.math.roundToInt

private val FUEL_PRESET_AMOUNTS = listOf(200, 300, 500, 1000)

@Composable
internal fun FuelPresetsLine(
    activeAmount: Double,
    hasAmountText: Boolean,
    onSelect: (Int) -> Unit
) {
    val f = currentFmt
    ChipLine(contentDescription = appStringResource(Res.string.fuel_presets_desc)) {
        FUEL_PRESET_AMOUNTS.forEach { preset ->
            val selected = hasAmountText && activeAmount.isFinite() && activeAmount.roundToInt() == preset
            EntryChip(
                text = f.money(preset.toDouble()),
                active = selected,
                onClick = { onSelect(preset) }
            )
        }
    }
}

@Composable
internal fun FuelOptionsLine(
    fullTank: Boolean,
    fuelType: String,
    dateKey: EntryDateKey,
    customDate: String,
    missedFill: Boolean,
    onToggleFull: () -> Unit,
    onSelectFuel: (String) -> Unit,
    onSelectDateKey: (EntryDateKey) -> Unit,
    onCustomDateChange: (String) -> Unit,
    onToggleMissedFill: () -> Unit
) {
    ChipLine(contentDescription = appStringResource(Res.string.fuel_options_desc)) {
        EntryChip(
            text = appStringResource(Res.string.fuel_full_tank),
            active = fullTank,
            onClick = onToggleFull
        )
        ChipLineDivider()
        EntryChip(
            text = appStringResource(Res.string.fuel_octane),
            active = fuelType == FuelDefaults.OCTANE,
            onClick = { onSelectFuel(FuelDefaults.OCTANE) }
        )
        EntryChip(
            text = appStringResource(Res.string.fuel_petrol),
            active = fuelType == FuelDefaults.PETROL,
            onClick = { onSelectFuel(FuelDefaults.PETROL) }
        )
        ChipLineDivider()
        DateChips(
            dateKey = dateKey,
            customDate = customDate,
            onSelectKey = onSelectDateKey,
            onCustomDateChange = onCustomDateChange
        )
        ChipLineDivider()
        EntryChip(
            text = appStringResource(Res.string.fuel_missed_fill_chip),
            active = missedFill,
            onClick = onToggleMissedFill
        )
    }
}
