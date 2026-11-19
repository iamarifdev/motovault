package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.*
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.application.fuel.*
import com.motovault.app.domain.garage.*
import com.motovault.app.domain.privacy.VaultAccessMode
import com.motovault.app.ui.privacy.PrivacyGate
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.*

@Composable
internal fun MotoVault(
    session: VaultWriter<VaultState>,
    backupRestore: VaultBackupRestorer,
    fuelEntry: FuelEntryBuilder<VaultState>,
    fuelEditor: FuelEntryEditor<VaultState>,
    recovery: VaultRecovery,
    backupOperations: VaultBackupOperations<VaultState>,
    captureDrafts: CaptureDraftFactory
) {
    val snapshot by session.snapshots.collectAsState()
    val state = snapshot.state
    rememberReminderAccess()
    val revision = snapshot.revision
    val loading = snapshot.loading
    var tab by remember { mutableStateOf(initialTab()) }
    var editingRide by remember { mutableStateOf(LaunchConfig.initialOnboarding) }
    var previewBrandId by remember { mutableStateOf<String?>(null) }
    var activeSheet by remember { mutableStateOf(LaunchConfig.initialSheet?.let { ActiveSheet(it) }) }
    var quickShortcutOpen by remember { mutableStateOf(false) }
    var currentToast by remember { mutableStateOf<ToastData?>(null) }
    var restoreSuccessVisible by remember { mutableStateOf(false) }
    var fuelEditError by remember { mutableStateOf<String?>(null) }
    var fuelCreateError by remember { mutableStateOf<FuelCreateError?>(null) }
    val odometerCapture = rememberOdometerCaptureState()
    val scope = rememberCoroutineScope()
    val fuelWorkflow = remember(session, fuelEntry, fuelEditor) { FuelWorkflow(session, fuelEntry, fuelEditor) }
    val text = vaultUiText(); val authMessages = rememberAuthMessages()
    val captureRejected = appStringResource(Res.string.capture_duplicate)
    val access = rememberVaultAccessController(state, text, authMessages) { activeSheet = null }

    fun commit(next: VaultState, onDone: ((Boolean) -> Unit)? = null) {
        val prepared = runCatching { next.acceptPrefill(activeSheet?.props as? CapturePrefill) }.getOrElse {
            currentToast = ToastData(message = captureRejected); onDone?.invoke(false); return
        }
        session.commit(prepared, revision) { ok -> if (scope.isActive) onDone?.invoke(ok) }
    }
    fun undo(savedRevision: Long) {
        session.undo(savedRevision) { ok ->
            val canRetry = session.snapshots.value.revision == savedRevision
            currentToast = if (ok) null else ToastData(
                id = "undo_failed_${Clock.System.now().toEpochMilliseconds()}",
                message = if (canRetry) text.undoFailedRetry else text.undoFailedChanged,
                undoAction = if (canRetry) { { undo(savedRevision) } } else null
            )
        }
    }

    MotoVaultEffects(
        state, loading, access.mode, activeSheet, editingRide, odometerCapture, text.reminderGone,
        onClearToast = { currentToast = null; restoreSuccessVisible = false },
        onToast = { currentToast = it }, onTab = { tab = it },
        onSheet = { activeSheet = it }, onCommit = ::commit
    )
    val activeBrandId = state.brandOverride ?: previewBrandId ?: state.ride?.brandId ?: "yamaha"
    val washActive = state.ride != null && !editingRide
    AppResourceEnvironment(state.language) {
        val restoreMessage = appStringResource(Res.string.restore_success)
        val effectiveToast = if (restoreSuccessVisible) ToastData(message = restoreMessage) else currentToast
        val dismissToast = { restoreSuccessVisible = false; currentToast = null }
        VaultTheme(activeBrandId, state.appearance, state.reduceMotion, state.textScale, state.digits, state.language, state.haptics) {
            PlatformAppearance(LocalVaultPalette.current.dark)
            if (state.privacy.enabled && (access.mode == VaultAccessMode.LOCKED || access.mode == VaultAccessMode.INSPECTION)) {
                PrivacyGate(
                    access.mode, state.documents.find { it.id == access.selectedInspectionDocId },
                    { access.unlock() }, { access.exitInspection() }, { access.lockNow() }, access.authMessage, state.language
                )
            } else {
                CompositionLocalProvider(LocalWashActive provides washActive, LocalSheetToast provides SheetToastBinding(effectiveToast, dismissToast)) {
                    MotoVaultShell(
                        state, revision, loading, snapshot.restorationFailed, snapshot.error, editingRide, tab,
                        activeSheet, quickShortcutOpen, washActive, effectiveToast, text, captureDrafts, backupOperations,
                        session, backupRestore, recovery, fuelWorkflow, access, odometerCapture, fuelCreateError, fuelEditError,
                        onTab = { quickShortcutOpen = false; tab = it }, onSheet = { quickShortcutOpen = false; activeSheet = it },
                        onQuickShortcutOpen = { quickShortcutOpen = it }, onEditingRide = { editingRide = it; if (!it) previewBrandId = null },
                        onPreviewBrand = { previewBrandId = it }, onCommit = ::commit, onUndo = ::undo,
                        onFuelCreateError = { fuelCreateError = it }, onFuelEditError = { fuelEditError = it },
                        onToast = { currentToast = it }, onDismissToast = dismissToast, onRestoreSuccess = { restoreSuccessVisible = true }
                    )
                }
            }
        }
    }
}

