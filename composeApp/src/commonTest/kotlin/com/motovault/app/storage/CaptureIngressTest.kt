package com.motovault.app.storage

import com.motovault.app.application.capture.*
import com.motovault.app.application.fuel.*
import com.motovault.app.domain.capture.*
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.vault.VaultState
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class CaptureIngressTest {
    @Test fun explicitInboxRejectsOversizedOrBinaryTextAndDoesNotReplaceWaitingShare() {
        ShareCaptureInbox.pending.value?.let { ShareCaptureInbox.take(it) }
        assertFalse(ShareCaptureInbox.offer(" "))
        assertFalse(ShareCaptureInbox.offer("a".repeat(MAX_CAPTURE_TEXT_LENGTH + 1)))
        assertFalse(ShareCaptureInbox.offer("text\u0000binary"))
        assertTrue(ShareCaptureInbox.offer("Fuel Tk 500.50"))
        assertFalse(ShareCaptureInbox.offer("Service Tk 900"))
        assertEquals("Fuel Tk 500.50", ShareCaptureInbox.pending.value)
        assertTrue(ShareCaptureInbox.take("Fuel Tk 500.50"))
        assertNull(ShareCaptureInbox.pending.value)
    }

    @Test fun correctingASharedDraftKeepsOriginalReceiptAndDuplicateProtection() {
        val factory = CaptureDraftFactory(CaptureFingerprintAdapter)
        val original = "Fuel Tk ৫০০.৫০"
        val first = factory.create(original, CaptureSource.SHARE, 0.0)
        val corrected = factory.create("Fuel Tk 501.25", CaptureSource.SHARE, 0.0, originalSharedText = original)
        assertEquals(first.id, corrected.id)
        assertEquals(50125L, corrected.amounts.single().value)
        assertTrue(CaptureIssue.DUPLICATE_SHARE in factory.create("Fuel Tk 502", CaptureSource.SHARE,
            0.0, acceptedIds = setOf(first.id), originalSharedText = original).issues)
    }

    @Test fun failedSaveConsumesNoReceiptAndSuccessfulSaveAndUndoKeepReceiptWithTheRecord() = runBlocking {
        var durable = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0))
        val original = durable
        var fail = true
        val writer = VaultSession(this, restore = { durable }, write = {
            if (fail) error("synthetic write failure") else durable = it
        }, cleanup = {}, recover = {})
        val workflow = FuelWorkflow(writer, FuelEntryAdapter, FuelEntryAdapter)
        val id = CaptureFingerprintAdapter.digest("synthetic explicit message")
        val request = FuelEntryRequest(600.50, 5.0, 120.10, 1100.0, LocalDate(2020, 1, 2),
            true, "octane", "", "", priceConfirmed = true, captureReceiptId = id)
        var result: FuelSaveOutcome? = null
        workflow.create(request, 0) { result = it }
        assertEquals(FuelSaveOutcome.FAILED, result)
        assertEquals(original, durable)
        fail = false
        workflow.create(request, 0) { result = it }
        assertEquals(FuelSaveOutcome.SAVED, result)
        assertEquals(listOf(id), durable.captureReceipts)
        assertEquals(60050L, durable.logs.single().costPoisha)
        workflow.create(request, 1) { result = it }
        assertEquals(FuelSaveOutcome.INVALID, result)
        assertEquals(1, durable.logs.size)
        var undone = false
        writer.undo(1) { undone = it }
        assertTrue(undone)
        assertEquals(original, durable)
    }
}
