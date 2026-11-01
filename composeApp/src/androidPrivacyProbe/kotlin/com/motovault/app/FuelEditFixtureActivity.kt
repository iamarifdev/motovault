package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultCoordinator
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.RecoverySummary
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.LaunchConfig
import com.motovault.app.ui.MotoVault
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.today
import com.motovault.app.ui.vaultContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID

/** Synthetic Room history, isolated from the primary app and owner records. */
class FuelEditFixtureActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var root: File
    private lateinit var database: VaultDatabase
    private lateinit var fuelId: String
    private fun openDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(this, File(root, "vault.db").absolutePath))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        vaultContext = applicationContext
        val failWrites = intent.getBooleanExtra("failWrites", false)
        LaunchConfig.applyArgs(if (failWrites) listOf("--tab", "history", "--sheet", "fuel") else listOf("--tab", "history"))
        val language = if (intent.getStringExtra("language") == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
        val seed = VaultState.seedData(today())
        val digits = if (intent.getStringExtra("digits") == "en") Digits.EN else Digits.BN
        val initial = seed.copy(language = language, digits = digits, ride = if (failWrites) seed.ride?.copy(odometer = 12400.0) else seed.ride)
        fuelId = initial.logs.single { it.isFuel && it.fuelLiters == 4.9 }.id
        root = File(cacheDir, "synthetic-fuel-${UUID.randomUUID()}").apply { check(mkdirs()) }
        database = openDatabase()
        runBlocking { database.vault().replace(initial) }
        val writer = VaultCoordinator(
            scope = scope, initialState = initial,
            restore = { requireNotNull(database.vault().read()) }, write = {
                if (failWrites) error("Synthetic write failure")
                database.vault().replace(it)
            },
            assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {}
        )
        val restorer = object : VaultBackupRestorer {
            override fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        val recovery = object : VaultRecovery {
            override suspend fun list(): List<RecoverySummary> = emptyList()
            override fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit) = completed(Result.failure(IllegalStateException()))
        }
        setContent { MotoVault(writer, restorer, FuelEntryAdapter, FuelEntryAdapter, recovery, syntheticBackupOperations) }
    }

    /** Uses a fresh database connection after the coordinator has reported success. */
    fun savedFuelMatches(cost: Double, litres: Double): Boolean = runBlocking {
        val reopened = openDatabase()
        try {
            val entry = reopened.vault().read()?.logs?.singleOrNull { it.id == fuelId }
            entry?.cost == cost && entry.fuelLiters == litres
        } finally {
            reopened.close()
        }
    }

    fun savedFuelCount(): Int = runBlocking { requireNotNull(database.vault().read()).logs.count { it.isFuel } }

    override fun onDestroy() {
        scope.cancel()
        if (::database.isInitialized) database.close()
        if (::root.isInitialized) root.deleteRecursively()
        super.onDestroy()
    }
}
