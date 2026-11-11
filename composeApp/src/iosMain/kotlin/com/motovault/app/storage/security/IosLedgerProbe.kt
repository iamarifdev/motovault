package com.motovault.app.storage.security

import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import kotlinx.coroutines.runBlocking
import platform.Foundation.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
fun runIosLedgerProbe(): String {
    if (!Platform.isDebugBinary || NSBundle.mainBundle.bundleIdentifier != "com.motovault.app.privacyprobe") return "REFUSED"
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val root = "$base/synthetic-ledger-${NSUUID().UUIDString}"
    val manager = NSFileManager.defaultManager
    manager.createDirectoryAtPath(root, true, null, null)
    return try {
        val result = runBlocking { verifyLedgerStorageProbe { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = "$root/ledger.db")) } }
        "PASS P06 iOS: $result"
    } catch (failure: Throwable) { "FAIL P06 iOS: ${failure.stackTraceToString()}" }
    finally { manager.removeItemAtPath(root, null) }
}
