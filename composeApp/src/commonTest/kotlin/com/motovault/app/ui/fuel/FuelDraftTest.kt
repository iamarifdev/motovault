package com.motovault.app.ui.fuel

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FuelDraftTest {
    @Test
    fun newAndExistingDraftsKeepTheirOwnValues() {
        val today = LocalDate(2025, 1, 10)
        val saved = FuelExistingEntry("id", 590.0, 4.9, null, 1000.0, "2025-01-02", false, "petrol", "Station")
        val edit = FuelDraft(saved, today)
        val fresh = FuelDraft(null, today)
        assertEquals(FuelDateSelection.CUSTOM, edit.dateSelection)
        assertEquals("2025-01-02", edit.customDate)
        assertEquals("Station", edit.place)
        assertTrue(edit.odometerTouched)
        assertFalse(edit.fullTank)
        edit.place = "New station"
        assertEquals("Station", saved.locationLabel)
        assertEquals(FuelDateSelection.TODAY, fresh.dateSelection)
        assertEquals("", fresh.place)
        assertFalse(fresh.odometerTouched)
    }
}
