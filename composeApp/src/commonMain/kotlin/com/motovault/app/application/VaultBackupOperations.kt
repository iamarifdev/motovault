package com.motovault.app.application

import com.motovault.app.domain.backup.BackupPreview
import com.motovault.app.domain.backup.BackupProgress

/** Backup creation and read-only preview; durable replacement remains with VaultBackupRestorer. */
internal interface VaultBackupOperations<T> {
    suspend fun create(state: T, revision: Long, password: String, progress: suspend (Float, BackupProgress) -> Unit): ByteArray
    suspend fun preview(state: T, archive: ByteArray, password: String): BackupPreview
}
