package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.GovernmentDocument
import com.motovault.app.domain.model.GovernmentDocumentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentExpiryUseCaseTest {

    private val useCase = DocumentExpiryUseCase()

    private val sampleDoc = GovernmentDocument(
        id = "doc_tax_01",
        vehicleId = "duke_125",
        type = GovernmentDocumentType.TAX_TOKEN,
        documentNumber = "TT-992014-2024",
        holderName = "MD. ARIFUR RAHMAN",
        issueDateFormatted = "14-OCT-2024",
        expiryDateFormatted = "13-OCT-2026",
        daysUntilExpiry = 100
    )

    @Test
    fun testValidDocument_MoreThan60DaysRemaining() {
        // Expiry is 100 days away
        val result = useCase.evaluate(
            document = sampleDoc,
            currentEpochDays = 1000L,
            expiryEpochDays = 1100L
        )

        assertEquals(DocumentExpiryStatus.VALID, result.status)
        assertEquals(100, result.daysRemaining)
        assertFalse(result.requiresImmediateAttention)
        assertNull(result.alertNotificationTitle)
    }

    @Test
    fun testExpiringSoonDocument_60DaysOrLessTriggersAlert() {
        // Expiry is 15 days away
        val result = useCase.evaluate(
            document = sampleDoc,
            currentEpochDays = 1000L,
            expiryEpochDays = 1015L
        )

        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, result.status)
        assertEquals(15, result.daysRemaining)
        assertTrue(result.requiresImmediateAttention)
        assertNotNull(result.alertNotificationTitle)
        assertTrue(result.alertNotificationTitle!!.contains("Renewal Due in 15 Days"))
    }

    @Test
    fun testExpiredDocument_Exactly0DaysTriggersUrgentAlert() {
        // Today is expiry day
        val result = useCase.evaluate(
            document = sampleDoc,
            currentEpochDays = 1000L,
            expiryEpochDays = 1000L
        )

        assertEquals(DocumentExpiryStatus.EXPIRED, result.status)
        assertEquals(0, result.daysRemaining)
        assertTrue(result.requiresImmediateAttention)
        assertNotNull(result.alertNotificationTitle)
        assertTrue(result.alertNotificationTitle!!.contains("Document Expired"))
    }

    @Test
    fun testExpiredDocument_PastDueNegativeDays() {
        // Expired 12 days ago
        val result = useCase.evaluate(
            document = sampleDoc,
            currentEpochDays = 1012L,
            expiryEpochDays = 1000L
        )

        assertEquals(DocumentExpiryStatus.EXPIRED, result.status)
        assertEquals(-12, result.daysRemaining)
        assertTrue(result.requiresImmediateAttention)
        assertTrue(result.alertNotificationBody!!.contains("Renew immediately"))
    }

    @Test
    fun renewalBoundaryMatchesTheModelAt60Days() {
        assertEquals(DocumentExpiryStatus.VALID, useCase.evaluate(sampleDoc, 1000, 1061).status)
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, useCase.evaluate(sampleDoc, 1000, 1060).status)
        assertTrue(sampleDoc.copy(daysUntilExpiry = 60).isExpiringSoon)
        assertFalse(sampleDoc.copy(daysUntilExpiry = 61).isExpiringSoon)
        assertFalse(sampleDoc.copy(daysUntilExpiry = 0).isExpiringSoon)
    }
}
