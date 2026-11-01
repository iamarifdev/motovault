package com.motovault.app.storage

import com.motovault.app.domain.backup.BackupSecurityException
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.Ride
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.DocumentPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupOperationsAdapterTest {
    @kotlin.test.BeforeTest
    fun initializeCrypto() = com.motovault.app.domain.backup.initializeBackupTestCrypto()
    @Test
    fun createsAndPreviewsThroughInjectedBoundary() = runBlocking {
        val adapter = BackupOperationsAdapter(::documentSha256) { error("No assets expected") }
        val state = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Test bike"))
        var progressSeen = false
        val archive = adapter.create(state, 7, "valid password") { _, _ -> progressSeen = true }
        val preview = adapter.preview(state, archive, "valid password")
        assertTrue(progressSeen)
        assertEquals(7L, preview.manifest.vaultRevision)
        assertEquals("Test bike", preview.verifiedSummary.bikeDisplayName)
        assertFailsWith<BackupSecurityException> { adapter.preview(state, archive, "wrong password") }
        Unit
    }

    @Test
    fun readsReferencedAssetsAndPropagatesCancellation() = runBlocking {
        val page = DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 10, 10, "Front")
        val document = VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "doc", pages = listOf(page))
        val state = VaultState(documents = listOf(document))
        val reads = mutableSetOf<String>()
        val adapter = BackupOperationsAdapter(::documentSha256) { id -> reads += id; id.encodeToByteArray() }
        val archive = adapter.create(state, 0, "valid password") { _, _ -> }
        assertEquals(setOf("original.jpg", "preview.jpg"), reads)
        assertEquals(2, adapter.preview(state, archive, "valid password").validatedAssetCount)
        val cancelled = BackupOperationsAdapter(::documentSha256) { throw CancellationException("synthetic cancellation") }
        assertFailsWith<CancellationException> { cancelled.create(state, 0, "valid password") { _, _ -> } }
        Unit
    }
}
