package com.motovault.app.ui

import android.content.Context
import android.util.AtomicFile
import java.io.File
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

lateinit var vaultContext: Context
private fun file() = AtomicFile(File(vaultContext.filesDir, "motovault.json"))
private fun legacyVault(): String? {
    val legacy = file()
    // AtomicFile.openRead restores a committed backup left by an interrupted write.
    if (legacy.baseFile.exists() || File(legacy.baseFile.path + ".bak").exists()) {
        return legacy.readFully().decodeToString()
    }
    return vaultContext.getSharedPreferences("motovault", Context.MODE_PRIVATE).getString("vault", null)
}
private val database by lazy { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(vaultContext.applicationContext, "motovault.db")) }
actual fun activeVaultDatabase(): VaultDatabase = database
actual suspend fun loadVault(): String? = withContext(Dispatchers.IO) {
    val state = database.vault().read() ?: database.vault().migrate(legacyVault())
    database.vault().prepareLocalSync()
    VaultState.encode(state)
}
actual suspend fun saveVault(value: String) = withContext(Dispatchers.IO) {
    database.vault().replace(requireNotNull(VaultState.decode(value)) { "Could not read records to save." })
}
