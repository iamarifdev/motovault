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

    @Test
    fun multiBikeBackupIncludesNonActiveGarageAssetsAndArchivedBikes() = runBlocking {
        val page1 = DocumentPage("bike1-orig.jpg", "bike1-prev.jpg", "image/jpeg", 10, 10, "Front")
        val bike1Doc = VaultDocument("Tax token", "TT-1", "Owner", "2028-01-01", id = "doc-b1", kind = "tax", pages = listOf(page1))
        val bike1 = VaultState(
            ride = Ride("yamaha", "Std", 2023, 5000.0, id = "primary", archived = true, modelName = "R15"),
            documents = listOf(bike1Doc)
        )
        val garage = com.motovault.app.domain.model.GarageBikeRecord(
            ride = bike1.ride!!,
            rules = bike1.rules,
            logs = bike1.logs,
            documents = bike1.documents
        )
        val ownerDoc = VaultDocument("Driving licence", "DL-1", "Owner", "2029-01-01", id = "doc-owner", kind = "licence")
        val multiState = VaultState(
            ride = Ride("honda", "Std", 2024, 300.0, id = "bike-2", modelName = "CBR"),
            documents = listOf(ownerDoc),
            garageBikes = listOf(garage)
        )
        val reads = mutableSetOf<String>()
        val adapter = BackupOperationsAdapter(::documentSha256) { id -> reads += id; id.encodeToByteArray() }
        val archive = adapter.create(multiState, 2, "valid password") { _, _ -> }
        assertEquals(setOf("bike1-orig.jpg", "bike1-prev.jpg"), reads)
        val preview = adapter.preview(multiState, archive, "valid password")
        assertEquals(2, preview.validatedAssetCount)
        assertEquals(1, preview.payload.preferences.garageBikes.size)
        assertTrue(preview.payload.preferences.garageBikes.single().ride.archived)
    }
}
