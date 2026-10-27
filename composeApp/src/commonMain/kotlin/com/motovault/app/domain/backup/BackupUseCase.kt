package com.motovault.app.domain.backup

import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.VaultSettings
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.assetIds
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    const val MIN_PASSPHRASE_LENGTH = 8

    fun validatePassphrase(passphrase: String) {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "Password must be at least $MIN_PASSPHRASE_LENGTH characters."
        }
    }

    suspend fun createBackup(
        state: VaultState,
        vaultRevision: Long,
        passphrase: String,
        installationId: String = "local-vault",
        assetReader: suspend (String) -> ByteArray,
        progress: ((Float, String) -> Unit)? = null
    ): ByteArray {
        validatePassphrase(passphrase)
        progress?.invoke(0.05f, "Preparing vault snapshot...")

        // 1. Freeze payload
        val payload = VaultExportPayload(
            version = 1,
            ride = state.ride,
            rules = state.rules,
            logs = state.logs,
            documents = state.documents,
            preferences = VaultSettings.from(state),
            appearance = state.appearance.name,
            autoLocation = state.autoLocation,
            legacyImported = false
        )

        // 2. Gather referenced assets
        progress?.invoke(0.15f, "Checking document originals...")
        val referencedAssetIds = state.documents.flatMap { it.assetIds() }.toSet().sorted()
        val plainAssets = mutableMapOf<String, ByteArray>()
        var totalAssetBytes = 0L

        for (id in referencedAssetIds) {
            val bytes = try {
                assetReader(id)
            } catch (e: Exception) {
                throw IllegalStateException("Missing referenced asset: $id. Backup cannot be completed without all originals.", e)
            }
            require(bytes.isNotEmpty()) { "Referenced asset $id is empty." }
            plainAssets[id] = bytes
            totalAssetBytes += bytes.size
        }

        // 3. Derive key
        progress?.invoke(0.35f, "Deriving encryption key...")
        val salt = BackupCrypto.generateRandomBytes(32)
        val key = BackupCrypto.deriveKey(passphrase, salt, iterations = 100_000)

        // 4. Encrypt assets
        progress?.invoke(0.50f, "Encrypting document originals...")
        val archiveEntries = mutableMapOf<String, ByteArray>()
        val assetEntries = mutableListOf<BackupAssetEntry>()

        for ((index, id) in referencedAssetIds.withIndex()) {
            val plain = plainAssets.getValue(id)
            val sha = documentSha256(plain).toHex()
            val path = "assets/$id.bin"
            val aad = "MOTOVAULT-ASSET-v1:$id".encodeToByteArray()
            val ciphertext = BackupCrypto.encrypt(key, plain, aad)
            archiveEntries[path] = ciphertext

            val nonceHex = ciphertext.copyOfRange(0, 12).toHex()
            assetEntries.add(
                BackupAssetEntry(
                    assetId = id,
                    path = path,
                    byteLength = plain.size,
                    sha256 = sha,
                    nonceHex = nonceHex
                )
            )
            val p = 0.50f + (0.25f * (index + 1) / referencedAssetIds.size.coerceAtLeast(1))
            progress?.invoke(p, "Encrypted ${index + 1} of ${referencedAssetIds.size} documents...")
        }

        // 5. Encrypt vault payload
        progress?.invoke(0.80f, "Encrypting vault database...")
        val createdAt = Clock.System.now().toEpochMilliseconds()
        val payloadBytes = json.encodeToString(payload).encodeToByteArray()
        val payloadAad = "MOTOVAULT-VAULT-v1:$createdAt".encodeToByteArray()
        val encPayload = BackupCrypto.encrypt(key, payloadBytes, payloadAad)
        archiveEntries["vault.enc"] = encPayload

        // 6. Build manifest
        val summary = BackupSummary(
            bikeDisplayName = state.ride?.displayName ?: "Motorcycle",
            odometer = state.ride?.odometer ?: 0.0,
            logCount = state.logs.size,
            documentCount = state.documents.size,
            totalAssetBytes = totalAssetBytes
        )
        val manifest = BackupManifest(
            formatVersion = 1,
            appVersion = "1.0.0",
            buildRevision = "1.0",
            createdAtEpochMillis = createdAt,
            installationId = installationId,
            vaultRevision = vaultRevision,
            kdf = BackupKdfParams(saltHex = salt.toHex()),
            cipher = BackupCipherParams(),
            summary = summary,
            assets = assetEntries
        )
        archiveEntries["manifest.json"] = json.encodeToString(manifest).encodeToByteArray()

        // 7. Bundle archive
        progress?.invoke(0.95f, "Writing archive bundle...")
        val zip = MvaZipArchive.write(archiveEntries)
        progress?.invoke(1.0f, "Backup ready.")
        return zip
    }
}
