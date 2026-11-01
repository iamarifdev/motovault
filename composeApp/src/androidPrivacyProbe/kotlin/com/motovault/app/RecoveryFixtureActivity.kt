package com.motovault.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.motovault.app.application.RecoverySummary
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultCoordinator
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.VaultWriteConflict
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.LaunchConfig
import com.motovault.app.ui.MotoVault
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.today
import com.motovault.app.ui.vaultContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val SYNTHETIC_DELAY_MILLIS = 10000L

/** In-memory UI behavior only, restricted to the isolated probe variant. */
class RecoveryFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        vaultContext = applicationContext
        val sheet = if (intent.getStringExtra("sheet") == "backup") "backup" else "recovery"
        LaunchConfig.applyArgs(listOf("--tab", "me", "--sheet", sheet))
        val language = if (intent.getStringExtra("language") == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
        val conflict = intent.getBooleanExtra("conflict", false)
        val initial = VaultState.seedData(today()).copy(language = language, documents = emptyList())
        val writer = VaultCoordinator(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), initialState = initial,
            restore = { initial }, write = {}, assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {}
        )
        val backup = object : VaultBackupRestorer {
            override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        val recovery = object : VaultRecovery {
            override suspend fun list() = listOf(RecoverySummary("synthetic", null, "Demo Bike", 8, 3))
            override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) {
                check(recoveryId == "synthetic" && expectedRevision == 0L)
                Handler(Looper.getMainLooper()).postDelayed({
                    completed(if (conflict) Result.failure(VaultWriteConflict()) else Result.success(Unit))
                }, SYNTHETIC_DELAY_MILLIS)
            }
        }
        setContent { MotoVault(writer, backup, FuelEntryAdapter, FuelEntryAdapter, recovery, syntheticBackupOperations) }
    }
}
