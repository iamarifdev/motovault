package com.motovault.app.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class VaultError { RESTORE_FAILED, SAVE_FAILED }

internal data class VaultSnapshot<T>(
    val state: T,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val restorationFailed: Boolean = false,
    val error: VaultError? = null,
    val revision: Long = 0
)

/** Application-facing contract; callers cannot access the storage adapter. */
internal interface VaultWriter<T> {
    val snapshots: StateFlow<VaultSnapshot<T>>
    fun commit(next: T, expectedRevision: Long, completed: (Boolean) -> Unit)
    fun undo(expectedRevision: Long, completed: (Boolean) -> Unit)
    fun commitPrepared(
        expectedRevision: Long,
        prepare: suspend () -> PreparedVaultChange<T>,
        completed: (Result<Unit>) -> Unit
    )
}

/** Owns only resources created during preparation, never previously committed assets. */
internal data class PreparedVaultChange<T>(
    val state: T,
    val preservePreviousAssets: Boolean = false,
    val discard: suspend () -> Unit
)

internal class VaultWriteConflict : IllegalStateException()

/** One authoritative durable writer; calls are serialized on its owning dispatcher. */
internal class VaultCoordinator<T>(
    private val scope: CoroutineScope,
    private val initialState: T,
    restore: suspend () -> T,
    private val write: suspend (T) -> Unit,
    private val assetIds: (T) -> Set<String>,
    private val cleanup: (Set<String>) -> Unit,
    recover: suspend (Set<String>) -> Unit,
    private val reconcile: suspend (T) -> Unit,
    private val writePrepared: suspend (T, T) -> Unit = { _, next -> write(next) },
    private val recoveryAssetIds: suspend () -> Set<String> = { emptySet() }
) : VaultWriter<T> {
    private val current = MutableStateFlow(VaultSnapshot(state = initialState))
    override val snapshots = current.asStateFlow()
    private var undoState: T? = null
    private var preservedUndoAssets: Set<String> = emptySet()

    override fun undo(expectedRevision: Long, completed: (Boolean) -> Unit) {
        val inverse = undoState
        if (inverse == null || current.value.revision != expectedRevision) {
            completed(false)
            return
        }
        commit(inverse, expectedRevision, completed)
    }

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val state = restore()
                try { recover(assetIds(state) + recoveryAssetIds()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep intact records available if cleanup fails. */ }
                current.value = VaultSnapshot(state = state, loading = false)
                try { reconcile(state) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Reminder failures do not invalidate restored records. */ }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                current.value = VaultSnapshot(state = initialState, loading = false, restorationFailed = true, error = VaultError.RESTORE_FAILED)
            }
        }
    }

    override fun commit(next: T, expectedRevision: Long, completed: (Boolean) -> Unit) {
        val before = current.value
        if (before.loading || before.restorationFailed || before.saving || before.revision != expectedRevision) {
            completed(false)
            return
        }
        current.value = before.copy(saving = true)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            // Once persistence starts, publish its result even if the owning UI scope is cancelled.
            val saved = withContext(NonCancellable) {
                try {
                    write(next)
                } catch (_: Exception) {
                    current.value = before.copy(error = VaultError.SAVE_FAILED)
                    completed(false)
                    return@withContext false
                }
                val retained = assetIds(before.state) + assetIds(next)
                val removed = undoState?.let(assetIds).orEmpty() - retained - preservedUndoAssets
                undoState = before.state
                preservedUndoAssets = emptySet()
                current.value = before.copy(state = next, saving = false, error = null, revision = before.revision + 1)
                try { cleanup(removed - recoveryAssetIds()) } catch (_: Exception) { /* Keep files when recovery inventory is unavailable. */ }
                completed(true)
                true
            }
            if (!saved) return@launch
            try { reconcile(next) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Reconciliation failure does not change a successful save. */ }
        }
    }

    override fun commitPrepared(
        expectedRevision: Long,
        prepare: suspend () -> PreparedVaultChange<T>,
        completed: (Result<Unit>) -> Unit
    ) {
        val before = current.value
        if (before.loading || before.restorationFailed || before.saving || before.revision != expectedRevision) {
            completed(Result.failure(VaultWriteConflict()))
            return
        }
        // Reserve the writer before staging files, so no normal save or Undo can race it.
        current.value = before.copy(saving = true)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val prepared = try {
                prepare()
            } catch (cancelled: CancellationException) {
                current.value = before
                throw cancelled
            } catch (failure: Exception) {
                current.value = before
                completed(Result.failure(failure))
                return@launch
            }
            // Do not report cancellation after Room has committed and then discard referenced files.
            withContext(NonCancellable) {
                try {
                    if (prepared.preservePreviousAssets) writePrepared(before.state, prepared.state) else write(prepared.state)
                } catch (failure: Exception) {
                    try { prepared.discard() } catch (cleanupFailure: Exception) {
                        failure.addSuppressed(cleanupFailure)
                    }
                    current.value = before.copy(error = VaultError.SAVE_FAILED)
                    completed(Result.failure(failure))
                    return@withContext
                }
                val retained = assetIds(before.state) + assetIds(prepared.state)
                val removed = undoState?.let(assetIds).orEmpty() - retained - preservedUndoAssets
                undoState = before.state
                preservedUndoAssets = if (prepared.preservePreviousAssets) assetIds(before.state) else emptySet()
                current.value = before.copy(state = prepared.state, saving = false, error = null, revision = before.revision + 1)
                try { cleanup(removed - recoveryAssetIds()) } catch (_: Exception) { /* Keep files when recovery inventory is unavailable. */ }
                completed(Result.success(Unit))
                try { reconcile(prepared.state) }
                catch (_: Exception) { /* Reconciliation cannot undo a durable commit. */ }
            }
        }
    }
}
