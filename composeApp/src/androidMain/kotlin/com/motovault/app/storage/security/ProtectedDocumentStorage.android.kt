package com.motovault.app.storage.security

import com.motovault.app.ui.vaultContext
import java.io.File
import java.io.FileOutputStream
import com.motovault.app.ui.validateDocumentAssetId

internal actual object ProtectedDocumentStorage {
    private val storageRoot get() = vaultContext.filesDir.canonicalFile
    private val directory get() = File(storageRoot, "documents-protected")
    private fun store() = SecureAssetStore(
        AndroidAssetFiles(File(storageRoot, "documents"), directory),
        AssetEnvelope(AndroidAssetCipher { AndroidAssetKey.load(allowCreation = false) })
    )
    private fun recoveryStore() = SecureAssetStore(
        AndroidAssetFiles(File(storageRoot, "documents-recovery"), File(storageRoot, "documents-recovery-protected")),
        AssetEnvelope(AndroidAssetCipher { AndroidAssetKey.load(allowCreation = false) })
    )
    private fun legacyIds(directory: File): Set<String> {
        if (!directory.exists()) return emptySet()
        check(directory.isDirectory && directory.canonicalFile == directory.absoluteFile) { "Invalid document storage." }
        return requireNotNull(directory.listFiles()).filter { it.isFile && it.canonicalFile == it.absoluteFile && runCatching { validateDocumentAssetId(it.name) }.isSuccess }.map { it.name }.toSet()
    }
    private fun protectedIds(directory: File): Set<String> {
        if (!directory.exists()) return emptySet()
        check(directory.isDirectory && directory.canonicalFile == directory.absoluteFile) { "Invalid document storage." }
        return requireNotNull(directory.list()).mapNotNull { name ->
            val id = when { name.endsWith(".mva") -> name.removeSuffix(".mva"); name.endsWith(".committed") -> name.removeSuffix(".committed"); else -> return@mapNotNull null }
            id.takeIf { runCatching { validateDocumentAssetId(it) }.isSuccess }
        }.toSet()
    }

    private fun prepare() {
        val root = directory
        check(root.canonicalFile == root.absoluteFile) { "Invalid protected storage." }
        check(!root.exists() || root.isDirectory) { "Protected storage is unavailable." }
        val names = if (root.exists()) requireNotNull(root.list()) { "Protected storage is unavailable." } else emptyArray()
        val recovery = File(storageRoot, "documents-recovery-protected")
        check(recovery.canonicalFile == recovery.absoluteFile)
        val recoveryNames = if (recovery.exists()) requireNotNull(recovery.list()) else emptyArray()
        AndroidAssetKey.load(allowCreation = names.isEmpty() && recoveryNames.isEmpty())
        check(root.isDirectory || root.mkdirs()) { "Protected storage is unavailable." }
        val marker = File(root, "key-v1.initialized")
        if (!marker.exists()) FileOutputStream(marker).use { it.write(1); it.fd.sync() }
    }

    @Synchronized actual fun read(id: String) = store().read(id)
    @Synchronized actual fun writeNew(id: String, plaintext: ByteArray) { prepare(); store().writeNew(id, plaintext) }
    @Synchronized actual fun migrate(ids: Set<String>) {
        if (ids.isEmpty()) return
        prepare()
        ids.sorted().forEach { store().migrate(it) }
    }
    @Synchronized actual fun discard(ids: Set<String>) {
        ids.forEach { id ->
            validateDocumentAssetId(id)
            listOf(File(storageRoot, "documents/$id"), File(directory, "$id.mva"), File(directory, "$id.committed")).forEach { file ->
                check(file.canonicalFile == file.absoluteFile) { "Invalid document storage." }
                check(!file.exists() || file.delete()) { "Could not remove this document copy." }
            }
        }
    }
    @Synchronized actual fun protectInventory(referenced: Set<String>) {
        val ids = referenced + legacyIds(File(storageRoot, "documents")) + protectedIds(directory)
        val recovery = legacyIds(File(storageRoot, "documents-recovery")) + protectedIds(File(storageRoot, "documents-recovery-protected"))
        if (ids.isEmpty() && recovery.isEmpty()) return
        prepare()
        ids.sorted().forEach { store().migrate(it) }
        recovery.sorted().forEach { recoveryStore().migrate(it) }
    }
    @Synchronized actual fun restoreRecovery(id: String): String {
        validateDocumentAssetId(id)
        val bytes = recoveryStore().read(id)
        prepare()
        store().writeNew(id, bytes)
        check(store().read(id).contentEquals(bytes)) { "Could not verify the restored original." }
        return id
    }
}
