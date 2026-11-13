package com.motovault.app.application.widget

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.GarageBikeRecord
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.domain.vault.VaultState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetContractTest {

    private fun activeRide(id: String, model: String, odo: Double, archived: Boolean = false) = Ride(
        id = id,
        brandId = "yamaha",
        edition = model,
        year = 2024,
        odometer = odo,
        modelName = model,
        archived = archived
    )

    private fun confirmedChain(lastKm: Double = 1000.0, vehicleId: String = "primary") = ComponentRule(
        id = "chain",
        vehicleId = vehicleId,
        name = "Chain",
        specification = "O-ring",
        intervalKm = 500.0,
        intervalDays = 30,
        lastServicedOdometerKm = lastKm,
        lastServicedEpochDays = 19_000L,
        baselineCertainty = BaselineCertainty.CONFIRMED
    )

    @Test
    fun selectsConfiguredBikeAndFallsBackWhenArchivedOrMissing() {
        val primary = activeRide("primary", "R15 V4", 2000.0)
        val secondary = activeRide("bike-2", "MT-15", 5000.0)
        val archived = activeRide("bike-3", "Old Bike", 9000.0, archived = true)
        val state = VaultState(
            ride = primary,
            rules = listOf(confirmedChain(1000.0)),
            garageBikes = listOf(
                GarageBikeRecord(secondary, listOf(confirmedChain(4900.0))),
                GarageBikeRecord(archived, listOf(confirmedChain(100.0)))
            ),
            widgetBikeId = "bike-2"
        )
        val selected = WidgetSnapshotProjector.project(state, 19_005L, 100_000L)
        assertEquals("bike-2", selected.bikeId)
        assertEquals(5000.0, selected.odometerKm)
        assertEquals(WidgetTaskStatus.OK, selected.taskStatus)

        val archivedTarget = WidgetSnapshotProjector.project(state.copy(widgetBikeId = "bike-3"), 19_040L, 100_000L)
        assertEquals("primary", archivedTarget.bikeId)
        assertEquals(WidgetTaskStatus.OVERDUE, archivedTarget.taskStatus)

        val missingTarget = WidgetSnapshotProjector.project(state.copy(widgetBikeId = "unknown"), 19_005L, 100_000L)
        assertEquals("primary", missingTarget.bikeId)

        val empty = WidgetSnapshotProjector.project(VaultState(), 19_005L, 100_000L)
        assertEquals(WidgetTaskStatus.NONE, empty.taskStatus)
        assertNull(empty.bikeId)
    }

    @Test
    fun lockedVaultRedactsAllBikeAndServiceFields() {
        val state = VaultState(
            ride = activeRide("primary", "R15 V4", 2400.0),
            rules = listOf(confirmedChain(1000.0)),
            privacy = VaultPrivacySettings(enabled = true)
        )
        val snap = WidgetSnapshotProjector.project(state, 19_040L, 100_000L)
        assertEquals(WidgetTaskStatus.LOCKED, snap.taskStatus)
        assertTrue(snap.privacyRedacted)
        assertNull(snap.bikeId)
        assertEquals("", snap.bikeName)
        assertNull(snap.odometerKm)
        assertNull(snap.taskItemKey)
    }

    @Test
    fun stalenessAndDeepLinkRoundTripWorkCorrectly() {
        val snap = WidgetSnapshot(updatedAtEpochMillis = 10_000L, staleAfterHours = 24)
        assertFalse(snap.isStale(10_000L + 23L * 3_600_000L))
        assertTrue(snap.isStale(10_000L + 25L * 3_600_000L))
        assertTrue(WidgetSnapshot(updatedAtEpochMillis = 0L).isStale(50_000L))

        val link = WidgetDeepLink(sheet = "fuel", bikeId = "bike-2")
        val parsed = WidgetDeepLink.parse(link.toUri())
        assertEquals(link, parsed)
        assertNull(WidgetDeepLink.parse("motovault://other/action?sheet=fuel"))
        assertNull(WidgetDeepLink.parse("motovault://widget/action?sheet=unknown"))
    }
}
