package com.motovault.app.domain.garage

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.DocumentPage
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.today
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.ui.create
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BikeOwnershipTest {
    private fun primaryStateWithOwnerLicence(): VaultState {
        val base = VaultState.create(Ride("yamaha", "Official", 2023, 5000.0, "yamaha-r15-v4", "R15 V4"))
            .logFuel(5100.0, 4.0, cost = 520.0)
        val ownerLicence = VaultDocument(
            title = "Driving licence", number = "DL-1001", holder = "Arif",
            expiry = "2028-01-01", id = "doc-licence", kind = "licence", vehicleId = null
        )
        val bikeTax = VaultDocument(
            title = "Tax token", number = "TT-1001", holder = "Arif",
            expiry = "2027-01-01", id = "doc-tax-1", kind = "tax",
            pages = listOf(DocumentPage("tax1_orig.jpg", "tax1_prev.jpg", "image/jpeg", 100, 100, "Front"))
        )
        return base.copy(documents = listOf(ownerLicence, bikeTax))
    }

    @Test
    fun singleBikeDefaultsUpgradeCleanly() {
        val single = VaultState.create(Ride("ktm", "Standard", 2024, 1200.0))
        assertEquals("primary", single.ride?.id)
        assertFalse(single.ride!!.archived)
        assertTrue(single.garageBikes.isEmpty())
        assertEquals(1, single.activeBikes().size)
    }

    @Test
    fun duplicateModelNamesStayIsolatedAndKeepOwnerLicenceAcrossSwitches() {
        val initial = primaryStateWithOwnerLicence()
        val secondRide = Ride("yamaha", "Duplicate name test", 2025, 300.0, "yamaha-r15-v4", "R15 V4", registration = "DHAKA-99")
        val secondRules = listOf(
            ComponentRule("r2-oil", "second", "Engine oil", "Oil", 3000.0, 180, 300.0, today().toEpochDays().toLong(), baselineCertainty = BaselineCertainty.CONFIRMED)
        )

        val withTwoBikes = initial.addBike(secondRide, secondRules)
        val secondId = withTwoBikes.ride!!.id
        assertNotEquals("primary", secondId)
        assertEquals(2, withTwoBikes.allBikes().size)
        assertTrue(withTwoBikes.logs.isEmpty())
        assertEquals(listOf("doc-licence"), withTwoBikes.documents.map { it.id })
        assertTrue("tax1_orig.jpg" in withTwoBikes.allDocumentAssetIds())

        val afterSecondFuel = withTwoBikes.logFuel(450.0, 3.0, cost = 400.0)
        assertEquals(450.0, afterSecondFuel.ride?.odometer)
        assertEquals(1, afterSecondFuel.logs.size)

        val backToPrimary = afterSecondFuel.switchToBike("primary")
        assertEquals("primary", backToPrimary.ride?.id)
        assertEquals(5100.0, backToPrimary.ride?.odometer)
        assertEquals(setOf("doc-licence", "doc-tax-1"), backToPrimary.documents.map { it.id }.toSet())
        assertEquals(1, backToPrimary.logs.size)
        assertEquals(5100.0, backToPrimary.logs.first().odometer)
    }

    @Test
    fun archivingActiveBikePromotesRemainingBikeAndGuardsLastActiveBike() {
        val s1 = primaryStateWithOwnerLicence()
        val s2 = s1.addBike(
            Ride("honda", "Standard", 2024, 800.0, "honda-cbr150r", "CBR 150R"),
            listOf(ComponentRule("r2-oil", "b2", "Engine oil", "Oil", 3000.0, 180, 800.0, today().toEpochDays().toLong()))
        )
        val activeId = s2.ride!!.id

        val archivedActive = s2.setBikeArchived(activeId, true)
        assertEquals("primary", archivedActive.ride?.id)
        assertTrue(archivedActive.garageBikes.single().ride.archived)
        assertEquals(1, archivedActive.activeBikes().size)

        assertFailsWith<IllegalArgumentException> {
            archivedActive.setBikeArchived("primary", true)
        }
        assertFailsWith<IllegalArgumentException> {
            archivedActive.switchToBike(activeId)
        }

        val restored = archivedActive.setBikeArchived(activeId, false)
        assertFalse(restored.garageBikes.single().ride.archived)
        assertEquals(2, restored.activeBikes().size)
        val switchedBack = restored.switchToBike(activeId)
        assertEquals(activeId, switchedBack.ride?.id)
        assertNull(switchedBack.ride?.odometerConfirmedDate)
    }
}
