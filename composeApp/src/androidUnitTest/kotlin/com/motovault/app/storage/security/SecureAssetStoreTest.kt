package com.motovault.app.storage.security

import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*

class SecureAssetStoreTest {
    private val original = "SYNTHETIC document original".encodeToByteArray()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val envelope = AssetEnvelope(AndroidAssetCipher { key })

    private class MemoryFiles(val original: ByteArray) : AssetFiles {
        var published: ByteArray? = null
        var staged: ByteArray? = null
        var interrupt = false
        var diskFull = false
        var originalPresent = true
        var retirementFailure = false
        var committed = false
        var inventory: ByteArray? = null
        var inventoryFailure = false
        override fun readInventory(id: String) = inventory?.copyOf()
        override fun commitInventory(id: String, encryptedReceipt: ByteArray) { check(!inventoryFailure); inventory = encryptedReceipt.copyOf(); committed = true }
        override fun originalExists(id: String) = id == "synthetic.pdf" && originalPresent
        override fun encryptedExists(id: String) = published != null || committed
        override fun readEncrypted(id: String, maximum: Int) = requireNotNull(published)
        override fun readOriginal(id: String, maximum: Int): ByteArray { check(originalPresent); return original.copyOf() }
        override fun retireOriginal(id: String) { check(!retirementFailure); originalPresent = false }
        override fun publishEncrypted(id: String, bytes: ByteArray, verify: (ByteArray) -> Unit) {
            if (diskFull) error("synthetic disk full")
            staged = bytes
            verify(requireNotNull(staged))
            if (interrupt) error("synthetic interruption before publish")
            published = staged
            committed = true
            staged = null
        }
    }

    @Test fun migrationPreservesOriginalBytesAndRetryDoesNotReencrypt() {
        val files = MemoryFiles(original)
        val store = SecureAssetStore(files, envelope)
        store.migrate("synthetic.pdf")
        val committed = requireNotNull(files.published).copyOf()
        assertFalse(committed.contentEquals(original))
        assertContentEquals(original, files.original)
        assertContentEquals(original, store.read("synthetic.pdf"))
        assertFalse(files.originalPresent)
        store.migrate("synthetic.pdf")
        assertContentEquals(committed, files.published)
    }

    @Test fun interruptedAndDiskFullMigrationsRetainReadableOriginalAndCanRetry() {
        for (diskFull in listOf(false, true)) {
            val files = MemoryFiles(original).apply { this.diskFull = diskFull; interrupt = !diskFull }
            val store = SecureAssetStore(files, envelope)
            assertFails { store.migrate("synthetic.pdf") }
            assertNull(files.published)
            assertTrue(files.originalPresent)
            assertContentEquals(original, store.read("synthetic.pdf"))
            files.diskFull = false; files.interrupt = false
            store.migrate("synthetic.pdf")
            assertContentEquals(original, store.read("synthetic.pdf"))
        }
    }

