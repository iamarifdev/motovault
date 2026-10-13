package com.motovault.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.motovault.app.ui.redesign.VaultState
import com.motovault.app.ui.redesign.assetIds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Local preparation only. There is deliberately no account assignment or network worker. */
@Entity(tableName = "sync_outbox")
data class SyncOutboxRow(
    @PrimaryKey val id: Int = 1,
    val installationId: String,
    val revision: Long,
    val payload: String,
    val tombstones: String,
    val attemptCount: Int = 0,
    val retryAfterEpochMillis: Long = 0,
    val failureCode: String? = null
) {
    init {
        require(id == 1 && installationId.isNotBlank() && revision > 0)
        require(attemptCount in 0..30 && retryAfterEpochMillis >= 0)
        require(failureCode == null || failureCode in setOf("network", "unavailable", "conflict", "authentication", "encryption"))
    }
    val operationId: String get() = "$installationId:$revision"
    val backupStatus: String get() = "Backup is not connected"
}

@Serializable
data class SyncTombstones(
    val documents: List<String> = emptyList(),
    val ledger: List<String> = emptyList(),
    val schedules: List<String> = emptyList(),
    val assets: List<String> = emptyList(),
    val vehicleRemoved: Boolean = false
)

internal object LocalSyncProtocol {
    private val codec = Json { ignoreUnknownKeys = true }
    fun tombstones(row: SyncOutboxRow?): SyncTombstones = row?.let { codec.decodeFromString(it.tombstones) } ?: SyncTombstones()
    fun next(previous: SyncOutboxRow?, before: VaultState?, next: VaultState, installationId: String): SyncOutboxRow {
        val old = tombstones(previous)
        fun removed(retained: List<String>, prior: List<String>, current: List<String>) =
            ((retained + (prior.toSet() - current.toSet())) - current.toSet()).distinct().sorted()
        val deleted = SyncTombstones(
            documents = removed(old.documents, before?.documents.orEmpty().map { it.id }, next.documents.map { it.id }),
            ledger = removed(old.ledger, before?.logs.orEmpty().map { it.id }, next.logs.map { it.id }),
            schedules = removed(old.schedules, before?.rules.orEmpty().map { it.id }, next.rules.map { it.id }),
            assets = removed(old.assets, before?.documents.orEmpty().flatMap { it.assetIds() }, next.documents.flatMap { it.assetIds() }),
            vehicleRemoved = next.ride == null && (old.vehicleRemoved || before?.ride != null)
        )
        check(previous == null || previous.revision < Long.MAX_VALUE) { "Local revision limit reached." }
        return SyncOutboxRow(installationId = previous?.installationId ?: installationId,
            revision = (previous?.revision ?: 0) + 1, payload = VaultState.encode(next), tombstones = codec.encodeToString(deleted))
    }

    fun failed(row: SyncOutboxRow, now: Long, code: String): SyncOutboxRow {
        require(now >= 0 && now <= Long.MAX_VALUE - 3_600_000)
        require(code in setOf("network", "unavailable", "conflict", "authentication", "encryption"))
        val attempt = row.attemptCount.coerceIn(0, 29) + 1
        val delay = (1_000L shl (attempt - 1).coerceAtMost(12)).coerceAtMost(3_600_000)
        return row.copy(attemptCount = attempt, retryAfterEpochMillis = now + delay, failureCode = code)
    }
}

/** Implement only after reviewing authentication, authenticated encryption and recovery. */
interface EncryptedVaultTransport {
    suspend fun publish(request: EncryptedVaultRequest): PublishResult
}

data class EncryptedVaultRequest(
    val ownerId: String, val vaultId: String, val operationId: String,
    val expectedRemoteRevision: Long, val ciphertext: ByteArray,
    val keyVersion: Int, val encryptedAssetObjectIds: List<String>
)

sealed interface PublishResult {
    data class Accepted(val remoteRevision: Long, val operationId: String) : PublishResult
    data class Conflict(val remoteRevision: Long) : PublishResult
    data class Retry(val code: String) : PublishResult
}
