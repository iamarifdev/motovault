package com.motovault.app.domain.usecase

import com.motovault.app.domain.preset.KtmDuke125Preset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LogMaintenanceUseCaseTest {

    private val useCase = LogMaintenanceUseCase()

    @Test
    fun test1TapChainLube_ResetsOnlyChainWearToCurrentOdometer() {
        // Initial setup at 4,000 km
        val initialRules = KtmDuke125Preset.defaultComponentRules("duke_125", initialOdometerKm = 4000.0)

        // Bike is now at 4,450 km (450 km since last lube, close to 500 km due date)
        // User taps 1-tap "Lube Chain"
        val result = useCase.resetChainLube(
            existingRules = initialRules,
            currentOdometerKm = 4450.0,
            currentEpochDays = 150L
        )

        assertEquals(1, result.resetCount)

        val updatedChain = result.updatedRules.find { it.name.contains("Chain") }
        assertNotNull(updatedChain)
        // Chain last serviced odometer must now be 4450 km
        assertEquals(4450.0, updatedChain.lastServicedOdometerKm)
        assertEquals(150L, updatedChain.lastServicedEpochDays)

        // Engine Oil must REMAIN at original 4000 km!
        val untouchedOil = result.updatedRules.find { it.name.contains("Engine Oil") }
        assertNotNull(untouchedOil)
        assertEquals(4000.0, untouchedOil.lastServicedOdometerKm)
    }

    @Test
    fun testRecordFullService_ResetsMultipleComponents() {
        val initialRules = KtmDuke125Preset.defaultComponentRules("duke_125", initialOdometerKm = 0.0)

        val oilRule = initialRules.find { it.name.contains("Engine Oil") }!!
        val airRule = initialRules.find { it.name.contains("Air Filter") }!!
        val plugRule = initialRules.find { it.name.contains("Spark Plug") }!!

        // Full service at 5,000 km: replaced Oil and Air Filter, but NOT Spark Plug
        val result = useCase.recordService(
            existingRules = initialRules,
            servicedComponentIds = setOf(oilRule.id, airRule.id),
            serviceOdometerKm = 5000.0,
            serviceEpochDays = 120L
        )

        assertEquals(2, result.resetCount)

        val updatedOil = result.updatedRules.find { it.id == oilRule.id }!!
        val updatedAir = result.updatedRules.find { it.id == airRule.id }!!
        val preservedPlug = result.updatedRules.find { it.id == plugRule.id }!!

        assertEquals(5000.0, updatedOil.lastServicedOdometerKm)
        assertEquals(5000.0, updatedAir.lastServicedOdometerKm)
        assertEquals(0.0, preservedPlug.lastServicedOdometerKm) // Still original 0.0
    }
}
