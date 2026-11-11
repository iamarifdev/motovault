package com.motovault.app.domain.ledger

import com.motovault.app.domain.usecase.FuelEfficiencyUseCase
import com.motovault.app.domain.usecase.FuelFill
import kotlin.test.*

class FuelConfidenceTest {
    private val rules = FuelEfficiencyUseCase()
    @Test fun partialFillsBelongToTheClosingFullTankAndTailStaysOpen() {
        val result = rules.confidence(listOf(FuelFill(1000.0, 8.0, true), FuelFill(1060.0, 2.0, false),
            FuelFill(1150.0, 3.0, true), FuelFill(1180.0, 1.0, false)))
        assertEquals(30.0, result.economy)
        assertEquals(1, result.completedCycles)
        assertTrue(result.openCycle)
        assertFalse(result.brokenCycle)
    }
    @Test fun missedFillBreaksCycleAndNextFullTankStartsANewBaseline() {
        val result = rules.confidence(listOf(FuelFill(1000.0, 8.0, true), FuelFill(1060.0, 2.0, false, true),
            FuelFill(1150.0, 3.0, true), FuelFill(1270.0, 4.0, true)))
        assertEquals(30.0, result.economy)
        assertEquals(1, result.completedCycles)
        assertTrue(result.brokenCycle)
    }
    @Test fun invalidOrMissingLitresCannotProduceACompletedCycle() {
        for (litres in listOf(null, Double.NaN, -1.0, 0.0)) {
            val result = rules.confidence(listOf(FuelFill(1000.0, 8.0, true), FuelFill(1060.0, litres, false), FuelFill(1150.0, 3.0, true)))
            assertNull(result.economy)
            assertTrue(result.brokenCycle)
        }
    }
}
