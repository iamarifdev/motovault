package com.motovault.app.domain.documents

import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.assetIds
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentRenewalTest {
    private val renewedOn = LocalDate(2026, 11, 5)

    private fun state(expiry: String = "2026-12-31", kind: String = "tax") = VaultState(
        ride = Ride("ktm", "Indian · BS6", 2023, 4280.0),
        documents = listOf(
            VaultDocument(
                "Tax token", "TT-1", "Owner", expiry, id = "doc-1", kind = kind,
                reminder = true, reminderSnoozedUntil = "2026-11-01T09:00",
                checkedRenewalSteps = setOf("tax_previous_token", "tax_fee_payment")
            )
        )
    )

    @Test
    fun renewalKeepsBothExpiriesThePaidFeeAndTheDocumentRevision() {
        val renewed = state().renewDocument(
            "doc-1", LocalDate(2027, 12, 31), renewedOn, feePoisha = 125000, receiptId = "receipt-1.jpg", notes = "Counter copy"
        )
        val document = renewed.documents.single()
        assertEquals("2027-12-31", document.expiry)
        assertEquals(1, document.revision)
        val event = document.renewals.single()
        assertEquals("2026-12-31", event.previousExpiry)
        assertEquals("2027-12-31", event.newExpiry)
        assertEquals("tax", event.kind)
        assertEquals(125000, event.feePoisha)
        assertEquals("receipt-1.jpg", event.receiptId)
        assertEquals(1, event.documentRevision)
        assertEquals("2026-11-05", event.renewedOn)
        assertEquals("Counter copy", event.notes)
        assertTrue("receipt-1.jpg" in document.assetIds())

        val fee = renewed.logs.single()
        assertEquals("document_fee", fee.kind)
        assertEquals(1250.0, fee.cost)
        assertEquals("doc-1", fee.documentId)
        assertEquals(event.id, fee.renewalId)
        assertEquals(fee.id, event.feeLogId)
        assertEquals("", fee.title)
        assertEquals("", fee.detail)
    }

    @Test
    fun renewalResetsTheReminderSnoozeAndTheChecklistTogether() {
        val document = state().renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn).documents.single()
        assertNull(document.reminderSnoozedUntil)
        assertTrue(document.checkedRenewalSteps.isEmpty())
        assertTrue(document.reminder)
    }

    @Test
    fun renewalWithoutAPaidFeeAddsNoLedgerEntry() {
        val renewed = state().renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn, feePoisha = 0)
        assertTrue(renewed.logs.isEmpty())
        assertNull(renewed.documents.single().renewals.single().feeLogId)
    }

    @Test
    fun renewalRejectsAnExpiryThatDoesNotExtendTheCurrentOne() {
        assertFailsWith<IllegalArgumentException> {
            state().renewDocument("doc-1", LocalDate(2026, 12, 31), renewedOn)
        }
        assertFailsWith<IllegalArgumentException> {
            state().renewDocument("doc-1", LocalDate(2026, 1, 1), renewedOn)
        }
    }

    @Test
    fun renewalRejectsADuplicateEvent() {
        val first = state().renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn)
        assertFailsWith<IllegalArgumentException> {
            first.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn)
        }
    }

    @Test
    fun renewalRejectsAStaleDocument() {
        assertFailsWith<IllegalStateException> {
            state().renewDocument("missing", LocalDate(2027, 12, 31), renewedOn)
        }
    }

    @Test
    fun renewalAcceptsADocumentWhoseStoredExpiryCannotBeParsed() {
        val renewed = state(expiry = "").renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn)
        assertEquals("", renewed.documents.single().renewals.single().previousExpiry)
        assertEquals("2027-12-31", renewed.documents.single().expiry)
    }

    @Test
    fun renewalRejectsInvalidMoneyReceiptAndNotes() {
        val state = state()
        listOf(-1, 10_000_001).forEach { fee ->
            assertFailsWith<IllegalArgumentException> {
                state.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn, feePoisha = fee)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            state.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn, receiptId = "../receipt.jpg")
        }
        assertFailsWith<IllegalArgumentException> {
            state.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn, notes = "x".repeat(501))
        }
    }

    @Test
    fun renewalRequiresABikeOnlyWhenAFeeIsRecorded() {
        val noRide = VaultState(documents = state().documents)
        assertTrue(noRide.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn).logs.isEmpty())
        assertFailsWith<IllegalArgumentException> {
            noRide.renewDocument("doc-1", LocalDate(2027, 12, 31), renewedOn, feePoisha = 100000)
        }
    }

    @Test
    fun checklistKeepsOnlyStableStepIdsThatBelongToTheGuide() {
        val guide = assertNotNull(RenewalChecklists.of("tax"))
        assertEquals(setOf("tax_previous_token", "tax_fee_payment", "tax_fee_receipt"), guide.stepIds)
        assertEquals("2026-11-05", guide.sourceCheckedOn)
        val updated = state().setRenewalChecklist("doc-1", setOf("tax_previous_token", "tax_fee_receipt"))
        assertEquals(setOf("tax_previous_token", "tax_fee_receipt"), updated.documents.single().checkedRenewalSteps)
        assertFailsWith<IllegalArgumentException> {
            state().setRenewalChecklist("doc-1", setOf("licence_nid"))
        }
        assertFailsWith<IllegalStateException> {
            state().setRenewalChecklist("missing", emptySet())
        }
    }

    @Test
    fun legacyStoredDocumentWithoutRenewalFieldsStillDecodes() {
        val json = """{"documents":[{"title":"Tax token","number":"TT-1","holder":"Owner","expiry":"2026-12-31","id":"doc-1","kind":"tax"}],"ride":{"brandId":"ktm","edition":"BS6","year":2023,"odometer":4280.0}}"""
        val decoded = assertNotNull(VaultState.decode(json))
        val document = decoded.documents.single()
        assertEquals(0, document.revision)
        assertTrue(document.renewals.isEmpty())
        assertTrue(document.checkedRenewalSteps.isEmpty())
    }

    @Test
    fun everyStoredKindResolvesToASourcedGuide() {
        listOf("registration", "licence", "tax", "insurance", "fitness").forEach { kind ->
            val guide = assertNotNull(RenewalChecklists.of(kind))
            assertTrue(guide.steps.isNotEmpty())
            assertEquals(RENEWAL_SOURCES_CHECKED_ON, guide.sourceCheckedOn)
        }
        assertNull(RenewalChecklists.of("unknown"))
        assertEquals("licence", com.motovault.app.domain.model.DocumentKinds.fromStored("", "Driving licence"))
        assertEquals("registration", com.motovault.app.domain.model.DocumentKinds.fromStored("smart_card", ""))
        assertNull(com.motovault.app.domain.model.DocumentKinds.fromStored("", "Some paper"))
    }
}