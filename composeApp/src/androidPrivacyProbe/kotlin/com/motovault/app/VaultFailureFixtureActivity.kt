package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultError
import com.motovault.app.application.VaultSnapshot
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.RecoverySummary
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.MotoVault
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.vaultContext
import com.motovault.app.storage.FuelEntryAdapter
import kotlinx.coroutines.flow.MutableStateFlow

/** Only the separate probe package can display this synthetic failure state. */
class VaultFailureFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        vaultContext = applicationContext
        val language = if (intent.getStringExtra("language") == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
        val writer = object : VaultWriter<VaultState> {
            override val snapshots = MutableStateFlow(VaultSnapshot(VaultState(language = language), loading = false, restorationFailed = true, error = VaultError.RESTORE_FAILED))
            override fun commit(next: VaultState, expectedRevision: Long, completed: (Boolean) -> Unit) = completed(false)
            override fun undo(expectedRevision: Long, completed: (Boolean) -> Unit) = completed(false)
            override fun commitPrepared(expectedRevision: Long, prepare: suspend () -> PreparedVaultChange<VaultState>, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        val restorer = object : VaultBackupRestorer {
            override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        val recovery = object : VaultRecovery {
            override suspend fun list(): List<RecoverySummary> = emptyList()
            override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        setContent { MotoVault(writer, restorer, FuelEntryAdapter, FuelEntryAdapter, recovery, syntheticBackupOperations) }
    }
}
