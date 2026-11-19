package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryFilterTest {

    private val today = LocalDate(2026, 11, 19)

    private fun sampleLogs(): List<RideLog> = listOf(
        RideLog(
            title = "Fuel",
            detail = "5.0 L",
            date = "2026-11-18",
            odometer = 1200.0,
            id = "fuel-recent",
            kind = "fuel",
            liters = 5.0,
            cost = 650.0,
            costPoisha = 65_000L
        ),
        RideLog(
            title = "Service",
            detail = "Oil change",
            date = "2026-11-05",
            odometer = 1150.0,
            id = "svc-month",
            kind = "service",
            cost = 900.0,
            serviceTotalPoisha = 90_000,
            visitState = "paid"
        ),
        RideLog(
            title = "Service quote",
            detail = "Brake pad quote",
            date = "2026-10-25",
            odometer = 1120.0,
            id = "svc-quote-30d",
            kind = "service",
            cost = 450.0,
            serviceTotalPoisha = 45_000,
            visitState = "quote"
        ),
        RideLog(
            title = "Old Fuel",
            detail = "4.0 L",
            date = "2026-09-10",
            odometer = 900.0,
            id = "fuel-old",
            kind = "fuel",
            liters = 4.0,
            cost = 520.0,
            costPoisha = 52_000L
        ),
        RideLog(
            title = "Renewal",
            detail = "Tax token",
            date = "2026-11-10",
            odometer = 1180.0,
            id = "doc-fee",
            kind = "document_fee",
            cost = 2300.0,
            costPoisha = 230_000L
        ),
        RideLog(
            title = "Parking",
            detail = "Mall",
            date = "2026-11-12",
            odometer = 1190.0,
            id = "exp-parking",
            kind = "expense",
            expenseCategory = "parking",
            cost = 50.0,
            costPoisha = 5_000L
        ),
        RideLog(
            title = "Toll",
            detail = "Bridge",
            date = "2026-11-15",
            odometer = 1195.0,
            id = "exp-toll",
            kind = "expense",
            expenseCategory = "toll",
            cost = 100.0,
            costPoisha = 10_000L
        ),
        RideLog(
            title = "Wash",
            detail = "Custom",
            date = "2026-10-28",
            odometer = 1130.0,
            id = "exp-custom",
            kind = "expense",
            expenseCategory = "custom",
            cost = 150.0,
            costPoisha = 15_000L
        ),
        RideLog(
            title = "Future Fuel",
            detail = "2.0 L",
            date = "2026-11-25",
            odometer = 1300.0,
            id = "fuel-future",
            kind = "fuel",
            liters = 2.0,
            cost = 260.0,
            costPoisha = 26_000L
        )
    )

    @Test
    fun timelineExcludesExpensesAndDocumentFeesWhileRetainingFuelPaidAndQuotedServices() {
        val logs = sampleLogs()

        val cases = mapOf(
            (CostPeriod.RECENT to null) to listOf("fuel-recent", "svc-month", "svc-quote-30d"),
            (CostPeriod.RECENT to CostCategory.FUEL) to listOf("fuel-recent"),
            (CostPeriod.RECENT to CostCategory.SERVICE) to listOf("svc-month", "svc-quote-30d"),
            (CostPeriod.MONTH to null) to listOf("fuel-recent", "svc-month"),
            (CostPeriod.MONTH to CostCategory.FUEL) to listOf("fuel-recent"),
            (CostPeriod.MONTH to CostCategory.SERVICE) to listOf("svc-month"),
            (CostPeriod.ALL to null) to listOf("fuel-recent", "svc-month", "svc-quote-30d", "fuel-old"),
            (CostPeriod.ALL to CostCategory.FUEL) to listOf("fuel-recent", "fuel-old"),
            (CostPeriod.ALL to CostCategory.SERVICE) to listOf("svc-month", "svc-quote-30d")
        )

        cases.forEach { (filter, expectedIds) ->
            val actual = filterHistoryTimeline(logs, today, filter.first, filter.second).map { it.id }
            assertEquals(expectedIds, actual, "Mismatch for filter $filter")
        }
    }

    @Test
    fun costBreakdownSumsRenewalParkingTollCustomIntoOtherAndExcludesQuotesFromTotal() {
        val summary = CostLedger.summarize(sampleLogs(), today, CostPeriod.RECENT, category = null)
        val breakdown = summary.toBreakdown()

        assertEquals(65_000L, breakdown.fuelPoisha)
        assertEquals(90_000L, breakdown.servicePoisha)
        assertEquals(230_000L + 5_000L + 10_000L + 15_000L, breakdown.otherPoisha)
        assertEquals(65_000L + 90_000L + 260_000L, summary.totalPoisha)
    }

    @Test
    fun ledgerPreferencesPreservesBudgetAndAllowanceFieldsForBackupCompatibility() {
        val prefs = LedgerPreferences(
            monthlyBudgetPoisha = 450_000L,
            maintenanceAllowancePoisha = 75_000L,
            workModeEnabled = true
        )
        val encoded = Json.encodeToString(LedgerPreferences.serializer(), prefs)
        val decoded = Json.decodeFromString(LedgerPreferences.serializer(), encoded)

        assertEquals(450_000L, decoded.monthlyBudgetPoisha)
        assertEquals(75_000L, decoded.maintenanceAllowancePoisha)
    }
}
