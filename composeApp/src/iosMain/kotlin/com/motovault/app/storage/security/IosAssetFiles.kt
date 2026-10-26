@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.storage.security

import com.motovault.app.ui.validateDocumentAssetId
import platform.Foundation.*
import platform.posix.*

internal class IosAssetFiles(private val originalDirectory: String, private val encryptedDirectory: String,
    private val availableBytes: (String) -> Long? = { path -> (NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue }) : AssetFiles {
    private val manager get() = NSFileManager.defaultManager
    private fun target(id: String) = "$encryptedDirectory/${validateDocumentAssetId(id)}.mva"
    private fun names(directory: String): Set<String> {
        if (!manager.fileExistsAtPath(directory)) {
            require(manager.attributesOfItemAtPath(directory, null) == null) { "Invalid document storage." }
            return emptySet()
        }
        check(manager.attributesOfItemAtPath(directory, null)?.get(NSFileType) == NSFileTypeDirectory) { "Invalid document storage." }
        return requireNotNull(manager.contentsOfDirectoryAtPath(directory, null)) { "Document storage is unavailable." }.filterIsInstance<String>().toSet()
    }
    override fun originalExists(id: String) = validateDocumentAssetId(id) in names(originalDirectory)
    override fun encryptedExists(id: String): Boolean {
        validateDocumentAssetId(id)
        val entries = names(encryptedDirectory)
        return "$id.mva" in entries || "$id.committed" in entries
    }
    override fun readEncrypted(id: String, maximum: Int) = readBounded(target(id), maximum)
    override fun readOriginal(id: String, maximum: Int) = readBounded("$originalDirectory/${validateDocumentAssetId(id)}", maximum)
    override fun readInventory(id: String): ByteArray? {
        validateDocumentAssetId(id)
        return if ("$id.committed" in names(encryptedDirectory)) readBounded("$encryptedDirectory/$id.committed", 4096) else null
    }

    override fun commitInventory(id: String, encryptedReceipt: ByteArray) {
        validateDocumentAssetId(id)
        require(encryptedReceipt.size in 34..4096)
        val path = "$encryptedDirectory/$id.committed"
        if ("$id.committed" in names(encryptedDirectory)) check(manager.attributesOfItemAtPath(path, null)?.get(NSFileType) == NSFileTypeRegular) { "Invalid document inventory." }
        check(encryptedReceipt.assetData().writeToFile(path, options = NSDataWritingAtomic or NSDataWritingFileProtectionComplete, error = null)) { "Could not commit document inventory." }
        syncPath(path)
        syncPath(encryptedDirectory)
    }

    override fun retireOriginal(id: String) {
        val path = "$originalDirectory/${validateDocumentAssetId(id)}"
        check(manager.attributesOfItemAtPath(target(id), null)?.get(NSFileType) == NSFileTypeRegular) { "Protected document is unavailable." }
        check(manager.attributesOfItemAtPath(path, null)?.get(NSFileType) == NSFileTypeRegular) { "Original document is unavailable." }
        val marker = "$encryptedDirectory/$id.committed"
        check(readBounded(marker, 4096).size > 33) { "Document inventory is incomplete." }
        syncPath(marker)
        syncPath(encryptedDirectory)
        check(manager.removeItemAtPath(path, null)) { "Original bytes are protected, but the old plaintext copy could not be retired." }
        syncPath(originalDirectory)
    }

    // Caller serializes the vault asset operations; no publication from arbitrary parallel tasks.
    override fun publishEncrypted(id: String, bytes: ByteArray, verify: (ByteArray) -> Unit) {
        check(manager.createDirectoryAtPath(encryptedDirectory, true, mapOf(NSFileProtectionKey to NSFileProtectionComplete), null)) { "Protected storage is unavailable." }
        check(manager.attributesOfItemAtPath(encryptedDirectory, null)?.get(NSFileType) == NSFileTypeDirectory) { "Invalid protected storage." }
        check((availableBytes(encryptedDirectory) ?: -1L) >= bytes.size.toLong() + 65536L) { "Not enough free space to protect this document. Original retained." }
        val destination = target(id)
        check(!manager.fileExistsAtPath(destination)) { "A protected file already uses this ID." }
        val stage = "$encryptedDirectory/.stage-${NSUUID().UUIDString}.mva"
        try {
            check(bytes.assetData().writeToFile(stage, options = NSDataWritingAtomic or NSDataWritingFileProtectionComplete, error = null)) { "Could not stage the protected document." }
            syncPath(stage)
            verify(readBounded(stage, AssetEnvelope.maximumEnvelopeBytes))
            check(!manager.fileExistsAtPath(destination) && manager.moveItemAtPath(stage, destination, null)) { "Could not publish the protected document." }
            syncPath(encryptedDirectory)
        } finally { manager.removeItemAtPath(stage, null) }
    }

    private fun syncPath(path: String) {
        val descriptor = open(path, O_RDONLY)
        check(descriptor >= 0) { "Protected storage is unavailable." }
        try { check(fsync(descriptor) == 0) { "Could not sync the protected document." } } finally { close(descriptor) }
    }

    private fun readBounded(path: String, maximum: Int): ByteArray {
        require(manager.attributesOfItemAtPath(path.substringBeforeLast('/'), null)?.get(NSFileType) == NSFileTypeDirectory) { "Invalid document storage." }
        val attributes = manager.attributesOfItemAtPath(path, null)
        require(attributes?.get(NSFileType) == NSFileTypeRegular) { "Document is unavailable." }
        val handle = requireNotNull(NSFileHandle.fileHandleForReadingAtPath(path)) { "Document is unavailable." }
        try {
            val data = handle.readDataOfLength(maximum.toULong() + 1uL)
            require(data.length in 1uL..maximum.toULong()) { "Document is unavailable or too large." }
            return data.assetBytes()
        } finally { handle.closeFile() }
    }
}
