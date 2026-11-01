package com.motovault.app

import com.motovault.app.ui.*
import kotlin.test.*

class FactorySetupTest {
    private fun supported() = Ride("ktm", "EU", 2023, 4000.0, "ktm-duke-125-2023", "Duke 125", market = RideMarket.EUROPE,
        emission = RideEmission.EURO5, modelCode = "F4003W1", factoryProfileId = "ktm-125-duke-2023-eu-3214785")

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
        val atZero = VaultState.create(ride, 0.0)
        assertTrue(atZero.rules.isNotEmpty())
        assertTrue(atZero.rules.all { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RECURRING })
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

    private fun bangladeshR15() = Ride("yamaha", "BD", 2023, 4000.0, "yamaha-r15-v4", "R15 V4",
        market = RideMarket.BANGLADESH, emission = RideEmission.BS6, modelCode = "YZF155-A",
        factoryProfileId = "yamaha-r15-v4-2023-bd-155")

    @Test fun bangladeshProfileIncludesRunningInAndRecurringSchedules() {
        val runningIn = VaultState.create(bangladeshR15(), 0.0)
        assertTrue(runningIn.rules.any { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RUNNING_IN })
        assertTrue(runningIn.rules.any { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RECURRING })
        assertEquals(0.0, runningIn.rules.first { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RUNNING_IN }.lastServicedOdometerKm)

        val recurring = VaultState.create(bangladeshR15(), 1000.0)
        assertFalse(recurring.rules.any { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RUNNING_IN })
        assertEquals(5000.0, recurring.rules.first { it.name == "Engine oil" }.intervalKm)
        assertEquals("BGN-F819V-E0", recurring.rules.first { it.name == "Engine oil" }.provenance.revision)
    }

    @Test fun unknownBaselineKeepsUncertaintyWithoutInventingServiceLogs() {
        val state = VaultState.create(bangladeshR15(), null)
        assertTrue(state.logs.isEmpty())
        assertTrue(state.rules.all { it.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.UNKNOWN })
        assertEquals(0.0, state.rules.first { it.name == "Engine oil" }.lastServicedOdometerKm)
    }

    @Test fun mt15ProfileIsAvailableAndUnsupportedModelsStayOwnerDefined() {
        assertTrue(FactoryProfiles.available("yamaha-mt-15-v2", 2027, RideMarket.BANGLADESH).isNotEmpty())
        val unsupported = VaultState.create(Ride("yamaha", "BD", 2023, 4000.0, "yamaha-r15-v3", "R15 V3",
            market = RideMarket.BANGLADESH, emission = RideEmission.BS6, modelCode = "YZF155-A"))
        assertNull(unsupported.ride?.factoryProfileId)
        assertTrue(unsupported.rules.all { it.provenance.source == com.motovault.app.domain.maintenance.ScheduleSource.OWNER })
    }

    @Test fun recordingRunningInServiceRemovesOnlyThatSchedule() {
        val state = VaultState.create(bangladeshR15(), 0.0)
        val first = state.rules.first { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RUNNING_IN }
        val oil = state.rules.first { it.name == "Engine oil" && it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RECURRING }
        val updated = state.logService(first, odometer = 1000.0)
        assertFalse(updated.rules.any { it.id == first.id })
        assertEquals(1000.0, updated.rules.first { it.id == oil.id }.lastServicedOdometerKm)
        assertEquals(1, updated.logs.count { it.componentId == first.id })
    }

    @Test fun preP04ScheduleJsonDecodesWithSafeDefaults() {
        val json = """{"ride":{"brandId":"yamaha","edition":"","year":2023,"odometer":4000.0,"modelId":"yamaha-r15-v4","modelName":"R15 V4","market":"BANGLADESH","emission":"BS6","modelCode":"YZF155-A","factoryProfileId":null},"rules":[{"id":"old","vehicleId":"primary","name":"Old rule","specification":"frozen","intervalKm":500.0,"intervalDays":30,"lastServicedOdometerKm":100.0,"lastServicedEpochDays":100,"isCritical":true,"reminder":true,"baselineCertainty":"CONFIRMED"}],"logs":[]}"""
        val state = assertNotNull(VaultState.decode(json))
        val rule = state.rules.single()
        assertEquals(com.motovault.app.domain.maintenance.MaintenanceAction.REPLACE, rule.action)
        assertEquals(com.motovault.app.domain.maintenance.ScheduleSource.OWNER, rule.provenance.source)
        assertEquals(com.motovault.app.domain.maintenance.SchedulePhase.RECURRING, rule.phase)
        assertFalse(rule.oneTime)
        assertNull(rule.catalogueId)
        assertEquals(state, VaultState.decode(VaultState.encode(state)))
    }
}
