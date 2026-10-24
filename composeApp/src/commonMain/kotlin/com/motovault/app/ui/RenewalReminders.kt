package com.motovault.app.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable
internal data class RenewalReminder(
    val key: String, val documentId: String, val expiry: String, val date: String, val kind: String,
    val minuteOfDay: Int = 540,
    val warningDays: Int = 60,
    val targetType: String = "document",
    val targetRevision: String? = null
)

/** Event identity includes the full durable ID and expiry, independent of display text or platform integers. */
internal fun renewalPlan(documents: List<VaultDocument>, now: LocalDateTime, previous: List<RenewalReminder>, consumed: Set<String>, policy: ReminderPolicy = ReminderPolicy()): List<RenewalReminder> {
    val today = now.date
    val localTime = LocalTime(policy.minuteOfDay / 60, policy.minuteOfDay % 60)
    val nextMorning = if (now.time < localTime) today else LocalDate.fromEpochDays(today.toEpochDays() + 1)
    return documents.flatMap { document ->
        if (!document.reminder) return@flatMap emptyList()
        val expiry = runCatching { LocalDate.parse(document.expiry) }.getOrNull() ?: return@flatMap emptyList()
        val snooze = document.reminderSnoozedUntil?.let { LocalDateTime.parse(it) }
        if (expiry < today && snooze == null) return@flatMap emptyList()
        val lead = document.reminderWarningDays ?: policy.warningDays
        require(lead in 1..365)
        fun event(kind: String, date: LocalDate) = RenewalReminder("renewal:${document.id.length}:${document.id}:${document.expiry}:$kind", document.id, document.expiry, date.toString(), kind, policy.minuteOfDay, lead)
        val warningDate = LocalDate.fromEpochDays(expiry.toEpochDays() - lead)
        val warning = event("warning", warningDate)
        val remembered = previous.firstOrNull { it.key == warning.key && it.warningDays == lead && it.minuteOfDay == policy.minuteOfDay }
        val warningPlan = when {
            remembered != null -> remembered
            warningDate >= today && (warningDate > today || now.time < localTime) -> warning
            expiry > nextMorning -> warning.copy(date = nextMorning.toString())
            else -> null // Expiry-day alert replaces an otherwise redundant late warning.
        }
        val expiryPlan = event("expiry", expiry)
        val snoozed = snooze?.let {
            event("snooze:$it", it.date).copy(kind = "snooze", minuteOfDay = it.hour * 60 + it.minute)
        }
        listOfNotNull(
            snoozed,
            warningPlan?.takeIf { snooze == null || LocalDateTime(warningDate, localTime) > snooze },
            expiryPlan.takeIf { snooze == null || it.localDateTime() > snooze }
        ).filter { it.key !in consumed }.distinctBy { it.localDateTime() }
    }
}

internal fun RenewalReminder.localDateTime() = LocalDateTime(LocalDate.parse(date), LocalTime(minuteOfDay / 60, minuteOfDay % 60))
internal expect suspend fun reconcileRenewalReminders(documents: List<VaultDocument>)
internal data class ReminderAccess(
    val enabled: Boolean, val allowed: Boolean, val busy: Boolean, val failed: Boolean,
    val request: () -> Unit, val disable: () -> Unit, val refresh: () -> Unit, val settings: () -> Unit,
    val plans: List<RenewalReminder> = emptyList()
)
@Composable internal expect fun rememberReminderAccess(): ReminderAccess
@Composable internal fun RenewalReminderControl() = ReminderAccessPanel(rememberReminderAccess())
