package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.DocumentExpiryStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentExpiryUseCaseTest {
    private val useCase = DocumentExpiryUseCase()

    @Test fun missingExpiryStaysUnclassifiedAsValidOrExpired() {
        assertEquals(DocumentExpiryStatus.NO_EXPIRY, useCase.evaluate(null))
    }

    @Test fun moreThanSixtyDaysIsValid() {
        assertEquals(DocumentExpiryStatus.VALID, useCase.evaluate(61))
    }

    @Test fun renewalWarningIncludesTheSixtiethAndLastRemainingDay() {
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, useCase.evaluate(60))
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, useCase.evaluate(1))
    }

    @Test fun expiryDayIsExpired() {
        assertEquals(DocumentExpiryStatus.EXPIRED, useCase.evaluate(0))
    }

    @Test fun pastExpiryIsExpired() {
        assertEquals(DocumentExpiryStatus.EXPIRED, useCase.evaluate(-12))
    }
}