    @Test fun authenticationFailureNeverFallsBackToRetainedPlaintext() {
        val files = MemoryFiles(original)
        val store = SecureAssetStore(files, envelope)
        store.migrate("synthetic.pdf")
        requireNotNull(files.published).let { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertFails { store.read("synthetic.pdf") }
        assertFails { store.migrate("synthetic.pdf") }
        assertContentEquals(original, files.original)
    }

    @Test fun assetIdentityAndEveryEnvelopeFieldAreAuthenticated() {
        val bytes = envelope.seal("synthetic.pdf", original)
        assertFails { envelope.open("different.pdf", bytes) }
        for (index in bytes.indices) {
            val changed = bytes.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertFails("offset $index") { envelope.open("synthetic.pdf", changed) }
        }
        assertFails { envelope.open("synthetic.pdf", bytes.copyOf(20)) }
        assertFails { envelope.open("../synthetic.pdf", bytes) }
    }

    @Test fun missingOrWrongKeyRetainsCiphertextWithoutReturningOriginal() {
        val files = MemoryFiles(original)
        SecureAssetStore(files, envelope).migrate("synthetic.pdf")
        val committed = requireNotNull(files.published).copyOf()
        val missing = SecureAssetStore(files, AssetEnvelope(AndroidAssetCipher { error("synthetic key unavailable") }))
        assertFails { missing.read("synthetic.pdf") }
        val wrongKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertFails { SecureAssetStore(files, AssetEnvelope(AndroidAssetCipher { wrongKey })).read("synthetic.pdf") }
        assertContentEquals(committed, files.published)
        assertContentEquals(original, files.original)
    }

    @Test fun newAssetsAreVerifiedAndNeverOverwritePublishedCiphertext() {
        val files = MemoryFiles(original)
        val store = SecureAssetStore(files, envelope)
        store.writeNew("new.jpg", original)
        val committed = requireNotNull(files.published).copyOf()
        assertFails { store.writeNew("new.jpg", "replacement".encodeToByteArray()) }
        assertContentEquals(committed, files.published)
        assertFails { envelope.seal("empty.jpg", byteArrayOf()) }
    }

    @Test fun randomNoncesProduceDistinctCiphertextsForSameOriginal() {
        val first = envelope.seal("synthetic.pdf", original)
        val second = envelope.seal("synthetic.pdf", original)
        assertFalse(first.contentEquals(second))
        assertContentEquals(original, envelope.open("synthetic.pdf", first))
        assertContentEquals(original, envelope.open("synthetic.pdf", second))
    }

    @Test fun newWriteCannotShadowAnImmutableLegacyOriginal() {
        val files = MemoryFiles(original)
        assertFails { SecureAssetStore(files, envelope).writeNew("synthetic.pdf", "different".encodeToByteArray()) }
        assertNull(files.published)
        assertContentEquals(original, files.original)
    }

    @Test fun cryptoKitSyntheticVectorOpensWithPlatformJce() {
        // Fixed key/nonce are synthetic fixtures only. Production seal always generates its IV.
        val hex = "0000000000000000000000009dfe0e6905253f27446ea1bcd986f07d1c1423a545cf4d1dbfc39948ca6010f1dc310997b70af9488f32dc"
        val combined = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val codec = AssetEnvelope(AndroidAssetCipher { SecretKeySpec(ByteArray(32), "AES") })
        assertContentEquals(original, codec.open("synthetic.pdf", byteArrayOf(77, 86, 65, 1, 1) + combined))
    }

    @Test fun interruptedRetirementKeepsBothCopiesAndRetryCompletes() {
        val files = MemoryFiles(original).apply { retirementFailure = true }
        val store = SecureAssetStore(files, envelope)
        assertFails { store.migrate("synthetic.pdf") }
        assertTrue(files.originalPresent)
        assertContentEquals(original, store.read("synthetic.pdf"))
        files.retirementFailure = false
        store.migrate("synthetic.pdf")
        assertFalse(files.originalPresent)
        assertContentEquals(original, store.read("synthetic.pdf"))
    }

    @Test fun missingCommittedCiphertextCannotExposeAnOldPlaintextCopy() {
        val files = MemoryFiles(original).apply { retirementFailure = true }
        val store = SecureAssetStore(files, envelope)
        assertFails { store.migrate("synthetic.pdf") }
        files.published = null
        assertTrue(files.originalPresent)
        assertFails { store.read("synthetic.pdf") }
        assertFails { store.migrate("synthetic.pdf") }
    }

    @Test fun sha256MatchesPublishedStandardVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", documentSha256("abc".encodeToByteArray()).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') })
    }

    @Test fun inventoryIsAuthenticatedAndSeparatesMetadataFromAssetCiphertext() {
        val files = MemoryFiles(original)
        val store = SecureAssetStore(files, envelope)
        store.migrate("synthetic.pdf")
        val receipt = store.receipt("synthetic.pdf")
        assertEquals("synthetic.pdf", receipt.assetId)
        assertEquals(original.size, receipt.plaintextBytes)
        assertEquals(64, receipt.sha256.length)
        val committed = requireNotNull(files.published).copyOf()
        val inventory = requireNotNull(files.inventory).copyOf()
        files.inventory = committed
        assertFails { store.receipt("synthetic.pdf") }
        files.published = inventory
        assertFails { store.read("synthetic.pdf") }
        files.published = committed
        store.migrate("synthetic.pdf")
        assertEquals(receipt, store.receipt("synthetic.pdf"))
        assertContentEquals(committed, files.published)
    }

    @Test fun inventoryCommitFailurePreservesPlaintextUntilVerifiedRetry() {
        val files = MemoryFiles(original).apply { inventoryFailure = true }
        val store = SecureAssetStore(files, envelope)
        assertFails { store.migrate("synthetic.pdf") }
        assertTrue(files.originalPresent)
        assertContentEquals(original, store.read("synthetic.pdf"))
        files.inventoryFailure = false
        store.migrate("synthetic.pdf")
        assertFalse(files.originalPresent)
        assertEquals(original.size, store.receipt("synthetic.pdf").plaintextBytes)
    }
}
