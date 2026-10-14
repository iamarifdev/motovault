package com.motovault.app.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable
internal data class RenewalReminder(val key: String, val documentId: String, val expiry: String, val date: String, val kind: String)

/** Event identity includes the full durable ID and expiry, independent of display text or platform integers. */
internal fun renewalPlan(documents: List<VaultDocument>, now: LocalDateTime, previous: List<RenewalReminder>, consumed: Set<String>): List<RenewalReminder> {
    val today = now.date
    val nextMorning = if (now.time < LocalTime(9, 0)) today else LocalDate.fromEpochDays(today.toEpochDays() + 1)
    return documents.flatMap { document ->
        val expiry = runCatching { LocalDate.parse(document.expiry) }.getOrNull() ?: return@flatMap emptyList()
        if (expiry < today) return@flatMap emptyList()
        fun event(kind: String, date: LocalDate) = RenewalReminder("renewal:${document.id.length}:${document.id}:${document.expiry}:$kind", document.id, document.expiry, date.toString(), kind)
        val warningDate = LocalDate.fromEpochDays(expiry.toEpochDays() - RENEWAL_WARNING_DAYS)
        val warning = event("warning", warningDate)
        val remembered = previous.firstOrNull { it.key == warning.key }
        val warningPlan = when {
            remembered != null -> remembered
            warningDate >= today && (warningDate > today || now.time < LocalTime(9, 0)) -> warning
            expiry > nextMorning -> warning.copy(date = nextMorning.toString())
            else -> null // Expiry-day alert replaces an otherwise redundant late warning.
        }
        val expiryPlan = event("expiry", expiry)
        listOfNotNull(warningPlan, expiryPlan).filter { it.key !in consumed }
    }
}

internal fun RenewalReminder.localDateTime() = LocalDateTime(LocalDate.parse(date), LocalTime(9, 0))
internal expect suspend fun reconcileRenewalReminders(documents: List<VaultDocument>)
@Composable internal expect fun RenewalReminderControl()
