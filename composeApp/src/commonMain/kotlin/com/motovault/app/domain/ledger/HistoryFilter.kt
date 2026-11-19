package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.datetime.LocalDate

/** Three-bucket spend breakdown matching the History cost card bar and legend. */
data class CostBreakdown(
    val fuelPoisha: Long,
    val servicePoisha: Long,
    val otherPoisha: Long
)

/** Aggregates [CostSummary.categories] into fuel, service, and other (renewal + extra expenses). */
fun CostSummary.toBreakdown(): CostBreakdown = CostBreakdown(
    fuelPoisha = categories[CostCategory.FUEL] ?: 0L,
    servicePoisha = categories[CostCategory.SERVICE] ?: 0L,
    otherPoisha = (categories[CostCategory.RENEWAL] ?: 0L) +
        (categories[CostCategory.PARKING] ?: 0L) +
        (categories[CostCategory.TOLL] ?: 0L) +
        (categories[CostCategory.CUSTOM] ?: 0L)
)

/**
 * Filters timeline rows for History.
 * Extra expenses belong in the cost card and document fees count in "Other" only;
 * fuel and service logs (including quotes/approved/cancelled visits) remain in the timeline.
 */
fun filterHistoryTimeline(
    logs: List<RideLog>,
    today: LocalDate,
    period: CostPeriod,
    category: CostCategory? = null
): List<RideLog> {
    val start = CostLedger.start(period, today)
    return logs.filter { log ->
        if (log.kind == "expense" || log.isDocumentFee) return@filter false
        val date = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@filter false
        if (date > today || (start != null && date < start)) return@filter false
        category == null || CostLedger.category(log) == category
    }.sortedWith(compareByDescending<RideLog> { it.date }.thenByDescending { it.odometer })
}
