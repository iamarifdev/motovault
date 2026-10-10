package com.motovault.app.ui.redesign

import kotlin.test.*

class VaultStateTest {
    private fun state(brand: String = "ktm") = VaultState.create(Ride(brand, "Indian · BS6", 2023, 4280.0))

    @Test fun newGarageStartsMaintenanceAtCurrentDistanceAndDate() {
        val state = state()
        assertEquals(3, state.rules.size)
        assertTrue(state.rules.all { it.lastServicedOdometerKm == 4280.0 && it.lastServicedEpochDays == today().toEpochDays().toLong() })
    }

    @Test fun everyBrandGetsExplicitOwnerDefinedExampleSchedules() {
        val yamaha = state("yamaha")
        assertEquals(listOf(500.0, 3000.0, 5000.0), yamaha.rules.map { it.intervalKm })
        assertTrue(yamaha.rules.all { it.specification.contains("Owner-defined example") })
    }

    @Test fun recordingServiceOnlyResetsSelectedComponent() {
        val state = state().copy(ride = state().ride!!.copy(odometer = 4500.0))
        val updated = state.logService(state.rules.first())
        assertEquals(4500.0, updated.rules.first().lastServicedOdometerKm)
        assertEquals(state.rules.drop(1), updated.rules.drop(1))
        assertEquals(state.rules.first().name, updated.logs.first().title)
    }

    @Test fun firstFuelLogSetsBaselineAndSecondCalculatesActualEconomy() {
        val first = state().logFuel(4300.0, 5.0)
        assertNull(first.economy)
        val second = first.logFuel(4490.0, 5.0)
        assertEquals(38.0, second.economy)
        assertEquals(4490.0, second.ride!!.odometer)
        assertEquals(2, second.logs.size)
    }

    @Test fun fuelLogRejectsRollbackAndInvalidFuel() {
        assertFailsWith<IllegalArgumentException> { state().logFuel(4300.0, 5.0).logFuel(4200.0, 5.0) }
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { fuel ->
            assertFailsWith<IllegalArgumentException> { state().logFuel(4300.0, fuel) }
        }
        assertFailsWith<IllegalArgumentException> { state().logFuel(Double.NaN, 5.0) }
    }

    @Test fun recordsSurviveSerializationRoundTrip() {
        val original = state().logFuel(4300.0, 5.0).copy(documents = listOf(VaultDocument("Tax token", "DEMO", "Rider", "2028-06-18")))
        assertEquals(original, VaultState.decode(VaultState.encode(original)))
        assertNull(VaultState.decode("not json"))
    }

    @Test fun partialFillsAccumulateAcrossACompleteCycle() {
        val result = state().logFuel(4300.0, 5.0)
            .logFuel(4400.0, 2.0, fullTank = false)
            .logFuel(4520.0, 3.0, fullTank = true)
        assertEquals(44.0, result.economy)
    }

    @Test fun economyUsesDistanceAndLitresAcrossAllCompletedCycles() {
        val result = state().logFuel(4300.0, 5.0)
            .logFuel(4500.0, 5.0)
            .logFuel(4800.0, 10.0)
        assertEquals(500.0 / 15.0, result.economy)
    }

    @Test fun partialFillsWithoutAFullBaselineDoNotReportEconomy() {
        val result = state().logFuel(4300.0, 2.0, fullTank = false).logFuel(4500.0, 5.0)
        assertNull(result.economy)
    }

    @Test fun deletingFuelRecalculatesCycles() {
        val original = state().logFuel(4300.0, 5.0).logFuel(4490.0, 5.0)
        val updated = original.deleteLog(original.logs.first())
        assertNull(updated.economy)
        assertEquals(4490.0, updated.ride!!.odometer)
    }

    @Test fun historyCannotDeleteTheActiveServiceBaseline() {
        val initial = state()
        val original = initial.logService(initial.rules.first())
        assertFailsWith<IllegalArgumentException> { original.deleteLog(original.logs.first()) }
        val adjusted = original.saveSchedule(original.rules.first().copy(lastServicedOdometerKm = 4000.0))
        assertTrue(adjusted.deleteLog(adjusted.logs.first()).logs.isEmpty())
    }

    @Test fun oldRecordsKeepTheirBikeAndDocumentIdentity() {
        val old = """{"ride":{"brandId":"yamaha","edition":"Bangladesh edition","year":2023,"odometer":4280.0},"documents":[{"title":"Tax token","number":"DEMO","holder":"Rider","expiry":"2028-06-18"}]}"""
        val restored = VaultState.decode(old)!!
        assertEquals("R15 V4", restored.ride!!.displayName)
        assertEquals(restored, VaultState.decode(VaultState.encode(restored)))
    }

    @Test fun selectedModelAndManualNameSurviveRestartWithoutWrongPhoto() {
        val model = BikeCatalogue.models.first { it.name == "R15M" }
        val selected = VaultState.create(Ride("yamaha", "ABS", 2023, 4280.0, model.id, model.name))
        assertEquals(model.id, VaultState.decode(VaultState.encode(selected))!!.ride!!.selectedModel!!.id)
        val manual = VaultState.create(Ride("yamaha", "Custom trim", 2023, 4280.0, modelName = "My custom bike"))
        assertNull(manual.ride!!.selectedModel)
        assertEquals("My custom bike", VaultState.decode(VaultState.encode(manual))!!.ride!!.displayName)
    }

    @Test fun catalogueSearchAndBrandBoundariesMatchThePrototype() {
        assertEquals(78, BikeCatalogue.models.size)
        assertEquals(78, BikeCatalogue.models.map { it.id }.toSet().size)
        assertTrue(BikeCatalogue.search("yamaha", "  r15m  ").any { it.name == "R15M" })
        assertTrue(BikeCatalogue.search("honda", "R15M").isEmpty())
        val wrong = BikeCatalogue.forBrand("yamaha").first()
        assertFailsWith<IllegalArgumentException> { VaultState.create(Ride("honda", "ABS", 2023, 4280.0, wrong.id, wrong.name)) }
    }

    @Test fun lastServiceBaselineUsesOwnerEnteredDistanceAndDate() {
        val date = kotlinx.datetime.LocalDate.parse("2026-01-01")
        val result = VaultState.create(Ride("yamaha", "ABS", 2023, 4280.0), 4000.0, date)
        assertTrue(result.rules.all { it.lastServicedOdometerKm == 4000.0 && it.lastServicedEpochDays == date.toEpochDays().toLong() })
    }

    @Test fun zeroSchedulesAndInvalidDocumentDateOrderAreRejected() {
        val original = state()
        assertFailsWith<IllegalArgumentException> { original.saveSchedule(original.rules.first().copy(intervalKm = 0.0, intervalDays = 0)) }
        assertFailsWith<IllegalArgumentException> { original.saveDocument(VaultDocument("Tax token", "DEMO", "Rider", "2026-01-01", issued = "2026-02-01")) }
        val noExpiry = VaultDocument("Registration smart card", "DEMO", "Rider", "")
        assertEquals("No expiry", original.saveDocument(noExpiry).documents.first().expiryLabel)
    }
}
