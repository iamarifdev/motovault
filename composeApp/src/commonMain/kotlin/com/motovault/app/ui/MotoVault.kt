package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.motovault.app.ui.backup.BackupSheet
import com.motovault.app.ui.fuel.FuelSheet
import com.motovault.app.ui.backup.BackupSheetTab
import com.motovault.app.ui.backup.RecoverySheet
import com.motovault.app.application.VaultRecovery
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.privacy.*
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.VaultError
import com.motovault.app.application.fuel.FuelEntryBuilder
import com.motovault.app.application.fuel.FuelEntryEditor
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.ui.privacy.*
import dev.sky.design.*
import dev.sky.design.tokens.SkyTokens
import kotlinx.coroutines.delay
import com.motovault.app.application.fuel.FuelWorkflow
import com.motovault.app.application.fuel.FuelSaveOutcome
import com.motovault.app.application.VaultBackupOperations
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToLong


private enum class FuelCreateError { SAVE_FAILED, CONFLICT }

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
                message = if (canRetry) "ফিরিয়ে আনা যায়নি। আবার চেষ্টা করুন।" else "আরেকটি পরিবর্তন হয়েছে। রেকর্ড দেখে নিন।",
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
            privacyAuthMessage = "যাচাই শুরু করা যায়নি।"
            return
        }
        scope.launch {
            val result = NativeSystemAuth.authenticate("MotoVault খুলুন", "আপনার তথ্য দেখতে ফোনের লক দিয়ে যাচাই করুন")
            val grant = accessPolicy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                accessMode = accessPolicy.mode
                selectedInspectionDocId = null
                privacyAuthMessage = null
            } else {
                privacyAuthMessage = result.message ?: "যাচাই সফল হয়নি।"
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
            val result = NativeSystemAuth.authenticate("কাগজ বন্ধ করুন", "মূল ভল্টে ফিরতে ফোনের লক দিয়ে যাচাই করুন")
            val grant = accessPolicy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                accessMode = accessPolicy.mode
                selectedInspectionDocId = null
                privacyAuthMessage = null
            } else {
                privacyAuthMessage = result.message ?: "যাচাই বাতিল হয়েছে।"
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
        if (target == null) currentToast = ToastData(message = "এই রিমাইন্ডারের রেকর্ড বদলেছে বা মুছে গেছে। তালিকা দেখে নিন।")
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
        digits = state.digits
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
                                edition = result.modelName.ifBlank { "নিজের সেটআপ" },
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
                    // Screen pages
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val reducedMotion = isReducedMotion
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(VaultMotion.pageTween(reducedMotion)) togetherWith fadeOut(VaultMotion.pageTween(reducedMotion))
                            },
                            label = "VaultScreenPage",
                            modifier = Modifier.fillMaxSize()
                        ) { destination ->
                            val mainScrollState = rememberScrollState()
                            LaunchedEffect(LaunchConfig.scrollToBottom, destination) {
                                if (LaunchConfig.scrollToBottom) {
                                    mainScrollState.scrollTo(mainScrollState.maxValue)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(mainScrollState)
                            ) {
                                when (destination) {
                                    Tab.GARAGE -> GarageScreen(
                                        state = state,
                                        onOpenSheet = { name, payload -> activeSheet = ActiveSheet(name, payload) },
                                        onSwitchTab = { nextTab -> tab = nextTab },
                                        onOpenServiceForItem = { itemKey -> activeSheet = ActiveSheet("service", itemKey) }
                                    )
                                    Tab.VAULT -> VaultScreen(
                                        state = state,
                                        onOpenAddDoc = { activeSheet = ActiveSheet("docAdd") },
                                        onOpenDocView = { doc -> activeSheet = ActiveSheet("docView", doc.id) },
                                        onToggleDocReminders = { enabled -> commit(state.copy(docReminders = enabled)) },
                                        onReminderSettings = { activeSheet = ActiveSheet("reminders") },
                                        onOpenPrivacy = { activeSheet = ActiveSheet("privacy") }
                                    )
                                    Tab.HISTORY -> HistoryScreen(
                                        state = state,
                                        onEditFuel = { log ->
                                            fuelEditError = null
                                            activeSheet = ActiveSheet("fuelEdit", FuelEditTarget(log, revision))
                                        },
                                        onDeleteLog = { log ->
                                            val next = runCatching { state.deleteLog(log) }.getOrNull()
                                            if (next != null) {
                                                commit(next) { ok ->
                                                    if (ok) {
                                                        currentToast = ToastData(
                                                            id = "del_${log.id}",
                                                            message = "রেকর্ড মোছা হয়েছে",
                                                            undoAction = {
                                                                undo(revision + 1)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Tab.ME -> MeScreen(
                                        state = state,
                                        onUpdateState = { next -> commit(next) },
                                        onStartOnboarding = { editingRide = true },
                                        onResetDemo = {
                                            val seeded = VaultState.seedData(today())
                                            commit(seeded)
                                        },
                                        onOpenSheet = { name, payload -> activeSheet = ActiveSheet(name, payload) }
                                    )
                                }
                            }
                        }
                    }

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

            // Sheets Orchestration
            if (!loading && (state.ride != null || activeSheet?.name == "paste" || activeSheet?.name == "quickAdd")) {
                activeSheet?.let { sheet ->
                when (sheet.name) {
                    "gaugeDetail" -> GaugeDetailSheet(state, sheet.props as? String,
                        onDismiss = { activeSheet = null }, onOpen = { name, id ->
                            activeSheet = if (name == "serviceReminder") ActiveSheet("reminders", "service" to id)
                                else ActiveSheet(name, id)
                        })
                    "schedule" -> state.rules.firstOrNull { it.id == sheet.props as? String }?.let { rule ->
                        ScheduleDialog(state, rule, onDismiss = { activeSheet = null },
                            onSave = { next, done -> commit(next, done) })
                    }
                    "reminders" -> {
                        val target = sheet.props as? Pair<*, *>
                        ReminderSettingsSheet(state, target?.first as? String, target?.second as? String,
                            onDismiss = { activeSheet = null },
                            onOpen = { type, id -> activeSheet = ActiveSheet("reminders", type to id) },
                            onSave = { next, done -> commit(next, done) })
                    }
                    "quickAdd" -> QuickAddSheet(
                        onDismiss = { activeSheet = null },
                        onSelectFlow = { flow -> activeSheet = ActiveSheet(flow) }
                    )
                    "fuel" -> state.fuelScreenInput(today())?.let { fuelInput ->
                        val failureText = when (fuelCreateError) {
                            FuelCreateError.SAVE_FAILED -> appStringResource(Res.string.fuel_create_save_failed)
                            FuelCreateError.CONFLICT -> appStringResource(Res.string.fuel_create_conflict)
                            null -> null
                        }
                        FuelSheet(
                        input = fuelInput,
                        editError = failureText,
                        initialAmount = (sheet.props as? Number)?.toDouble(),
                        onDismiss = { activeSheet = null; fuelCreateError = null },
                        onSave = { request, msg, onDone ->
                            fuelWorkflow.create(request, revision) { outcome ->
                                if (scope.isActive && outcome == FuelSaveOutcome.SAVED) {
                                    fuelCreateError = null
                                    currentToast = ToastData(
                                        id = "fuel_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                } else if (scope.isActive) fuelCreateError = when (outcome) {
                                    FuelSaveOutcome.CONFLICT, FuelSaveOutcome.INVALID -> FuelCreateError.CONFLICT
                                    else -> FuelCreateError.SAVE_FAILED
                                }
                            }
                        }
                    ) }
                    "fuelEdit" -> {
                        val target = sheet.props as? FuelEditTarget
                        if (target != null) {
                            val conflictText = appStringResource(Res.string.fuel_edit_conflict)
                            val failureText = appStringResource(Res.string.fuel_edit_save_failed)
                            state.fuelScreenInput(today())?.let { fuelInput -> FuelSheet(
                                input = fuelInput, existing = target.record.fuelExistingEntry(), editError = fuelEditError,
                                onDismiss = { activeSheet = null; fuelEditError = null },
                                onSave = { _, _, _ -> },
                                onEdit = { request, message ->
                                    fuelWorkflow.edit(request, target.revision) { outcome ->
                                            if (scope.isActive) {
                                                if (outcome == FuelSaveOutcome.SAVED) {
                                                    currentToast = ToastData(id = "fuel_edit_${request.id}", message = message, undoAction = { undo(target.revision + 1) })
                                                    activeSheet = null
                                                    fuelEditError = null
                                                } else fuelEditError = if (outcome == FuelSaveOutcome.FAILED) failureText else conflictText
                                            }
                                    }
                                }
                            ) }
                        }
                    }
                    "service" -> {
                        val itemKey = sheet.props as? String
                        val parsedRows = (sheet.props as? List<*>)?.filterIsInstance<ParsedLine>()
                        ServiceSheet(
                            state = state,
                            initialItemKey = itemKey,
                            initialRows = parsedRows,
                            onDismiss = { activeSheet = null },
                            onSave = { nextState, msg, onDone ->
                                commit(nextState) { ok ->
                                    if (ok) {
                                        currentToast = ToastData(
                                            id = "svc_${Clock.System.now().toEpochMilliseconds()}",
                                            message = msg,
                                            undoAction = {
                                                undo(revision + 1)
                                            }
                                        )
                                        onDone()
                                        activeSheet = null
                                    }
                                }
                            }
                        )
                    }
                    "odometer" -> OdometerSheet(
                        state = state,
                        onDismiss = { activeSheet = null },
                        onSave = { nextState, msg, onDone ->
                            commit(nextState) { ok ->
                                if (ok) {
                                    currentToast = ToastData(
                                        id = "odo_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                }
                            }
                        }
                    )
                    "docAdd" -> DocAddSheet(
                        state = state,
                        onDismiss = { activeSheet = null },
                        onSave = { nextState, msg, onDone ->
                            commit(nextState) { ok ->
                                if (ok) {
                                    currentToast = ToastData(
                                        id = "doc_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                }
                            }
                        }
                    )
                    "docView" -> {
                        val docId = sheet.props as? String
                        val doc = state.documents.firstOrNull { it.id == docId }
                        if (doc != null) {
                            DocViewSheet(
                                doc = doc,
                                onDismiss = { activeSheet = null },
                                onUpdateReminder = { enabled ->
                                    val updated = state.documents.map { if (it.id == doc.id) it.copy(reminder = enabled) else it }
                                    commit(state.copy(documents = updated))
                                },
                                onReminderSettings = { activeSheet = ActiveSheet("reminders", "document" to doc.id) },
                                warningDays = doc.reminderWarningDays ?: state.reminderPolicy.warningDays,
                                privacyEnabled = state.privacy.enabled,
                                onInspect = { inspectDocument(doc.id) },
                                onOpenPrivacy = { activeSheet = ActiveSheet("privacy") },
                                language = state.language
                            )
                        }
                    }
                    "privacy" -> PrivacySheet(
                        state = state,
                        onUpdatePrivacy = { next ->
                            val request = if (!state.privacy.enabled && next.enabled) {
                                accessPolicy.beginAuthentication(VaultAuthAction.ENABLE)
                            } else if (state.privacy.enabled && !next.enabled) {
                                accessPolicy.beginAuthentication(VaultAuthAction.DISABLE)
                            } else null
                            val grant = request?.let { accessPolicy.completeAuthentication(it, true) }
                            val committed = accessPolicy.settingsCommitted(next, grant)
                            if (committed) {
                                commit(state.copy(privacy = next))
                            }
                        },
                        onLockNow = { lockNow() },
                        onDismiss = { activeSheet = null }
                    )
                    "paste" -> PasteSheet(
                        onDismiss = { activeSheet = null },
                        onSelectFlow = { flow, payload ->
                            activeSheet = ActiveSheet(flow, payload)
                        },
                        language = state.language
                    )
                    "backup", "backup-restore" -> BackupSheet(
                        state = state,
                        stateRevision = revision,
                        backupOperations = backupOperations,
                        vaultRevision = { session.snapshots.value.revision },
                        initialTab = if (sheet.name == "backup-restore" || sheet.props == BackupSheetTab.RESTORE) BackupSheetTab.RESTORE else BackupSheetTab.CREATE,
                        onDismiss = { activeSheet = null },
                        onBackupSaved = { timestamp ->
                            commit(state.copy(lastBackupEpochMillis = timestamp))
                        },
                        onRestore = { archive, password, expectedRevision, completed ->
                            backupRestore.restore(archive, password, expectedRevision) { result ->
                                completed(result)
                                if (result.isSuccess) {
                                    activeSheet = null
                                    currentToast = null
                                    restoreSuccessVisible = true
                                }
                            }
                        }
                    )
                    "recovery" -> RecoverySheet(recovery, revision = { session.snapshots.value.revision }, onDismiss = { activeSheet = null })
                }
            }
            }
        }
        }
    }
    }
}
