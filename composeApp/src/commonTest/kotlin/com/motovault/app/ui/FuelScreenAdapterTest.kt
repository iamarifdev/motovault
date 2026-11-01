package com.motovault.app.ui

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuelScreenAdapterTest {
    @Test
    fun mapsOnlyFuelHistoryAndPreservesInvalidDatesForConflictChecks() {
        val valid = RideLog("Fuel", "", "2025-01-02", 1000.0, id = "valid", kind = "fuel", liters = 4.9, cost = 590.0, fuelPrice = null)
        val invalid = RideLog("Fuel", "", "not-a-date", 1200.0, id = "invalid", kind = "fuel", liters = 5.0, cost = 600.0)
        val service = RideLog("Service", "", "2025-01-03", 1300.0, id = "service", kind = "service")
        val state = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0), logs = listOf(valid, invalid, service))
        val input = requireNotNull(state.fuelScreenInput(LocalDate(2025, 1, 10)))
        assertEquals(1200.0, input.lastFuelOdometer)
        assertEquals(LocalDate(2025, 1, 2), input.lastFuelDate)
        assertEquals("valid", input.timeline.first()?.id)
        assertNull(input.timeline.last())
        val edit = valid.fuelExistingEntry()
        assertEquals(590.0, edit.cost)
        assertEquals(4.9, edit.fuelLiters)
        assertNull(edit.fuelPrice)
    }

    @Test
    fun noRideProducesNoFuelScreenInput() {
        assertNull(VaultState().fuelScreenInput(LocalDate(2025, 1, 10)))
    }
}
