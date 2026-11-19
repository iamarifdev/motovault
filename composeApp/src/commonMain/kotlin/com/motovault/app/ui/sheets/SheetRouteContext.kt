package com.motovault.app.ui

import com.motovault.app.application.VaultBackupOperations
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.application.fuel.FuelWorkflow
import com.motovault.app.domain.privacy.VaultAccessPolicy
import kotlinx.coroutines.CoroutineScope

internal class SheetRouteContext(
    val sheet: ActiveSheet,
    val state: VaultState,
    val scopedState: VaultState,
    val revision: Long,
    val captureDrafts: CaptureDraftFactory,
    val backupOperations: VaultBackupOperations<VaultState>,
    val session: VaultWriter<VaultState>,
    val backupRestore: VaultBackupRestorer,
    val recovery: VaultRecovery,
    val fuelWorkflow: FuelWorkflow<VaultState>,
    val accessPolicy: VaultAccessPolicy,
    val fuelCreateError: FuelCreateError?,
    val fuelEditError: String?,
    val scope: CoroutineScope,
    val onSwitchTab: (Tab) -> Unit,
    val onSheet: (ActiveSheet?) -> Unit,
    val onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    val onCommitScoped: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    val onUndo: (Long) -> Unit,
    val onFuelCreateError: (FuelCreateError?) -> Unit,
    val onFuelEditError: (String?) -> Unit,
    val onToast: (ToastData?) -> Unit,
    val onRestoreSuccess: () -> Unit,
    val onInspect: (String) -> Unit,
    val onLockNow: () -> Unit,
    val onStartOnboarding: () -> Unit
)
