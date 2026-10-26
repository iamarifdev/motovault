package com.motovault.app.storage.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidAssetCipher(private val key: () -> SecretKey) : AssetCipher {
    override fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(associatedData)
        check(cipher.iv.size == 12) { "Unsupported document encryption provider." }
        return cipher.iv + cipher.doFinal(plaintext)
    }

    override fun open(combined: ByteArray, associatedData: ByteArray): ByteArray {
        require(combined.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, combined.copyOfRange(0, 12)))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(combined, 12, combined.size - 12)
    }
}

internal object AndroidAssetKey {
    private const val ALIAS = "com.motovault.document-assets.v1"

    /** Creation is allowed only for a verified fresh encrypted store, never on read failure. */
    @Synchronized
    fun load(allowCreation: Boolean, alias: String = ALIAS): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(alias)) return requireNotNull(store.getKey(alias, null) as? SecretKey) {
            "Document key is unavailable. Existing files have been retained."
        }
        check(allowCreation) { "Document key is missing. Restore an encrypted backup; existing files have been retained." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
}
