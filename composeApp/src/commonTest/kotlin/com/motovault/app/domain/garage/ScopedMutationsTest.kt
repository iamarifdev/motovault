package com.motovault.app.domain.garage

import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.ui.CapturePrefill
import com.motovault.app.ui.acceptPrefill
import com.motovault.app.ui.applyPrefillScopedMutation
import com.motovault.app.ui.create
import com.motovault.app.ui.reminderPlan
import com.motovault.app.ui.reminderTarget
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScopedMutationsTest {
    private val validReceipt = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    private fun twoBikeState(): VaultState {
        val bike1 = VaultState.create(Ride("yamaha", "R15", 2023, 5000.0, id = "primary"))
        val bike2Rules = listOf(
            ComponentRule(
                id = "r-oil-b2", vehicleId = "bike-2", name = "Engine oil", specification = "10W-40",
                intervalKm = 1000.0, intervalDays = 60,
                lastServicedOdometerKm = 1000.0, lastServicedEpochDays = 20000L,
                baselineCertainty = BaselineCertainty.CONFIRMED, reminder = true
            )
        )
        return bike1.addBike(Ride("honda", "Hornet", 2024, 2200.0, id = "bike-2"), bike2Rules)
    }

    @Test
    fun fuelEntryAndEditStayScopedAfterSwitchingBikes() {
        val state = twoBikeState()
        assertEquals("bike-2", state.ride?.id)

        val date = LocalDate(2026, 10, 15)
        val afterCreate = FuelEntryAdapter.build(
            state,
            FuelEntryRequest(
                amount = 650.0,
                litres = 5.0,
                price = 130.0,
                odometer = 5250.0,
                date = date,
                fullTank = true,
                fuelType = FuelDefaults.OCTANE,
                place = "Padma",
                legacyDetail = "5.00 L · Full tank",
                priceConfirmed = true,
                captureReceiptId = validReceipt,
                targetBikeId = "primary"
            )
        )

        assertEquals("bike-2", afterCreate.ride?.id)
        assertEquals(2200.0, afterCreate.ride?.odometer)
        assertTrue(afterCreate.logs.isEmpty())

        val primary = afterCreate.garageBikes.single { it.ride.id == "primary" }
        assertEquals(5250.0, primary.ride.odometer)
        assertEquals(1, primary.logs.size)
        assertEquals("primary", primary.logs.single().vehicleId)
        assertTrue(validReceipt in afterCreate.captureReceipts)

        val createdId = primary.logs.single().id
        val afterEdit = FuelEntryAdapter.edit(
            afterCreate,
            FuelEditRequest(
                id = createdId,
                amount = 660.0,
                litres = 5.0,
                price = 132.0,
                odometer = 5260.0,
                date = date,
                fullTank = true,
                fuelType = FuelDefaults.OCTANE,
                place = "Meghna",
                updatedLegacyDetail = null,
                priceConfirmed = true,
                targetBikeId = "primary"
            )
        )
        val updatedPrimary = afterEdit.garageBikes.single { it.ride.id == "primary" }
        assertEquals(660.0, updatedPrimary.logs.single().cost)
        assertEquals(5260.0, updatedPrimary.ride.odometer)
    }

    @Test
    fun scopedFuelAndCapturePrefillRejectArchivedTargetBike() {
        val archivedPrimary = twoBikeState().setBikeArchived("primary", true)
        assertFailsWith<IllegalArgumentException> {
            FuelEntryAdapter.build(
                archivedPrimary,
                FuelEntryRequest(
                    amount = 650.0, litres = 5.0, price = 130.0, odometer = 5300.0,
                    date = LocalDate(2026, 10, 15), fullTank = true,
                    fuelType = FuelDefaults.OCTANE, place = "", legacyDetail = "5.00 L",
                    priceConfirmed = true, targetBikeId = "primary"
                )
            )
        }

        val prefill = CapturePrefill(validReceipt, 50000L, 5300.0, emptyList(), targetBikeId = "primary")
        assertFailsWith<IllegalArgumentException> {
            archivedPrimary.acceptPrefill(prefill)
        }
        assertFailsWith<IllegalArgumentException> {
            archivedPrimary.applyPrefillScopedMutation(prefill) { it }
        }
    }

    @Test
    fun remindersExcludeArchivedBikeDocsAndRulesWhileKeepingOwnerLicence() {
        val now = LocalDateTime(2026, 10, 15, 10, 0)
        val licence = VaultDocument(
            title = "Driving licence", number = "DL-1", holder = "Owner",
            expiry = "2026-10-20", id = "doc-licence", reminder = true, kind = "licence", vehicleId = null
        )
        val bike1Tax = VaultDocument(
            title = "Tax token", number = "TT-1", holder = "Owner",
            expiry = "2026-10-22", id = "doc-tax-primary", reminder = true, kind = "tax", vehicleId = "primary"
        )
        val bike1Rule = ComponentRule(
            id = "r-oil-b1", vehicleId = "primary", name = "Engine oil", specification = "10W-40",
            intervalKm = 1000.0, intervalDays = 60,
            lastServicedOdometerKm = 1000.0, lastServicedEpochDays = 20000L,
            baselineCertainty = BaselineCertainty.CONFIRMED, reminder = true
        )
        val bike1 = VaultState.create(Ride("yamaha", "R15", 2023, 5000.0, id = "primary"))
            .copy(documents = listOf(licence, bike1Tax), rules = listOf(bike1Rule))
        val activeWithBike1 = bike1
            .addBike(Ride("honda", "Hornet", 2024, 2200.0, id = "bike-2"), emptyList())
            .switchToBike("primary")

        val planBeforeArchive = reminderPlan(activeWithBike1, now, emptyList(), emptySet())
        val taxReminder = planBeforeArchive.first { it.documentId == "doc-tax-primary" }
        val serviceReminder = planBeforeArchive.first { it.documentId == "r-oil-b1" }
        val licenceReminder = planBeforeArchive.first { it.documentId == "doc-licence" }

        assertEquals("primary", reminderTarget(activeWithBike1, taxReminder)?.vehicleId)
        assertEquals("primary", reminderTarget(activeWithBike1, serviceReminder)?.vehicleId)
        assertNull(reminderTarget(activeWithBike1, licenceReminder)?.vehicleId)

        val afterArchive = activeWithBike1.setBikeArchived("primary", true)
        val planAfterArchive = reminderPlan(afterArchive, now, emptyList(), emptySet())
        assertTrue(planAfterArchive.any { it.documentId == "doc-licence" })
        assertTrue(planAfterArchive.none { it.documentId == "doc-tax-primary" })
        assertNull(reminderTarget(afterArchive, taxReminder))
        assertNull(reminderTarget(afterArchive, serviceReminder))
        assertNotNull(reminderTarget(afterArchive, licenceReminder))
    }
}
