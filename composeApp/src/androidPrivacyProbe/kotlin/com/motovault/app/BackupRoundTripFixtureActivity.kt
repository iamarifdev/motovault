package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.motovault.app.application.VaultCoordinator
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.RecoverySummary
import com.motovault.app.storage.CoordinatedBackupRestore
import com.motovault.app.storage.BackupOperationsAdapter
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.storage.security.AndroidAssetCipher
import com.motovault.app.storage.security.AndroidAssetKey
import com.motovault.app.storage.security.RecoverySnapshotCipher
import com.motovault.app.storage.security.AndroidAssetFiles
import com.motovault.app.storage.security.AssetEnvelope
import com.motovault.app.storage.security.SecureAssetStore
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.*
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/** Disposable vaults and encrypted files for exercising the real screen and restore writer. */
class BackupRoundTripFixtureActivity : ComponentActivity() {
    private val id = UUID.randomUUID().toString()
    private val alias = "motovault-synthetic-backup-$id"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: File
    private lateinit var database: VaultDatabase
    private lateinit var writer: VaultCoordinator<VaultState>
    private lateinit var archiveStore: SecureAssetStore
    private lateinit var destinationStore: SecureAssetStore
    @Volatile private lateinit var activeStore: SecureAssetStore
    private val originalId = "synthetic-original.jpg"
    private val previewId = "synthetic-preview.jpg"
    private val archiveOriginal = "synthetic archive original".encodeToByteArray()
    private val archivePreview = "synthetic archive preview".encodeToByteArray()
    private val replacedOriginal = "synthetic current original".encodeToByteArray()
    private fun store(name: String) = SecureAssetStore(
        AndroidAssetFiles(File(root, "$name/original"), File(root, "$name/protected")),
        AssetEnvelope(AndroidAssetCipher { AndroidAssetKey.load(false, alias) })
    )
    private fun openDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(this, File(root, "vault.db").absolutePath))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        root = File(cacheDir.canonicalFile, "synthetic-backup-roundtrip-$id").apply { check(mkdirs()) }
        AndroidAssetKey.load(true, alias)
        archiveStore = store("archive")
        destinationStore = store("destination")
        archiveStore.writeNew(originalId, archiveOriginal)
        archiveStore.writeNew(previewId, archivePreview)
        destinationStore.writeNew(originalId, replacedOriginal)
        destinationStore.writeNew(previewId, archivePreview)
        activeStore = archiveStore
        database = openDatabase()
        val cipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, alias) })
        val snapshots = RecoverySnapshotStore(database.vault(), { "synthetic-${UUID.randomUUID()}.pdf" }, cipher::seal, cipher::open)
        val document = VaultDocument("Synthetic document", "TEST", "Synthetic", "2030-01-01", id = "synthetic-document",
            pages = listOf(DocumentPage(originalId, previewId, "image/jpeg", 10, 10, "Front")))
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Archive Bike"),
            documents = listOf(document), language = AppLanguage.EN)
        runBlocking { database.vault().replace(original) }
        writer = VaultCoordinator(
            scope = scope, initialState = original, restore = { requireNotNull(database.vault().read()) },
            write = { database.vault().replace(it) }, assetIds = { state ->
                state.documents.flatMap { doc -> doc.pages.flatMap { listOfNotNull(it.originalId, it.previewId, it.acceptedId) } + listOfNotNull(doc.originalPdf) }.toSet()
            }, cleanup = {}, recover = {}, reconcile = {},
            writePrepared = snapshots::replace, recoveryAssetIds = snapshots::referencedAssets
        )
        val recovery = object : VaultRecovery {
            override suspend fun list(): List<RecoverySummary> = emptyList()
            override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) =
                completed(Result.failure(IllegalStateException("Not part of this fixture")))
        }
        vaultContext = applicationContext
        LaunchConfig.applyArgs(listOf("--tab", "me", "--sheet", "backup"))
        val restorer = CoordinatedBackupRestore(writer, stageAsset = { assetId, bytes -> activeStore.stageRestore(assetId, bytes) },
            discardAssets = { ids -> ids.forEach { assetId ->
                listOf("$assetId.mva", "$assetId.committed").forEach { name ->
                    val file = File(root, "destination/protected/$name")
                    check(!file.exists() || file.delete())
                }
            } })
        val operations = BackupOperationsAdapter(::documentSha256) { assetId -> activeStore.read(assetId) }
        setContent { MotoVault(writer, restorer, FuelEntryAdapter, FuelEntryAdapter, recovery, operations) }
    }

    fun replaceSyntheticCurrent(completed: (Boolean) -> Unit) {
        val current = writer.snapshots.value
        val changed = current.state.copy(ride = Ride("test", "Synthetic", 2021, 2000.0, modelName = "Current Bike"))
        activeStore = destinationStore
        writer.commit(changed, current.revision) { saved ->
            if (!saved) activeStore = archiveStore
            completed(saved)
        }
    }

    fun isCurrentBike() = writer.snapshots.value.state.ride?.modelName == "Current Bike"

    fun verifyReopened(): Boolean = runBlocking {
        val reopened = openDatabase()
        try {
            val row = reopened.vault().restoreRecoveryRows().single()
            val cipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, alias) })
            val previous = VaultState.decode(cipher.open(row.id, row.payload).decodeToString())
            val restored = reopened.vault().read()
            val restoredPage = restored?.documents?.singleOrNull()?.pages?.singleOrNull()
            restored?.ride?.modelName == "Archive Bike" && previous?.ride?.modelName == "Current Bike" &&
                previous.documents.single().pages.single().originalId == originalId &&
                restoredPage != null && restoredPage.originalId != originalId && restoredPage.previewId == previewId &&
                destinationStore.read(originalId).contentEquals(replacedOriginal) &&
                destinationStore.read(restoredPage.originalId).contentEquals(archiveOriginal) &&
                destinationStore.read(previewId).contentEquals(archivePreview) &&
                archiveStore.read(originalId).contentEquals(archiveOriginal)
        } finally { reopened.close() }
    }

    override fun onDestroy() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        if (::root.isInitialized) root.deleteRecursively()
        super.onDestroy()
    }
}
