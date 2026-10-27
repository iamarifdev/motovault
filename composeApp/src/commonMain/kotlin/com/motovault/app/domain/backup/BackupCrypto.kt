package com.motovault.app.domain.backup

expect object BackupCrypto {
    fun generateRandomBytes(size: Int): ByteArray
    fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int = 100_000): ByteArray
    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray
    fun decrypt(key: ByteArray, combined: ByteArray, aad: ByteArray): ByteArray
}

fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Invalid hex string length: $length" }
    val result = ByteArray(length / 2)
    for (i in indices step 2) {
        result[i / 2] = substring(i, i + 2).toInt(16).toByte()
    }
    return result
}
