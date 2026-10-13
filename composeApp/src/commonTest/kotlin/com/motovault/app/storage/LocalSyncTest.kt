package com.motovault.app.storage

import com.motovault.app.ui.redesign.*
import kotlin.test.*

class LocalSyncTest {
    private fun state() = VaultState(documents = listOf(VaultDocument("Registration", "TEST", "Owner", "", id = "doc",
        frontPhoto = "legacy-base64", pages = listOf(DocumentPage("raw.jpg", "preview.jpg", "image/jpeg", 100, 80, "Front", acceptedId = "accepted.jpg")))))

    @Test fun coalescingKeepsInstallationAndIncrementsRevision() {
        val first = LocalSyncProtocol.next(null, null, state(), "installation")
        val second = LocalSyncProtocol.next(first, state(), state().copy(appearance = Appearance.DARK), "ignored")
        assertEquals("installation", second.installationId)
        assertEquals(2L, second.revision)
        assertEquals("installation:2", second.operationId)
        assertEquals("legacy-base64", VaultState.decode(second.payload)!!.documents.single().frontPhoto)
        assertEquals("Backup is not connected", second.backupStatus)
    }

    @Test fun deletesAccumulateButRecreatedIdsAreRemovedFromTombstones() {
        val original = state()
        val first = LocalSyncProtocol.next(null, null, original, "installation")
        val deleted = LocalSyncProtocol.next(first, original, VaultState(), "ignored")
        assertEquals(listOf("doc"), LocalSyncProtocol.tombstones(deleted).documents)
        assertEquals(listOf("accepted.jpg", "preview.jpg", "raw.jpg"), LocalSyncProtocol.tombstones(deleted).assets)
        val restored = LocalSyncProtocol.next(deleted, VaultState(), original, "ignored")
        assertTrue(LocalSyncProtocol.tombstones(restored).documents.isEmpty())
        assertTrue(LocalSyncProtocol.tombstones(restored).assets.isEmpty())
    }

    @Test fun sharedAssetsAreNeverMarkedDeleted() {
        val original = state()
        val shared = original.copy(documents = original.documents + original.documents.single().copy(id = "second"))
        val row = LocalSyncProtocol.next(null, shared, original, "installation")
        assertEquals(listOf("second"), LocalSyncProtocol.tombstones(row).documents)
        assertTrue(LocalSyncProtocol.tombstones(row).assets.isEmpty())
    }

    @Test fun retryHasBoundedDelayAndNewSnapshotClearsFailure() {
        val first = LocalSyncProtocol.next(null, null, state(), "installation")
        var failed = first
        repeat(100) { failed = LocalSyncProtocol.failed(failed, 10, "network") }
        assertEquals(30, failed.attemptCount)
        assertEquals(3_600_010L, failed.retryAfterEpochMillis)
        val next = LocalSyncProtocol.next(failed, state(), state(), "ignored")
        assertEquals(0, next.attemptCount)
        assertNull(next.failureCode)
    }

    @Test fun retryDoesNotPersistArbitrarySensitiveErrors() {
        val row = LocalSyncProtocol.next(null, null, state(), "installation")
        assertFailsWith<IllegalArgumentException> { LocalSyncProtocol.failed(row, 0, "secret token") }
        assertFailsWith<IllegalArgumentException> { LocalSyncProtocol.failed(row, -1, "network") }
    }

    @Test fun revisionOverflowFailsWithoutWraparound() {
        val row = LocalSyncProtocol.next(null, null, state(), "installation").copy(revision = Long.MAX_VALUE)
        assertFailsWith<IllegalStateException> { LocalSyncProtocol.next(row, state(), state(), "ignored") }
    }

    @Test fun malformedPersistedRetryMetadataIsRejected() {
        val row = LocalSyncProtocol.next(null, null, state(), "installation")
        assertFailsWith<IllegalArgumentException> { row.copy(attemptCount = Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { row.copy(attemptCount = -1) }
        assertFailsWith<IllegalArgumentException> { row.copy(retryAfterEpochMillis = -1) }
    }
}
