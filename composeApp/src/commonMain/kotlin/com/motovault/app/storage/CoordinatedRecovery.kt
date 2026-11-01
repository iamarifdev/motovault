package com.motovault.app.storage

import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.RecoverySummary
import com.motovault.app.application.VaultWriter
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.assetIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Reinstates a snapshot only when every referenced original is readable. */
internal class CoordinatedRecovery(
    private val writer: VaultWriter<VaultState>,
    private val snapshots: RecoverySnapshotStore,
    private val readAsset: suspend (String) -> ByteArray
) : VaultRecovery {
    override suspend fun list(): List<RecoverySummary> = withContext(Dispatchers.IO) { snapshots.summaries() }
    override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) {
        writer.commitPrepared(expectedRevision, prepare = {
            val state = withContext(Dispatchers.IO) {
                snapshots.read(recoveryId).also { snapshot ->
                    snapshot.documents.flatMap { it.assetIds() }.toSet().forEach { id ->
                        check(readAsset(id).isNotEmpty()) { "A recovery original is unavailable." }
                    }
                }
            }
            PreparedVaultChange(state, preservePreviousAssets = true, discard = {})
        }, completed = completed)
    }
}
