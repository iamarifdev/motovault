package com.motovault.app.storage

import com.motovault.app.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class VaultSnapshot(
    val state: VaultState = VaultState(), val loading: Boolean = true,
    val saving: Boolean = false, val restorationFailed: Boolean = false,
    val error: String? = null, val revision: Long = 0
)

/** One owner for the durable vault across UI recreation. Calls run on the UI dispatcher. */
internal class VaultSession(
    private val scope: CoroutineScope,
    restore: suspend () -> VaultState,
    private val write: suspend (VaultState) -> Unit,
    private val cleanup: (Set<String>) -> Unit,
    recover: suspend (Set<String>) -> Unit,
    private val reconcile: suspend (List<VaultDocument>) -> Unit = {}
) {
    private val current = MutableStateFlow(VaultSnapshot())
    val snapshots = current.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val state = restore()
                try { recover(state.documents.flatMap { it.assetIds() }.toSet()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Cleanup failure must not block intact owner records. */ }
                current.value = VaultSnapshot(state = state, loading = false)
                try { reconcile(state.documents) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Reminders never invalidate restored records. */ }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                current.value = VaultSnapshot(loading = false, restorationFailed = true, error = failure.message)
            }
        }
    }

    fun commit(next: VaultState, expectedRevision: Long, completed: (Boolean) -> Unit) {
        val before = current.value
        if (before.loading || before.restorationFailed || before.saving || before.revision != expectedRevision) {
            completed(false)
            return
        }
        // Register ownership before launching, so a recreated UI immediately observes the pending save.
        current.value = before.copy(saving = true)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                write(next)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                current.value = before.copy(error = "Could not save on this device. Your previous records are unchanged.")
                completed(false)
                return@launch
            }
            val removed = before.state.documents.flatMap { it.assetIds() }.toSet() - next.documents.flatMap { it.assetIds() }.toSet()
            current.value = before.copy(state = next, saving = false, error = null, revision = before.revision + 1)
            runCatching { cleanup(removed) }
            completed(true)
            try { reconcile(next.documents) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Reminder failure never changes a successful save. */ }
        }
    }
}

internal val activeVaultSession by lazy {
    VaultSession(
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        restore = { VaultState.restore() },
        write = { saveVault(VaultState.encode(it)) },
        cleanup = ::removeDocumentAssets,
        recover = ::recoverOrphanDocumentAssets,
        reconcile = ::reconcileRenewalReminders
    )
}
