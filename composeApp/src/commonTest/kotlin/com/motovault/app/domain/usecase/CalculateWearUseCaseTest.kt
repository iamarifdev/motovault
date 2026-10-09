package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.WearHealthStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculateWearUseCaseTest {

    private val useCase = CalculateWearUseCase()

    private val sampleOilRule = ComponentRule(
        id = "rule_oil_01",
        vehicleId = "duke_125",
        name = "Engine Oil",
        specification = "10W-50",
        intervalKm = 5000.0,
        intervalDays = 120,
        lastServicedOdometerKm = 1000.0,
        lastServicedEpochDays = 100L
    )

    @Test
    fun testHealthyComponent_UnderThreshold() {
        // 2000 km ridden (1000 km delta out of 5000 = 20% distance wear)
        // 20 days elapsed (20 delta out of 120 = 16.6% time wear)
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 2000.0,
            currentEpochDays = 120L
        )

        assertEquals(20, result.wearPercentage)
        assertEquals(WearHealthStatus.HEALTHY, result.healthStatus)
        assertEquals(4000.0, result.remainingDistanceKm)
        assertEquals(100, result.remainingDays)
        assertFalse(result.isDistanceExceeded)
        assertFalse(result.isTimeExceeded)
    }

    @Test
    fun testServiceSoon_At75PercentThreshold() {
        // 4750 km odometer (3750 delta out of 5000 = 75% distance wear)
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 4750.0,
            currentEpochDays = 100L
        )

        assertEquals(75, result.wearPercentage)
        assertEquals(WearHealthStatus.SERVICE_SOON, result.healthStatus)
        assertEquals(1250.0, result.remainingDistanceKm)
        assertFalse(result.isDistanceExceeded)
    }

    @Test
    fun testOverdueComponent_At100PercentDistanceThreshold() {
        // Exactly 6000 km odometer (5000 delta out of 5000 = 100%)
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 6000.0,
            currentEpochDays = 100L
        )

        assertEquals(100, result.wearPercentage)
        assertEquals(WearHealthStatus.OVERDUE, result.healthStatus)
        assertEquals(0.0, result.remainingDistanceKm)
        assertTrue(result.isDistanceExceeded)
    }

    @Test
    fun testOverdueComponent_Beyond100Percent() {
        // 7000 km odometer (6000 delta out of 5000 = 120%)
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 7000.0,
            currentEpochDays = 100L
        )

        assertEquals(120, result.wearPercentage)
        assertEquals(WearHealthStatus.OVERDUE, result.healthStatus)
        assertEquals(0.0, result.remainingDistanceKm)
        assertTrue(result.isDistanceExceeded)
    }

    @Test
    fun testEdgeCase_TimeExceededWhileDistanceIsMinimal() {
        // Edge Case: Bike sat parked in garage for 150 days (150 delta out of 120 = 125% time wear)
        // But only 50 km were ridden!
        // Dual-threshold rule MUST trigger OVERDUE based on elapsed time because oil degrades.
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 1050.0,
            currentEpochDays = 250L
        )

        assertEquals(125, result.wearPercentage)
        assertEquals(WearHealthStatus.OVERDUE, result.healthStatus)
        assertFalse(result.isDistanceExceeded)
        assertTrue(result.isTimeExceeded)
        assertEquals(0, result.remainingDays)
    }

    @Test
    fun testEdgeCase_OdometerRollbackOrTypoDoesNotProduceNegativeWear() {
        // Edge Case: User accidentally types odometer lower than last service
        val result = useCase.execute(
            rule = sampleOilRule,
            currentOdometerKm = 500.0, // Less than lastServiced (1000)
            currentEpochDays = 50L   // Less than lastServiced (100)
        )

        assertEquals(0, result.wearPercentage)
        assertEquals(WearHealthStatus.HEALTHY, result.healthStatus)
        assertEquals(5000.0, result.remainingDistanceKm)
        assertEquals(120, result.remainingDays)
    }

    @Test
    fun testEdgeCase_ZeroIntervalRulesDoNotDivideByZero() {
        val zeroRule = ComponentRule(
            id = "zero_rule",
            vehicleId = "v1",
            name = "No Interval",
            specification = "N/A",
            intervalKm = 0.0,
            intervalDays = 0,
            lastServicedOdometerKm = 0.0,
            lastServicedEpochDays = 0L
        )

        val result = useCase.execute(zeroRule, currentOdometerKm = 100.0, currentEpochDays = 10L)

        assertEquals(0, result.wearPercentage)
        assertEquals(WearHealthStatus.HEALTHY, result.healthStatus)
        assertEquals(0.0, result.remainingDistanceKm)
        assertEquals(0, result.remainingDays)
    }
}
