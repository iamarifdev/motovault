package com.motovault.app.application

/** A restore is authorized against the revision shown in its preview. */
internal interface VaultBackupRestorer {
    fun restore(archive: ByteArray, passphrase: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit)
}

/** Restores an authenticated local recovery snapshot against the displayed vault revision. */
internal interface VaultRecoveryRestorer {
    fun restore(recoveryId: String, expectedRevision: Long, completed: (Result<Unit>) -> Unit)
}

internal data class RecoverySummary(
    val id: String,
    val createdAtEpochMillis: Long?,
    val bikeName: String?,
    val recordCount: Int,
    val documentCount: Int
)

internal interface VaultRecoveryCatalog {
    suspend fun list(): List<RecoverySummary>
}

internal interface VaultRecovery : VaultRecoveryCatalog, VaultRecoveryRestorer
