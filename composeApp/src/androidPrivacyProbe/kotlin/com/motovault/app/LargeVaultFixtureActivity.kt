package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.motovault.app.application.RecoverySummary
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultCoordinator
import com.motovault.app.application.VaultRecovery
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.LaunchConfig
import com.motovault.app.ui.MotoVault
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.seedData
import com.motovault.app.ui.today
import com.motovault.app.ui.vaultContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import java.io.File
import java.util.UUID

/** Synthetic large vault for P08-02 performance measurement; no owner data and no production records. */
class LargeVaultFixtureActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: File
    private lateinit var database: VaultDatabase
    private lateinit var writer: VaultCoordinator<VaultState>

    private fun openDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(this, File(root, "vault.db").absolutePath))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        vaultContext = applicationContext
        LaunchConfig.applyArgs(listOf("--tab", "history"))
        val language = if (intent.getStringExtra("language") == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
        val digits = if (intent.getStringExtra("digits") == "en") Digits.EN else Digits.BN
        val seed = VaultState.seedData(today())
        val d = { n: Int -> LocalDate.fromEpochDays(today().toEpochDays() + n).toString() }
        val baseFuel = seed.logs.first { it.isFuel }
        val bigLogs = List(1200) { i ->
            baseFuel.copy(
                id = "big-f$i",
                date = d(-(i / 3)),
                odometer = (12340.0 - i * 4).coerceAtLeast(1000.0),
                cost = 500.0 + (i % 37) * 10.0,
                liters = 4.0 + (i % 9) * 0.3
            )
        }
        val baseDoc = seed.documents.first()
        val bigDocs = List(200) { i ->
            baseDoc.copy(id = "big-d$i", title = "Synthetic document $i", expiry = d(30 + i), reminder = false)
        }
        val initial = seed.copy(
            language = language,
            digits = digits,
            logs = seed.logs + bigLogs,
            documents = seed.documents + bigDocs
        )
        root = File(cacheDir, "synthetic-large-${UUID.randomUUID()}").apply { check(mkdirs()) }
        database = openDatabase()
        runBlocking { database.vault().replace(initial) }
        writer = VaultCoordinator(
            scope = scope,
            initialState = initial,
            restore = { requireNotNull(database.vault().read()) },
            write = { database.vault().replace(it) },
            assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {}
        )
        val restorer = object : VaultBackupRestorer {
            override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) =
                completed(Result.failure(IllegalStateException()))
        }
        val recovery = object : VaultRecovery {
            override suspend fun list(): List<RecoverySummary> = emptyList()
            override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) =
                completed(Result.failure(IllegalStateException()))
        }
        setContent {
            MotoVault(writer, restorer, FuelEntryAdapter, FuelEntryAdapter, recovery, syntheticBackupOperations, AppComposition.captureDrafts)
        }
    }
}
