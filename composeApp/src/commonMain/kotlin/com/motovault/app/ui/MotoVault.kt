package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.motovault.app.ui.backup.BackupSheet
import com.motovault.app.ui.fuel.FuelSheet
import com.motovault.app.ui.backup.BackupSheetTab
import com.motovault.app.ui.backup.RecoverySheet
import com.motovault.app.application.VaultRecovery
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.privacy.*
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.VaultError
import com.motovault.app.application.fuel.FuelEntryBuilder
import com.motovault.app.application.fuel.FuelEntryEditor
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.ui.privacy.*
import dev.sky.design.*
import com.motovault.app.application.fuel.FuelWorkflow
import com.motovault.app.application.fuel.FuelSaveOutcome
import com.motovault.app.application.VaultBackupOperations
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.*


@Composable
internal fun MotoVault(session: VaultWriter<VaultState>, backupRestore: VaultBackupRestorer, fuelEntry: FuelEntryBuilder<VaultState>, fuelEditor: FuelEntryEditor<VaultState>, recovery: VaultRecovery, backupOperations: VaultBackupOperations<VaultState>) {
    val snapshot by session.snapshots.collectAsState()
    val state = snapshot.state
    // Refresh actual OS permission and committed requests when returning from system settings.
    rememberReminderAccess()
    val revision = snapshot.revision
    val storageError = snapshot.error
    val loading = snapshot.loading
    var tab by remember {
        mutableStateOf(
            when (LaunchConfig.initialTab?.lowercase()) {
                "vault" -> Tab.VAULT
                "history" -> Tab.HISTORY
                "me" -> Tab.ME
                else -> Tab.GARAGE
            }
        )
    }
    var editingRide by remember { mutableStateOf(LaunchConfig.initialOnboarding) }
    var activeSheet by remember {
        mutableStateOf(
            LaunchConfig.initialSheet?.let { ActiveSheet(it) }
        )
    }
    var currentToast by remember { mutableStateOf<ToastData?>(null) }
    var restoreSuccessVisible by remember { mutableStateOf(false) }
    var fuelEditError by remember { mutableStateOf<String?>(null) }
    var fuelCreateError by remember { mutableStateOf<FuelCreateError?>(null) }

    val scope = rememberCoroutineScope()
    val fuelWorkflow = remember(session, fuelEntry, fuelEditor) { FuelWorkflow(session, fuelEntry, fuelEditor) }
    val text = vaultUiText(); val authMessages = rememberAuthMessages()
    fun commit(next: VaultState, onDone: ((Boolean) -> Unit)? = null) {
        session.commit(next, revision) { ok ->
            if (scope.isActive) {
                onDone?.invoke(ok)
            }
        }
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

    val accessPolicy = remember {
        VaultAccessPolicy(state.privacy) { Clock.System.now().toEpochMilliseconds() }
    }
    var accessMode by remember { mutableStateOf(accessPolicy.mode) }
    var selectedInspectionDocId by remember { mutableStateOf(accessPolicy.selectedDocumentId) }
    var privacyAuthMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.privacy) {
        accessPolicy.settingsCommitted(state.privacy)
        accessMode = accessPolicy.mode
        selectedInspectionDocId = accessPolicy.selectedDocumentId
    }

    LaunchedEffect(accessMode, state.privacy.enabled) {
        if (!state.privacy.enabled || accessMode == VaultAccessMode.LOCKED) return@LaunchedEffect
        while (isActive) {
            delay(1000L)
            accessPolicy.tick()
            if (accessPolicy.mode != accessMode) {
                accessMode = accessPolicy.mode
                selectedInspectionDocId = accessPolicy.selectedDocumentId
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                accessPolicy.background()
                accessMode = accessPolicy.mode
                selectedInspectionDocId = null
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                accessPolicy.active(true)
                accessMode = accessPolicy.mode
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun unlockVault() {
        val request = runCatching { accessPolicy.beginAuthentication(VaultAuthAction.UNLOCK) }.getOrNull()
        if (request == null) {
            privacyAuthMessage = text.privacyStartFailed
            return
        }
        scope.launch {
            val result = NativeSystemAuth.authenticate(text.unlockTitle, text.unlockReason)
            val grant = accessPolicy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                accessMode = accessPolicy.mode
                selectedInspectionDocId = null
                privacyAuthMessage = null
            } else {
                privacyAuthMessage = authMessages.forStatus(result.status)
            }
        }
    }

    fun exitInspection() {
        val request = runCatching { accessPolicy.beginAuthentication(VaultAuthAction.EXIT_INSPECTION) }.getOrNull()
        if (request == null) {
            accessPolicy.lock()
            accessMode = accessPolicy.mode
            selectedInspectionDocId = null
            return
        }
        scope.launch {
            val result = NativeSystemAuth.authenticate(text.exitTitle, text.exitReason)
            val grant = accessPolicy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                accessMode = accessPolicy.mode
                selectedInspectionDocId = null
                privacyAuthMessage = null
            } else {
                privacyAuthMessage = authMessages.forStatus(result.status)
            }
        }
    }

    fun lockNow() {
        accessPolicy.lock()
        accessMode = accessPolicy.mode
        selectedInspectionDocId = null
        activeSheet = null
    }

    fun inspectDocument(docId: String) {
        accessPolicy.inspect(docId)
        accessMode = accessPolicy.mode
        selectedInspectionDocId = accessPolicy.selectedDocumentId
        activeSheet = null
    }

    val reminderRequest by ReminderNavigation.requests.collectAsState()
    LaunchedEffect(reminderRequest, loading) {
        val payload = reminderRequest ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        if (state.privacy.enabled && accessMode == VaultAccessMode.LOCKED) {
            return@LaunchedEffect
        }
        val target = decodeReminderPayload(payload)?.let { reminderTarget(state, it) }
        if (target == null) currentToast = ToastData(message = text.reminderGone)
        else if (target.type == "document") {
            tab = Tab.VAULT
            activeSheet = ActiveSheet("docView", target.id)
        } else {
            tab = Tab.GARAGE
            activeSheet = ActiveSheet("service", target.id)
        }
        ReminderNavigation.handled(payload)
    }

    LaunchedEffect(LaunchConfig.initialSeed, loading) {
        if (LaunchConfig.initialSeed && !loading && LaunchConfig.consumeSeed()) {
            val seeded = VaultState.seedData(today())
            commit(seeded)
        }
    }

    val activeBrandId = state.brandOverride ?: state.ride?.brandId ?: "yamaha"

    AppResourceEnvironment(state.language) {
    val restoreSuccessMessage = appStringResource(Res.string.restore_success)
    VaultTheme(
        brandId = activeBrandId,
        appearance = state.appearance,
        reduceMotion = state.reduceMotion,
        textScale = state.textScale,
        digits = state.digits,
        language = state.language
    ) {
        PlatformAppearance(LocalVaultPalette.current.dark)

        if (state.privacy.enabled && (accessMode == VaultAccessMode.LOCKED || accessMode == VaultAccessMode.INSPECTION)) {
            val selectedDoc = state.documents.find { it.id == selectedInspectionDocId }
            PrivacyGate(
                mode = accessMode,
                selectedDoc = selectedDoc,
                onUnlock = { unlockVault() },
                onExitInspection = { exitInspection() },
                onLock = { lockNow() },
                authMessage = privacyAuthMessage,
                language = state.language
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CanvasColor)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .then(if (activeSheet != null) Modifier.excludeFromAccessibility() else Modifier)
            ) {
                storageError?.let {
                    Text(
                        text = appStringResource(when (it) {
                            VaultError.SAVE_FAILED -> Res.string.storage_save_failed
                            VaultError.RESTORE_FAILED -> Res.string.storage_restore_failed
                        }),
                        color = ErrorColor,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 13.sp
                    )
                }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandColor)
                    }
                } else if (snapshot.restorationFailed) {
                    // Keep the failure visible; an unreadable vault is not an empty new vault.
                } else if (state.ride == null || editingRide) {
                    OnboardingScreen(
                        onDismiss = if (state.ride != null) { { editingRide = false } } else null,
                        onComplete = { result ->
                            val today = today()
                            val ride = Ride(
                                brandId = result.brandId,
                                edition = result.modelName.ifBlank { text.defaultEdition },
                                year = result.year,
                                odometer = result.odometer,
                                modelId = result.modelId,
                                modelName = result.modelName,
                                market = result.market,
                                emission = RideEmission.UNKNOWN,
                                modelCode = result.modelCode,
                                factoryProfileId = result.factoryProfileId,
                                odometerConfirmedDate = today.toString()
                            )
                            val created = VaultState.create(ride, result.baselineKm, result.baselineDate, result.baselineCertainty)
                            val nextState = created.copy(
                                documents = state.documents,
                                appearance = state.appearance,
                                language = state.language,
                                digits = state.digits,
                                haptics = state.haptics,
                                reduceMotion = state.reduceMotion,
                                textScale = state.textScale
                            )
                            commit(nextState) { ok ->
                                if (ok) {
                                    editingRide = false
                                    tab = Tab.GARAGE
                                }
                            }
                        }
                    )
                } else {
                    VaultTabContent(
                        tab = tab,
                        state = state,
                        revision = revision,
                        reducedMotion = isReducedMotion,
                        recordDeletedMessage = text.recordDeleted,
                        onSheet = { activeSheet = it },
                        onSwitchTab = { tab = it },
                        onCommit = { next, done -> commit(next, done) },
                        onEditFuel = { log -> fuelEditError = null; activeSheet = ActiveSheet("fuelEdit", FuelEditTarget(log, revision)) },
                        onUndo = { undo(it) },
                        onToast = { currentToast = it },
                        onStartOnboarding = { editingRide = true },
                        onResetDemo = { commit(VaultState.seedData(today())) }
                    )

                    // Bottom Navigation Bar with Center "+" Button
                    VaultBottomNavigation(
                        selected = tab,
                        onSelect = { tab = it },
                        onQuickAdd = { activeSheet = ActiveSheet("quickAdd") }
                    )
                }
            }

            // Floating Toast notification with Undo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            ) {
                ToastLayer(
                    toast = if (restoreSuccessVisible) ToastData(message = restoreSuccessMessage) else currentToast,
                    onDismiss = { restoreSuccessVisible = false; currentToast = null }
                )
            }

            VaultSheetHost(
                activeSheet = activeSheet,
                state = state,
                revision = revision,
                loading = loading,
                backupOperations = backupOperations,
                session = session,
                backupRestore = backupRestore,
                recovery = recovery,
                fuelWorkflow = fuelWorkflow,
                accessPolicy = accessPolicy,
                fuelCreateError = fuelCreateError,
                fuelEditError = fuelEditError,
                onSheet = { activeSheet = it },
                onCommit = { next, done -> commit(next, done) },
                onUndo = { undo(it) },
                onFuelCreateError = { fuelCreateError = it },
                onFuelEditError = { fuelEditError = it },
                onToast = { currentToast = it },
                onRestoreSuccess = { restoreSuccessVisible = true },
                onInspect = { inspectDocument(it) },
                onLockNow = { lockNow() }
            )
            }
            }
        }
        }
    }
