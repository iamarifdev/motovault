package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.garage.*
import com.motovault.app.ui.*
import motovault.composeapp.generated.resources.*

@Composable
internal fun BikeSwitcherSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSelectBike: (VaultState, String) -> Unit,
    onAddBike: () -> Unit
) {
    val activeBikes = state.activeBikes()
    val archivedBikes = state.allBikes().filter { it.archived }
    val currentId = state.ride?.id
    var archiveError by remember { mutableStateOf<String?>(null) }
    val lastBikeError = appStringResource(Res.string.garage_archive_last_error)

    Sheet(
        title = appStringResource(Res.string.garage_switcher_title),
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.garage_add_bike),
                full = true,
                onClick = {
                    onDismiss()
                    onAddBike()
                }
            )
        }
    ) {
        Text(
            text = appStringResource(Res.string.garage_active_bikes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activeBikes.forEach { bike ->
                val isCurrent = bike.id == currentId
                val switchedMsg = appStringResource(Res.string.garage_bike_switched, bike.displayName)
                val archivedMsg = appStringResource(Res.string.garage_bike_archived, bike.displayName)
                BikeRowCard(
                    bike = bike,
                    isCurrent = isCurrent,
                    canSelect = !isCurrent,
                    primaryActionLabel = appStringResource(Res.string.garage_use_bike),
                    secondaryActionLabel = appStringResource(Res.string.garage_archive_bike),
                    statusLabel = if (isCurrent) appStringResource(Res.string.garage_active_badge) else null,
                    onPrimaryAction = {
                        archiveError = null
                        onSelectBike(state.switchToBike(bike.id), switchedMsg)
                    },
                    onSecondaryAction = {
                        if (activeBikes.size <= 1) {
                            archiveError = lastBikeError
                        } else {
                            archiveError = null
                            onSelectBike(state.setBikeArchived(bike.id, true), archivedMsg)
                        }
                    }
                )
            }
        }

        if (archivedBikes.isNotEmpty()) {
            Text(
                text = appStringResource(Res.string.garage_archived_bikes),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                archivedBikes.forEach { bike ->
                    val restoredMsg = appStringResource(Res.string.garage_bike_restored, bike.displayName)
                    BikeRowCard(
                        bike = bike,
                        isCurrent = false,
                        canSelect = true,
                        primaryActionLabel = appStringResource(Res.string.garage_restore_bike),
                        secondaryActionLabel = null,
                        statusLabel = appStringResource(Res.string.garage_archived_badge),
                        onPrimaryAction = {
                            archiveError = null
                            onSelectBike(state.setBikeArchived(bike.id, false), restoredMsg)
                        },
                        onSecondaryAction = null
                    )
                }
            }
        }

        archiveError?.let { error ->
            Text(text = error, fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BikeRowCard(
    bike: Ride,
    isCurrent: Boolean,
    canSelect: Boolean,
    primaryActionLabel: String,
    secondaryActionLabel: String?,
    statusLabel: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (() -> Unit)?
) {
    val f = currentFmt
    val brand = worlds.firstOrNull { it.id == bike.brandId } ?: worlds.first()
    val borderColor = if (isCurrent) BrandColor else BorderColor
    val bgColor = if (isCurrent) BrandTintColor else SurfaceColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .then(if (canSelect) Modifier.clickable { onPrimaryAction() } else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(brand.accent)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = bike.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = listOf(brand.name, f.d(bike.year.toString()), f.km(bike.odometer)).joinToString(" · "),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            statusLabel?.let { label ->
                VaultChip(text = label, active = isCurrent, onClick = {})
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canSelect) {
                VaultButton(
                    text = primaryActionLabel,
                    size = ButtonSize.SM,
                    kind = ButtonKind.SECONDARY,
                    onClick = onPrimaryAction
                )
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                VaultButton(
                    text = secondaryActionLabel,
                    size = ButtonSize.SM,
                    kind = ButtonKind.GHOST,
                    onClick = onSecondaryAction
                )
            }
        }
    }
}
