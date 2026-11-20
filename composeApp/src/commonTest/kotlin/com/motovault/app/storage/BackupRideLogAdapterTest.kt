package com.motovault.app.storage

import com.motovault.app.ui.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRideLogAdapterTest {
    @Test
    fun rideFieldsAndLegacyCatalogueNameRemainUnchanged() {
        val ride = Ride("yamaha", "Standard", 2023, 12345.0, "yamaha-r15-v4", "", "SYN-1",
            RideMarket.INDIA, RideEmission.BS6, "MODEL", "profile", "2026-01-01")
        val archive = ride.toBackupRide()
        assertEquals(ride, archive.toRide())
        assertEquals(ride.displayName, backupRideDisplayName(archive))
        assertEquals(Json.parseToJsonElement(Json.encodeToString(ride)), Json.parseToJsonElement(Json.encodeToString(archive)))
    }

    @Test
    fun rideCcFieldRoundTripsAndPreservesWireJson() {
        val ride = Ride("bajaj", "Dual ABS", 2024, 1500.0, modelId = "bajaj-pulsar-n160", modelName = "Pulsar N160", cc = 165)
        val archive = ride.toBackupRide()
        assertEquals(ride, archive.toRide())
        assertEquals(165, archive.cc)
        assertEquals(Json.parseToJsonElement(Json.encodeToString(ride)), Json.parseToJsonElement(Json.encodeToString(archive)))
    }

    @Test
    fun logAndNestedFieldsKeepTheirWireRepresentation() {
        val oil = EngineOilMetadata("full_synthetic", "10W-40", "SN", "MA2", "Synthetic", "bottle", 1000, 2.0, 900)
        val line = ServiceLineItem("engine_oil", "Oil", "replace", 1.5, "L", 10000, "schedule", true, oil, "synthetic")
        val location = GeoCapture(23.7, 90.4, 5.0, 1700000000000L, "synthetic")
        val log = RideLog("Service", "Synthetic detail", "2026-01-01", 12000.0, "log-1", "service", 4.0,
            false, 150.0, "Synthetic workshop", "Synthetic note", "engine_oil", listOf(line),
            100, 200, 50, 250, listOf("schedule"), location, "Synthetic place", "c3ludGhldGlj", "petrol", 125.0)
        val archive = log.toBackupLog()
        assertEquals(log, archive.toRideLog())
        assertEquals(Json.parseToJsonElement(Json.encodeToString(log)), Json.parseToJsonElement(Json.encodeToString(archive)))
    }

    @Test
    fun quoteWarrantyMonthsRoundTripsAndOldBackupsDefaultToZero() {
        val quote = RideLog("Brake pads", "", "2026-01-01", 9000.0, "q-1", "service", visitState = "quotation", quoteWarrantyMonths = 6)
        val archive = quote.toBackupLog()
        assertEquals(6, archive.quoteWarrantyMonths)
        assertEquals(6, archive.toRideLog().quoteWarrantyMonths)
        val frozenOldBackup = """{"title":"Brake pads","detail":"","date":"2026-01-01","odometer":9000.0,"id":"q-1","kind":"service"}"""
        assertEquals(0, Json.decodeFromString<com.motovault.app.domain.backup.BackupLog>(frozenOldBackup).quoteWarrantyMonths)
    }
}
