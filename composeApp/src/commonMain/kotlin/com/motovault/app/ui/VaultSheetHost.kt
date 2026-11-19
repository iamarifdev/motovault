package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import com.motovault.app.application.VaultBackupOperations
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.application.fuel.FuelWorkflow
import com.motovault.app.domain.garage.mutateBike
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.domain.privacy.VaultAccessPolicy

internal enum class FuelCreateError { SAVE_FAILED, CONFLICT }

@Composable
internal fun VaultSheetHost(
    activeSheet: ActiveSheet?,
    captureDrafts: CaptureDraftFactory,
    state: VaultState,
    revision: Long,
    loading: Boolean,
    backupOperations: VaultBackupOperations<VaultState>,
    session: VaultWriter<VaultState>,
    backupRestore: VaultBackupRestorer,
    recovery: VaultRecovery,
    fuelWorkflow: FuelWorkflow<VaultState>,
    accessPolicy: VaultAccessPolicy,
    fuelCreateError: FuelCreateError?,
    fuelEditError: String?,
    onSheet: (ActiveSheet?) -> Unit,
    onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    onUndo: (Long) -> Unit,
    onFuelCreateError: (FuelCreateError?) -> Unit,
    onFuelEditError: (String?) -> Unit,
    onToast: (ToastData?) -> Unit,
    onRestoreSuccess: () -> Unit,
    onInspect: (String) -> Unit,
    onLockNow: () -> Unit,
    onStartOnboarding: () -> Unit = {},
    onSwitchTab: (Tab) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val canShow = !loading && (
        state.ride != null ||
            activeSheet?.name in setOf("paste", "capture", "quickAdd")
        )
    if (!canShow || activeSheet == null) return

    val targetBikeId = (activeSheet.props as? CapturePrefill)?.targetBikeId
    val scopedState = if (targetBikeId != null && targetBikeId != state.ride?.id) {
        runCatching { state.switchToBike(targetBikeId) }.getOrDefault(state)
    } else state
    val commitScoped: (VaultState, ((Boolean) -> Unit)?) -> Unit = { nextScoped, done ->
        val merged = runCatching {
            if (targetBikeId == null) nextScoped else state.mutateBike(targetBikeId) { nextScoped }
        }.getOrNull()
        if (merged == null) done?.invoke(false) else onCommit(merged, done)
    }

    key(activeSheet.name, activeSheet.props) {
        val ctx = SheetRouteContext(
            sheet = activeSheet, state = state, scopedState = scopedState, revision = revision,
            captureDrafts = captureDrafts, backupOperations = backupOperations, session = session,
            backupRestore = backupRestore, recovery = recovery, fuelWorkflow = fuelWorkflow,
            accessPolicy = accessPolicy, fuelCreateError = fuelCreateError, fuelEditError = fuelEditError,
            scope = scope, onSwitchTab = onSwitchTab, onSheet = onSheet, onCommit = onCommit, onCommitScoped = commitScoped,
            onUndo = onUndo, onFuelCreateError = onFuelCreateError, onFuelEditError = onFuelEditError,
            onToast = onToast, onRestoreSuccess = onRestoreSuccess, onInspect = onInspect,
            onLockNow = onLockNow, onStartOnboarding = onStartOnboarding
        )
        when (activeSheet.name) {
            "quickAdd", "gaugeDetail", "odometer", "bikeSwitcher", "breakdown" -> RenderGarageSheet(ctx)
            "expense", "fuel", "fuelEdit", "service", "serviceEdit", "paste", "capture", "work-ledger" -> RenderEntrySheet(ctx)
            "docAdd", "docView", "renewal" -> RenderDocumentSheet(ctx)
            "settings", "reminders", "privacy", "backup", "backup-restore", "recovery", "cloud-backup" -> RenderSettingsSheet(ctx)
        }
    }
}
