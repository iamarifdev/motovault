package com.motovault.app.domain.backup

import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.Appearance
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.assetIds
import com.motovault.app.ui.reconcileRenewalReminders
import kotlinx.serialization.json.Json

object RestoreUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    fun validateAndPreview(
        archiveBytes: ByteArray,
        passphrase: String,
        currentState: VaultState? = null
    ): BackupPreview {
        BackupUseCase.validatePassphrase(passphrase)
        val entries = MvaZipArchive.read(archiveBytes)

        val manifestBytes = requireNotNull(entries["manifest.json"]) {
            "Invalid MotoVault backup: missing manifest.json."
        }
        val manifest = try {
            json.decodeFromString<BackupManifest>(manifestBytes.decodeToString())
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted backup manifest: ${e.message}", e)
        }

        require(manifest.formatVersion <= 1) {
            "This backup was created by a newer version of MotoVault (format version ${manifest.formatVersion}). Please update MotoVault to restore it."
        }

        // Derive key
        val salt = manifest.kdf.saltHex.hexToByteArray()
        val key = try {
            BackupCrypto.deriveKey(passphrase, salt, iterations = manifest.kdf.iterations)
        } catch (e: Exception) {
            throw BackupSecurityException("Could not derive decryption key: ${e.message}", e)
        }

        // Decrypt vault payload
        val encPayload = requireNotNull(entries["vault.enc"]) {
            "Invalid MotoVault backup: missing vault.enc."
        }
        val payloadAad = "MOTOVAULT-VAULT-v1:${manifest.createdAtEpochMillis}".encodeToByteArray()
        val payloadBytes = try {
            BackupCrypto.decrypt(key, encPayload, payloadAad)
        } catch (e: Exception) {
            throw BackupSecurityException("Incorrect password or corrupted backup payload.", e)
        }

        val payload = try {
            json.decodeFromString<VaultExportPayload>(payloadBytes.decodeToString())
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted vault records: ${e.message}", e)
        }

        // Validate entity IDs to prevent duplicates
        require(payload.rules.map { it.id }.distinct().size == payload.rules.size) {
            "Backup contains duplicate schedule IDs."
        }
        require(payload.logs.map { it.id }.distinct().size == payload.logs.size) {
            "Backup contains duplicate ledger IDs."
        }
        require(payload.documents.map { it.id }.distinct().size == payload.documents.size) {
            "Backup contains duplicate document IDs."
        }

        // Validate and authenticate every asset in the manifest
        var validatedAssetBytes = 0L
        val manifestAssetIds = manifest.assets.map { it.assetId }.toSet()

        for (asset in manifest.assets) {
            val encAsset = requireNotNull(entries[asset.path]) {
                "Missing referenced asset file in backup: ${asset.path}"
            }
            val aad = "MOTOVAULT-ASSET-v1:${asset.assetId}".encodeToByteArray()
            val plain = try {
                BackupCrypto.decrypt(key, encAsset, aad)
            } catch (e: Exception) {
                throw BackupSecurityException("Failed to decrypt asset ${asset.assetId}. Archive may be damaged or tampered.", e)
            }
            require(plain.size == asset.byteLength) {
                "Asset length mismatch for ${asset.assetId}: manifest specifies ${asset.byteLength}, extracted ${plain.size}."
            }
            val sha = documentSha256(plain).toHex()
            require(sha.equals(asset.sha256, ignoreCase = true)) {
                "SHA-256 verification failed for asset ${asset.assetId}. Expected ${asset.sha256}, calculated $sha."
            }
            validatedAssetBytes += plain.size
        }

        // Verify document references: all referenced assets must be present
        val referencedInDocs = payload.documents.flatMap { it.assetIds() }.toSet()
        val missingFromManifest = referencedInDocs - manifestAssetIds
        require(missingFromManifest.isEmpty()) {
            "Backup is incomplete: referenced document assets are missing: $missingFromManifest"
        }

        val conflicts = currentState?.let { current ->
            BackupConflictSummary(
                currentBikeDisplayName = current.ride?.displayName,
                incomingBikeDisplayName = manifest.summary.bikeDisplayName,
                currentOdometer = current.ride?.odometer,
                incomingOdometer = manifest.summary.odometer,
                currentLogCount = current.logs.size,
                incomingLogCount = manifest.summary.logCount,
                currentDocumentCount = current.documents.size,
                incomingDocumentCount = manifest.summary.documentCount,
                bikeMatches = current.ride?.brandId == payload.ride?.brandId && current.ride?.displayName == payload.ride?.displayName
            )
        }

        return BackupPreview(
            manifest = manifest,
            payload = payload,
            validatedAssetCount = manifest.assets.size,
            totalAssetBytes = validatedAssetBytes,
            conflicts = conflicts
        )
    }

    suspend fun executeRestore(
        archiveBytes: ByteArray,
        passphrase: String,
        database: VaultDatabase,
        assetWriter: suspend (id: String, bytes: ByteArray) -> Unit,
        onComplete: suspend (VaultState) -> Unit
    ) {
        val preview = validateAndPreview(archiveBytes, passphrase)
        val entries = MvaZipArchive.read(archiveBytes)
        val salt = preview.manifest.kdf.saltHex.hexToByteArray()
        val key = BackupCrypto.deriveKey(passphrase, salt, iterations = preview.manifest.kdf.iterations)

        // 1. Write and protect all assets first
        for (asset in preview.manifest.assets) {
            val encAsset = requireNotNull(entries[asset.path])
            val aad = "MOTOVAULT-ASSET-v1:${asset.assetId}".encodeToByteArray()
            val plain = BackupCrypto.decrypt(key, encAsset, aad)
            assetWriter(asset.assetId, plain)
        }

        // 2. Prepare new VaultState
        val targetAppearance = runCatching { Appearance.valueOf(preview.payload.appearance) }.getOrDefault(Appearance.SYSTEM)
        val restoredState = preview.payload.preferences.applyTo(
            VaultState(
                ride = preview.payload.ride,
                rules = preview.payload.rules,
                logs = preview.payload.logs,
                documents = preview.payload.documents,
                appearance = targetAppearance,
                autoLocation = preview.payload.autoLocation,
                economy = VaultState.calculateEconomy(preview.payload.logs),
                lastFuelOdometer = preview.payload.logs.filter { it.isFuel }.maxOfOrNull { it.odometer }
            )
        )

        // 3. Atomically replace database records
        database.vault().replace(restoredState, legacyImported = preview.payload.legacyImported)
        database.vault().prepareLocalSync()

        // 4. Reconcile reminders
        runCatching { reconcileRenewalReminders(restoredState.documents) }

        // 5. Trigger session refresh
        onComplete(restoredState)
    }
}
