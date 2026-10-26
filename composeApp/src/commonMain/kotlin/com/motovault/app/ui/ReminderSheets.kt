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

@Composable
private fun ReminderAction(text: String, onClick: () -> Unit) = VaultButton(text, onClick, kind = ButtonKind.SECONDARY, full = true)

@Composable
internal fun ReminderAccessPanel(access: ReminderAccess, fingerprint: Any? = null) {
    val f = currentFmt
    LaunchedEffect(fingerprint) { if (fingerprint != null) access.refresh() }
    Panel(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        DocumentNote(when {
            access.busy -> "নোটিফিকেশনের অনুমতি যাচাই হচ্ছে"
            !access.allowed -> "ফোনের সেটিংসে নোটিফিকেশনের অনুমতি নেই"
            access.enabled -> "ফোনে জানানোর অনুমতি আছে"
            else -> "ফোনে জানানো বন্ধ আছে"
        })
        DocumentCopy("অ্যাপের ভেতরে তারিখ দেখা যাবে। ফোনের সময় বদলালে রিমাইন্ডারও স্থানীয় সময় অনুসরণ করবে।")
        if (!access.busy) {
            ReminderAction(if (access.enabled && access.allowed) "ফোনে জানানো বন্ধ করুন" else "ফোনে জানানোর অনুমতি দিন") {
                if (access.enabled && access.allowed) access.disable() else access.request()
            }
            if (!access.allowed) {
                DocumentLink("নোটিফিকেশন সেটিংস খুলুন", access.settings)
                DocumentCopy("ফোনের Settings > Notifications > MotoVault থেকেও অনুমতি দিতে পারবেন।")
            }
            DocumentLink("অনুমতি আবার যাচাই করুন", access.refresh)
        }
        if (access.failed) DocumentNote("রিমাইন্ডার আপডেট করা যায়নি। আগের তথ্য রাখা আছে। আবার চেষ্টা করুন।")
        else if (!access.busy && access.enabled && access.allowed) {
            DocumentCopy("${f.n(access.plans.size.toLong())}টি রিমাইন্ডার নির্ধারিত আছে।")
            access.plans.minByOrNull { it.localDateTime() }?.let { DocumentCopy("পরের সময়: ${f.d(it.localDateTime().toString().replace('T', ' '))}") }
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
    Sheet("রিমাইন্ডার", onDismiss = onDismiss, footer = {
        if (failed) Text("সেভ হয়নি। আগের তথ্য রাখা আছে। আবার চেষ্টা করুন।", color = ErrorColor, fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (saved) Text("রিমাইন্ডারের সময় সেভ হয়েছে", fontSize = 14.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (id == null) VaultButton(if (saving) "সেভ হচ্ছে" else "সেভ করুন", onClick = { persist(state.copy(reminderPolicy = ReminderPolicy(lead, minute))) }, full = true, enabled = !saving)
        else VaultButton("ফিরে যান", onClick = onDismiss, full = true, kind = ButtonKind.SECONDARY)
    }) {
        ReminderAccessPanel(access, state)
        if (id != null) {
            Column(Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (doc == null && rule == null) DocumentNote("এই রেকর্ড বদলেছে বা মুছে গেছে। তালিকা দেখে নিন।")
            else {
                DocumentNote(doc?.title ?: rule!!.name)
                if (doc != null && !state.docReminders) DocumentCopy("কাগজের রিমাইন্ডার আমি পাতায় বন্ধ আছে।")
                if (rule != null && !state.serviceReminders) DocumentCopy("সার্ভিসের রিমাইন্ডার আমি পাতায় বন্ধ আছে।")
                if (rule != null && rule.baselineCertainty != com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) DocumentCopy("শেষ সার্ভিস নিশ্চিত নয়। সার্ভিসের তথ্য দিলে সময় নির্ধারণ করা যাবে।")
                if (doc != null) DocumentCopy(if (doc.expiry.isBlank()) "মেয়াদ নেই। মেয়াদের রিমাইন্ডার নেই।" else "মেয়াদ: ${doc.expiry}")
                val enabled = doc?.reminder ?: rule!!.reminder
                VaultSwitch("এই রেকর্ডের রিমাইন্ডার", checked = enabled, onCheckedChange = { value ->
                    persist(if (doc != null) state.copy(documents = state.documents.map { if (it.id == doc.id) it.copy(reminder = value) else it })
                    else state.copy(rules = state.rules.map { if (it.id == rule!!.id) it.copy(reminder = value) else it }))
                })
                if (doc != null && doc.expiry.isNotBlank()) {
                    DocumentNote("মেয়াদের কত দিন আগে")
                    VaultSegmented(listOf(30 to "৩০ দিন", 14 to "১৪ দিন", 7 to "৭ দিন"), doc.reminderWarningDays ?: state.reminderPolicy.warningDays, onSelect = { value ->
                        persist(state.copy(documents = state.documents.map { if (it.id == doc.id) it.copy(reminderWarningDays = value) else it }))
                    })
                }
                DocumentCopy("পরে জানানো সার্ভিস বা নবায়ন সম্পন্ন করে না।")
                (doc?.reminderSnoozedUntil ?: rule?.reminderSnoozedUntil)?.let { DocumentNote("পরে জানাবেন: ${f.d(it.replace('T', ' '))}") }
                if ((doc != null && doc.expiry.isNotBlank()) || rule?.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) {
                    ReminderAction("আগামীকাল সকাল ৯টা") { snooze(1) }
                    ReminderAction("৭ দিন পরে সকাল ৯টা") { snooze(7) }
                }
            }
            }
        } else {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DocumentNote("মেয়াদের কত দিন আগে")
                VaultSegmented(listOf(30 to "৩০ দিন", 14 to "১৪ দিন", 7 to "৭ দিন"), lead, onSelect = { lead = it })
            }
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DocumentNote("ফোনের স্থানীয় সময়")
                VaultSegmented(listOf(540 to "সকাল ৯টা", 1080 to "সন্ধ্যা ৬টা"), minute, onSelect = { minute = it })
                DocumentCopy("এখন: ${f.n(lead.toLong())} দিন আগে, ${f.n((minute / 60).toLong())}:${f.d((minute % 60).toString().padStart(2, '0'))}")
                if (state.documents.isEmpty() && state.rules.isEmpty()) DocumentNote("কাগজ বা সার্ভিসের সময়সূচি যোগ করলে এখানে দেখা যাবে।")
            }
            state.documents.forEach { ReminderAction(it.title) { onOpen("document", it.id) } }
            state.rules.forEach { ReminderAction(it.name) { onOpen("service", it.id) } }
        }
    }
}
