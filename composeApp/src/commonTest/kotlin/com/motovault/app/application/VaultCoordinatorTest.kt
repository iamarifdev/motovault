package com.motovault.app.application

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultCoordinatorTest {
    private data class Record(val value: Int, val assets: Set<String> = emptySet())

    @Test
    fun restoreFailureDoesNotExposeExceptionText() = runBlocking {
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { error("private storage detail") },
            write = {}, assetIds = { it.assets }, cleanup = {}, recover = {}, reconcile = {}
        )
        assertTrue(coordinator.snapshots.value.restorationFailed)
        assertEquals(VaultError.RESTORE_FAILED, coordinator.snapshots.value.error)
    }

    @Test
    fun staleRevisionCannotOverwriteAndUndoCommitsPreviousState() = runBlocking {
        var durable = Record(1)
        var writes = 0
        val coordinator = VaultCoordinator(
            scope = this,
            initialState = Record(0),
            restore = { durable },
            write = { durable = it; writes++ },
            assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )

        var result: Boolean? = null
        coordinator.commit(Record(2), 0) { result = it }
        assertEquals(true, result)
        assertEquals(Record(2), durable)
        assertEquals(1, coordinator.snapshots.value.revision)
        coordinator.commit(Record(3), 0) { result = it }
        assertEquals(false, result)
        assertEquals(1, writes)
        coordinator.undo(1) { result = it }
        assertEquals(true, result)
        assertEquals(Record(1), durable)
        assertEquals(2, coordinator.snapshots.value.revision)
    }

    @Test
    fun pendingOrFailedSaveNeverReportsSuccessOrChangesDurableState() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var durable = Record(1)
        var firstResult: Boolean? = null
        val coordinator = VaultCoordinator(
            scope = this,
            initialState = Record(0),
            restore = { durable },
            write = { gate.await(); error("disk full") },
            assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )

        coordinator.commit(Record(2), 0) { firstResult = it }
        assertTrue(coordinator.snapshots.value.saving)
        var secondResult: Boolean? = null
        coordinator.commit(Record(3), 0) { secondResult = it }
        assertFalse(requireNotNull(secondResult))
        gate.complete(Unit)
        kotlinx.coroutines.yield()
        assertEquals(false, firstResult)
        assertEquals(Record(1), durable)
        assertEquals(Record(1), coordinator.snapshots.value.state)
        assertEquals(VaultError.SAVE_FAILED, coordinator.snapshots.value.error)
        assertEquals(0, coordinator.snapshots.value.revision)
    }

    @Test
    fun previousUndoAssetsAreRetiredOnlyAfterNewDurableCommit() = runBlocking {
        var durable = Record(1, setOf("old"))
        val retired = mutableListOf<Set<String>>()
        val coordinator = VaultCoordinator(
            scope = this,
            initialState = Record(0),
            restore = { durable },
            write = { durable = it },
            assetIds = { it.assets },
            cleanup = { retired += it }, recover = {}, reconcile = {}
        )

        coordinator.commit(Record(2, setOf("current")), 0) {}
        assertEquals(emptySet(), retired.single())
        coordinator.commit(Record(3, setOf("next")), 1) {}
        assertEquals(setOf("old"), retired.last())
    }

    @Test
    fun restoreKeepsReplacedOriginalsAfterUndoExpires() = runBlocking {
        var durable = Record(1, setOf("owner-original"))
        val retired = mutableSetOf<String>()
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { durable },
            write = { durable = it }, assetIds = { it.assets },
            cleanup = { retired += it }, recover = {}, reconcile = {}
        )
        var result: Result<Unit>? = null
        coordinator.commitPrepared(0, prepare = {
            PreparedVaultChange(Record(2, setOf("restored")), preservePreviousAssets = true, discard = {})
        }) { result = it }
        assertTrue(requireNotNull(result).isSuccess)
        coordinator.commit(Record(3, setOf("restored")), 1) {}
        assertFalse("owner-original" in retired)
        assertEquals(Record(3, setOf("restored")), durable)
    }

    @Test
    fun recoveryInventoryProtectsAssetsAndFailureSkipsCleanup() = runBlocking {
        var durable = Record(1, setOf("saved-recovery"))
        var inventoryAvailable = true
        val retired = mutableSetOf<String>()
        val recovered = mutableSetOf<String>()
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { durable },
            write = { durable = it }, assetIds = { it.assets },
            cleanup = { retired += it }, recover = { recovered += it }, reconcile = {},
            recoveryAssetIds = { check(inventoryAvailable); setOf("saved-recovery") }
        )
        assertTrue("saved-recovery" in recovered)
        coordinator.commit(Record(2, setOf("current")), 0) {}
        coordinator.commit(Record(3, setOf("next")), 1) {}
        assertFalse("saved-recovery" in retired)
        inventoryAvailable = false
        coordinator.commit(Record(4), 2) {}
        assertFalse("current" in retired)
    }

    @Test
    fun preparedRestoreUsesDedicatedWriterAndDiscardsWhenRecoveryWriteFails() = runBlocking {
        val original = Record(1, setOf("owner"))
        var durable = original
        var discarded = false
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { durable },
            write = { durable = it }, assetIds = { it.assets }, cleanup = {}, recover = {}, reconcile = {},
            writePrepared = { previous, _ -> assertEquals(original, previous); error("key unavailable") }
        )
        var result: Result<Unit>? = null
        coordinator.commitPrepared(0, prepare = {
            PreparedVaultChange(Record(2, setOf("staged")), preservePreviousAssets = true, discard = { discarded = true })
        }) { result = it }
        assertTrue(requireNotNull(result).isFailure)
        assertTrue(discarded)
        assertEquals(original, durable)
        assertEquals(original, coordinator.snapshots.value.state)
    }

    @Test
    fun preparedRestoreReservesWriterAndRejectsStaleChanges() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var durable = Record(1)
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { durable },
            write = { durable = it }, assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )
        var result: Result<Unit>? = null
        coordinator.commitPrepared(0, prepare = {
            gate.await()
            PreparedVaultChange(Record(2, setOf("restored")), discard = {})
        }) { result = it }
        assertTrue(coordinator.snapshots.value.saving)
        var concurrent: Boolean? = null
        coordinator.commit(Record(3), 0) { concurrent = it }
        assertEquals(false, concurrent)
        gate.complete(Unit)
        kotlinx.coroutines.yield()
        assertTrue(requireNotNull(result).isSuccess)
        assertEquals(Record(2, setOf("restored")), durable)
        assertEquals(1, coordinator.snapshots.value.revision)
        var stalePrepared = false
        coordinator.commitPrepared(0, prepare = {
            stalePrepared = true
            PreparedVaultChange(Record(4), discard = {})
        }) { result = it }
        assertFalse(stalePrepared)
        assertTrue(requireNotNull(result).exceptionOrNull() is VaultWriteConflict)
        coordinator.undo(1) { concurrent = it }
        assertEquals(true, concurrent)
        assertEquals(Record(1), durable)
    }

    @Test
    fun failedPreparedRestoreDiscardsOnlyItsStagingAndLeavesVaultUntouched() = runBlocking {
        val original = Record(1, setOf("owner"))
        var discarded = false
        val coordinator = VaultCoordinator(
            scope = this, initialState = Record(0), restore = { original },
            write = { error("disk full") }, assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )
        var result: Result<Unit>? = null
        coordinator.commitPrepared(0, prepare = {
            PreparedVaultChange(Record(2, setOf("staged")), discard = { discarded = true })
        }) { result = it }
        assertTrue(discarded)
        assertTrue(requireNotNull(result).isFailure)
        assertEquals(original, coordinator.snapshots.value.state)
        assertEquals(0, coordinator.snapshots.value.revision)
        assertFalse(coordinator.snapshots.value.saving)
    }

    @Test
    fun cancellationAfterDurableWriteBeginsDoesNotDiscardCommittedFiles() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val owner = Job()
        val scope = CoroutineScope(owner + Dispatchers.Unconfined)
        var durable = Record(1)
        var discarded = false
        var result: Result<Unit>? = null
        val coordinator = VaultCoordinator(
            scope = scope, initialState = Record(0), restore = { durable },
            write = { gate.await(); durable = it }, assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )
        coordinator.commitPrepared(0, prepare = {
            PreparedVaultChange(Record(2, setOf("restored")), discard = { discarded = true })
        }) { result = it }
        owner.cancel()
        gate.complete(Unit)
        assertEquals(Record(2, setOf("restored")), durable)
        assertEquals(Record(2, setOf("restored")), coordinator.snapshots.value.state)
        assertFalse(discarded)
        assertTrue(requireNotNull(result).isSuccess)
    }

    @Test
    fun cancellationDuringOrdinarySaveStillPublishesDurableResult() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val owner = Job()
        val scope = CoroutineScope(owner + Dispatchers.Unconfined)
        var durable = Record(1)
        var result: Boolean? = null
        val coordinator = VaultCoordinator(
            scope = scope, initialState = Record(0), restore = { durable },
            write = { gate.await(); durable = it }, assetIds = { it.assets },
            cleanup = {}, recover = {}, reconcile = {}
        )
        coordinator.commit(Record(2), 0) { result = it }
        assertTrue(coordinator.snapshots.value.saving)
        owner.cancel()
        gate.complete(Unit)
        assertEquals(Record(2), durable)
        assertEquals(Record(2), coordinator.snapshots.value.state)
        assertEquals(1, coordinator.snapshots.value.revision)
        assertFalse(coordinator.snapshots.value.saving)
        assertEquals(true, result)
    }

    @Test
    fun failedOrdinarySaveAfterCancellationClearsSavingWithoutChangingState() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val owner = Job()
        val coordinator = VaultCoordinator(
            scope = CoroutineScope(owner + Dispatchers.Unconfined), initialState = Record(0),
            restore = { Record(1) }, write = { gate.await(); error("disk full") },
            assetIds = { it.assets }, cleanup = {}, recover = {}, reconcile = {}
        )
        var result: Boolean? = null
        coordinator.commit(Record(2), 0) { result = it }
        owner.cancel()
        gate.complete(Unit)
        assertEquals(false, result)
        assertEquals(Record(1), coordinator.snapshots.value.state)
        assertEquals(0, coordinator.snapshots.value.revision)
        assertFalse(coordinator.snapshots.value.saving)
        assertEquals(VaultError.SAVE_FAILED, coordinator.snapshots.value.error)
    }
}
