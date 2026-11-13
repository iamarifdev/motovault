package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkLedgerTest {

    @Test
    fun allocatesMixedMileageExpenseWithoutDoubleCountingOrTreatingAllowanceAsPaid() {
        val logs = listOf(
            RideLog(
                title = "Fuel",
                detail = "4.0 L",
                date = "2026-11-10",
                odometer = 5100.0,
                id = "f1",
                kind = "fuel",
                liters = 4.0,
                cost = 500.0,
                costPoisha = 50_000L
            ),
            RideLog(
                title = "Service quote",
                detail = "Draft",
                date = "2026-11-10",
                odometer = 5100.0,
                id = "q1",
                kind = "service",
                cost = 900.0,
                costPoisha = 90_000L,
                visitState = "quote"
            )
        )
        val shifts = listOf(
            WorkShiftRecord(
                id = "s1",
                bikeId = "bike-1",
                date = "2026-11-10",
                grossEarningsPoisha = 150_000L,
                deductionsPoisha = 15_000L,
                refundsPoisha = 5_000L,
                workKm = 80.0,
                totalDayKm = 100.0
            ),
            WorkShiftRecord(
                id = "s2-other-bike",
                bikeId = "bike-2",
                date = "2026-11-10",
                grossEarningsPoisha = 50_000L,
                workKm = 20.0,
                totalDayKm = 50.0
            )
        )

        val summary = WorkLedger.reconcile(
            logs = logs,
            shifts = shifts,
            bikeId = "bike-1",
            allowancePoisha = 25_000L
        )
        assertEquals(1, summary.shiftCount)
        assertEquals(150_000L, summary.grossPoisha)
        assertEquals(15_000L, summary.deductionsPoisha)
        assertEquals(5_000L, summary.refundsPoisha)
        assertEquals(130_000L, summary.netEarningsPoisha)
        assertEquals(40_000L, summary.allocatedExpensePoisha)
        assertEquals(50_000L, summary.actualTotalExpensePoisha)
        assertEquals(25_000L, summary.allowanceReservePoisha)
        assertEquals(90_000L, summary.netAfterAllocatedExpensePoisha)
        assertTrue(summary.issues.isEmpty())
    }

    @Test
    fun flagsPendingPayoutsMissingMileageAndUnlinkedCorrections() {
        val shifts = listOf(
            WorkShiftRecord(
                id = "s1",
                bikeId = "bike-1",
                date = "2026-11-01",
                grossEarningsPoisha = 80_000L,
                payoutStatus = WorkPayoutStatus.PENDING
            ),
            WorkShiftRecord(
                id = "c1",
                bikeId = "bike-1",
                date = "2026-11-01",
                grossEarningsPoisha = 0L,
                deductionsPoisha = 5_000L,
                workKm = 10.0,
                totalDayKm = 10.0,
                correctsShiftId = "missing-parent"
            )
        )

        val summary = WorkLedger.reconcile(emptyList(), shifts, "bike-1")
        assertEquals(80_000L, summary.pendingPayoutPoisha)
        assertTrue(WorkLedgerIssue.PENDING_PAYOUT in summary.issues)
        assertTrue(WorkLedgerIssue.INCOMPLETE_MILEAGE in summary.issues)
        assertTrue(WorkLedgerIssue.UNLINKED_CORRECTION in summary.issues)
    }

    @Test
    fun roundTripsLedgerPreferencesWithWorkModeAndShifts() {
        val prefs = LedgerPreferences(
            maintenanceAllowancePoisha = 30_000L,
            workModeEnabled = false,
            workShifts = listOf(
                WorkShiftRecord(
                    id = "s1",
                    bikeId = "primary",
                    date = "2026-11-11",
                    grossEarningsPoisha = 120_000L,
                    workKm = 42.0,
                    totalDayKm = 60.0
                )
            )
        )
        val encoded = Json.encodeToString(LedgerPreferences.serializer(), prefs)
        val decoded = Json.decodeFromString(LedgerPreferences.serializer(), encoded)
        assertFalse(decoded.workModeEnabled)
        assertEquals(1, decoded.workShifts.size)
        assertEquals(120_000L, decoded.workShifts.single().grossEarningsPoisha)
    }
}