private fun initialTab(): Tab = when (LaunchConfig.initialTab?.lowercase()) {
    "vault" -> Tab.VAULT
    "history" -> Tab.HISTORY
    "me" -> Tab.ME
    else -> Tab.GARAGE
}

@Composable
private fun MotoVaultEffects(
    state: VaultState, loading: Boolean, accessMode: VaultAccessMode, activeSheet: ActiveSheet?,
    editingRide: Boolean, odometerCapture: OdometerCaptureState, reminderGoneText: String,
    onClearToast: () -> Unit, onToast: (ToastData?) -> Unit, onTab: (Tab) -> Unit,
    onSheet: (ActiveSheet?) -> Unit, onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit
) {
    LaunchedEffect(activeSheet?.name, editingRide) {
        if (activeSheet != null) onClearToast()
        odometerCapture.close()
    }
    CaptureIntake(enabled = !loading && state.ride != null && accessMode == VaultAccessMode.OWNER && activeSheet == null && !editingRide) {
        onSheet(ActiveSheet("capture", it))
    }
    val reminderRequest by ReminderNavigation.requests.collectAsState()
    LaunchedEffect(reminderRequest, loading) {
        val payload = reminderRequest ?: return@LaunchedEffect
        if (loading || (state.privacy.enabled && accessMode == VaultAccessMode.LOCKED)) return@LaunchedEffect
        val target = decodeReminderPayload(payload)?.let { reminderTarget(state, it) }
        if (target == null) onToast(ToastData(message = reminderGoneText))
        else {
            if (target.vehicleId != null && target.vehicleId != state.ride?.id) {
                runCatching { state.switchToBike(target.vehicleId) }.getOrNull()?.let { onCommit(it, null) }
            }
            if (target.type == "document") { onTab(Tab.VAULT); onSheet(ActiveSheet("docView", target.id)) }
            else { onTab(Tab.GARAGE); onSheet(ActiveSheet("service", target.id)) }
        }
        ReminderNavigation.handled(payload)
    }
    WidgetNavigationEffect(state, loading, accessMode, onCommit = { onCommit(it, null) }, onTab = onTab, onSheet = onSheet)
    LaunchedEffect(LaunchConfig.initialSeed, loading) {
        if (LaunchConfig.initialSeed && !loading && LaunchConfig.consumeSeed()) onCommit(VaultState.seedData(today()), null)
    }
}

