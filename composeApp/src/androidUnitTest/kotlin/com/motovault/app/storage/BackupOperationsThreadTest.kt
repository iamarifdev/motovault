package com.motovault.app.storage

import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.DocumentPage
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.VaultState
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupOperationsThreadTest {
    @Test
    fun archiveWorkRunsOffCallerAndProgressReturnsToCaller() = runBlocking {
        val caller = Thread.currentThread().id
        val workerThreads = mutableSetOf<Long>()
        val callbackThreads = mutableSetOf<Long>()
        val page = DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 10, 10, "Front")
        val document = VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "doc", pages = listOf(page))
        val adapter = BackupOperationsAdapter(
            digest = { bytes -> workerThreads += Thread.currentThread().id; documentSha256(bytes) },
            readAsset = { id -> id.encodeToByteArray() }
        )
        adapter.create(VaultState(documents = listOf(document)), 0, "valid password") { _, _ ->
            callbackThreads += Thread.currentThread().id
        }
        assertTrue(workerThreads.isNotEmpty())
        workerThreads.forEach { assertNotEquals(caller, it) }
        assertTrue(callbackThreads.isNotEmpty())
        assertTrue(callbackThreads.all { it == caller })
    }
}
