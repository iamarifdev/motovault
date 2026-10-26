@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.storage.security

import platform.Foundation.*
import com.motovault.app.ui.validateDocumentAssetId

internal actual object ProtectedDocumentStorage {
    private val lock = NSRecursiveLock()
    private val root get() = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    private fun store() = SecureAssetStore(IosAssetFiles("$root/documents", "$root/documents-protected"), AssetEnvelope(IosAssetCipher()))
    private fun recoveryStore() = SecureAssetStore(IosAssetFiles("$root/documents-recovery", "$root/documents-recovery-protected"), AssetEnvelope(IosAssetCipher()))
    private fun legacyIds(directory: String): Set<String> {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(directory)) return emptySet()
        check(manager.attributesOfItemAtPath(directory, null)?.get(NSFileType) == NSFileTypeDirectory) { "Invalid document storage." }
        return requireNotNull(manager.contentsOfDirectoryAtPath(directory, null)).filterIsInstance<String>().filter { id ->
            runCatching { validateDocumentAssetId(id) }.isSuccess && manager.attributesOfItemAtPath("$directory/$id", null)?.get(NSFileType) == NSFileTypeRegular
        }.toSet()
    }
    private fun protectedIds(directory: String): Set<String> {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(directory)) return emptySet()
        check(manager.attributesOfItemAtPath(directory, null)?.get(NSFileType) == NSFileTypeDirectory) { "Invalid document storage." }
        return requireNotNull(manager.contentsOfDirectoryAtPath(directory, null)).filterIsInstance<String>().mapNotNull { name ->
            val id = when { name.endsWith(".mva") -> name.removeSuffix(".mva"); name.endsWith(".committed") -> name.removeSuffix(".committed"); else -> return@mapNotNull null }
            id.takeIf { runCatching { validateDocumentAssetId(it) }.isSuccess }
        }.toSet()
    }
    private fun <T> serialized(block: () -> T): T { lock.lock(); return try { block() } finally { lock.unlock() } }
    private fun prepare() { check(IosDocumentCrypto.provider?.prepareFreshStore() == true) { "Document key is unavailable. Existing files have been retained." } }

    actual fun read(id: String): ByteArray = serialized { store().read(id) }
    actual fun writeNew(id: String, plaintext: ByteArray) = serialized { prepare(); store().writeNew(id, plaintext) }
    actual fun migrate(ids: Set<String>) = serialized {
        if (ids.isNotEmpty()) { prepare(); ids.sorted().forEach { store().migrate(it) } }
    }
    actual fun discard(ids: Set<String>) = serialized {
        ids.forEach { id ->
            validateDocumentAssetId(id)
            listOf("$root/documents/$id", "$root/documents-protected/$id.mva", "$root/documents-protected/$id.committed").forEach { path ->
                val manager = NSFileManager.defaultManager
                if (manager.fileExistsAtPath(path)) check(manager.removeItemAtPath(path, null)) { "Could not remove this document copy." }
            }
        }
    }
    actual fun protectInventory(referenced: Set<String>) = serialized {
        val ids = referenced + legacyIds("$root/documents") + protectedIds("$root/documents-protected")
        val recovery = legacyIds("$root/documents-recovery") + protectedIds("$root/documents-recovery-protected")
        if (ids.isNotEmpty() || recovery.isNotEmpty()) {
            prepare()
            ids.sorted().forEach { store().migrate(it) }
            recovery.sorted().forEach { recoveryStore().migrate(it) }
        }
    }
    actual fun restoreRecovery(id: String): String = serialized {
        validateDocumentAssetId(id)
        val bytes = recoveryStore().read(id)
        prepare()
        store().writeNew(id, bytes)
        check(store().read(id).contentEquals(bytes)) { "Could not verify the restored original." }
        id
    }
}
