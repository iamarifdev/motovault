package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.datetime.LocalDate

private const val RECENT_WINDOW_DAYS = 30
enum class CostCategory(val code: String) {
    FUEL("fuel"), SERVICE("service"), RENEWAL("renewal"), PARKING("parking"), TOLL("toll"), CUSTOM("custom")
}
enum class CostPeriod { RECENT, MONTH, ALL }
enum class CostIssue { DUPLICATE, INVALID_RECORD, MILEAGE }
data class DistanceCoverage(val first: LocalDate, val last: LocalDate, val km: Double, val costPoisha: Long)
data class CostSummary(
    val logs: List<RideLog>, val totalPoisha: Long?, val categories: Map<CostCategory, Long>,
    val coverage: DistanceCoverage?, val issues: Set<CostIssue>
)

object CostLedger {
    fun category(log: RideLog): CostCategory = when {
        log.isFuel -> CostCategory.FUEL
        log.isDocumentFee -> CostCategory.RENEWAL
        log.kind == "expense" -> CostCategory.entries.firstOrNull { it.code == log.expenseCategory } ?: CostCategory.CUSTOM
        else -> CostCategory.SERVICE
    }

    fun amount(log: RideLog): Long? {
        val integer = if (category(log) == CostCategory.SERVICE) log.serviceTotalPoisha?.toLong() ?: log.costPoisha else log.costPoisha
        return if (integer != null) integer.takeIf { it in 0..MAX_COST_POISHA } else Money.fromLegacy(log.cost)
    }

    fun start(period: CostPeriod, today: LocalDate): LocalDate? = when (period) {
        CostPeriod.RECENT -> LocalDate.fromEpochDays(today.toEpochDays() - RECENT_WINDOW_DAYS + 1)
        CostPeriod.MONTH -> LocalDate(today.year, today.monthNumber, 1)
        CostPeriod.ALL -> null
    }

    fun summarize(logs: List<RideLog>, today: LocalDate, period: CostPeriod, category: CostCategory? = null): CostSummary {
        val eligibleLogs = logs.filterNot { it.visitState.isNotBlank() && it.visitState != "paid" }
        val issues = mutableSetOf<CostIssue>()
        if (eligibleLogs.map { it.id }.distinct().size != eligibleLogs.size ||
            eligibleLogs.mapNotNull { it.renewalId }.let { it.distinct().size != it.size }) issues += CostIssue.DUPLICATE
        val start = start(period, today)
        val dated = eligibleLogs.mapNotNull { log ->
            val date = runCatching { LocalDate.parse(log.date) }.getOrNull()
            if (date == null || amount(log) == null) issues += CostIssue.INVALID_RECORD
            date?.let { log to it }
        }.filter { (_, date) -> date <= today && (start == null || date >= start) }
        val selected = dated.filter { category == null || category(it.first) == category }.map { it.first }
        val totals = selected.groupBy(::category).mapValues { (_, rows) -> rows.sumOf { amount(it) ?: 0 } }
        val coverage = coverage(dated, selected, issues)
        val valid = issues.none { it == CostIssue.DUPLICATE || it == CostIssue.INVALID_RECORD }
        return CostSummary(selected.sortedWith(compareByDescending<RideLog> { it.date }.thenByDescending { it.odometer }),
            totals.values.sum().takeIf { valid }, totals, coverage.takeIf { valid }, issues)
    }

    private fun coverage(dated: List<Pair<RideLog, LocalDate>>, selected: List<RideLog>, issues: MutableSet<CostIssue>): DistanceCoverage? {
        val readings = dated.filter { (log, _) -> log.isFuel || category(log) == CostCategory.SERVICE }
            .sortedWith(compareBy<Pair<RideLog, LocalDate>> { it.second }.thenBy { it.first.odometer })
        if (readings.size < 2) return null
        if (readings.any { !it.first.odometer.isFinite() || it.first.odometer < 0 } ||
            readings.zipWithNext().any { (a, b) -> b.first.odometer < a.first.odometer }) {
            issues += CostIssue.MILEAGE
            return null
        }
        val first = readings.first(); val last = readings.last()
        val distance = last.first.odometer - first.first.odometer
        if (distance <= 0 || first.second == last.second) return null
        val coveredCost = selected.filter { it.date > first.second.toString() && it.date <= last.second.toString() }
            .sumOf { amount(it) ?: 0 }
        return DistanceCoverage(first.second, last.second, distance, coveredCost)
    }
}
