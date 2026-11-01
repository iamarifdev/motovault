package com.motovault.app.domain.fuel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class FuelEntryRulesTest {
    @Test
    fun amountAndLitreModesPreserveExistingCalculation() {
        val byAmount = FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 600.0, 0.0, 120.0, 1100.0, 1000.0)
        assertEquals(600.0, byAmount.amount)
        assertEquals(5.0, byAmount.litres)
        assertNull(byAmount.issue)
        val byLitres = FuelEntryRules.calculate(FuelEntryMode.LITRES, 0.0, 5.0, 120.0, 1100.0, 1000.0)
        assertEquals(600.0, byLitres.amount)
        assertEquals(5.0, byLitres.litres)
        assertNull(byLitres.issue)
    }

    @Test
    fun validationRetainsPriorityAndOdometerError() {
        val missingAmount = FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 0.0, 0.0, 120.0, 1000.0, 1000.0)
        assertEquals(FuelEntryIssue.ENTER_AMOUNT, missingAmount.issue)
        assertTrue(missingAmount.odometerTooLow)
        assertEquals(FuelEntryIssue.ENTER_LITRES, FuelEntryRules.calculate(FuelEntryMode.LITRES, 0.0, 0.0, 120.0, 1.0, 0.0).issue)
        assertEquals(FuelEntryIssue.ENTER_PRICE, FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 1.0, 0.0, 0.0, 1.0, 0.0).issue)
        val odometer = FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 1.0, 0.0, 120.0, 1000.0, 1000.0)
        assertEquals(FuelEntryIssue.ODOMETER_TOO_LOW, odometer.issue)
        assertFalse(FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 1.0, 0.0, 120.0, 0.0, 0.0).odometerTooLow)
    }

    @Test
    fun rejectsOutOfRangeAndNonFiniteValues() {
        assertEquals(FuelEntryIssue.LITRES_TOO_HIGH, FuelEntryRules.calculate(FuelEntryMode.LITRES, 0.0, 101.0, 120.0, 1100.0, 1000.0).issue)
        assertEquals(FuelEntryIssue.INVALID_ODOMETER, FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 10.0, 0.0, 120.0, Double.NaN, 0.0).issue)
        assertEquals(FuelEntryIssue.INVALID_ODOMETER, FuelEntryRules.calculate(FuelEntryMode.AMOUNT, 10.0, 0.0, 120.0, 10000000.0, 0.0).issue)
        assertEquals(FuelEntryIssue.ENTER_AMOUNT, FuelEntryRules.calculate(FuelEntryMode.AMOUNT, Double.POSITIVE_INFINITY, 0.0, 120.0, 10.0, 0.0).issue)
    }

    @Test
    fun rejectsInvalidFutureAndOutOfOrderDates() {
        val today = LocalDate(2026, 10, 27)
        val last = LocalDate(2026, 10, 25)
        assertEquals(FuelEntryIssue.INVALID_DATE, FuelEntryRules.validateDate(null, today, last))
        assertEquals(FuelEntryIssue.FUTURE_DATE, FuelEntryRules.validateDate(LocalDate(2026, 10, 28), today, last))
        assertEquals(FuelEntryIssue.DATE_BEFORE_LAST_FILL, FuelEntryRules.validateDate(LocalDate(2026, 10, 24), today, last))
        assertNull(FuelEntryRules.validateDate(last, today, last))
    }

    @Test
    fun editingRequiresExistingRecordAndPreservesTimelineOrder() {
        val entries = listOf(
            FuelTimelineEntry("first", 1000.0, LocalDate(2026, 10, 1)),
            FuelTimelineEntry("middle", 1200.0, LocalDate(2026, 10, 10)),
            FuelTimelineEntry("last", 1400.0, LocalDate(2026, 10, 20))
        )
        assertNull(FuelEntryRules.validateEditOrder(FuelTimelineEntry("middle", 1250.0, LocalDate(2026, 10, 11)), entries))
        assertEquals(FuelEntryIssue.RECORD_MISSING, FuelEntryRules.validateEditOrder(FuelTimelineEntry("gone", 1250.0, LocalDate(2026, 10, 11)), entries))
        assertEquals(FuelEntryIssue.TIMELINE_CONFLICT, FuelEntryRules.validateEditOrder(FuelTimelineEntry("middle", 1400.0, LocalDate(2026, 10, 11)), entries))
        assertEquals(FuelEntryIssue.TIMELINE_CONFLICT, FuelEntryRules.validateEditOrder(FuelTimelineEntry("middle", 1250.0, LocalDate(2026, 10, 21)), entries))
    }

    @Test
    fun editDoesNotRecalculateUnchangedHistoricAmountAndLitres() {
        val amountMode = FuelEntryRules.calculateEdit(FuelEntryMode.AMOUNT, 600.0, 5.1, 120.0, 600.0, 5.1, 120.0, 120.0, true)
        assertEquals(600.0, amountMode.amount)
        assertEquals(5.1, amountMode.litres)
        val litreMode = FuelEntryRules.calculateEdit(FuelEntryMode.LITRES, 600.0, 5.1, 120.0, 600.0, 5.1, null, 120.0, true)
        assertEquals(600.0, litreMode.amount)
        assertEquals(5.1, litreMode.litres)
        assertNull(litreMode.price)
        val changed = FuelEntryRules.calculateEdit(FuelEntryMode.AMOUNT, 720.0, 5.1, 120.0, 600.0, 5.1, null, 120.0, true)
        assertEquals(5.1, changed.litres)
        assertNull(changed.price)
        val priceEntered = FuelEntryRules.calculateEdit(FuelEntryMode.AMOUNT, 720.0, 5.1, 120.0, 600.0, 5.1, null, 120.0, true, priceTouched = true)
        assertEquals(120.0, priceEntered.price)
        assertEquals(6.0, priceEntered.litres)
        val changedWithPrice = FuelEntryRules.calculateEdit(FuelEntryMode.AMOUNT, 720.0, 5.1, 120.0, 600.0, 5.1, 120.0, 120.0, true)
        assertEquals(6.0, changedWithPrice.litres)
    }
}
