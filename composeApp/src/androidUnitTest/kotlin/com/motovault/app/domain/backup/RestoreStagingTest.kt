package com.motovault.app.domain.backup

import com.motovault.app.storage.backupSnapshot

import com.motovault.app.storage.security.documentSha256
import com.motovault.app.storage.security.RestoreAssetCollision

import com.motovault.app.ui.DocumentPage
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.VaultState
import com.motovault.app.storage.BackupRestoreStaging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RestoreStagingTest {
    private val first = "synthetic first original".encodeToByteArray()
    private val second = "synthetic second original".encodeToByteArray()
    private val password = "synthetic test password"
    private val ownerState = VaultState(documents = listOf(VaultDocument(
        title = "Synthetic", number = "test", holder = "test", expiry = "",
        pages = listOf(DocumentPage("a.jpg", "b.jpg", "image/jpeg", 10, 10, "Front"))
    )))

    private suspend fun archive() = BackupUseCase.createBackup(::documentSha256, 
        snapshot = ownerState.backupSnapshot(), vaultRevision = 0, passphrase = password,
        assetReader = { if (it == "a.jpg") first else second }
    )

    @Test
    fun existingIdenticalAssetIsReusedAndOnlyNewAssetIsOwnedByRestore() = runBlocking {
        val files = mutableMapOf("a.jpg" to first.copyOf())
        val staged = BackupRestoreStaging.prepare(
            archive(), password,
            assetWriter = { id, bytes ->
                val old = files[id]
                if (old != null) { check(old.contentEquals(bytes)); false }
                else { files[id] = bytes.copyOf(); true }
            },
            discardAssets = { ids -> ids.forEach(files::remove) }
        )
        assertEquals(setOf("b.jpg"), staged.createdAssets)
        assertEquals(ownerState.documents, staged.state.documents)
        assertContentEquals(first, files.getValue("a.jpg"))
        assertContentEquals(second, files.getValue("b.jpg"))
    }

    @Test
    fun conflictAfterFirstStagedAssetRollsBackOnlyNewAsset() = runBlocking {
        val ownerBytes = "existing different bytes".encodeToByteArray()
        val files = mutableMapOf("b.jpg" to ownerBytes.copyOf())
        val removed = mutableSetOf<String>()
        assertFailsWith<IllegalStateException> {
            BackupRestoreStaging.prepare(
                archive(), password,
                assetWriter = { id, bytes ->
                    val old = files[id]
                    if (old != null) { check(old.contentEquals(bytes)); false }
                    else { files[id] = bytes.copyOf(); true }
                },
                discardAssets = { ids -> removed += ids; ids.forEach(files::remove) }
            )
        }
        assertEquals(setOf("a.jpg"), removed)
        assertEquals(setOf("b.jpg"), files.keys)
        assertContentEquals(ownerBytes, files.getValue("b.jpg"))
        assertTrue(ownerState.documents.isNotEmpty())
    }

    @Test
    fun cancellationAfterStagingCleansOnlyRestoreOwnedFiles() = runBlocking {
        val retained = "existing owner data".encodeToByteArray()
        val files = mutableMapOf("owner.jpg" to retained.copyOf())
        val removed = mutableSetOf<String>()
        val cancelled = CancellationException("synthetic cancellation")
        val thrown = assertFailsWith<CancellationException> {
            BackupRestoreStaging.prepare(
                archive(), password,
                assetWriter = { id, bytes ->
                    if (id == "b.jpg") throw cancelled
                    files[id] = bytes.copyOf()
                    true
                },
                discardAssets = { ids -> removed += ids; ids.forEach(files::remove) }
            )
        }
        assertEquals(cancelled.message, thrown.message)
        assertEquals(setOf("a.jpg"), removed)
        assertEquals(setOf("owner.jpg"), files.keys)
        assertContentEquals(retained, files.getValue("owner.jpg"))
    }

    @Test
    fun differentExistingAssetIsRemappedWithoutReplacingOriginal() = runBlocking {
        val retained = "existing different bytes".encodeToByteArray()
        val files = mutableMapOf("a.jpg" to retained.copyOf())
        val staged = BackupRestoreStaging.prepare(
            archive(), password,
            assetWriter = { id, bytes ->
                val existing = files[id]
                if (existing != null && !existing.contentEquals(bytes)) throw RestoreAssetCollision()
                if (existing != null) false else { files[id] = bytes.copyOf(); true }
            },
            discardAssets = { ids -> ids.forEach(files::remove) },
            newAssetId = { "new-a.jpg" }
        )
        assertContentEquals(retained, files.getValue("a.jpg"))
        assertContentEquals(first, files.getValue("new-a.jpg"))
        assertContentEquals(second, files.getValue("b.jpg"))
        assertEquals(setOf("new-a.jpg", "b.jpg"), staged.createdAssets)
        assertEquals("new-a.jpg", staged.state.documents.single().pages.single().originalId)
        assertEquals("b.jpg", staged.state.documents.single().pages.single().previewId)
    }

    @Test
    fun collisionRemapsAcceptedAndPdfReferencesAcrossDocuments() = runBlocking {
        val document = ownerState.documents.single().copy(
            pages = listOf(DocumentPage("a.jpg", "b.jpg", "image/jpeg", 10, 10, "Front", acceptedId = "accepted.jpg")),
            originalPdf = "original.pdf"
        )
        val state = VaultState(documents = listOf(document, document.copy(id = "second")))
        val archive = BackupUseCase.createBackup(::documentSha256, state.backupSnapshot(), 0, password, assetReader = { it.encodeToByteArray() })
        val files = mutableMapOf("accepted.jpg" to "owner accepted".encodeToByteArray(), "original.pdf" to "owner pdf".encodeToByteArray())
        val staged = BackupRestoreStaging.prepare(
            archive, password,
            assetWriter = { id, bytes ->
                val old = files[id]
                if (old != null && !old.contentEquals(bytes)) throw RestoreAssetCollision()
                if (old != null) false else { files[id] = bytes.copyOf(); true }
            },
            discardAssets = { ids -> ids.forEach(files::remove) },
            newAssetId = { id -> "replacement-$id" }
        )
        staged.state.documents.forEach {
            assertEquals("replacement-accepted.jpg", it.pages.single().acceptedId)
            assertEquals("replacement-original.pdf", it.originalPdf)
        }
        assertContentEquals("owner accepted".encodeToByteArray(), files.getValue("accepted.jpg"))
        assertContentEquals("owner pdf".encodeToByteArray(), files.getValue("original.pdf"))
        assertContentEquals("accepted.jpg".encodeToByteArray(), files.getValue("replacement-accepted.jpg"))
        assertContentEquals("original.pdf".encodeToByteArray(), files.getValue("replacement-original.pdf"))
    }

    @Test
    fun exhaustingReplacementIdsCleansPreviouslyStagedFiles() = runBlocking {
        val original = "owner bytes".encodeToByteArray()
        val files = mutableMapOf("b.jpg" to original.copyOf())
        val removed = mutableSetOf<String>()
        assertFailsWith<IllegalStateException> {
            BackupRestoreStaging.prepare(
                archive(), password,
                assetWriter = { id, bytes ->
                    if (id == "b.jpg") throw RestoreAssetCollision()
                    files[id] = bytes.copyOf(); true
                },
                discardAssets = { ids -> removed += ids; ids.forEach(files::remove) },
                newAssetId = { "b.jpg" }
            )
        }
        assertEquals(setOf("a.jpg"), removed)
        assertEquals(setOf("b.jpg"), files.keys)
        assertContentEquals(original, files.getValue("b.jpg"))
    }
}
