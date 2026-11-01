package com.motovault.app.domain.backup

import kotlinx.serialization.json.Json

object RestoreUseCase {
    private val json = Json { ignoreUnknownKeys = true }

    fun validateAndPreview(
        digest: (ByteArray) -> ByteArray,
        rideDisplayName: (BackupRide) -> String,
        archiveBytes: ByteArray,
        passphrase: String,
        currentState: BackupCurrentSummary? = null
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

        require(manifest.kdf.iterations in 1..MAX_RESTORE_KDF_ITERATIONS) {
            "Backup key derivation work is outside the supported range."
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
        val payloadAad = vaultAuthenticationData(manifest.createdAtEpochMillis)
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
        require(manifestAssetIds.size == manifest.assets.size) { "Backup contains duplicate asset IDs." }
        require(manifest.assets.map { it.path }.toSet().size == manifest.assets.size) { "Backup contains duplicate asset paths." }

        for (asset in manifest.assets) {
            val encAsset = requireNotNull(entries[asset.path]) {
                "Missing referenced asset file in backup: ${asset.path}"
            }
            val aad = assetAuthenticationData(asset.assetId)
            val plain = try {
                BackupCrypto.decrypt(key, encAsset, aad)
            } catch (e: Exception) {
                throw BackupSecurityException("Failed to decrypt asset ${asset.assetId}. Archive may be damaged or tampered.", e)
            }
            require(plain.size == asset.byteLength) {
                "Asset length mismatch for ${asset.assetId}: manifest specifies ${asset.byteLength}, extracted ${plain.size}."
            }
            val sha = digest(plain).toHex()
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

        // Manifest summary is plaintext and is not covered by the encrypted payload's tag.
        val verifiedSummary = BackupCurrentSummary(
            bikeBrandId = payload.ride?.brandId,
            bikeDisplayName = payload.ride?.let(rideDisplayName),
            odometer = payload.ride?.odometer,
            logCount = payload.logs.size,
            documentCount = payload.documents.size
        )
        val conflicts = currentState?.let { current ->
            BackupConflictSummary(
                currentBikeDisplayName = current.bikeDisplayName,
                incomingBikeDisplayName = verifiedSummary.bikeDisplayName,
                currentOdometer = current.odometer,
                incomingOdometer = verifiedSummary.odometer ?: 0.0,
                currentLogCount = current.logCount,
                incomingLogCount = verifiedSummary.logCount,
                currentDocumentCount = current.documentCount,
                incomingDocumentCount = verifiedSummary.documentCount,
                bikeMatches = current.bikeBrandId == verifiedSummary.bikeBrandId && current.bikeDisplayName == verifiedSummary.bikeDisplayName
            )
        }

        return BackupPreview(
            manifest = manifest,
            payload = payload,
            verifiedSummary = verifiedSummary,
            validatedAssetCount = manifest.assets.size,
            totalAssetBytes = validatedAssetBytes,
            conflicts = conflicts
        )
    }

}
