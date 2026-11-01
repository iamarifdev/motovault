@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.motovault.app.domain.backup

import kotlinx.cinterop.*
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import platform.Foundation.NSData
import platform.posix.*
import com.motovault.app.storage.security.IosAssetCryptography
import com.motovault.app.storage.security.IosDocumentCrypto

private typealias NativeCall = CPointer<CFunction<(CPointer<ByteVar>?) -> CPointer<ByteVar>?>>

private class SwiftBackupTestBridge(private val call: NativeCall) : IosBackupCryptoBridge, IosAssetCryptography {
    private fun invoke(operation: String, fields: Map<String, JsonElement>): NSData? {
        val request = buildJsonObject {
            put("operation", operation)
            fields.forEach { (key, value) -> put(key, value) }
        }.toString()
        val response = memScoped { call(request.cstr.getPointer(this)) } ?: return null
        return try { Base64.decode(response.toKString()).toNSData() } finally { free(response) }
    }

    private fun NSData.encoded() = JsonPrimitive(Base64.encode(toByteArray()))
    override fun generateRandomBytes(size: Int): NSData? = null // production fallback uses SecRandomCopyBytes
    override fun deriveKey(passphrase: String, salt: NSData, iterations: Int): NSData? = invoke("derive", mapOf(
        "password" to JsonPrimitive(passphrase), "salt" to salt.encoded(), "iterations" to JsonPrimitive(iterations)
    ))
    override fun encrypt(key: NSData, plaintext: NSData, aad: NSData): NSData? = invoke("encrypt", mapOf(
        "key" to key.encoded(), "payload" to plaintext.encoded(), "aad" to aad.encoded()
    ))
    override fun decrypt(key: NSData, combined: NSData, aad: NSData): NSData? = invoke("decrypt", mapOf(
        "key" to key.encoded(), "payload" to combined.encoded(), "aad" to aad.encoded()
    ))
    override fun sha256(data: NSData): NSData = requireNotNull(invoke("sha256", mapOf("payload" to data.encoded())))
    override fun prepareFreshStore(): Boolean = error("Document key operations require the app host")
    override fun seal(plaintext: NSData, associatedData: NSData): NSData? = error("Document key operations require the app host")
    override fun open(combined: NSData, associatedData: NSData): NSData? = error("Document key operations require the app host")
}

internal actual fun initializeBackupTestCrypto() {
    if (IosBackupCryptoRegistry.bridge != null) return
    val path = getenv("MOTOVAULT_TEST_CRYPTO_LIBRARY")?.toKString()
        ?: error("MOTOVAULT_TEST_CRYPTO_LIBRARY must point to the Swift simulator test bridge")
    val library = dlopen(path, RTLD_NOW) ?: error("Cannot load Swift backup test bridge: ${dlerror()?.toKString()}")
    val symbol = dlsym(library, "motovault_test_backup_crypto")
        ?: error("Swift backup test bridge entry point is missing")
    val bridge = SwiftBackupTestBridge(symbol.reinterpret())
    IosBackupCryptoRegistry.bridge = bridge
    IosDocumentCrypto.provider = bridge
}
