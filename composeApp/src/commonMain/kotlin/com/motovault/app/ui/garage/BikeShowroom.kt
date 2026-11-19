package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.garage.carouselBikes
import com.motovault.app.domain.model.Ride
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_estimated_today
import motovault.composeapp.generated.resources.garage_health_due
import motovault.composeapp.generated.resources.garage_health_ok
import motovault.composeapp.generated.resources.garage_showroom_desc
import motovault.composeapp.generated.resources.garage_subtitle
import motovault.composeapp.generated.resources.garage_subtitle_cc
import motovault.composeapp.generated.resources.garage_switch_bike_desc
import motovault.composeapp.generated.resources.garage_unit_km

@Composable
internal fun BikeShowroom(
    state: VaultState,
    snapshot: GarageHomeSnapshot,
    onOpenBikeSwitcher: () -> Unit,
    onSwitchBike: (String, (Boolean) -> Unit) -> Unit,
    onOpenOdometer: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNextJob: () -> Unit
) {
    val ride = state.ride ?: return
    val bikes = remember(state.ride, state.garageBikes) { state.carouselBikes() }
    val washActive = LocalWashActive.current
    val showroomDesc = appStringResource(Res.string.garage_showroom_desc)
    val cardShape = RoundedCornerShape(20.dp)
    val cardMod = if (washActive) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clip(cardShape).background(SurfaceColor).border(1.dp, BorderColor, cardShape)
    }

    Column(modifier = cardMod.semantics { contentDescription = showroomDesc }) {
        ShowroomTopRow(ride = ride, snapshot = snapshot, washActive = washActive, onOpenBikeSwitcher = onOpenBikeSwitcher)
        ShowroomCarousel(bikes = bikes, activeBikeId = ride.id, onSwitchBike = onSwitchBike)
        ShowroomOdometerBlock(ride = ride, estimate = snapshot.estimate, onOpenOdometer = onOpenOdometer)
        ShowroomStatsStrip(
            snapshot = snapshot,
            washActive = washActive,
            onFuelStats = onOpenHistory,
            onNextJob = onOpenNextJob
        )
    }
}

@Composable
private fun ShowroomTopRow(
    ride: Ride,
    snapshot: GarageHomeSnapshot,
    washActive: Boolean,
    onOpenBikeSwitcher: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val palette = LocalVaultPalette.current
    val brand = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()
    val cc = ride.cc ?: BikeCatalogueFamilies.variantOf(ride.modelId)?.cc
    val yearText = f.d(ride.year.toString())
    val subtitle = if (cc != null) {
        appStringResource(Res.string.garage_subtitle_cc, brand.name, yearText, f.d(cc.toString()))
    } else {
        appStringResource(Res.string.garage_subtitle, brand.name, yearText)
    }
    val switchDesc = appStringResource(Res.string.garage_switch_bike_desc, ride.displayName)
    val tone = when (snapshot.health) {
        GarageHealth.OVERDUE -> StatusTone.OVERDUE
        GarageHealth.SOON -> StatusTone.SOON
        GarageHealth.OK -> StatusTone.OK
    }
    val pillBg = palette.statusBadgeBg(tone)
    val pillFg = palette.statusBadgeFg(tone)
    val pillIcon = when (snapshot.health) {
        GarageHealth.OVERDUE -> "fi_triangle-alert.svg"
        GarageHealth.SOON -> "fi_timer.svg"
        GarageHealth.OK -> "fi_circle-check.svg"
    }
    val pillLabel = if (snapshot.dueCount > 0) {
        appStringResource(Res.string.garage_health_due, f.d(snapshot.dueCount.toString()))
    } else {
        appStringResource(Res.string.garage_health_ok)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (washActive) 2.dp else 14.dp,
                top = 12.dp,
                end = if (washActive) 2.dp else 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) {
                    haptic.perform(HapticKind.TICK)
                    onOpenBikeSwitcher()
                }
                .semantics { contentDescription = switchDesc }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = ride.displayName,
                    fontSize = 18.sp,
                    lineHeight = 22.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                V3Icon(path = "feather_icon/fi_chevron-down.svg", description = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
            }
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HealthPill(icon = pillIcon, label = pillLabel, bg = pillBg, fg = pillFg, accent = pillBorderColor(tone, palette))
    }
}

private fun pillBorderColor(tone: StatusTone, palette: VaultPalette): Color = when (tone) {
    StatusTone.OK -> palette.ok.copy(alpha = 0.24f)
    StatusTone.SOON -> palette.warn.copy(alpha = 0.26f)
    StatusTone.OVERDUE -> palette.err.copy(alpha = 0.28f)
}

@Composable
private fun HealthPill(icon: String, label: String, bg: Color, fg: Color, accent: Color) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, accent, shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(14.dp), tint = fg, strokeWidth = 2.2f)
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun ShowroomOdometerBlock(ride: Ride, estimate: Double, onOpenOdometer: () -> Unit) {
    val f = currentFmt
    val haptic = currentHaptic
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) {
                haptic.perform(HapticKind.TICK)
                onOpenOdometer()
            }
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            CountUp(
                value = ride.odometer,
                format = { n -> f.n(n) },
                style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 41.4.sp, letterSpacing = (-0.36).sp, color = TextPrimary)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = appStringResource(Res.string.garage_unit_km),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = appStringResource(Res.string.garage_estimated_today, f.n(estimate.toLong())),
                fontSize = 13.sp,
                color = TextSecondary
            )
            V3Icon(path = "feather_icon/fi_chevron-right.svg", description = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
        }
    }
}