@Composable
private fun MotoVaultShell(
    state: VaultState, revision: Long, loading: Boolean, restorationFailed: Boolean, storageError: VaultError?,
    editingRide: Boolean, tab: Tab, activeSheet: ActiveSheet?, quickShortcutOpen: Boolean, washActive: Boolean,
    effectiveToast: ToastData?, text: VaultUiText, captureDrafts: CaptureDraftFactory,
    backupOperations: VaultBackupOperations<VaultState>, session: VaultWriter<VaultState>, backupRestore: VaultBackupRestorer,
    recovery: VaultRecovery, fuelWorkflow: FuelWorkflow<VaultState>, access: VaultAccessController,
    odometerCapture: OdometerCaptureState, fuelCreateError: FuelCreateError?, fuelEditError: String?,
    onTab: (Tab) -> Unit, onSheet: (ActiveSheet?) -> Unit, onQuickShortcutOpen: (Boolean) -> Unit,
    onEditingRide: (Boolean) -> Unit, onPreviewBrand: (String?) -> Unit,
    onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit, onUndo: (Long) -> Unit,
    onFuelCreateError: (FuelCreateError?) -> Unit, onFuelEditError: (String?) -> Unit,
    onToast: (ToastData?) -> Unit, onDismissToast: () -> Unit, onRestoreSuccess: () -> Unit
) {
    val palette = LocalVaultPalette.current
    val isOnboarding = !loading && !restorationFailed && (state.ride == null || editingRide)
    CompositionLocalProvider(LocalOdometerCapture provides odometerCapture) {
        Box(
            modifier = Modifier.fillMaxSize().background(CanvasColor).screenBrandWash(washActive, palette, isReducedMotion),
            contentAlignment = Alignment.TopCenter
        ) {
            val frameInsets = if (isOnboarding) WindowInsets(0) else WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().windowInsetsPadding(frameInsets)
                    .then(if (activeSheet != null) Modifier.excludeFromAccessibility() else Modifier)
            ) {
                StorageErrorBanner(storageError, isOnboarding)
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandColor) }
                } else if (!restorationFailed && isOnboarding) {
                    MotoVaultOnboarding(state, text, onEditingRide, onPreviewBrand, onTab, onCommit)
                } else if (!restorationFailed) {
                    VaultTabContent(
                        tab, state, revision, text.recordDeleted, onSheet, onTab, onCommit,
                        { log -> onFuelEditError(null); onSheet(ActiveSheet("fuelEdit", FuelEditTarget(log, revision))) },
                        onUndo, onToast
                    )
                    VaultBottomNavigation(tab, onTab, { onSheet(ActiveSheet("quickAdd")) }, { onQuickShortcutOpen(true) })
                }
            }
            QuickShortcutPop(quickShortcutOpen && activeSheet == null, { onQuickShortcutOpen(false) }, { flow -> onSheet(ActiveSheet(flow)) })
            VaultToastHost(if (activeSheet == null) effectiveToast else null, locked = false, onDismissToast, Modifier.align(Alignment.BottomCenter))
            VaultSheetHost(
                activeSheet, captureDrafts, state, revision, loading, backupOperations, session, backupRestore, recovery,
                fuelWorkflow, access.policy, fuelCreateError, fuelEditError, onSheet, onCommit, onUndo, onFuelCreateError,
                onFuelEditError, onToast, onRestoreSuccess, { access.inspectDocument(it) }, { access.lockNow() }, { onEditingRide(true) }, onTab
            )
        }
    }
}

@Composable
private fun StorageErrorBanner(storageError: VaultError?, applyTopInset: Boolean) {
    storageError ?: return
    val insetModifier = if (applyTopInset) Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)) else Modifier
    Text(
        text = appStringResource(when (storageError) {
            VaultError.SAVE_FAILED -> Res.string.storage_save_failed
            VaultError.RESTORE_FAILED -> Res.string.storage_restore_failed
        }),
        color = ErrorColor,
        modifier = insetModifier.fillMaxWidth().padding(16.dp),
        fontSize = 13.sp
    )
}

@Composable
private fun MotoVaultOnboarding(
    state: VaultState, text: VaultUiText, onEditingRide: (Boolean) -> Unit,
    onPreviewBrand: (String?) -> Unit, onTab: (Tab) -> Unit, onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit
) = OnboardingScreen(
    onDismiss = if (state.ride != null) { { onEditingRide(false) } } else null,
    onPreviewBrand = onPreviewBrand,
    onComplete = { result ->
        val currentDay = today()
        val ride = Ride(
            brandId = result.brandId, edition = text.defaultEdition,
            year = result.year, odometer = result.odometer, modelId = result.modelId,
            modelName = result.modelName, market = RideMarket.UNKNOWN, emission = RideEmission.UNKNOWN,
            modelCode = "", factoryProfileId = null,
            odometerConfirmedDate = currentDay.toString(), cc = result.cc
        )
        val nextId = if (state.ride == null) "primary" else "bike-${currentDay.toEpochDays()}-${state.allBikes().size + 1}"
        val next = runCatching {
            val created = VaultState.create(ride.copy(id = nextId, archived = false), result.baselineKm, result.baselineDate, result.baselineCertainty)
            state.completeBikeOnboarding(created)
        }.getOrNull() ?: return@OnboardingScreen
        onCommit(next) { ok -> if (ok) { onEditingRide(false); onTab(Tab.GARAGE) } }
    }
)
