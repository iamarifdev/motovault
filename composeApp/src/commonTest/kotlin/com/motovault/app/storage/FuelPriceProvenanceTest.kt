package com.motovault.app.storage

import com.motovault.app.application.fuel.*
import com.motovault.app.domain.ledger.*
import com.motovault.app.domain.model.*
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.ui.parseNum
import com.motovault.app.ui.toAscii
import kotlinx.datetime.LocalDate
import kotlin.test.*

class FuelPriceProvenanceTest {
    private val initial = VaultState(ride = Ride("yamaha", "BD", 2020, 1000.0))
    private val request = FuelEntryRequest(243.0, 2.0, 121.5, 1100.0, LocalDate(2020, 1, 1), true, "octane", "", "")

    @Test fun firstPriceRequiresConfirmationAndRetainsDecimalsAndEffectiveDate() {
        assertFailsWith<IllegalArgumentException> { FuelEntryAdapter.build(initial, request) }
        val saved = FuelEntryAdapter.build(initial, request.copy(priceConfirmed = true))
        assertEquals(ConfirmedFuelPrice(121.5, "2020-01-01"), saved.ledger.confirmedPrices["octane"])
        assertEquals(24300L, saved.logs.single().costPoisha)
        val next = FuelEntryAdapter.build(saved, request.copy(odometer = 1200.0, date = LocalDate(2020, 1, 2), missedFill = true))
        assertTrue(next.logs.first().missedFuelFill)
    }

    @Test fun oldFillEditPreservesNewerRememberedPriceAndUpdatesCycleFlag() {
        val saved = FuelEntryAdapter.build(initial, request.copy(priceConfirmed = true))
            .copy(ledger = LedgerPreferences().confirmPrice("octane", 125.75, "2020-02-01"))
        val row = saved.logs.single()
        val edit = FuelEditRequest(row.id, 244.0, 2.0, 122.0, row.odometer, LocalDate(2020, 1, 1), true, "octane", "", null,
            priceConfirmed = true, missedFill = true)
        val corrected = FuelEntryAdapter.edit(saved, edit)
        assertEquals(125.75, corrected.ledger.confirmedPrices["octane"]?.takaPerLitre)
        assertEquals(24400L, corrected.logs.single().costPoisha)
        assertTrue(corrected.logs.single().missedFuelFill)
    }

    @Test fun canonicalMoneyAcceptsSupportedInputDigitsIndependentlyOfDisplay() {
        for (value in listOf("121.50", "১২১.৫০", "١٢١.٥٠")) {
            assertEquals(121.5, parseNum(value))
            assertEquals(12150L, Money.parse(toAscii(value)))
        }
    }
}
