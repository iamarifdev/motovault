package com.motovault.app.ui

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

private fun path(): String = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/motovault.json"

@OptIn(ExperimentalForeignApi::class)
private fun legacyVault(): String? {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path())) return NSUserDefaults.standardUserDefaults.stringForKey("motovault")
    val data = requireNotNull(NSData.dataWithContentsOfFile(path())) { "Could not read stored records." }
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes.decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val directory = path().substringBeforeLast('/')
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)) { "Could not create local storage." }
    return "$directory/motovault.db"
}
private val database by lazy { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = databasePath())) }
actual suspend fun loadVault(): String? = withContext(Dispatchers.IO) {
    val state = database.vault().read() ?: database.vault().migrate(legacyVault())
    database.vault().prepareLocalSync()
    VaultState.encode(state)
}
actual suspend fun saveVault(value: String) = withContext(Dispatchers.IO) {
    database.vault().replace(requireNotNull(VaultState.decode(value)) { "Could not read records to save." })
}
