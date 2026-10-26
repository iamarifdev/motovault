@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.motovault.app.storage.security

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/** Installed by the native host before the vault opens. Errors expose no platform diagnostics. */
interface IosAssetCryptography {
    fun prepareFreshStore(): Boolean
    fun seal(plaintext: NSData, associatedData: NSData): NSData?
    fun open(combined: NSData, associatedData: NSData): NSData?
    fun sha256(data: NSData): NSData
}

object IosDocumentCrypto {
    var provider: IosAssetCryptography? = null
}

internal fun ByteArray.assetData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
internal fun NSData.assetBytes(): ByteArray {
    require(length in 1uL..AssetEnvelope.maximumEnvelopeBytes.toULong()) { "Invalid document size." }
    return ByteArray(length.toInt()).also { output -> output.usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

internal class IosAssetCipher(private val provider: () -> IosAssetCryptography? = { IosDocumentCrypto.provider }) : AssetCipher {
    override fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        requireNotNull(provider()?.seal(plaintext.assetData(), associatedData.assetData())) {
            "Document key is unavailable. Existing files have been retained."
        }.assetBytes()

    override fun open(combined: ByteArray, associatedData: ByteArray): ByteArray =
        requireNotNull(provider()?.open(combined.assetData(), associatedData.assetData())) {
            "Could not unlock this document. Existing files have been retained."
        }.assetBytes()
}

internal actual fun documentSha256(bytes: ByteArray): ByteArray =
    requireNotNull(IosDocumentCrypto.provider) { "Document cryptography is unavailable." }.sha256(bytes.assetData()).assetBytes().also { check(it.size == 32) }
