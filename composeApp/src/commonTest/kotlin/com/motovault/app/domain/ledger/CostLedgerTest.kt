package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.datetime.LocalDate
import kotlin.test.*

class CostLedgerTest {
    private val today = LocalDate(2026, 11, 11)
    private fun row(id: String, date: String = "2026-11-01", cost: Double = 1.0, km: Double = 1000.0, kind: String = "service") =
        RideLog("", "", date, km, id = id, cost = cost, kind = kind)

    @Test fun roundsLegacyMoneyHalfUpAndRejectsInvalidValues() {
        assertEquals(101, Money.fromLegacy(1.005))
        assertEquals(200, Money.fromLegacy(2.004))
        assertEquals(301, CostLedger.summarize(listOf(row("a", cost = 1.005), row("b", cost = 2.004)), today, CostPeriod.ALL).totalPoisha)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.01).forEach { assertNull(Money.fromLegacy(it)) }
        assertEquals(125050, Money.parse("1250.50"))
        listOf("1.005", "-1", "1e3", "100000000.01").forEach { assertNull(Money.parse(it)) }
    }

    @Test fun recentWindowIsExactlyThirtyCalendarDatesAndExcludesFuture() {
        val rows = listOf(row("before", "2026-10-12"), row("first", "2026-10-13"), row("now", "2026-11-11"), row("future", "2026-11-12"))
        assertEquals(setOf("first", "now"), CostLedger.summarize(rows, today, CostPeriod.RECENT).logs.map { it.id }.toSet())
        assertEquals(listOf("now"), CostLedger.summarize(rows, today, CostPeriod.MONTH).logs.map { it.id })
        assertEquals(3, CostLedger.summarize(rows, today, CostPeriod.ALL).logs.size)
    }

    @Test fun authoritativePoishaAndRenewalFeeCountOnce() {
        val service = row("service", cost = 999.0).copy(serviceTotalPoisha = 101)
        val renewal = row("renewal", cost = 1250.0, kind = "document_fee").copy(renewalId = "r")
        val parking = row("parking", cost = 999.0, kind = "expense").copy(costPoisha = 99, expenseCategory = "parking")
        val summary = CostLedger.summarize(listOf(service, renewal, parking), today, CostPeriod.ALL)
        assertEquals(125200, summary.totalPoisha)
        assertEquals(99, summary.categories[CostCategory.PARKING])
        assertNull(CostLedger.amount(service.copy(serviceTotalPoisha = -1)))
        assertNull(CostLedger.amount(parking.copy(costPoisha = -1)))
    }

    @Test fun duplicateIdsAndRenewalLinksSuppressTotalsWithoutDroppingRecords() {
        val a = row("a").copy(renewalId = "same")
        val b = row("b").copy(renewalId = "same")
        for (rows in listOf(listOf(a, a), listOf(a, b))) {
            val summary = CostLedger.summarize(rows, today, CostPeriod.ALL)
            assertNull(summary.totalPoisha)
            assertEquals(2, summary.logs.size)
            assertTrue(CostIssue.DUPLICATE in summary.issues)
        }
    }

    @Test fun ratioUsesOnlyActualReadingsAndCostsWithinKnownDates() {
        val rows = listOf(row("start", "2026-11-01", 100.0, 1000.0), row("end", "2026-11-10", 200.0, 1100.0),
            row("fee", "2026-11-05", 50.0, 9999.0, "document_fee"), row("tail", "2026-11-11", 40.0, 9999.0, "expense"))
        val summary = CostLedger.summarize(rows, today, CostPeriod.ALL)
        assertEquals(39000, summary.totalPoisha)
        assertEquals(100.0, summary.coverage?.km)
        assertEquals(25000, summary.coverage?.costPoisha)
        val filtered = CostLedger.summarize(rows, today, CostPeriod.ALL, CostCategory.RENEWAL)
        assertEquals(5000, filtered.totalPoisha)
        assertEquals(100.0, filtered.coverage?.km)
        assertEquals(5000, filtered.coverage?.costPoisha)
    }

    @Test fun incompleteAndContradictoryMileageHasNoRatio() {
        val fees = listOf(row("a", kind = "document_fee"), row("b", "2026-11-02", kind = "expense"))
        assertNull(CostLedger.summarize(fees, today, CostPeriod.ALL).coverage)
        val backwards = listOf(row("a", km = 2000.0), row("b", "2026-11-02", km = 1000.0))
        assertNull(CostLedger.summarize(backwards, today, CostPeriod.ALL).coverage)
        val bad = CostLedger.summarize(listOf(row("a", date = "invalid")), today, CostPeriod.ALL)
        assertNull(bad.totalPoisha)
    }
}
