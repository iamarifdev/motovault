package com.motovault.app

import com.motovault.app.ui.*
import kotlin.test.*

class FactorySetupTest {
    private fun supported() = Ride("ktm", "EU", 2023, 4000.0, "ktm-duke-125-2023", "Duke 125", market = RideMarket.EUROPE,
        emission = RideEmission.EURO5, modelCode = "F4003W1", factoryProfileId = FactoryProfiles.profiles.single().id)

    @Test fun selectedManualReachesActiveGarageWithoutInventingEconomy() {
        val state = VaultState.create(supported(), 1000.0)
        assertEquals(7500.0, state.rules.first { it.name == "Engine oil" }.intervalKm)
        assertEquals(365, state.rules.first { it.name == "Engine oil" }.intervalDays)
        assertNull(state.economy)
        assertEquals(state, VaultState.decode(VaultState.encode(state)))
    }

    @Test fun manualCannotLeakAcrossModelsYearsMarketsOrCodes() {
        val ride = supported()
        listOf(ride.copy(year = 2022), ride.copy(market = RideMarket.INDIA, emission = RideEmission.BS6),
            ride.copy(modelId = "ktm-rc-125-2022"), ride.copy(modelCode = "NOT-VERIFIED")).forEach {
            assertFailsWith<IllegalArgumentException> { VaultState.create(it, 1000.0) }
        }
        assertFailsWith<IllegalArgumentException> { VaultState.create(ride, 0.0) }
    }

    @Test fun incompatibleMarketsAreRejectedWithoutInferringEmissionFromYear() {
        assertFailsWith<IllegalArgumentException> { VaultState.create(supported().copy(emission = RideEmission.BS6), 1000.0) }
        val earlierEuro5 = supported().copy(year = 2020, factoryProfileId = null)
        assertEquals(RideEmission.EURO5, VaultState.create(earlierEuro5).ride?.emission)
        val importedBs4 = supported().copy(year = 2021, market = RideMarket.INDIA,
            emission = RideEmission.BS4, factoryProfileId = null)
        assertEquals(RideEmission.BS4, VaultState.create(importedBs4).ride?.emission)
        assertTrue(RideEmission.UNKNOWN in FactoryProfiles.emissions(RideMarket.UNKNOWN))
        assertTrue(RideEmission.BS6 in FactoryProfiles.emissions(RideMarket.UNKNOWN))
    }

    @Test fun legacyRestorePreservesIdentityAndCustomSchedules() {
        val original = VaultState.create(Ride("ktm", "Imported edition", 2019, 4000.0))
        val custom = original.saveSchedule(original.rules.first().copy(intervalKm = 700.0))
        val restored = assertNotNull(VaultState.decode(VaultState.encode(custom)))
        assertEquals("Imported edition", restored.ride?.edition)
        assertEquals(RideMarket.UNKNOWN, restored.ride?.market)
        assertNull(restored.ride?.factoryProfileId)
        assertEquals(700.0, restored.rules.first().intervalKm)
    }

    @Test fun restoringManualProfileKeepsOwnerAdjustedIntervalsAndBaselines() {
        val original = VaultState.create(supported(), 1000.0)
        val oil = original.rules.first { it.name == "Engine oil" }
        val custom = original.saveSchedule(oil.copy(intervalKm = 5000.0, intervalDays = 180,
            lastServicedOdometerKm = 3000.0))
        val restored = assertNotNull(VaultState.decode(VaultState.encode(custom)))
        val restoredOil = restored.rules.first { it.id == oil.id }
        assertEquals(5000.0, restoredOil.intervalKm)
        assertEquals(180, restoredOil.intervalDays)
        assertEquals(3000.0, restoredOil.lastServicedOdometerKm)
        assertEquals(original.ride?.factoryProfileId, restored.ride?.factoryProfileId)
    }

    @Test fun unknownManualModelsRemainOwnerDefined() {
        val state = VaultState.create(Ride("honda", "", 2025, 2500.0, modelName = "My motorcycle"))
        assertEquals(3, state.rules.size)
        assertTrue(state.rules.all { "Owner-defined" in it.specification })
    }
}
