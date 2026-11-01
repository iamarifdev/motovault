package com.motovault.app.storage

import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.application.fuel.FuelEditRejected
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.ui.GeoCapture
import com.motovault.app.ui.Ride
import com.motovault.app.ui.RideLog
import com.motovault.app.ui.VaultState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith

class FuelEntryAdapterTest {
    @Test
    fun editPreservesIdentityHistoricPriceAndUnrelatedMetadata() {
        val location = GeoCapture(1.0, 2.0, 10.0, 1, "synthetic")
        val existing = RideLog("Fuel fill-up", "legacy detail", "2020-01-02", 1100.0, id = "fuel-1", kind = "fuel", liters = 5.1, cost = 600.0, notes = "note", location = location, receiptBase64 = "synthetic", fuelPrice = null, locationLabel = "Old")
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1300.0), logs = listOf(existing), lastPrices = mapOf("octane" to 130.0), places = listOf("Recent"))
        val request = FuelEditRequest("fuel-1", 600.0, 5.1, null, 1100.0, LocalDate(2020, 1, 2), true, "octane", " Old ", null)
        val updated = FuelEntryAdapter.edit(original, request)
        assertEquals(existing, updated.logs.single())
        assertEquals(original.ride, updated.ride)
        assertEquals(original.lastPrices, updated.lastPrices)
        assertEquals(original.places, updated.places)
        assertEquals(updated, VaultState.decode(VaultState.encode(updated)))
    }

    @Test
    fun editRejectsMissingAndConflictingRecordsWithoutChangingOriginal() {
        val first = RideLog("Fuel fill-up", "first", "2020-01-01", 1000.0, id = "first", kind = "fuel", liters = 5.0)
        val second = RideLog("Fuel fill-up", "second", "2020-01-10", 1200.0, id = "second", kind = "fuel", liters = 5.0)
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1200.0), logs = listOf(second, first))
        val request = FuelEditRequest("first", 600.0, 5.0, null, 1200.0, LocalDate(2020, 1, 1), true, "octane", "", null)
        assertEquals(FuelEntryIssue.TIMELINE_CONFLICT, assertFailsWith<FuelEditRejected> { FuelEntryAdapter.edit(original, request) }.issue)
        assertEquals(FuelEntryIssue.RECORD_MISSING, assertFailsWith<FuelEditRejected> { FuelEntryAdapter.edit(original, request.copy(id = "missing")) }.issue)
        val duplicate = original.copy(logs = original.logs + RideLog("Service", "synthetic", "2020-01-03", 1050.0, id = "first"))
        assertEquals(FuelEntryIssue.RECORD_MISSING, assertFailsWith<FuelEditRejected> { FuelEntryAdapter.edit(duplicate, request) }.issue)
        assertEquals(listOf(second, first), original.logs)
    }

    @Test
    fun editingAFillUpdatesEconomyAndPreservesRecordIdentity() {
        val first = RideLog("Fuel fill-up", "first", "2020-01-01", 1000.0, id = "first", kind = "fuel", liters = 5.0)
        val second = RideLog("Fuel fill-up", "second", "2020-01-10", 1200.0, id = "second", kind = "fuel", liters = 5.0, notes = "keep")
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1200.0), logs = listOf(second, first), economy = 40.0, lastFuelOdometer = 1200.0)
        val request = FuelEditRequest("second", 720.0, 6.0, 120.0, 1200.0, LocalDate(2020, 1, 10), true, "octane", "", "6 L · Full tank")
        val result = FuelEntryAdapter.edit(original, request)
        assertEquals("second", result.logs.first().id)
        assertEquals("keep", result.logs.first().notes)
        assertEquals(6.0, result.logs.first().liters)
        assertEquals(200.0 / 6.0, result.economy)
        assertEquals(result, VaultState.decode(VaultState.encode(result)))
    }
    @Test
    fun rejectsInvalidValuesAtTheStorageBoundary() {
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0))
        val request = FuelEntryRequest(600.0, 101.0, 120.0, 1100.0, LocalDate(2020, 1, 2), true, "octane", "", "legacy")
        assertFailsWith<IllegalArgumentException> { FuelEntryAdapter.build(original, request) }
        assertEquals(emptyList(), original.logs)
    }
    @Test
    fun newFullFillUpdatesEconomyWithoutWaitingForReload() {
        val date = LocalDate(2020, 1, 2)
        val previousFill = RideLog("Fuel fill-up", "previous", "2020-01-01", 1000.0, id = "previous", kind = "fuel", liters = 5.0, fullTank = true)
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0), logs = listOf(previousFill))
        val request = FuelEntryRequest(600.0, 5.0, 120.0, 1200.0, date, true, "octane", "", "5.00 L · Full tank")
        val result = FuelEntryAdapter.build(original, request)
        assertEquals(40.0, result.economy)
        assertEquals(1200.0, result.lastFuelOdometer)
        assertEquals(result, VaultState.decode(VaultState.encode(result)))
    }

    @Test
    fun createsLegacyCompatibleRecordAndPreservesOtherVaultData() {
        val date = LocalDate(2020, 1, 2)
        val oldLog = RideLog("Service", "Synthetic", "2020-01-01", 900.0, id = "existing")
        val original = VaultState(
            ride = Ride("test", "Synthetic", 2020, 1000.0), logs = listOf(oldLog),
            places = listOf("Pump A", "Pump B"), lastPrices = mapOf("octane" to 110.0),
            lastFuelOdometer = 900.0, economy = 42.0
        )
        val request = FuelEntryRequest(600.0, 5.0, 120.0, 1100.0, date, true, "octane", " Pump B ", "৫.০০ লিটার · Full tank")
        val result = FuelEntryAdapter.build(original, request)
        val created = result.logs.first()
        assertEquals("Fuel fill-up", created.title)
        assertEquals("fuel", created.kind)
        assertEquals(request.legacyDetail, created.detail)
        assertEquals(5.0, created.liters)
        assertEquals(600.0, created.cost)
        assertEquals(120.0, created.fuelPrice)
        assertEquals("Pump B", created.locationLabel)
        assertEquals(listOf("Pump B", "Pump A"), result.places)
        assertEquals(120.0, result.lastPrices["octane"])
        assertEquals(oldLog, result.logs[1])
        assertNotEquals(original.ride, result.ride)
        assertEquals(1100.0, result.lastFuelOdometer)
        assertEquals(null, result.economy)
        assertEquals(original.documents, result.documents)
        assertEquals(result, VaultState.decode(VaultState.encode(result)))
    }
}
