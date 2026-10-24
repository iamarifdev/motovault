package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.DocumentExpiryStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentExpiryUseCaseTest {
    private val useCase = DocumentExpiryUseCase()

    @Test fun missingExpiryStaysUnclassifiedAsValidOrExpired() {
        assertEquals(DocumentExpiryStatus.NO_EXPIRY, useCase.evaluate(null))
    }

    @Test fun moreThanThirtyDaysIsValid() {
        assertEquals(DocumentExpiryStatus.VALID, useCase.evaluate(31))
    }

    @Test fun renewalWarningIncludesTheThirtiethAndLastRemainingDay() {
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, useCase.evaluate(30))
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, useCase.evaluate(1))
    }

    @Test fun expiryDayIsExpired() {
        assertEquals(DocumentExpiryStatus.EXPIRED, useCase.evaluate(0))
    }

    @Test fun pastExpiryIsExpired() {
        assertEquals(DocumentExpiryStatus.EXPIRED, useCase.evaluate(-12))
    }
}
