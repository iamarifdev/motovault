package com.motovault.app.domain.backup

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.ui.Ride
import com.motovault.app.ui.RideLog
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.VaultSettings
import kotlinx.serialization.Serializable

@Serializable
data class BackupKdfParams(
    val algorithm: String = "PBKDF2-HMAC-SHA256",
    val iterations: Int = 100_000,
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
    val ride: Ride? = null,
    val rules: List<ComponentRule> = emptyList(),
    val logs: List<RideLog> = emptyList(),
    val documents: List<VaultDocument> = emptyList(),
    val preferences: VaultSettings = VaultSettings(),
    val appearance: String = "SYSTEM",
    val autoLocation: Boolean = false,
    val legacyImported: Boolean = false
)

data class BackupConflictSummary(
    val currentBikeDisplayName: String?,
    val incomingBikeDisplayName: String,
    val currentOdometer: Double?,
    val incomingOdometer: Double,
    val currentLogCount: Int,
    val incomingLogCount: Int,
    val currentDocumentCount: Int,
    val incomingDocumentCount: Int,
    val bikeMatches: Boolean
)

data class BackupPreview(
    val manifest: BackupManifest,
    val payload: VaultExportPayload,
    val validatedAssetCount: Int,
    val totalAssetBytes: Long,
    val conflicts: BackupConflictSummary? = null
)

class BackupSecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)


