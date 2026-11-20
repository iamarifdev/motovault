package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Serializable
enum class WorkPayoutStatus { SETTLED, PENDING }

enum class WorkLedgerIssue { INCOMPLETE_MILEAGE, PENDING_PAYOUT, UNLINKED_CORRECTION }

@Serializable
data class WorkShiftRecord(
    val id: String,
    val bikeId: String,
    val date: String,
    val platformCode: String = "delivery",
    val grossEarningsPoisha: Long,
    val deductionsPoisha: Long = 0L,
    val refundsPoisha: Long = 0L,
    val workKm: Double? = null,
    val totalDayKm: Double? = null,
    val payoutStatus: WorkPayoutStatus = WorkPayoutStatus.SETTLED,
    val correctsShiftId: String? = null,
    val notes: String = ""
)

data class WorkReconciledSummary(
    val shiftCount: Int,
    val grossPoisha: Long,
    val deductionsPoisha: Long,
    val refundsPoisha: Long,
    val netEarningsPoisha: Long,
    val pendingPayoutPoisha: Long,
    val workKm: Double,
    val totalTrackedKm: Double,
    val allocatedExpensePoisha: Long,
    val actualTotalExpensePoisha: Long,
    val allowanceReservePoisha: Long,
    val netAfterAllocatedExpensePoisha: Long,
    val issues: Set<WorkLedgerIssue>
)

object WorkLedger {
    fun reconcile(
        logs: List<RideLog>,
        shifts: List<WorkShiftRecord>,
        bikeId: String?,
        allowancePoisha: Long? = null
    ): WorkReconciledSummary {
        val scopedShifts = if (bikeId == null) shifts else shifts.filter { it.bikeId == bikeId }
        val shiftIds = scopedShifts.mapTo(mutableSetOf()) { it.id }
        val issues = mutableSetOf<WorkLedgerIssue>()
        collectShiftIssues(scopedShifts, shiftIds, issues)

        // v4 workLedger: only fuel and paid service count; expenses and document fees are not riding cost.
        val eligibleLogs = logs
            .filterNot { it.visitState.isNotBlank() && it.visitState != "paid" }
            .filter { it.isFuel || (!it.isDocumentFee && it.kind != "expense") }
        val actualTotal = eligibleLogs.sumOf { CostLedger.amount(it) ?: 0L }
        val allocatedExpense = computeAllocatedExpense(eligibleLogs, scopedShifts, issues)

        val gross = scopedShifts.sumOf { it.grossEarningsPoisha }
        val deductions = scopedShifts.sumOf { it.deductionsPoisha }
        val refunds = scopedShifts.sumOf { it.refundsPoisha }
        val netEarnings = gross - deductions - refunds
        val pending = scopedShifts.filter { it.payoutStatus == WorkPayoutStatus.PENDING }
            .sumOf { (it.grossEarningsPoisha - it.deductionsPoisha - it.refundsPoisha).coerceAtLeast(0L) }
        val workKm = scopedShifts.sumOf { (it.workKm ?: 0.0).coerceAtLeast(0.0) }
        val totalKm = scopedShifts.groupBy { it.date }.values
            .sumOf { dayShifts -> dayShifts.maxOfOrNull { it.totalDayKm ?: 0.0 } ?: 0.0 }
        val reserve = (allowancePoisha ?: 0L).coerceAtLeast(0L)

        return WorkReconciledSummary(
            shiftCount = scopedShifts.size,
            grossPoisha = gross,
            deductionsPoisha = deductions,
            refundsPoisha = refunds,
            netEarningsPoisha = netEarnings,
            pendingPayoutPoisha = pending,
            workKm = workKm,
            totalTrackedKm = totalKm,
            allocatedExpensePoisha = allocatedExpense,
            actualTotalExpensePoisha = actualTotal,
            allowanceReservePoisha = reserve,
            netAfterAllocatedExpensePoisha = netEarnings - allocatedExpense,
            issues = issues
        )
    }

    private fun collectShiftIssues(
        scopedShifts: List<WorkShiftRecord>,
        shiftIds: Set<String>,
        issues: MutableSet<WorkLedgerIssue>
    ) {
        for (shift in scopedShifts) {
            if (shift.payoutStatus == WorkPayoutStatus.PENDING) {
                issues += WorkLedgerIssue.PENDING_PAYOUT
            }
            if (shift.correctsShiftId != null && shift.correctsShiftId !in shiftIds) {
                issues += WorkLedgerIssue.UNLINKED_CORRECTION
            }
            val wKm = shift.workKm
            val tKm = shift.totalDayKm
            if (wKm == null || tKm == null || !wKm.isFinite() || !tKm.isFinite() || tKm <= 0.0 || wKm < 0.0 || wKm > tKm) {
                issues += WorkLedgerIssue.INCOMPLETE_MILEAGE
            }
        }
    }

    private fun computeAllocatedExpense(
        eligibleLogs: List<RideLog>,
        scopedShifts: List<WorkShiftRecord>,
        issues: MutableSet<WorkLedgerIssue>
    ): Long {
        val expenseByDate = eligibleLogs.groupBy { it.date }
            .mapValues { (_, dayLogs) -> dayLogs.sumOf { CostLedger.amount(it) ?: 0L } }
        var totalAllocated = 0L
        for ((date, dayShifts) in scopedShifts.groupBy { it.date }) {
            val dayExpense = expenseByDate[date] ?: 0L
            if (dayExpense <= 0L) continue
            val dayTotalKm = dayShifts.maxOfOrNull { it.totalDayKm ?: 0.0 } ?: 0.0
            val dayWorkKm = dayShifts.sumOf { (it.workKm ?: 0.0).coerceAtLeast(0.0) }
            if (dayTotalKm <= 0.0 || dayWorkKm > dayTotalKm) {
                issues += WorkLedgerIssue.INCOMPLETE_MILEAGE
            } else {
                val ratio = (dayWorkKm / dayTotalKm).coerceIn(0.0, 1.0)
                totalAllocated += (dayExpense * ratio).roundToLong()
            }
        }
        return totalAllocated
    }
}
