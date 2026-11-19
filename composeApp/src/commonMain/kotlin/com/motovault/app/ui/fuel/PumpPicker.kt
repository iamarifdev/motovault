package com.motovault.app.ui.fuel

import androidx.compose.runtime.Composable
import com.motovault.app.ui.NamePickerContent
import com.motovault.app.ui.RecentNameOption
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_pump_clear
import motovault.composeapp.generated.resources.fuel_pump_last_date
import motovault.composeapp.generated.resources.fuel_pump_picker_close
import motovault.composeapp.generated.resources.fuel_pump_picker_title
import motovault.composeapp.generated.resources.fuel_pump_recent_header
import motovault.composeapp.generated.resources.fuel_pump_search_placeholder
import motovault.composeapp.generated.resources.fuel_pump_use_typed

@Composable
internal fun PumpPickerSheet(
    value: String,
    recentPumps: List<RecentPump>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val f = currentFmt
    val recent = recentPumps.map { pump ->
        val subtitle = pump.lastDate?.let { iso ->
            runCatching { LocalDate.parse(iso) }.getOrNull()?.let { date ->
                appStringResource(Res.string.fuel_pump_last_date, f.date(date))
            }
        }
        RecentNameOption(name = pump.name, subtitle = subtitle)
    }

    Sheet(
        title = appStringResource(Res.string.fuel_pump_picker_title),
        onDismiss = onDismiss,
        closeLabel = appStringResource(Res.string.fuel_pump_picker_close)
    ) {
        NamePickerContent(
            value = value,
            recent = recent,
            placeholder = appStringResource(Res.string.fuel_pump_search_placeholder),
            recentHeader = appStringResource(Res.string.fuel_pump_recent_header),
            useTypedLabel = { typed -> appStringResource(Res.string.fuel_pump_use_typed, typed) },
            clearLabel = appStringResource(Res.string.fuel_pump_clear),
            onPick = onPick
        )
    }
}
