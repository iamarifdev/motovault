package com.motovault.app.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.*
import motovault.composeapp.generated.resources.*

@Composable
private fun ReminderAction(text: String, onClick: () -> Unit) = VaultButton(text, onClick, kind = ButtonKind.SECONDARY, full = true)

@Composable
internal fun ReminderAccessPanel(access: ReminderAccess, fingerprint: Any? = null) {
    val f = currentFmt
    LaunchedEffect(fingerprint) { if (fingerprint != null) access.refresh() }
    Panel(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        DocumentNote(when {
            access.busy -> appStringResource(Res.string.reminder_checking_permission)
            !access.allowed -> appStringResource(Res.string.reminder_permission_missing)
            access.enabled -> appStringResource(Res.string.reminder_permission_granted)
            else -> appStringResource(Res.string.reminder_notifications_off)
        })
        DocumentCopy(appStringResource(Res.string.reminder_in_app_note))
        if (!access.busy) {
            ReminderAction(appStringResource(if (access.enabled && access.allowed) Res.string.reminder_disable else Res.string.reminder_enable)) {
                if (access.enabled && access.allowed) access.disable() else access.request()
            }
            if (!access.allowed) {
                DocumentLink(appStringResource(Res.string.reminder_open_settings), access.settings)
                DocumentCopy(appStringResource(Res.string.reminder_settings_hint))
            }
            DocumentLink(appStringResource(Res.string.reminder_recheck), access.refresh)
        }
        if (access.failed) DocumentNote(appStringResource(Res.string.reminder_update_failed))
        else if (!access.busy && access.enabled && access.allowed) {
            DocumentCopy(appStringResource(Res.string.reminder_count, f.n(access.plans.size.toLong())))
            access.plans.minByOrNull { it.localDateTime() }?.let { DocumentCopy(appStringResource(Res.string.reminder_next, f.d(it.localDateTime().toString().replace('T', ' ')))) }
        }
    }
}

