@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.motovault.app.domain.backup

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.kSecRandomDefault
import platform.Security.SecRandomCopyBytes
import platform.posix.memcpy

interface IosBackupCryptoBridge {
    fun generateRandomBytes(size: Int): NSData?
    fun deriveKey(passphrase: String, salt: NSData, iterations: Int): NSData?
    fun encrypt(key: NSData, plaintext: NSData, aad: NSData): NSData?
    fun decrypt(key: NSData, combined: NSData, aad: NSData): NSData?
}

object IosBackupCryptoRegistry {
    var bridge: IosBackupCryptoBridge? = null
}

internal fun ByteArray.toNSData(): NSData = if (isEmpty()) NSData() else usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

internal fun NSData.toByteArray(): ByteArray {
    if (length == 0uL) return ByteArray(0)
    val result = ByteArray(length.toInt())
    result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}

actual object BackupCrypto {
    actual fun generateRandomBytes(size: Int): ByteArray {
        require(size > 0) { "Size must be positive." }
        val bridgeResult = IosBackupCryptoRegistry.bridge?.generateRandomBytes(size)
        if (bridgeResult != null) return bridgeResult.toByteArray()

        val bytes = ByteArray(size)
        val status = bytes.usePinned {
            SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0))
        }
        check(status == 0) { "Could not generate secure random bytes on iOS." }
        return bytes
    }

    actual fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        require(passphrase.isNotBlank()) { "Passphrase cannot be blank." }
        require(salt.isNotEmpty()) { "Salt cannot be empty." }
        require(iterations >= 10_000) { "Iterations must be at least 10,000." }
        val bridge = requireNotNull(IosBackupCryptoRegistry.bridge) { "iOS Backup cryptography bridge is not initialized." }
        val derived = bridge.deriveKey(passphrase, salt.toNSData(), iterations)
        return requireNotNull(derived) { "Key derivation failed on iOS." }.toByteArray().also {
            check(it.size == 32) { "Derived key must be 32 bytes." }
        }
    }

    actual fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes." }
        val bridge = requireNotNull(IosBackupCryptoRegistry.bridge) { "iOS Backup cryptography bridge is not initialized." }
        val sealed = bridge.encrypt(key.toNSData(), plaintext.toNSData(), aad.toNSData())
        return requireNotNull(sealed) { "Encryption failed on iOS." }.toByteArray()
    }

    actual fun decrypt(key: ByteArray, combined: ByteArray, aad: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 key must be 32 bytes." }
        require(combined.size >= 12 + 16) { "Encrypted payload is too short." }
        val bridge = requireNotNull(IosBackupCryptoRegistry.bridge) { "iOS Backup cryptography bridge is not initialized." }
        val opened = bridge.decrypt(key.toNSData(), combined.toNSData(), aad.toNSData())
        return requireNotNull(opened) { "Decryption or authentication failed on iOS." }.toByteArray()
    }
}
