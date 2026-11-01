package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.maintenance.MaintenanceAction
import com.motovault.app.domain.maintenance.ScheduleSource
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
internal fun GaugeDetailSheet(state: VaultState, id: String?, onDismiss: () -> Unit, onOpen: (String, String?) -> Unit) {
    val ride = state.ride ?: return
    val rule = state.confirmedRules().firstOrNull { it.id == id }
    val f = currentFmt
    val actionLabel = rule?.let {
        appStringResource(when (it.action) {
            MaintenanceAction.INSPECT -> Res.string.action_inspect
            MaintenanceAction.CLEAN -> Res.string.action_clean
            MaintenanceAction.LUBRICATE -> Res.string.action_lubricate
            MaintenanceAction.ADJUST -> Res.string.action_adjust
            MaintenanceAction.REPLACE -> Res.string.action_replace
        })
    }
    val sourceLabel = rule?.let {
        when (it.provenance.source) {
            ScheduleSource.MANUAL -> appStringResource(Res.string.schedule_source_manual)
            ScheduleSource.DEALER -> appStringResource(Res.string.schedule_source_dealer)
            ScheduleSource.OWNER -> appStringResource(Res.string.schedule_source_owner)
        }
    }
    Sheet(rule?.name ?: appStringResource(Res.string.schedule_details_title), onDismiss = onDismiss) {
        Text(appStringResource(Res.string.gauge_physical_note), fontSize = 14.sp, color = TextSecondary)
        if (rule == null) Text(appStringResource(Res.string.schedule_missing), fontSize = 14.sp)
        else {
            val wear = WearCalculator.wearOf(rule, ride.odometer, today().toEpochDays().toLong())
            Panel {
                DocumentNote(if (wear.status == WearStatus.UNKNOWN) appStringResource(Res.string.schedule_unknown_baseline) else appStringResource(Res.string.schedule_next_due) + " · " + f.n(wear.life.toLong()) + "%")
                DocumentCopy(WearCalculator.describeWear(wear, f))
                DocumentNote("${appStringResource(Res.string.schedule_source)} · $sourceLabel · $actionLabel")
                DocumentCopy(
                    if (rule.provenance.isManualBacked) rule.provenance.displayReference
                    else appStringResource(Res.string.schedule_owner_defined)
                )
                if (rule.provenance.isManualBacked) {
                    DocumentCopy("${appStringResource(Res.string.schedule_applicability)}: ${rule.provenance.applicability}")
                    val uri = LocalUriHandler.current
                    TextButton(onClick = { rule.provenance.url.takeIf { it.isNotBlank() }?.let(uri::openUri) }) {
                        Text(appStringResource(Res.string.schedule_manual_open), color = BrandColor)
                    }
                }
                DocumentCopy(
                    "${if (rule.intervalKm > 0) f.km(rule.intervalKm) else appStringResource(Res.string.schedule_no_distance_limit)} · " +
                        "${if (rule.intervalDays > 0) f.days(rule.intervalDays) else appStringResource(Res.string.schedule_no_time_limit)}, " +
                        appStringResource(Res.string.schedule_whichever_first)
                )
                DocumentNote(appStringResource(Res.string.schedule_baseline_basis))
                DocumentCopy(
                    if (wear.status == WearStatus.UNKNOWN) appStringResource(Res.string.schedule_unknown_baseline)
                    else "${f.km(rule.lastServicedOdometerKm)} · ${f.date(LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt()))}"
                )
                DocumentNote(appStringResource(Res.string.schedule_last_confirmed_km))
                DocumentCopy("${f.km(ride.odometer)} · ${ride.odometerConfirmedDate ?: appStringResource(Res.string.schedule_date_unknown)}")
                DocumentCopy(appStringResource(Res.string.schedule_old_km_hint))
            }
            VaultButton(appStringResource(Res.string.schedule_edit_intervals), onClick = { onOpen("schedule", rule.id) }, full = true)
            VaultButton(appStringResource(Res.string.schedule_confirm_odometer), onClick = { onOpen("odometer", null) }, full = true, kind = ButtonKind.SECONDARY)
            VaultButton(appStringResource(Res.string.schedule_log_service), onClick = { onOpen("service", rule.id) }, full = true, kind = ButtonKind.SECONDARY)
            VaultButton(appStringResource(Res.string.schedule_reminder), onClick = { onOpen("serviceReminder", rule.id) }, full = true, kind = ButtonKind.SECONDARY)
        }
    }
}
