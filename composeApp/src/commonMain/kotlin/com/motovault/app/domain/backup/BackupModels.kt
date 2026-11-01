package com.motovault.app.domain.backup

import com.motovault.app.domain.model.ComponentRule
import kotlinx.serialization.Serializable

@Serializable
data class BackupKdfParams(
    val algorithm: String = "PBKDF2-HMAC-SHA256",
    val iterations: Int = BACKUP_KDF_ITERATIONS,
    val saltHex: String
)

@Serializable
data class BackupCipherParams(
    val algorithm: String = "AES-256-GCM",
    val tagLengthBits: Int = 128
)

@Serializable
data class BackupSummary(
    val bikeDisplayName: String,
    val odometer: Double,
    val logCount: Int,
    val documentCount: Int,
    val totalAssetBytes: Long
)

@Serializable
data class BackupAssetEntry(
    val assetId: String,
    val path: String,
    val byteLength: Int,
    val sha256: String,
    val nonceHex: String
)

@Serializable
data class BackupManifest(
    val formatVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val buildRevision: String = "1.0",
    val createdAtEpochMillis: Long,
    val installationId: String,
    val vaultRevision: Long,
    val kdf: BackupKdfParams,
    val cipher: BackupCipherParams = BackupCipherParams(),
    val summary: BackupSummary,
    val assets: List<BackupAssetEntry> = emptyList()
)

@Serializable
data class VaultExportPayload(
    val version: Int = 1,
    val ride: BackupRide? = null,
    val rules: List<ComponentRule> = emptyList(),
    val logs: List<BackupLog> = emptyList(),
    val documents: List<BackupDocument> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences(),
    val appearance: String = "SYSTEM",
    val autoLocation: Boolean = false,
    val legacyImported: Boolean = false
)

data class BackupConflictSummary(
    val currentBikeDisplayName: String?,
    val incomingBikeDisplayName: String?,
    val currentOdometer: Double?,
    val incomingOdometer: Double,
    val currentLogCount: Int,
    val incomingLogCount: Int,
    val currentDocumentCount: Int,
    val incomingDocumentCount: Int,
    val bikeMatches: Boolean
)

/** Only the current values needed to compare an incoming archive. */
data class BackupCurrentSummary(
    val bikeBrandId: String?,
    val bikeDisplayName: String?,
    val odometer: Double?,
    val logCount: Int,
    val documentCount: Int
)

/** Frozen export payload and metadata supplied by the legacy-state adapter. */
data class BackupSnapshot(
    val payload: VaultExportPayload,
    val assetIds: Set<String>,
    val bikeDisplayName: String,
    val odometer: Double
)

enum class BackupProgressStage { PREPARING, CHECKING_ASSETS, DERIVING_KEY, ENCRYPTING_ASSETS, ENCRYPTED_ASSETS, ENCRYPTING_VAULT, WRITING_ARCHIVE, READY }

data class BackupProgress(val stage: BackupProgressStage, val completed: Int = 0, val total: Int = 0)

data class BackupPreview(
    val manifest: BackupManifest,
    val payload: VaultExportPayload,
    val verifiedSummary: BackupCurrentSummary,
    val validatedAssetCount: Int,
    val totalAssetBytes: Long,
    val conflicts: BackupConflictSummary? = null
)

class BackupSecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)


