package com.motovault.app.domain.usecase

import kotlin.test.*

class FuelEfficiencyUseCaseTest {

    private val useCase = FuelEfficiencyUseCase()

    @Test
    fun testInitialFillUp_EstablishesBaselineWithoutEfficiency() {
        val firstFill = FuelLogEntry(
            id = "fuel_01",
            vehicleId = "duke_125",
            odometerKm = 1000.0,
            liters = 11.0,
            totalCost = 1430.0,
            isFullTank = true,
            epochMillis = 1000000L
        )

        val result = useCase.calculate(previousEntry = null, currentEntry = firstFill)

        assertNull(result.calculatedEconomyKmPerLiter)
        assertNull(result.costPerKm)
        assertFalse(result.isReliable)
        assertTrue(result.statusMessage.contains("Initial fill-up"))
    }

    @Test
    fun testStandardFullToFull_CalculatesAccurateKmPerLiter() {
        val firstFill = FuelLogEntry(
            id = "fuel_01",
            vehicleId = "duke_125",
            odometerKm = 1000.0,
            liters = 10.0,
            totalCost = 1300.0,
            isFullTank = true,
            epochMillis = 1000000L
        )

        // Rode 385 km (1385 - 1000) and put in 10 liters to top up full tank
        val secondFill = FuelLogEntry(
            id = "fuel_02",
            vehicleId = "duke_125",
            odometerKm = 1385.0,
            liters = 10.0,
            totalCost = 1350.0,
            isFullTank = true,
            epochMillis = 2000000L
        )

        val result = useCase.calculate(previousEntry = firstFill, currentEntry = secondFill)

        assertTrue(result.isReliable)
        assertEquals(38.5, result.calculatedEconomyKmPerLiter)
        assertEquals(3.51, result.costPerKm) // 1350 / 385 = 3.506... rounded to 3.51
        assertEquals(385.0, result.deltaDistanceKm)
    }

    @Test
    fun testPartialFillUp_DoesNotCalculatePrematureEconomy() {
        val firstFill = FuelLogEntry(
            id = "fuel_01",
            vehicleId = "duke_125",
            odometerKm = 1000.0,
            liters = 10.0,
            totalCost = 1300.0,
            isFullTank = true,
            epochMillis = 1000000L
        )

        // Second fill was only partial (isFullTank = false)
        val secondFillPartial = FuelLogEntry(
            id = "fuel_02",
            vehicleId = "duke_125",
            odometerKm = 1200.0,
            liters = 5.0,
            totalCost = 650.0,
            isFullTank = false,
            epochMillis = 2000000L
        )

        val result = useCase.calculate(previousEntry = firstFill, currentEntry = secondFillPartial)

        assertNull(result.calculatedEconomyKmPerLiter)
        assertFalse(result.isReliable)
        assertTrue(result.statusMessage.contains("Partial fill-up"))
    }

    @Test
    fun testEdgeCase_NegativeOrZeroDistanceDelta() {
        val firstFill = FuelLogEntry(
            id = "fuel_01",
            vehicleId = "duke_125",
            odometerKm = 1000.0,
            liters = 10.0,
            totalCost = 1300.0,
            isFullTank = true,
            epochMillis = 1000000L
        )

        val invalidFill = FuelLogEntry(
            id = "fuel_02",
            vehicleId = "duke_125",
            odometerKm = 950.0, // Typo: Odometer rolled backwards!
            liters = 8.0,
            totalCost = 1000.0,
            isFullTank = true,
            epochMillis = 2000000L
        )

        val result = useCase.calculate(previousEntry = firstFill, currentEntry = invalidFill)

        assertNull(result.calculatedEconomyKmPerLiter)
        assertFalse(result.isReliable)
        assertTrue(result.statusMessage.contains("Invalid odometer delta"))
    }

    @Test
    fun testEdgeCase_ZeroOrNegativeLiters() {
        val firstFill = FuelLogEntry(
            id = "fuel_01",
            vehicleId = "duke_125",
            odometerKm = 1000.0,
            liters = 10.0,
            totalCost = 1300.0,
            isFullTank = true,
            epochMillis = 1000000L
        )

        val zeroLitersFill = FuelLogEntry(
            id = "fuel_02",
            vehicleId = "duke_125",
            odometerKm = 1200.0,
            liters = 0.0,
            totalCost = 0.0,
            isFullTank = true,
            epochMillis = 2000000L
        )

        val result = useCase.calculate(previousEntry = firstFill, currentEntry = zeroLitersFill)

        assertNull(result.calculatedEconomyKmPerLiter)
        assertFalse(result.isReliable)
        assertTrue(result.statusMessage.contains("Fuel volume must be positive"))
    }
}
