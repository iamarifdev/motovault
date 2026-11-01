package com.motovault.app.storage

import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.model.assetIds
import com.motovault.app.application.RecoverySummary

// Current recovery IDs use the existing record ID format followed by the envelope's file suffix.
private val RECOVERY_ID_PATTERN = Regex("^([0-9]{13})-(-?[0-9]{1,19})\\.pdf$")
internal fun recoveryCreatedAt(id: String): Long? = RECOVERY_ID_PATTERN.matchEntire(id)?.groupValues?.get(1)?.toLongOrNull()

/** Keeps prior vault records and their asset references available after a restore. */
internal class RecoverySnapshotStore(
    private val dao: VaultDao,
    private val newId: () -> String,
    private val seal: (String, ByteArray) -> ByteArray,
    private val open: (String, ByteArray) -> ByteArray
) {
    suspend fun replace(previous: VaultState, next: VaultState) {
        // Existing snapshots must remain readable before any key-creation path can run.
        referencedAssets()
        val id = newId()
        val serialized = VaultState.encode(previous)
        val sealed = seal(id, serialized.encodeToByteArray())
        check(open(id, sealed).decodeToString() == serialized) { "Recovery snapshot verification failed." }
        dao.replaceWithRecovery(next, id, serialized, sealed)
    }

    suspend fun referencedAssets(): Set<String> = dao.restoreRecoveryRows().flatMap { row ->
        val plaintext = open(row.id, row.payload).decodeToString()
        requireNotNull(VaultState.decode(plaintext)) { "Recovery snapshot could not be read." }
            .documents.flatMap { it.assetIds() }
    }.toSet()

    suspend fun read(id: String): VaultState {
        val row = requireNotNull(dao.restoreRecoveryRows().singleOrNull { it.id == id }) { "Recovery snapshot not found." }
        return requireNotNull(VaultState.decode(open(row.id, row.payload).decodeToString())) { "Recovery snapshot could not be read." }
    }

    suspend fun summaries(): List<RecoverySummary> = dao.restoreRecoveryRows().map { row ->
        val snapshot = requireNotNull(VaultState.decode(open(row.id, row.payload).decodeToString())) { "Recovery snapshot could not be read." }
        RecoverySummary(
            id = row.id,
            createdAtEpochMillis = recoveryCreatedAt(row.id),
            bikeName = snapshot.ride?.modelName?.takeIf { it.isNotBlank() },
            recordCount = snapshot.logs.size,
            documentCount = snapshot.documents.size
        )
    }.sortedWith(compareByDescending<RecoverySummary> { it.createdAtEpochMillis }.thenBy { it.id })
}
