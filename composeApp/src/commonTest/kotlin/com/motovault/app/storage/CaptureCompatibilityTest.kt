package com.motovault.app.storage

import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.domain.backup.initializeBackupTestCrypto
import com.motovault.app.domain.capture.*
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.vault.*
import com.motovault.app.storage.security.documentSha256
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class CaptureCompatibilityTest {
    @BeforeTest fun crypto() = initializeBackupTestCrypto()

    @Test fun fingerprintUsesProductionSha256AndCanonicalTextDetectsDigitWhitespaceDuplicates() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", CaptureFingerprintAdapter.digest("abc"))
        val factory = CaptureDraftFactory(CaptureFingerprintAdapter)
        val first = factory.create("Fuel  Tk ৫০০.৫০", CaptureSource.SHARE, 0.0)
        val again = factory.create(" fuel tk 500.50 ", CaptureSource.SHARE, 0.0, acceptedIds = setOf(first.id))
        assertEquals(first.id, again.id)
        assertTrue(CaptureIssue.DUPLICATE_SHARE in again.issues)
    }

    @Test fun acceptedIdsAreBoundedAndOnlyBecomeDurableWithTheReturnedAggregate() = runBlocking {
        val original = VaultState(ride = Ride("yamaha", "BD", 2026, 1000.0))
        assertTrue(requireNotNull(VaultState.decode("{}" )).captureReceipts.isEmpty())
        val id = CaptureFingerprintAdapter.digest("synthetic explicit share")
        val accepted = original.acceptCapture(id)
        assertTrue(original.captureReceipts.isEmpty())
        assertEquals(listOf(id), accepted.captureReceipts)
        assertFailsWith<IllegalArgumentException> { accepted.acceptCapture(id) }
        val previous = (0 until MAX_CAPTURE_RECEIPTS).map { CaptureFingerprintAdapter.digest(it.toString()) }
        assertEquals(MAX_CAPTURE_RECEIPTS, CaptureReceipts.accept(previous, id).size)
        assertEquals(accepted, VaultState.decode(VaultState.encode(accepted)))
        val adapter = BackupOperationsAdapter(::documentSha256) { error("No assets") }
        val archive = adapter.create(accepted, 1, "synthetic capture password") { _, _ -> }
        val preferences = adapter.preview(accepted, archive, "synthetic capture password").payload.preferences
        assertEquals(accepted.captureReceipts, preferences.toVaultSettings().captureReceipts)
    }
}
