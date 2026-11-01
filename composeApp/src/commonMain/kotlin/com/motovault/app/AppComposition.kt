package com.motovault.app

import com.motovault.app.application.VaultWriter
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.storage.CoordinatedBackupRestore
import com.motovault.app.storage.VaultSession
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.BackupOperationsAdapter
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.CoordinatedRecovery
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.reconcileRenewalReminders
import com.motovault.app.ui.recoverOrphanDocumentAssets
import com.motovault.app.ui.removeDocumentAssets
import com.motovault.app.ui.saveVault
import com.motovault.app.ui.activeVaultDatabase
import com.motovault.app.ui.recordId
import com.motovault.app.ui.restore
import com.motovault.app.storage.security.ProtectedDocumentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** App lifetime owner: all UI consumers share the same coordinated writer. */
internal object AppComposition {
    private const val RECOVERY_ID_EXTENSION = ".pdf"
    private val recoverySnapshots by lazy {
        RecoverySnapshotStore(
            dao = activeVaultDatabase().vault(),
            newId = { recordId() + RECOVERY_ID_EXTENSION },
            seal = ProtectedDocumentStorage::sealRecoverySnapshot,
            open = ProtectedDocumentStorage::openRecoverySnapshot
        )
    }
    val vault: VaultWriter<VaultState> by lazy {
        VaultSession(
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            restore = { VaultState.restore() },
            write = { saveVault(VaultState.encode(it)) },
            cleanup = ::removeDocumentAssets,
            recover = ::recoverOrphanDocumentAssets,
            reconcile = ::reconcileRenewalReminders,
            writePrepared = { previous, next -> withContext(Dispatchers.IO) {
                recoverySnapshots.replace(previous, next)
            } },
            recoveryAssetIds = { withContext(Dispatchers.IO) {
                recoverySnapshots.referencedAssets()
            } }
        )
    }
    val backupRestore: VaultBackupRestorer by lazy { CoordinatedBackupRestore(vault) }
    val backupOperations = BackupOperationsAdapter(::documentSha256) { id -> ProtectedDocumentStorage.read(id) }
    val recoveryRestore by lazy { CoordinatedRecovery(vault, recoverySnapshots) { id -> ProtectedDocumentStorage.read(id) } }
    val fuelEntry = FuelEntryAdapter
}
