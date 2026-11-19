package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_econ_empty
import motovault.composeapp.generated.resources.garage_fuel_last_date
import motovault.composeapp.generated.resources.garage_fuel_missing_days
import motovault.composeapp.generated.resources.garage_fuel_note_empty
import motovault.composeapp.generated.resources.garage_next_job_empty
import motovault.composeapp.generated.resources.garage_next_job_label
import motovault.composeapp.generated.resources.garage_unit_km_per_litre

@Composable
internal fun ShowroomStatsStrip(
    snapshot: GarageHomeSnapshot,
    washActive: Boolean,
    onFuelStats: () -> Unit,
    onNextJob: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val shape = RoundedCornerShape(16.dp)
    val econValue = snapshot.economy?.let { f.n(it, 1) } ?: appStringResource(Res.string.garage_econ_empty)
    val econText = "$econValue ${appStringResource(Res.string.garage_unit_km_per_litre)}"
    val fuelNoteText = when (val note = snapshot.fuelNote) {
        is FuelNoteState.DaysWithoutFuel -> appStringResource(Res.string.garage_fuel_missing_days, f.d(note.days.toString()))
        is FuelNoteState.LastFuelDate -> appStringResource(Res.string.garage_fuel_last_date, f.date(note.date))
        FuelNoteState.Empty -> appStringResource(Res.string.garage_fuel_note_empty)
    }

    val containerMod = if (washActive) {
        Modifier.fillMaxWidth().clip(shape).background(SurfaceColor).border(1.dp, BorderColor, shape)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = containerMod) {
        if (!washActive) HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            StatsCell(
                icon = "fi_fuel.svg",
                title = econText,
                subtitle = fuelNoteText,
                subtitleColor = TextSecondary,
                subtitleBold = false,
                onClick = { haptic.perform(HapticKind.TICK); onFuelStats() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor)
            )
            NextJobCell(
                nextJob = snapshot.nextJob,
                f = f,
                onClick = { haptic.perform(HapticKind.TICK); onNextJob() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun NextJobCell(
    nextJob: Wear?,
    f: Fmt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (nextJob == null) {
        StatsCell(
            icon = "fi_tool.svg",
            title = appStringResource(Res.string.garage_next_job_label),
            subtitle = appStringResource(Res.string.garage_next_job_empty),
            subtitleColor = TextSecondary,
            subtitleBold = false,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        val subtitleColor = when (nextJob.status) {
            WearStatus.OVERDUE -> ErrorColor
            WearStatus.SOON -> WarningColor
            else -> TextSecondary
        }
        StatsCell(
            icon = "fi_tool.svg",
            title = componentShortLabel(nextJob.rule),
            subtitle = WearCalculator.describeWear(nextJob, f),
            subtitleColor = subtitleColor,
            subtitleBold = nextJob.status == WearStatus.OVERDUE || nextJob.status == WearStatus.SOON,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun StatsCell(
    icon: String,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    subtitleBold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(18.dp), tint = BrandColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = if (subtitleBold) FontWeight.SemiBold else FontWeight.Normal,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
