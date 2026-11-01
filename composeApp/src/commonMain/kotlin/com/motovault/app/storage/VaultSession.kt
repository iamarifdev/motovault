package com.motovault.app.storage

import com.motovault.app.application.VaultCoordinator
import com.motovault.app.application.VaultWriter
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.model.assetIds
import kotlinx.coroutines.CoroutineScope

/** Compatibility adapter while persisted models remain in the UI package. */
internal class VaultSession(
    scope: CoroutineScope,
    restore: suspend () -> VaultState,
    write: suspend (VaultState) -> Unit,
    cleanup: (Set<String>) -> Unit,
    recover: suspend (Set<String>) -> Unit,
    reconcile: suspend (List<VaultDocument>) -> Unit = {},
    writePrepared: suspend (VaultState, VaultState) -> Unit = { _, next -> write(next) },
    recoveryAssetIds: suspend () -> Set<String> = { emptySet() }
) : VaultWriter<VaultState> by VaultCoordinator(
    scope = scope,
    initialState = VaultState(),
    restore = restore,
    write = write,
    assetIds = { state -> state.documents.flatMap { it.assetIds() }.toSet() },
    cleanup = cleanup,
    recover = recover,
    reconcile = { state -> reconcile(state.documents) },
    writePrepared = writePrepared,
    recoveryAssetIds = recoveryAssetIds
)
