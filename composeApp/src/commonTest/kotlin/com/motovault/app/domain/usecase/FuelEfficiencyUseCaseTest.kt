package com.motovault.app.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuelEfficiencyUseCaseTest {
    private val useCase = FuelEfficiencyUseCase()

    @Test fun firstFullTankOnlyEstablishesTheBaseline() {
        assertNull(useCase.calculate(listOf(FuelFill(1000.0, 11.0, true))))
    }

    @Test fun fullToFullExcludesTheBaselinePurchase() {
        assertEquals(38.5, useCase.calculate(listOf(FuelFill(1000.0, 11.0, true), FuelFill(1385.0, 10.0, true))))
    }

    @Test fun partialFillWaitsForTheNextFullTank() {
        assertNull(useCase.calculate(listOf(FuelFill(1000.0, 10.0, true), FuelFill(1100.0, 2.0, false))))
        assertEquals(44.0, useCase.calculate(listOf(FuelFill(1000.0, 10.0, true), FuelFill(1100.0, 2.0, false), FuelFill(1220.0, 3.0, true))))
    }

    @Test fun partialFillsBeforeTheBaselineDoNotCount() {
        assertNull(useCase.calculate(listOf(FuelFill(900.0, 4.0, false), FuelFill(1000.0, 10.0, true))))
    }

    @Test fun averagesCompletedCyclesByDistanceAndLitresDespiteStorageOrder() {
        val entries = listOf(FuelFill(1550.0, 2.0, false), FuelFill(1500.0, 10.0, true), FuelFill(1000.0, 10.0, true), FuelFill(1200.0, 5.0, true))
        assertEquals(500.0 / 15.0, useCase.calculate(entries))
    }

    @Test fun legacyRecordsWithoutLitresDoNotInventAnEconomy() {
        assertNull(useCase.calculate(listOf(FuelFill(1000.0, 10.0, true), FuelFill(1200.0, null, true))))
    }
}
