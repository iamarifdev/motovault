package com.motovault.app.ui.fuel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.ui.*
import motovault.composeapp.generated.resources.*

@Composable
internal fun FuelDateControls(fullTank: Boolean, dateKey: FuelDateSelection, customDate: String,
    onFull: () -> Unit, onDate: (FuelDateSelection) -> Unit, onCustom: (String) -> Unit
) {
        // Full tank & date chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(text = appStringResource(if (fullTank) Res.string.fuel_full_chip else Res.string.fuel_partial_chip), active = fullTank, onClick = onFull)
            VaultChip(text = appStringResource(Res.string.date_today), active = dateKey == FuelDateSelection.TODAY, onClick = { onDate(FuelDateSelection.TODAY) })
            VaultChip(text = appStringResource(Res.string.date_yesterday), active = dateKey == FuelDateSelection.YESTERDAY, onClick = { onDate(FuelDateSelection.YESTERDAY) })
            VaultChip(text = appStringResource(Res.string.date_other), active = dateKey == FuelDateSelection.CUSTOM, onClick = { onDate(FuelDateSelection.CUSTOM) })
        }

        // Custom date field if custom selected
        AnimatedVisibility(visible = dateKey == FuelDateSelection.CUSTOM) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor2)
                    .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = customDate,
                    onValueChange = onCustom,
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary)
                )
            }
        }

}
