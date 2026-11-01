package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultCoordinator
import com.motovault.app.storage.CoordinatedRecovery
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.storage.security.AndroidAssetCipher
import com.motovault.app.storage.security.AndroidAssetKey
import com.motovault.app.storage.security.RecoverySnapshotCipher
import com.motovault.app.ui.*
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/** A private synthetic database and key, used only by the isolated probe. */
class RecoveryDurableFixtureActivity : ComponentActivity() {
    private val id = UUID.randomUUID().toString()
    private val alias = "motovault-synthetic-recovery-$id"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: File
    private lateinit var database: VaultDatabase
    private var sequence = 0
    private fun openDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(this, File(root, "vault.db").absolutePath))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        root = File(cacheDir, "synthetic-recovery-$id").apply { check(mkdirs()) }
        AndroidAssetKey.load(true, alias)
        database = openDatabase()
        val cipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, alias) })
        val snapshots = RecoverySnapshotStore(database.vault(), { "synthetic-${sequence++}.pdf" }, cipher::seal, cipher::open)
        val previous = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Previous Bike"), language = AppLanguage.EN)
        val current = previous.copy(ride = Ride("test", "Synthetic", 2021, 2000.0, modelName = "Current Bike"))
        runBlocking {
            database.vault().replace(previous)
            snapshots.replace(requireNotNull(database.vault().read()), current)
        }
        vaultContext = applicationContext
        LaunchConfig.applyArgs(listOf("--tab", "me", "--sheet", "recovery"))
        val writer = VaultCoordinator(
            scope = scope, initialState = current, restore = { requireNotNull(database.vault().read()) },
            write = { database.vault().replace(it) }, assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {},
            writePrepared = snapshots::replace, recoveryAssetIds = snapshots::referencedAssets
        )
        val backup = object : VaultBackupRestorer {
            override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        val recovery = CoordinatedRecovery(writer, snapshots) { error("No synthetic assets expected") }
        setContent { MotoVault(writer, backup, FuelEntryAdapter, FuelEntryAdapter, recovery, syntheticBackupOperations) }
    }

    fun verifyReopened(): Boolean = runBlocking {
        database.close()
        database = openDatabase()
        val cipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, alias) })
        val rows = database.vault().restoreRecoveryRows()
        val names = rows.map { row -> VaultState.decode(cipher.open(row.id, row.payload).decodeToString())?.ride?.modelName }.toSet()
        database.vault().read()?.ride?.modelName == "Previous Bike" && rows.size == 2 && names == setOf("Previous Bike", "Current Bike")
    }

    override fun onDestroy() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        if (::root.isInitialized) root.deleteRecursively()
        super.onDestroy()
    }
}
