package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.garage.activeBikes
import com.motovault.app.domain.garage.allBikes
import com.motovault.app.domain.garage.setBikeArchived
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.domain.model.Ride
import com.motovault.app.ui.BRAND_COLOR_MAP
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.BorderStrongColor
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.BrandTintColor
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.LocalVaultPalette
import com.motovault.app.ui.OnBrandColor
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.SurfaceColor2
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.TextTertiary
import com.motovault.app.ui.V3Icon
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import com.motovault.app.ui.currentHaptic
import com.motovault.app.ui.displayName
import com.motovault.app.ui.v3DashedBorder
import com.motovault.app.ui.worlds
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_active_badge
import motovault.composeapp.generated.resources.garage_add_bike
import motovault.composeapp.generated.resources.garage_archive_bike
import motovault.composeapp.generated.resources.garage_archived_bikes
import motovault.composeapp.generated.resources.garage_bike_archived
import motovault.composeapp.generated.resources.garage_bike_restored
import motovault.composeapp.generated.resources.garage_bike_switched
import motovault.composeapp.generated.resources.garage_restore_bike
import motovault.composeapp.generated.resources.garage_subtitle
import motovault.composeapp.generated.resources.garage_switcher_meta
import motovault.composeapp.generated.resources.garage_switcher_title
import motovault.composeapp.generated.resources.garage_use_bike

@Composable
internal fun BikeSwitcherSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSwitchBike: (VaultState, String) -> Unit,
    onToggleArchive: (VaultState, String, Boolean) -> Unit,
    onAddBike: () -> Unit
) {
    val haptic = currentHaptic
    val currentId = state.ride?.id
    val activeBikes = state.activeBikes().sortedByDescending { it.id == currentId }
    val archivedBikes = state.allBikes().filter { it.archived }
    val canArchive = activeBikes.size > 1

    Sheet(title = appStringResource(Res.string.garage_switcher_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeBikes.forEach { bike ->
                val isCurrent = bike.id == currentId
                val switchedMsg = appStringResource(Res.string.garage_bike_switched, bike.displayName)
                val archivedMsg = appStringResource(Res.string.garage_bike_archived, bike.displayName)
                ActiveBikeRow(
                    bike = bike,
                    isCurrent = isCurrent,
                    canArchive = canArchive,
                    onSelect = {
                        haptic.perform(HapticKind.TICK)
                        onSwitchBike(state.switchToBike(bike.id), switchedMsg)
                    },
                    onArchive = {
                        haptic.perform(HapticKind.TICK)
                        onToggleArchive(state.setBikeArchived(bike.id, true), archivedMsg, true)
                    }
                )
            }
            AddAnotherBikeRow {
                haptic.perform(HapticKind.TICK)
                onDismiss()
                onAddBike()
            }
        }

        if (archivedBikes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = appStringResource(Res.string.garage_archived_bikes), fontSize = 13.sp, color = TextSecondary)
                archivedBikes.forEach { bike ->
                    val restoredMsg = appStringResource(Res.string.garage_bike_restored, bike.displayName)
                    ArchivedBikeRow(
                        bike = bike,
                        onRestore = {
                            haptic.perform(HapticKind.TICK)
                            onToggleArchive(state.setBikeArchived(bike.id, false), restoredMsg, false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveBikeRow(
    bike: Ride,
    isCurrent: Boolean,
    canArchive: Boolean,
    onSelect: () -> Unit,
    onArchive: () -> Unit
) {
    val f = currentFmt
    val dark = LocalVaultPalette.current.dark
    val brand = worlds.firstOrNull { it.id == bike.brandId } ?: worlds.first()
    val colors = BRAND_COLOR_MAP[bike.brandId] ?: BRAND_COLOR_MAP["ktm"]!!
    val bikeAccent = Color(if (dark) colors.dark else colors.light)
    val shape = RoundedCornerShape(16.dp)
    val archiveDesc = "${appStringResource(Res.string.garage_archive_bike)} ${bike.displayName}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) BrandTintColor else SurfaceColor)
            .border(1.5.dp, if (isCurrent) BrandColor else BorderColor, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !isCurrent, role = Role.Button, onClick = onSelect)
                .semantics { selected = isCurrent }
                .padding(start = 12.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bikeAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/fi_bike.svg", description = null, modifier = Modifier.size(22.dp), tint = bikeAccent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bike.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = appStringResource(Res.string.garage_switcher_meta, brand.name, f.d(bike.year.toString()), f.km(bike.odometer)),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isCurrent) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(BrandColor).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    V3Icon(path = "feather_icon/fi_check.svg", description = null, modifier = Modifier.size(14.dp), tint = OnBrandColor)
                    Text(text = appStringResource(Res.string.garage_active_badge), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnBrandColor)
                }
            } else {
                Text(text = appStringResource(Res.string.garage_use_bike), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
            }
        }
        if (canArchive) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onArchive)
                    .semantics { contentDescription = archiveDesc },
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/fi_archive.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
            }
        }
    }
}

@Composable
private fun AddAnotherBikeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .v3DashedBorder(BorderStrongColor, 16.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandTintColor),
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/fi_plus.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
        }
        Text(text = appStringResource(Res.string.garage_add_bike), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
    }
}

@Composable
private fun ArchivedBikeRow(bike: Ride, onRestore: () -> Unit) {
    val f = currentFmt
    val brand = worlds.firstOrNull { it.id == bike.brandId } ?: worlds.first()
    val shape = RoundedCornerShape(16.dp)
    val restoreDesc = "${appStringResource(Res.string.garage_restore_bike)} ${bike.displayName}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.75f)
            .clip(shape)
            .background(SurfaceColor)
            .border(1.5.dp, BorderColor, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(start = 12.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceColor2),
                contentAlignment = Alignment.Center
            ) {
                V3Icon(path = "feather_icon/fi_bike.svg", description = null, modifier = Modifier.size(22.dp), tint = TextTertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bike.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = appStringResource(Res.string.garage_subtitle, brand.name, f.km(bike.odometer)), fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRestore)
                .semantics { contentDescription = restoreDesc },
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/fi_archive-restore.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
        }
    }
}
