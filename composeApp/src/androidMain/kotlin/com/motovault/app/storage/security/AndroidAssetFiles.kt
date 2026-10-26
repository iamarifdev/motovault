package com.motovault.app.storage.security

import com.motovault.app.ui.validateDocumentAssetId
import java.io.File
import java.io.FileOutputStream
import android.system.Os
import android.system.OsConstants

internal class AndroidAssetFiles(private val originalDirectory: File, private val encryptedDirectory: File,
    private val availableBytes: (File) -> Long = { it.usableSpace }) : AssetFiles {
    private fun target(id: String) = File(encryptedDirectory, validateDocumentAssetId(id) + ".mva")
    private fun names(directory: File): Set<String> = if (!directory.exists()) {
        check(directory.canonicalFile == directory.absoluteFile) { "Invalid document storage." }; emptySet()
    } else requireNotNull(directory.list()) { "Document storage is unavailable." }.toSet()
    override fun originalExists(id: String) = validateDocumentAssetId(id) in names(originalDirectory)
    override fun encryptedExists(id: String): Boolean {
        validateDocumentAssetId(id)
        val entries = names(encryptedDirectory)
        return "$id.mva" in entries || "$id.committed" in entries
    }
    override fun readEncrypted(id: String, maximum: Int) = readBounded(target(id), maximum)
    override fun readOriginal(id: String, maximum: Int) = readBounded(File(originalDirectory, validateDocumentAssetId(id)), maximum)
    override fun readInventory(id: String): ByteArray? {
        validateDocumentAssetId(id)
        return if ("$id.committed" in names(encryptedDirectory)) readBounded(File(encryptedDirectory, "$id.committed"), 4096) else null
    }

    override fun commitInventory(id: String, encryptedReceipt: ByteArray) {
        validateDocumentAssetId(id)
        require(encryptedReceipt.size in 34..4096)
        val marker = File(encryptedDirectory, "$id.committed")
        if (marker.name in names(encryptedDirectory)) check(marker.isFile && marker.canonicalFile == marker.absoluteFile) { "Invalid document inventory." }
        val stage = File.createTempFile(".inventory-", ".tmp", encryptedDirectory)
        try {
            FileOutputStream(stage).use { it.write(encryptedReceipt); it.fd.sync() }
            check(readBounded(stage, 4096).contentEquals(encryptedReceipt))
            check(stage.renameTo(marker)) { "Could not commit document inventory." }
            val descriptor = Os.open(encryptedDirectory.absolutePath, OsConstants.O_RDONLY, 0)
            try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
        } finally { stage.delete() }
    }

    override fun retireOriginal(id: String) {
        val original = File(originalDirectory, validateDocumentAssetId(id))
        check(target(id).isFile && target(id).canonicalFile == target(id).absoluteFile) { "Protected document is unavailable." }
        check(original.isFile && original.canonicalFile == original.absoluteFile) { "Original document is unavailable." }
        val marker = File(encryptedDirectory, "$id.committed")
        check(marker.isFile && marker.canonicalFile == marker.absoluteFile && marker.length() > 33) { "Document inventory is incomplete." }
        val protectedDescriptor = Os.open(encryptedDirectory.absolutePath, OsConstants.O_RDONLY, 0)
        try { Os.fsync(protectedDescriptor) } finally { Os.close(protectedDescriptor) }
        check(original.delete()) { "Original bytes are protected, but the old plaintext copy could not be retired." }
        val descriptor = Os.open(originalDirectory.absolutePath, OsConstants.O_RDONLY, 0)
        try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
    }

    @Synchronized
    override fun publishEncrypted(id: String, bytes: ByteArray, verify: (ByteArray) -> Unit) {
        check(encryptedDirectory.isDirectory || encryptedDirectory.mkdirs()) { "Protected storage is unavailable." }
        check(encryptedDirectory.canonicalFile == encryptedDirectory.absoluteFile) { "Invalid protected storage." }
        check(availableBytes(encryptedDirectory) >= bytes.size.toLong() + 65536L) { "Not enough free space to protect this document. Original retained." }
        val destination = target(id)
        check(!destination.exists()) { "A protected file already uses this ID." }
        val stage = File.createTempFile(".stage-", ".mva", encryptedDirectory)
        try {
            FileOutputStream(stage).use { stream -> stream.write(bytes); stream.fd.sync() }
            verify(readBounded(stage, AssetEnvelope.maximumEnvelopeBytes))
            check(!destination.exists() && stage.renameTo(destination)) { "Could not publish the protected document." }
            val descriptor = Os.open(encryptedDirectory.absolutePath, OsConstants.O_RDONLY, 0)
            try {
                Os.fsync(descriptor)
            } finally { Os.close(descriptor) }
        } finally { stage.delete() }
    }

    private fun readBounded(file: File, maximum: Int): ByteArray {
        require(file.isFile && file.canonicalFile == file.absoluteFile && file.length() in 1..maximum.toLong()) { "Document is unavailable or too large." }
        return file.inputStream().use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= maximum) { "Document is unavailable or too large." }
                output.write(buffer, 0, count)
            }
            val data = output.toByteArray()
            require(data.size in 1..maximum) { "Document is unavailable or too large." }
            data
        }
    }
}
