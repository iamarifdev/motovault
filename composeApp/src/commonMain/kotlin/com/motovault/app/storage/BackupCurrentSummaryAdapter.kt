package com.motovault.app.storage

import com.motovault.app.domain.backup.BackupCurrentSummary
import com.motovault.app.domain.backup.BackupSnapshot
import com.motovault.app.domain.backup.VaultExportPayload
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.vault.VaultSettings
import com.motovault.app.domain.model.assetIds

/** Maps the legacy persisted state to the data needed by archive preview. */
internal fun VaultState.backupCurrentSummary() = BackupCurrentSummary(
    bikeBrandId = ride?.brandId,
    bikeDisplayName = ride?.modelName?.ifBlank { "Motorcycle" },
    odometer = ride?.odometer,
    logCount = logs.size,
    documentCount = documents.size
)

internal fun VaultState.backupSnapshot() = BackupSnapshot(
    payload = VaultExportPayload(
        version = 1, ride = ride?.toBackupRide(), rules = rules, logs = logs.map { it.toBackupLog() }, documents = documents.map { it.toBackupDocument() },
        preferences = VaultSettings.from(this).toBackupPreferences(), appearance = appearance.name,
        autoLocation = autoLocation, legacyImported = false
    ),
    assetIds = documents.flatMap { it.assetIds() }.toSet(),
    bikeDisplayName = ride?.modelName?.ifBlank { "Motorcycle" } ?: "Motorcycle",
    odometer = ride?.odometer ?: 0.0
)
