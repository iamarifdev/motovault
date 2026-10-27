package com.motovault.app.domain.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object BackupCrypto {
    private val random = SecureRandom()

    actual fun generateRandomBytes(size: Int): ByteArray {
        require(size > 0) { "Size must be positive." }
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }

    actual fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        require(passphrase.isNotBlank()) { "Passphrase cannot be blank." }
        require(salt.isNotEmpty()) { "Salt cannot be empty." }
        require(iterations >= 10_000) { "Iterations must be at least 10,000." }
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    actual fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes." }
        val nonce = generateRandomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val ciphertextAndTag = cipher.doFinal(plaintext)
        return nonce + ciphertextAndTag
    }

    actual fun decrypt(key: ByteArray, combined: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes." }
        require(combined.size >= 12 + 16) { "Encrypted payload is too short." }
        val nonce = combined.copyOfRange(0, 12)
        val ciphertextAndTag = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(ciphertextAndTag)
    }
}
