package com.motovault.app.ui.fuel

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.ConfirmedFuelPrice
import com.motovault.app.ui.*
import motovault.composeapp.generated.resources.*

@Composable
internal fun FuelConfidenceControls(confirmed: Boolean, remembered: ConfirmedFuelPrice?, missedFill: Boolean, priceAvailable: Boolean,
    onConfirm: (Boolean) -> Unit, onMissed: (Boolean) -> Unit
) {
    remembered?.let { Text(appStringResource(Res.string.cost_price_date, localizedCostDate(it.effectiveDate)), color = TextSecondary, fontSize = 13.sp) }
    if (!priceAvailable) Text(appStringResource(Res.string.fuel_price_unknown), color = TextSecondary, fontSize = 13.sp)
    else Row(modifier = Modifier.toggleable(confirmed, role = Role.Checkbox, onValueChange = onConfirm).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(confirmed, null)
        Text(appStringResource(if (confirmed) Res.string.cost_price_confirmed else Res.string.cost_price_confirm), color = TextPrimary, fontSize = 14.sp)
    }
    Row(modifier = Modifier.toggleable(missedFill, role = Role.Checkbox, onValueChange = onMissed).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(missedFill, null)
        Text(appStringResource(Res.string.cost_missed_fill), color = TextPrimary, fontSize = 14.sp)
    }
    Text(appStringResource(Res.string.cost_cycle_open), color = TextSecondary, fontSize = 13.sp)
}