@Composable
internal fun ReminderSettingsSheet(
    state: VaultState, type: String?, id: String?, onDismiss: () -> Unit,
    onOpen: (String, String) -> Unit, onSave: (VaultState, (Boolean) -> Unit) -> Unit
) {
    val access = rememberReminderAccess()
    val f = currentFmt
    var lead by remember { mutableStateOf(state.reminderPolicy.warningDays) }
    var minute by remember { mutableStateOf(state.reminderPolicy.minuteOfDay) }
    var failed by remember(id, type) { mutableStateOf(false) }
    var saved by remember(id, type) { mutableStateOf(false) }
    var saving by remember(id, type) { mutableStateOf(false) }
    val doc = state.documents.firstOrNull { type == "document" && it.id == id }
    val rule = state.confirmedRules().firstOrNull { type == "service" && it.id == id }
    fun persist(next: VaultState) {
        if (saving) return
        saving = true
        onSave(next) { ok -> saving = false; failed = !ok; saved = ok; if (ok) access.refresh() }
    }
    fun snooze(days: Int) {
        val date = LocalDate.fromEpochDays(today().toEpochDays() + days)
        val until = LocalDateTime(date, LocalTime(9, 0)).toString()
        persist(if (doc != null) state.copy(documents = state.documents.map { if (it.id == doc.id) it.copy(reminderSnoozedUntil = until) else it })
            else state.copy(rules = state.rules.map { if (it.id == rule?.id) it.copy(reminderSnoozedUntil = until) else it }))
    }
    Sheet(appStringResource(Res.string.reminder_title), onDismiss = onDismiss, footer = {
        if (failed) Text(appStringResource(Res.string.reminder_save_failed), color = ErrorColor, fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (saved) Text(appStringResource(Res.string.reminder_saved), fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (id == null) VaultButton(appStringResource(if (saving) Res.string.reminder_saving else Res.string.reminder_save), onClick = { persist(state.copy(reminderPolicy = ReminderPolicy(lead, minute))) }, full = true, enabled = !saving)
        else VaultButton(appStringResource(Res.string.reminder_back), onClick = onDismiss, full = true, kind = ButtonKind.SECONDARY)
    }) {
        ReminderAccessPanel(access, state)
        if (id != null) {
            Column(Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (doc == null && rule == null) DocumentNote(appStringResource(Res.string.reminder_record_gone))
            else {
                DocumentNote(doc?.title ?: rule!!.name)
                if (doc != null && !state.docReminders) DocumentCopy(appStringResource(Res.string.reminder_doc_off))
                if (rule != null && !state.serviceReminders) DocumentCopy(appStringResource(Res.string.reminder_service_off))
                if (rule != null && rule.baselineCertainty != com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) DocumentCopy(appStringResource(Res.string.reminder_baseline_uncertain))
                if (doc != null) DocumentCopy(if (doc.expiry.isBlank()) appStringResource(Res.string.reminder_no_expiry) else appStringResource(Res.string.reminder_expiry, doc.expiry))
                val enabled = doc?.reminder ?: rule!!.reminder
                VaultSwitch(appStringResource(Res.string.reminder_record_toggle), checked = enabled, onCheckedChange = { value ->
                    persist(if (doc != null) state.copy(documents = state.documents.map { if (it.id == doc.id) it.copy(reminder = value) else it })
                    else state.copy(rules = state.rules.map { if (it.id == rule!!.id) it.copy(reminder = value) else it }))
                })
                if (doc != null && doc.expiry.isNotBlank()) {
                    DocumentNote(appStringResource(Res.string.reminder_lead_days))
                    VaultSegmented(listOf(30 to appStringResource(Res.string.reminder_days_30), 14 to appStringResource(Res.string.reminder_days_14), 7 to appStringResource(Res.string.reminder_days_7)), doc.reminderWarningDays ?: state.reminderPolicy.warningDays, onSelect = { value ->
                        persist(state.copy(documents = state.documents.map { if (it.id == doc.id) it.copy(reminderWarningDays = value) else it }))
                    })
                }
                DocumentCopy(appStringResource(Res.string.reminder_snooze_note))
                (doc?.reminderSnoozedUntil ?: rule?.reminderSnoozedUntil)?.let { DocumentNote(appStringResource(Res.string.reminder_snoozed_until, f.d(it.replace('T', ' ')))) }
                if ((doc != null && doc.expiry.isNotBlank()) || rule?.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) {
                    ReminderAction(appStringResource(Res.string.reminder_tomorrow_9)) { snooze(1) }
                    ReminderAction(appStringResource(Res.string.reminder_in_7_days_9)) { snooze(7) }
                }
            }
            }
        } else {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DocumentNote(appStringResource(Res.string.reminder_lead_days))
                VaultSegmented(listOf(30 to appStringResource(Res.string.reminder_days_30), 14 to appStringResource(Res.string.reminder_days_14), 7 to appStringResource(Res.string.reminder_days_7)), lead, onSelect = { lead = it })
            }
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DocumentNote(appStringResource(Res.string.reminder_local_time))
                VaultSegmented(listOf(540 to appStringResource(Res.string.reminder_morning_9), 1080 to appStringResource(Res.string.reminder_evening_6)), minute, onSelect = { minute = it })
                DocumentCopy(appStringResource(Res.string.reminder_current_summary, f.n(lead.toLong()), "${f.n((minute / 60).toLong())}:${f.d((minute % 60).toString().padStart(2, '0'))}"))
                if (state.documents.isEmpty() && state.rules.isEmpty()) DocumentNote(appStringResource(Res.string.reminder_empty))
            }
            state.documents.forEach { ReminderAction(it.title) { onOpen("document", it.id) } }
            state.rules.forEach { ReminderAction(it.name) { onOpen("service", it.id) } }
        }
    }
}
