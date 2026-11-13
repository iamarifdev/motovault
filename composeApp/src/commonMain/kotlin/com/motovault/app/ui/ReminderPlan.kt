package com.motovault.app.ui

import com.motovault.app.domain.garage.activeReminderDocuments
import com.motovault.app.domain.garage.allBikes
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.reminderRevision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun reminderPlan(state: VaultState, now: LocalDateTime, previous: List<RenewalReminder>, consumed: Set<String>): List<RenewalReminder> {
    val documents = if (state.docReminders) renewalPlan(state.activeReminderDocuments(), now, previous, consumed, state.reminderPolicy) else emptyList()
    val ride = state.ride ?: return documents
    if (ride.archived || !state.serviceReminders) return documents
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
            snooze.hour * 60 + snooze.minute, 0, "service", revision, vehicleId = ride.id
        ).takeUnless { it.key in consumed }
        if (!distanceDue && calendarDue == null) return@mapNotNull null
        val due = if (distanceDue || calendarDue!! < nextLocal) nextLocal else calendarDue
        val prior = previous.firstOrNull { it.key == key && it.minuteOfDay == state.reminderPolicy.minuteOfDay }
        val event = prior?.takeUnless { distanceDue && it.localDateTime() > LocalDateTime(nextLocal, time) }
            ?: RenewalReminder(key, rule.id, due.toString(), due.toString(), "service",
            state.reminderPolicy.minuteOfDay, 0, "service", revision, vehicleId = ride.id)
        event.takeUnless { it.key in consumed }
    }
    val warranties = state.warranties.mapNotNull { w ->
        if (!w.reminder || w.isExpired(now.date, ride.odometer)) return@mapNotNull null
        val expiry = runCatching { LocalDate.parse(w.expiryDate) }.getOrNull() ?: return@mapNotNull null
        val key = "warranty:${w.id.length}:${w.id}:${w.expiryDate}:due"
        val due = if (expiry < nextLocal) nextLocal else expiry
        RenewalReminder(
            key, w.id, w.expiryDate, due.toString(), "warranty",
            state.reminderPolicy.minuteOfDay, 0, "warranty", null, vehicleId = ride.id
        ).takeUnless { it.key in consumed }
    }
    return (documents + services + warranties).sortedBy { it.localDateTime() }
}

internal data class ReminderTarget(
    val type: String,
    val id: String,
    val title: String,
    val digits: Digits = Digits.BN,
    val language: AppLanguage = AppLanguage.BN,
    val vehicleId: String? = null
)

internal expect fun reminderHeading(plan: RenewalReminder, target: ReminderTarget): String
internal expect fun reminderMessage(plan: RenewalReminder, target: ReminderTarget): String
internal expect fun localizedMonthName(language: AppLanguage, monthIndex: Int): String

internal fun reminderDate(plan: RenewalReminder, target: ReminderTarget): String {
    val date = LocalDate.parse(plan.expiry)
    val format = Fmt(target.digits, target.language, "", "", "")
    return "${format.d(date.dayOfMonth.toString())} ${localizedMonthName(target.language, date.monthNumber - 1)} ${format.d(date.year.toString())}"
}

/** Never resolve by label, array index or an integer hash. Stale notifications explain instead of opening another record. */
internal fun reminderTarget(state: VaultState, plan: RenewalReminder): ReminderTarget? = when (plan.targetType) {
    "document" -> state.activeReminderDocuments().firstOrNull { it.id == plan.documentId && it.expiry == plan.expiry }
        ?.let {
            ReminderTarget(
                type = "document",
                id = it.id,
                title = it.title,
                digits = state.digits,
                language = state.language,
                vehicleId = it.vehicleId.takeUnless { _ -> it.isOwnerScoped }
            )
        }
    "service" -> activeBikeRules(state).firstNotNullOfOrNull { (bikeId, bikeRules) ->
        bikeRules.firstOrNull { it.id == plan.documentId && it.reminderRevision() == plan.targetRevision }
            ?.let { ReminderTarget("service", it.id, it.name, state.digits, state.language, vehicleId = bikeId) }
    }
    "warranty" -> activeBikeWarranties(state).firstOrNull { (_, w) -> w.id == plan.documentId && w.expiryDate == plan.expiry }
        ?.let { (bikeId, w) -> ReminderTarget("service", w.id, w.partName, state.digits, state.language, vehicleId = bikeId) }
    else -> null
}

private fun activeBikeRules(state: VaultState): List<Pair<String, List<ComponentRule>>> {
    val current = state.ride?.takeUnless { it.archived }?.let { it.id to state.rules }
    val others = state.garageBikes.filterNot { it.ride.archived }.map { it.ride.id to it.rules }
    return listOfNotNull(current) + others
}

private fun activeBikeWarranties(state: VaultState): List<Pair<String, com.motovault.app.domain.model.PartWarranty>> {
    val current = state.ride?.takeUnless { it.archived }?.let { r -> state.warranties.map { r.id to it } }.orEmpty()
    val others = state.garageBikes.filterNot { it.ride.archived }.flatMap { b -> b.warranties.map { b.ride.id to it } }
    return current + others
}

/** Delivery consumes a scheduling event, but a current alert stays visible until the owner opens or dismisses it. */
internal fun deliveredReminderIsCurrent(state: VaultState, plan: RenewalReminder): Boolean = runCatching {
    if (reminderTarget(state, plan) == null) return@runCatching false
    when (plan.targetType) {
        "document" -> {
            val document = state.activeReminderDocuments().first { it.id == plan.documentId }
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
            val activeRules = activeBikeRules(state).flatMap { (_, bikeRules) ->
                bikeRules.filter { it.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.CONFIRMED }
            }
            val rule = activeRules.first { it.id == plan.documentId }
            if (state.ride == null || !state.serviceReminders || !rule.reminder) return@runCatching false
            val snooze = rule.reminderSnoozedUntil?.let { LocalDateTime.parse(it) }
            if (snooze == null) plan.key.endsWith(":due")
            else plan.localDateTime() == snooze && plan.key.endsWith(":snooze:$snooze")
        }
        "warranty" -> {
            val warranty = activeBikeWarranties(state).first { (_, w) -> w.id == plan.documentId }.second
            val rideOdo = state.allBikes().firstOrNull { it.id == plan.vehicleId }?.odometer ?: state.ride?.odometer ?: 0.0
            state.serviceReminders && warranty.reminder && !warranty.isExpired(Clock.System.todayIn(TimeZone.currentSystemDefault()), rideOdo)
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
