package com.motovault.app.storage.security

import com.motovault.app.ui.validateDocumentAssetId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val MAX_PRIVATE_ASSET_BYTES = 32 * 1024 * 1024
private val envelopeHeader = byteArrayOf(77, 86, 65, 1, 1) // MVA, format 1, local key 1
private const val GCM_OVERHEAD = 12 + 16

/** Implementations use platform AES-256-GCM; combined bytes are nonce + ciphertext + tag. */
internal interface AssetCipher {
    fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray
    fun open(combined: ByteArray, associatedData: ByteArray): ByteArray
}

/** Publication must be atomic, durable and serialized with other writes for the same asset. */
internal interface AssetFiles {
    fun originalExists(id: String): Boolean
    fun encryptedExists(id: String): Boolean
    fun readEncrypted(id: String, maximum: Int): ByteArray
    fun readOriginal(id: String, maximum: Int): ByteArray
    fun publishEncrypted(id: String, bytes: ByteArray, verify: (ByteArray) -> Unit)
    fun retireOriginal(id: String)
    fun readInventory(id: String): ByteArray?
    fun commitInventory(id: String, encryptedReceipt: ByteArray)
}

internal class AssetEnvelope(private val cipher: AssetCipher, private val purpose: String = "") {
    private fun aad(id: String) = envelopeHeader + purpose.encodeToByteArray() + validateDocumentAssetId(id).encodeToByteArray()

    fun inventoryEnvelope() = AssetEnvelope(cipher, "inventory/v1\u0000")

    fun seal(id: String, plaintext: ByteArray): ByteArray {
        require(plaintext.size in 1..MAX_PRIVATE_ASSET_BYTES) { "Invalid document size." }
        val combined = cipher.seal(plaintext, aad(id))
        check(combined.size == plaintext.size + GCM_OVERHEAD) { "Invalid encrypted document." }
        return envelopeHeader + combined
    }

    fun open(id: String, envelope: ByteArray): ByteArray {
        validateDocumentAssetId(id)
        require(envelope.size in (envelopeHeader.size + GCM_OVERHEAD + 1)..maximumEnvelopeBytes) { "Invalid encrypted document." }
        require(envelope.copyOfRange(0, envelopeHeader.size).contentEquals(envelopeHeader)) { "Unsupported encrypted document version." }
        return cipher.open(envelope.copyOfRange(envelopeHeader.size, envelope.size), aad(id)).also {
            check(it.size == envelope.size - envelopeHeader.size - GCM_OVERHEAD) { "Invalid decrypted document." }
        }
    }

    companion object { const val maximumEnvelopeBytes = MAX_PRIVATE_ASSET_BYTES + 5 + GCM_OVERHEAD }
}

/**
 * Encrypted publication is the per-file commit point. IDs and owner references stay stable.
 * Original bytes remain immutable in authenticated ciphertext. Plaintext is retired only after
 * verified durable publication. Interrupted publication leaves the plaintext original intact.
 */
internal class SecureAssetStore(private val files: AssetFiles, private val envelope: AssetEnvelope) {
    fun read(id: String): ByteArray {
        validateDocumentAssetId(id)
        // Never catch authentication/key failures and fall back to an older plaintext original.
        return if (files.encryptedExists(id)) envelope.open(id, files.readEncrypted(id, AssetEnvelope.maximumEnvelopeBytes))
        else files.readOriginal(id, MAX_PRIVATE_ASSET_BYTES).also { require(it.size in 1..MAX_PRIVATE_ASSET_BYTES) }
    }

    fun writeNew(id: String, plaintext: ByteArray) {
        validateDocumentAssetId(id)
        check(!files.originalExists(id)) { "An original file already uses this ID." }
        check(!files.encryptedExists(id)) { "A protected file already uses this ID." }
        publish(id, plaintext)
    }

    fun migrate(id: String) {
        validateDocumentAssetId(id)
        if (files.encryptedExists(id)) {
            val protected = read(id)
            commitReceipt(id, protected)
            if (!files.originalExists(id)) return
            check(protected.contentEquals(files.readOriginal(id, MAX_PRIVATE_ASSET_BYTES))) { "Protected and original documents differ. Both copies are retained." }
        } else {
            val original = files.readOriginal(id, MAX_PRIVATE_ASSET_BYTES)
            publish(id, original)
            check(read(id).contentEquals(original)) { "Document verification failed. Original retained." }
        }
        files.retireOriginal(id)
    }

    private fun publish(id: String, plaintext: ByteArray) {
        val bytes = envelope.seal(id, plaintext)
        files.publishEncrypted(id, bytes) { staged ->
            check(envelope.open(id, staged).contentEquals(plaintext)) { "Document verification failed. Original retained." }
        }
        commitReceipt(id, plaintext)
    }

    fun receipt(id: String): ProtectedAssetReceipt {
        val plaintext = read(id)
        val receipt = storedReceipt(id)
        check(receipt == ProtectedAssetReceipt.from(id, plaintext)) { "Document inventory does not match its original." }
        return receipt
    }

    private fun storedReceipt(id: String): ProtectedAssetReceipt {
        val receiptBytes = requireNotNull(files.readInventory(id)) { "Document inventory is incomplete." }
        return Json.decodeFromString<ProtectedAssetReceipt>(envelope.inventoryEnvelope().open(id, receiptBytes).decodeToString())
    }

    private fun commitReceipt(id: String, plaintext: ByteArray) {
        val expected = ProtectedAssetReceipt.from(id, plaintext)
        val old = runCatching { storedReceipt(id) }.getOrNull()
        if (old == expected) return
        val bytes = envelope.inventoryEnvelope().seal(id, Json.encodeToString(expected).encodeToByteArray())
        files.commitInventory(id, bytes)
        check(storedReceipt(id) == expected) { "Could not verify the document inventory. Original retained." }
    }
}

internal expect fun documentSha256(bytes: ByteArray): ByteArray

@Serializable
internal data class ProtectedAssetReceipt(
    val assetId: String, val plaintextBytes: Int, val sha256: String,
    val formatVersion: Int = 1, val localKeyVersion: Int = 1
) {
    companion object {
        fun from(id: String, bytes: ByteArray) = ProtectedAssetReceipt(validateDocumentAssetId(id), bytes.size,
            documentSha256(bytes).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') })
    }
}
