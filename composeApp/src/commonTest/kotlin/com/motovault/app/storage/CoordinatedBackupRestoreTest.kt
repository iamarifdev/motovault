package com.motovault.app.storage

import com.motovault.app.application.VaultCoordinator
import com.motovault.app.domain.backup.initializeBackupTestCrypto
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.DocumentPage
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.VaultState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinatedBackupRestoreTest {
    @BeforeTest
    fun initializeCrypto() = initializeBackupTestCrypto()

    @Test
    fun failedCommitDiscardsOnlyNewlyStagedAssets() = runBlocking {
        val document = VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "doc",
            pages = listOf(DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 10, 10, "Front")))
        val archived = VaultState(documents = listOf(document))
        val files = mutableMapOf("preview.jpg" to "preview".encodeToByteArray())
        val source = mapOf("original.jpg" to "original".encodeToByteArray(), "preview.jpg" to files.getValue("preview.jpg"))
        val archive = BackupOperationsAdapter(::documentSha256) { source.getValue(it) }
            .create(archived, 0, "valid password") { _, _ -> }
        val initial = VaultState()
        val writer = VaultCoordinator(
            scope = this, initialState = initial, restore = { initial }, write = {},
            assetIds = { _: VaultState -> emptySet() }, cleanup = {}, recover = {}, reconcile = {},
            writePrepared = { _, _ -> error("synthetic commit failure") }
        )
        val removed = mutableSetOf<String>()
        val restorer = CoordinatedBackupRestore(writer,
            stageAsset = { id, bytes ->
                val existing = files[id]
                if (existing == null) { files[id] = bytes; true }
                else { check(existing.contentEquals(bytes)); false }
            },
            discardAssets = { ids -> removed.addAll(ids); ids.forEach { files.remove(it) } }
        )
        val result = CompletableDeferred<Result<Unit>>()
        restorer.restore(archive, "valid password", 0) { result.complete(it) }
        assertTrue(result.await().isFailure)
        assertEquals(initial, writer.snapshots.value.state)
        assertEquals(0L, writer.snapshots.value.revision)
        assertEquals(setOf("original.jpg"), removed)
        assertEquals(setOf("preview.jpg"), files.keys)
        assertTrue(files.getValue("preview.jpg").contentEquals(source.getValue("preview.jpg")))
    }
}
