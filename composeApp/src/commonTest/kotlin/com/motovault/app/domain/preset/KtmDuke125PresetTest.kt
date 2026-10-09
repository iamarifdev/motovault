package com.motovault.app.domain.preset

import com.motovault.app.domain.model.Brand
import com.motovault.app.domain.model.EmissionStandard
import com.motovault.app.domain.model.VehicleEdition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtmDuke125PresetTest {

    @Test
    fun testPresetCreation_IndianBS4_Has10Point2LitersTank() {
        val vehicle = KtmDuke125Preset.createVehicle(
            id = "v_bs4",
            edition = VehicleEdition.INDIAN,
            emissionStandard = EmissionStandard.BS4,
            releaseYear = 2019,
            initialOdometerKm = 5000.0
        )

        assertEquals("KTM Duke 125", vehicle.name)
        assertEquals(Brand.KTM, vehicle.brand)
        assertEquals(VehicleEdition.INDIAN, vehicle.edition)
        assertEquals(EmissionStandard.BS4, vehicle.emissionStandard)
        assertEquals(10.2, vehicle.fuelTankCapacityLiters)
        assertEquals(124.7, vehicle.engineDisplacementCc)
        assertEquals(0xFFFF6600, vehicle.activeAccentColorHex)
    }

    @Test
    fun testPresetCreation_IndianBS6_Has13Point5LitersTank() {
        val vehicle = KtmDuke125Preset.createVehicle(
            id = "v_bs6",
            edition = VehicleEdition.INDIAN,
            emissionStandard = EmissionStandard.BS6,
            releaseYear = 2021,
            initialOdometerKm = 4200.0
        )

        assertEquals(EmissionStandard.BS6, vehicle.emissionStandard)
        assertEquals(13.5, vehicle.fuelTankCapacityLiters)
        assertEquals(4200.0, vehicle.currentOdometerKm)
    }

    @Test
    fun testDefaultComponentRules_ContainsAll7FactoryMaintenanceItems() {
        val rules = KtmDuke125Preset.defaultComponentRules("test_duke", 4200.0)

        assertEquals(7, rules.size)

        // 1. Chain Lube rule
        val chain = rules.find { it.name.contains("Chain") }
        assertNotNull(chain)
        assertEquals(500.0, chain.intervalKm)
        assertEquals(30, chain.intervalDays)
        assertTrue(chain.isCritical)

        // 2. Engine Oil rule
        val oil = rules.find { it.name.contains("Engine Oil") }
        assertNotNull(oil)
        assertEquals(5000.0, oil.intervalKm)
        assertEquals(120, oil.intervalDays)
        assertTrue(oil.isCritical)

        // 3. Air Filter rule
        val air = rules.find { it.name.contains("Air Filter") }
        assertNotNull(air)
        assertEquals(5000.0, air.intervalKm)

        // 4. Spark Plug rule
        val plug = rules.find { it.name.contains("Spark Plug") }
        assertNotNull(plug)
        assertEquals(10000.0, plug.intervalKm)
        assertTrue(plug.specification.contains("LKAR8A-9"))

        // 5. Brake Pads rule
        val brakePads = rules.find { it.name.contains("Brake Pads") }
        assertNotNull(brakePads)
        assertEquals(7500.0, brakePads.intervalKm)

        // 6. Coolant rule
        val coolant = rules.find { it.name.contains("Coolant") }
        assertNotNull(coolant)
        assertEquals(15000.0, coolant.intervalKm)

        // 7. Brake Fluid rule
        val fluid = rules.find { it.name.contains("Brake Fluid") }
        assertNotNull(fluid)
        assertEquals(15000.0, fluid.intervalKm)
        assertTrue(fluid.specification.contains("DOT 4"))
    }
}
