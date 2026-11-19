package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.privacy_auto_lock_15m
import motovault.composeapp.generated.resources.privacy_auto_lock_1m
import motovault.composeapp.generated.resources.privacy_auto_lock_5m
import motovault.composeapp.generated.resources.privacy_auto_lock_title
import motovault.composeapp.generated.resources.reminder_days_14
import motovault.composeapp.generated.resources.reminder_days_30
import motovault.composeapp.generated.resources.reminder_days_7
import motovault.composeapp.generated.resources.reminder_evening_6
import motovault.composeapp.generated.resources.reminder_lead_days
import motovault.composeapp.generated.resources.reminder_morning_9
import motovault.composeapp.generated.resources.settings_check_notifications
import motovault.composeapp.generated.resources.settings_doc_reminders_short_desc
import motovault.composeapp.generated.resources.settings_doc_reminders_title
import motovault.composeapp.generated.resources.settings_lock_details_action
import motovault.composeapp.generated.resources.settings_lock_switch_desc
import motovault.composeapp.generated.resources.settings_reminder_when
import motovault.composeapp.generated.resources.settings_row_lock
import motovault.composeapp.generated.resources.settings_sale_brta_desc
import motovault.composeapp.generated.resources.settings_sale_privacy_desc
import motovault.composeapp.generated.resources.settings_sale_privacy_title
import motovault.composeapp.generated.resources.settings_service_reminders_short_desc
import motovault.composeapp.generated.resources.settings_service_reminders_title

private const val MINUTE_MORNING_9AM = 540
private const val MINUTE_EVENING_6PM = 1080

@Composable
internal fun RemindersSettingsContent(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onOpenSheet: (String) -> Unit
) {
    val f = currentFmt
    val minute = state.reminderPolicy.minuteOfDay
    val hh = f.d((minute / 60).toString().padStart(2, '0'))
    val mm = f.d((minute % 60).toString().padStart(2, '0'))
    val timeText = "$hh:$mm"

    SettingsCardStack {
        VaultSwitch(
            title = appStringResource(Res.string.settings_service_reminders_title),
            subtitle = appStringResource(Res.string.settings_service_reminders_short_desc),
            checked = state.serviceReminders,
            onCheckedChange = { onUpdateState(state.copy(serviceReminders = it)) }
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        VaultSwitch(
            title = appStringResource(Res.string.settings_doc_reminders_title),
            subtitle = appStringResource(Res.string.settings_doc_reminders_short_desc),
            checked = state.docReminders,
            onCheckedChange = { onUpdateState(state.copy(docReminders = it)) }
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.reminder_lead_days), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    30 to appStringResource(Res.string.reminder_days_30),
                    14 to appStringResource(Res.string.reminder_days_14),
                    7 to appStringResource(Res.string.reminder_days_7)
                ),
                selected = state.reminderPolicy.warningDays,
                onSelect = { onUpdateState(state.copy(reminderPolicy = state.reminderPolicy.copy(warningDays = it))) }
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.settings_reminder_when, timeText), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    MINUTE_MORNING_9AM to appStringResource(Res.string.reminder_morning_9),
                    MINUTE_EVENING_6PM to appStringResource(Res.string.reminder_evening_6)
                ),
                selected = minute,
                onSelect = { onUpdateState(state.copy(reminderPolicy = state.reminderPolicy.copy(minuteOfDay = it))) }
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            VaultButton(
                text = appStringResource(Res.string.settings_check_notifications),
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onOpenSheet("reminders") }
            )
        }
    }
}

@Composable
internal fun LockSettingsContent(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onOpenSheet: (String) -> Unit
) {
    SettingsCardStack {
        VaultSwitch(
            title = appStringResource(Res.string.settings_row_lock),
            subtitle = appStringResource(Res.string.settings_lock_switch_desc),
            checked = state.privacy.enabled,
            onCheckedChange = { onOpenSheet("privacy") }
        )
        if (state.privacy.enabled) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.privacy_auto_lock_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                VaultSegmented(
                    options = listOf(
                        60 to appStringResource(Res.string.privacy_auto_lock_1m),
                        300 to appStringResource(Res.string.privacy_auto_lock_5m),
                        900 to appStringResource(Res.string.privacy_auto_lock_15m)
                    ),
                    selected = state.privacy.timeoutSeconds,
                    onSelect = { seconds -> onUpdateState(state.copy(privacy = state.privacy.copy(timeoutSeconds = seconds))) }
                )
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            VaultButton(
                text = appStringResource(Res.string.settings_lock_details_action),
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onOpenSheet("privacy") }
            )
        }
    }
}

@Composable
internal fun SaleSettingsContent() {
    SettingsCardStack {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = appStringResource(Res.string.settings_sale_privacy_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = appStringResource(Res.string.settings_sale_privacy_desc), fontSize = 13.sp, color = TextSecondary)
            Text(text = appStringResource(Res.string.settings_sale_brta_desc), fontSize = 13.sp, color = TextSecondary)
        }
    }
}
