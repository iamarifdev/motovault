package com.motovault.app.ui

import com.motovault.app.domain.model.ComponentRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun ComponentRule.reminderRevision() = "$lastServicedOdometerKm:$lastServicedEpochDays:$intervalKm:$intervalDays"

internal fun reminderPlan(state: VaultState, now: LocalDateTime, previous: List<RenewalReminder>, consumed: Set<String>): List<RenewalReminder> {
    val documents = if (state.docReminders) renewalPlan(state.documents, now, previous, consumed, state.reminderPolicy) else emptyList()
    val ride = state.ride ?: return documents
    if (!state.serviceReminders) return documents
    val time = LocalTime(state.reminderPolicy.minuteOfDay / 60, state.reminderPolicy.minuteOfDay % 60)
    val nextLocal = if (now.time < time) now.date else LocalDate.fromEpochDays(now.date.toEpochDays() + 1)
    val services = state.confirmedRules().mapNotNull { rule ->
        if (rule.baselineCertainty != com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) return@mapNotNull null
        if (!rule.lastServicedOdometerKm.isFinite() || rule.lastServicedOdometerKm < 0 || rule.lastServicedEpochDays < 0 ||
            !rule.intervalKm.isFinite() || rule.intervalKm !in 0.0..1000000.0 || rule.intervalDays !in 0..10000 ||
            (rule.intervalKm == 0.0 && rule.intervalDays == 0)) return@mapNotNull null
        if (!rule.reminder || rule.lastServicedOdometerKm > ride.odometer || rule.lastServicedEpochDays > now.date.toEpochDays()) return@mapNotNull null
        val distanceDue = rule.intervalKm > 0 && ride.odometer - rule.lastServicedOdometerKm >= rule.intervalKm
        val calendarDue = if (rule.intervalDays > 0) LocalDate.fromEpochDays((rule.lastServicedEpochDays + rule.intervalDays).toInt()) else null
        val revision = rule.reminderRevision()
        val key = "service:${rule.id.length}:${rule.id}:$revision:due"
        val snooze = rule.reminderSnoozedUntil?.let { LocalDateTime.parse(it) }
        if (snooze != null) return@mapNotNull RenewalReminder(
            key + ":snooze:$snooze", rule.id, snooze.date.toString(), snooze.date.toString(), "service",
            snooze.hour * 60 + snooze.minute, 0, "service", revision
        ).takeUnless { it.key in consumed }
        if (!distanceDue && calendarDue == null) return@mapNotNull null
        val due = if (distanceDue || calendarDue!! < nextLocal) nextLocal else calendarDue
        val prior = previous.firstOrNull { it.key == key && it.minuteOfDay == state.reminderPolicy.minuteOfDay }
        val event = prior?.takeUnless { distanceDue && it.localDateTime() > LocalDateTime(nextLocal, time) }
            ?: RenewalReminder(key, rule.id, due.toString(), due.toString(), "service",
            state.reminderPolicy.minuteOfDay, 0, "service", revision)
        event.takeUnless { it.key in consumed }
    }
    return (documents + services).sortedBy { it.localDateTime() }
}

internal data class ReminderTarget(val type: String, val id: String, val title: String, val digits: Digits = Digits.BN)

internal fun reminderHeading(plan: RenewalReminder) = if (plan.targetType == "service") "সার্ভিসের রিমাইন্ডার" else "কাগজের রিমাইন্ডার"

internal fun reminderMessage(plan: RenewalReminder, target: ReminderTarget) = if (target.type == "service")
    "${target.title} · রেকর্ড করা সার্ভিসের ব্যবধান দেখুন।"
else "${target.title} · মেয়াদ শেষ: ${Fmt(target.digits).dateFull(LocalDate.parse(plan.expiry))}"

/** Never resolve by label, array index or an integer hash. Stale notifications explain instead of opening another record. */
internal fun reminderTarget(state: VaultState, plan: RenewalReminder): ReminderTarget? = when (plan.targetType) {
    "document" -> state.documents.firstOrNull { it.id == plan.documentId && it.expiry == plan.expiry }
        ?.let { ReminderTarget("document", it.id, it.title, state.digits) }
    "service" -> state.rules.firstOrNull { it.id == plan.documentId && it.reminderRevision() == plan.targetRevision }
        ?.let { ReminderTarget("service", it.id, it.name, state.digits) }
    else -> null
}

/** Delivery consumes a scheduling event, but a current alert stays visible until the owner opens or dismisses it. */
internal fun deliveredReminderIsCurrent(state: VaultState, plan: RenewalReminder): Boolean = runCatching {
    if (reminderTarget(state, plan) == null) return@runCatching false
    when (plan.targetType) {
        "document" -> {
            val document = state.documents.first { it.id == plan.documentId }
            if (!state.docReminders || !document.reminder) return@runCatching false
            LocalDate.parse(document.expiry)
            val snooze = document.reminderSnoozedUntil?.let { LocalDateTime.parse(it) }
            when (plan.kind) {
                "snooze" -> snooze == plan.localDateTime()
                "warning", "expiry" -> snooze == null || plan.localDateTime() > snooze
                else -> false
            }
        }
        "service" -> {
            val rule = state.confirmedRules().first { it.id == plan.documentId }
            if (state.ride == null || !state.serviceReminders || !rule.reminder ||
                rule.baselineCertainty != com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) return@runCatching false
            val snooze = rule.reminderSnoozedUntil?.let { LocalDateTime.parse(it) }
            if (snooze == null) plan.key.endsWith(":due")
            else plan.localDateTime() == snooze && plan.key.endsWith(":snooze:$snooze")
        }
        else -> false
    }
}.getOrDefault(false)

object ReminderNavigation {
    private val request = MutableStateFlow<String?>(null)
    internal val requests = request.asStateFlow()
    fun open(payload: String) { request.value = payload }
    internal fun handled(payload: String) { if (request.value == payload) request.value = null }
}

internal fun reminderPayload(plan: RenewalReminder) = Json.encodeToString(plan)
internal fun decodeReminderPayload(payload: String) = runCatching {
    require(payload.length <= 8192)
    Json { ignoreUnknownKeys = true }.decodeFromString<RenewalReminder>(payload)
}.getOrNull()
