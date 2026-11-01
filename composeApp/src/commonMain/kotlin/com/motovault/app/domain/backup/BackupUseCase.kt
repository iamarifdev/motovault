package com.motovault.app.domain.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
        digest: (ByteArray) -> ByteArray,
        snapshot: BackupSnapshot,
        vaultRevision: Long,
        passphrase: String,
        installationId: String = "local-vault",
        assetReader: suspend (String) -> ByteArray,
        progress: (suspend (Float, BackupProgress) -> Unit)? = null
    ): ByteArray {
        validatePassphrase(passphrase)
        progress?.invoke(0.05f, BackupProgress(BackupProgressStage.PREPARING))

        val payload = snapshot.payload
        require(snapshot.assetIds == payload.documents.flatMap { it.assetIds() }.toSet()) {
            "Backup asset inventory does not match document references."
        }

        // 2. Gather referenced assets
        progress?.invoke(0.15f, BackupProgress(BackupProgressStage.CHECKING_ASSETS))
        val referencedAssetIds = snapshot.assetIds.sorted()
        val plainAssets = mutableMapOf<String, ByteArray>()
        var totalAssetBytes = 0L

        for (id in referencedAssetIds) {
            currentCoroutineContext().ensureActive()
            val bytes = try {
                assetReader(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                throw IllegalStateException("Missing referenced asset: $id. Backup cannot be completed without all originals.", e)
            }
            require(bytes.isNotEmpty()) { "Referenced asset $id is empty." }
            plainAssets[id] = bytes
            totalAssetBytes += bytes.size
        }

        // 3. Derive key
        progress?.invoke(0.35f, BackupProgress(BackupProgressStage.DERIVING_KEY))
        val salt = BackupCrypto.generateRandomBytes(32)
        val key = BackupCrypto.deriveKey(passphrase, salt, iterations = BACKUP_KDF_ITERATIONS)

        // 4. Encrypt assets
        progress?.invoke(0.50f, BackupProgress(BackupProgressStage.ENCRYPTING_ASSETS))
        val archiveEntries = mutableMapOf<String, ByteArray>()
        val assetEntries = mutableListOf<BackupAssetEntry>()

        for ((index, id) in referencedAssetIds.withIndex()) {
            currentCoroutineContext().ensureActive()
            val plain = plainAssets.getValue(id)
            val sha = digest(plain).toHex()
            val path = "assets/$id.bin"
            val aad = assetAuthenticationData(id)
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
            progress?.invoke(p, BackupProgress(BackupProgressStage.ENCRYPTED_ASSETS, index + 1, referencedAssetIds.size))
        }

        // 5. Encrypt vault payload
        progress?.invoke(0.80f, BackupProgress(BackupProgressStage.ENCRYPTING_VAULT))
        val createdAt = Clock.System.now().toEpochMilliseconds()
        val payloadBytes = json.encodeToString(payload).encodeToByteArray()
        val payloadAad = vaultAuthenticationData(createdAt)
        val encPayload = BackupCrypto.encrypt(key, payloadBytes, payloadAad)
        archiveEntries["vault.enc"] = encPayload

        // 6. Build manifest
        val summary = BackupSummary(
            bikeDisplayName = snapshot.bikeDisplayName,
            odometer = snapshot.odometer,
            logCount = payload.logs.size,
            documentCount = payload.documents.size,
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
        progress?.invoke(0.95f, BackupProgress(BackupProgressStage.WRITING_ARCHIVE))
        val zip = MvaZipArchive.write(archiveEntries)
        progress?.invoke(1.0f, BackupProgress(BackupProgressStage.READY))
        return zip
    }
}
