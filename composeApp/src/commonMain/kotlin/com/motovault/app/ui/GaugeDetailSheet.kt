package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.gauge_current_odo
import motovault.composeapp.generated.resources.gauge_edit_last_desc
import motovault.composeapp.generated.resources.gauge_interval_every_label
import motovault.composeapp.generated.resources.gauge_interval_every_value
import motovault.composeapp.generated.resources.gauge_last_done_empty
import motovault.composeapp.generated.resources.gauge_last_done_label
import motovault.composeapp.generated.resources.gauge_last_done_value
import motovault.composeapp.generated.resources.gauge_last_saved
import motovault.composeapp.generated.resources.gauge_log_service_action
import motovault.composeapp.generated.resources.gauge_reset_action
import motovault.composeapp.generated.resources.gauge_reset_saved
import motovault.composeapp.generated.resources.gauge_unknown_hint
import motovault.composeapp.generated.resources.schedule_details_title
import motovault.composeapp.generated.resources.schedule_missing
import motovault.composeapp.generated.resources.wear_or

@Composable
internal fun GaugeDetailSheet(
    state: VaultState,
    id: String?,
    onDismiss: () -> Unit,
    onOpen: (String, String?) -> Unit,
    onSaveSchedule: (VaultState, String, Boolean) -> Unit = { _, _, _ -> }
) {
    val ride = state.ride ?: return
    val rule = state.confirmedRules().firstOrNull { it.id == id }
    if (rule == null) {
        Sheet(title = appStringResource(Res.string.schedule_details_title), onDismiss = onDismiss) {
            Text(text = appStringResource(Res.string.schedule_missing), fontSize = 15.sp, color = TextSecondary)
        }
        return
    }

    val f = currentFmt
    val haptic = currentHaptic
    val currentDay = today()
    val wear = WearCalculator.wearOf(rule, ride.odometer, currentDay.toEpochDays().toLong())
    val known = rule.baselineCertainty == BaselineCertainty.CONFIRMED
    var editing by remember(rule.id) { mutableStateOf(false) }
    val resetMsg = appStringResource(Res.string.gauge_reset_saved)
    val lastSavedMsg = appStringResource(Res.string.gauge_last_saved)

    Sheet(title = componentLabel(rule), onDismiss = onDismiss) {
        GaugeDetailCardStack(
            rule = rule,
            wear = wear,
            rideOdometer = ride.odometer,
            today = currentDay,
            known = known,
            editing = editing,
            f = f,
            onStartEdit = { haptic.perform(HapticKind.TICK); editing = true },
            onCancelEdit = { editing = false },
            onSaveEdit = { savedKm, savedDate ->
                val updated = rule.copy(
                    lastServicedOdometerKm = savedKm,
                    lastServicedEpochDays = savedDate.toEpochDays().toLong(),
                    baselineCertainty = BaselineCertainty.CONFIRMED
                )
                val next = runCatching { state.saveSchedule(updated) }.getOrNull()
                if (next != null) {
                    haptic.perform(HapticKind.SUCCESS)
                    editing = false
                    onSaveSchedule(next, lastSavedMsg, false)
                } else {
                    haptic.perform(HapticKind.ERROR)
                }
            }
        )

        if (!editing) {
            VaultButton(
                text = appStringResource(Res.string.gauge_reset_action),
                full = true,
                onClick = {
                    val updated = rule.copy(
                        lastServicedOdometerKm = ride.odometer,
                        lastServicedEpochDays = currentDay.toEpochDays().toLong(),
                        baselineCertainty = BaselineCertainty.CONFIRMED
                    )
                    val next = runCatching { state.saveSchedule(updated) }.getOrNull()
                    if (next != null) {
                        haptic.perform(HapticKind.SUCCESS)
                        onSaveSchedule(next, resetMsg, true)
                    } else {
                        haptic.perform(HapticKind.ERROR)
                    }
                }
            )
            VaultButton(
                text = appStringResource(Res.string.gauge_log_service_action),
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onOpen("service", rule.id) }
            )
        }
    }
}

@Composable
private fun GaugeDetailCardStack(
    rule: ComponentRule,
    wear: Wear,
    rideOdometer: Double,
    today: LocalDate,
    known: Boolean,
    editing: Boolean,
    f: Fmt,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (Double, LocalDate) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val intervalParts = buildList {
        if (rule.intervalKm > 0) add(f.km(rule.intervalKm))
        if (rule.intervalDays > 0) add(f.days(rule.intervalDays))
    }

    Column(modifier = Modifier.fillMaxWidth().clip(shape).background(SurfaceColor).border(1.dp, BorderColor, shape)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = WearCalculator.describeWear(wear, f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            val sub = if (wear.status == WearStatus.UNKNOWN) appStringResource(Res.string.gauge_unknown_hint)
            else appStringResource(Res.string.gauge_current_odo, f.km(rideOdometer))
            Text(text = sub, fontSize = 13.sp, color = TextSecondary)
        }

        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        if (editing) {
            val initKm = if (known) rule.lastServicedOdometerKm.toLong().toString() else ""
            val initDate = if (known && rule.lastServicedEpochDays > 0L) LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt()).toString() else today.toString()
            GaugeLastDoneEditor(initKm, initDate, rideOdometer, today, onSaveEdit, onCancelEdit)
        } else {
            val editDesc = appStringResource(Res.string.gauge_edit_last_desc)
            val lastText = if (!known) appStringResource(Res.string.gauge_last_done_empty)
            else appStringResource(
                Res.string.gauge_last_done_value,
                f.km(rule.lastServicedOdometerKm),
                f.dateFull(LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt()))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onStartEdit)
                    .semantics { contentDescription = editDesc }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = appStringResource(Res.string.gauge_last_done_label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = lastText, fontSize = 14.sp, color = TextSecondary)
                }
                V3Icon(path = "feather_icon/fi_edit-2.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
            }
        }

        if (intervalParts.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = appStringResource(Res.string.gauge_interval_every_label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                val joined = intervalParts.joinToString(appStringResource(Res.string.wear_or))
                Text(text = appStringResource(Res.string.gauge_interval_every_value, joined), fontSize = 14.sp, color = TextSecondary)
            }
        }
    }
}
