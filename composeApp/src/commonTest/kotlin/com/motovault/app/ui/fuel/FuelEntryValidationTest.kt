package com.motovault.app.ui.fuel

import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelTimelineEntry
import com.motovault.app.domain.ledger.ConfirmedFuelPrice
import com.motovault.app.ui.EntryDateKey
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuelEntryValidationTest {
    private val today = LocalDate(2026, 11, 15)
    private val lastFuelDate = LocalDate(2026, 11, 10)

    private fun baseInput(
        confirmedPrices: Map<String, ConfirmedFuelPrice> = mapOf("octane" to ConfirmedFuelPrice(130.0, "2026-11-10")),
        timeline: List<FuelTimelineEntry?> = emptyList()
    ) = FuelScreenInput(
        estimatedOdometer = 1250.0,
        lastFuelOdometer = 1000.0,
        lastFuelDate = lastFuelDate,
        timeline = timeline,
        lastPrices = mapOf("octane" to 130.0, "petrol" to 125.0),
        places = emptyList(),
        confirmedPrices = confirmedPrices
    )

    private fun evaluate(
        input: FuelScreenInput = baseInput(),
        existing: FuelExistingEntry? = null,
        mode: FuelEntryMode = FuelEntryMode.AMOUNT,
        fuelType: String = "octane",
        dateKey: EntryDateKey = EntryDateKey.TODAY,
        customDate: String = today.toString(),
        amountRaw: String = "500",
        litresRaw: String = "",
        priceRaw: String = "130",
        odoRaw: String = "1250",
        priceTouched: Boolean = false,
        confirmedPriceKey: String? = null,
        odometerTouched: Boolean = true
    ) = FuelEntryValidation.evaluate(
        input = input,
        existing = existing,
        today = today,
        mode = mode,
        fuelType = fuelType,
        dateKey = dateKey,
        customDate = customDate,
        amountRaw = amountRaw,
        litresRaw = litresRaw,
        priceRaw = priceRaw,
        odoRaw = odoRaw,
        priceTouched = priceTouched,
        confirmedPriceKey = confirmedPriceKey,
        odometerTouched = odometerTouched,
        fallbackPrice = 130.0
    )

    @Test
    fun enforcesV4PriorityOrderAcrossAllChecks() {
        assertEquals(FuelValidationReason.ENTER_AMOUNT, evaluate(amountRaw = "", priceRaw = "0", odometerTouched = false).reason)
        assertEquals(FuelValidationReason.ENTER_LITRES, evaluate(mode = FuelEntryMode.LITRES, litresRaw = "", priceRaw = "0", odometerTouched = false).reason)
        assertEquals(FuelValidationReason.ENTER_PRICE, evaluate(priceRaw = "0", odometerTouched = false).reason)
        assertEquals(FuelValidationReason.CONFIRM_PRICE, evaluate(priceRaw = "135", odometerTouched = false).reason)
        assertEquals(
            FuelValidationReason.CONFIRM_ODOMETER,
            evaluate(priceRaw = "135", confirmedPriceKey = "octane:135.0", odometerTouched = false).reason
        )
        assertEquals(FuelValidationReason.LITRES_TOO_HIGH, evaluate(mode = FuelEntryMode.LITRES, litresRaw = "101", odoRaw = "900").reason)
        assertEquals(FuelValidationReason.INVALID_ODOMETER, evaluate(odoRaw = "").reason)
        assertEquals(FuelValidationReason.INVALID_ODOMETER, evaluate(odoRaw = "10000000").reason)

        val lowOdo = evaluate(odoRaw = "1000", customDate = "bad", dateKey = EntryDateKey.CUSTOM)
        assertEquals(FuelValidationReason.ODOMETER_TOO_LOW, lowOdo.odometerError)
        assertEquals(FuelValidationReason.ODOMETER_TOO_LOW, lowOdo.reason)

        assertEquals(FuelValidationReason.INVALID_DATE, evaluate(dateKey = EntryDateKey.CUSTOM, customDate = "not-a-date").reason)
        assertEquals(FuelValidationReason.FUTURE_DATE, evaluate(dateKey = EntryDateKey.CUSTOM, customDate = "2026-11-16").reason)
        assertEquals(FuelValidationReason.DATE_BEFORE_LAST_FILL, evaluate(dateKey = EntryDateKey.CUSTOM, customDate = "2026-11-09").reason)
        assertNull(evaluate().reason)
    }

    @Test
    fun handlesLiterWarningPriceConfirmationAndHistoricNullPriceEdit() {
        assertFalse(evaluate(mode = FuelEntryMode.LITRES, litresRaw = "40").literWarning)
        assertTrue(evaluate(mode = FuelEntryMode.LITRES, litresRaw = "40.1").literWarning)

        val unconfirmed = evaluate(input = baseInput(confirmedPrices = emptyMap()), priceRaw = "130")
        assertFalse(unconfirmed.priceKnown)
        assertFalse(unconfirmed.priceConfirmed)
        assertEquals("octane:130.0", unconfirmed.priceKey)
        assertEquals(FuelValidationReason.CONFIRM_PRICE, unconfirmed.reason)

        val confirmed = evaluate(
            input = baseInput(confirmedPrices = emptyMap()),
            priceRaw = "130",
            confirmedPriceKey = "octane:130.0"
        )
        assertFalse(confirmed.priceKnown)
        assertTrue(confirmed.priceConfirmed)
        assertNull(confirmed.reason)

        val legacy = FuelExistingEntry("f1", 0.0, 4.9, null, 1000.0, "2026-11-10", false, "octane", "")
        val editNullPrice = evaluate(
            input = baseInput(
                confirmedPrices = emptyMap(),
                timeline = listOf(FuelTimelineEntry("f1", 1000.0, LocalDate(2026, 11, 10)))
            ),
            existing = legacy,
            amountRaw = "0",
            odoRaw = "1000",
            dateKey = EntryDateKey.CUSTOM,
            customDate = "2026-11-10"
        )
        assertNull(editNullPrice.savedPrice)
        assertTrue(editNullPrice.priceKnown)
        assertNull(editNullPrice.reason)
    }

    @Test
    fun editModeDetectsTimelineConflictsAndMissingRecords() {
        val existing = FuelExistingEntry("f1", 590.0, 4.9, 120.0, 1200.0, "2026-11-10", false, "octane", "")
        val timeline = listOf(
            FuelTimelineEntry("f0", 1000.0, LocalDate(2026, 11, 5)),
            FuelTimelineEntry("f1", 1200.0, LocalDate(2026, 11, 10)),
            FuelTimelineEntry("f2", 1400.0, LocalDate(2026, 11, 12))
        )
        val conflict = evaluate(
            input = baseInput(timeline = timeline),
            existing = existing,
            priceRaw = "120",
            odoRaw = "1400",
            dateKey = EntryDateKey.CUSTOM,
            customDate = "2026-11-10"
        )
        assertEquals(FuelValidationReason.TIMELINE_CONFLICT, conflict.odometerError)
        assertEquals(FuelValidationReason.TIMELINE_CONFLICT, conflict.reason)

        val missing = evaluate(
            input = baseInput(timeline = listOf(FuelTimelineEntry("other", 1000.0, LocalDate(2026, 11, 5)))),
            existing = existing,
            priceRaw = "120",
            odoRaw = "1200",
            dateKey = EntryDateKey.CUSTOM,
            customDate = "2026-11-10"
        )
        assertEquals(FuelValidationReason.RECORD_MISSING, missing.reason)
    }
}
