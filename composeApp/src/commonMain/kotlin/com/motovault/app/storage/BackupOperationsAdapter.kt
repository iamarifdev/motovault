package com.motovault.app.storage

import com.motovault.app.application.VaultBackupOperations
import com.motovault.app.domain.backup.BackupPreview
import com.motovault.app.domain.backup.BackupProgress
import com.motovault.app.domain.backup.BackupUseCase
import com.motovault.app.domain.backup.RestoreUseCase
import com.motovault.app.domain.vault.VaultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext

/** Connects backup workflows to the legacy vault representation and injected native services. */
internal class BackupOperationsAdapter(
    private val digest: (ByteArray) -> ByteArray,
    private val readAsset: suspend (String) -> ByteArray
) : VaultBackupOperations<VaultState> {
    override suspend fun create(state: VaultState, revision: Long, password: String, progress: suspend (Float, BackupProgress) -> Unit): ByteArray {
        val callerContext = currentCoroutineContext()
        return withContext(Dispatchers.Default) {
            BackupUseCase.createBackup(digest, state.backupSnapshot(), revision, password,
                assetReader = { id -> withContext(Dispatchers.IO) { readAsset(id) } },
                progress = { fraction, stage -> withContext(callerContext) { progress(fraction, stage) } })
        }
    }

    override suspend fun preview(state: VaultState, archive: ByteArray, password: String): BackupPreview =
        withContext(Dispatchers.Default) { RestoreUseCase.validateAndPreview(digest, ::backupRideDisplayName, archive, password, state.backupCurrentSummary()) }
}
