package com.motovault.app.storage

import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultWriter
import com.motovault.app.storage.security.ProtectedDocumentStorage
import com.motovault.app.domain.vault.VaultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Bridges the legacy backup representation to the one coordinated writer. */
internal class CoordinatedBackupRestore(
    private val writer: VaultWriter<VaultState>,
    private val stageAsset: (String, ByteArray) -> Boolean = ProtectedDocumentStorage::stageRestore,
    private val discardAssets: (Set<String>) -> Unit = ProtectedDocumentStorage::discard
) : VaultBackupRestorer {
    override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) {
        writer.commitPrepared(expectedRevision, prepare = {
            val staged = BackupRestoreStaging.prepare(
                archive, passphrase,
                assetWriter = { id, bytes -> withContext(Dispatchers.IO) { stageAsset(id, bytes) } },
                discardAssets = { ids -> withContext(Dispatchers.IO) { discardAssets(ids) } }
            )
            PreparedVaultChange(staged.state, preservePreviousAssets = true) {
                withContext(Dispatchers.IO) { discardAssets(staged.createdAssets) }
            }
        }, completed = completed)
    }
}
