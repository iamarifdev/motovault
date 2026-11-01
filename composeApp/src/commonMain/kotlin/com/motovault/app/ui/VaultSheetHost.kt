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

internal enum class FuelCreateError { SAVE_FAILED, CONFLICT }

@Composable
internal fun VaultSheetHost(
    activeSheet: ActiveSheet?,
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
    onLockNow: () -> Unit
) {
    val scope = rememberCoroutineScope()
            if (!loading && (state.ride != null || activeSheet?.name == "paste" || activeSheet?.name == "quickAdd")) {
                activeSheet?.let { sheet ->
                when (sheet.name) {
                    "gaugeDetail" -> GaugeDetailSheet(state, sheet.props as? String,
                        onDismiss = { onSheet(null) }, onOpen = { name, id ->
                            onSheet(if (name == "serviceReminder") ActiveSheet("reminders", "service" to id)
                                else ActiveSheet(name, id))
                        })
                    "schedule" -> state.rules.firstOrNull { it.id == sheet.props as? String }?.let { rule ->
                        ScheduleDialog(state, rule, onDismiss = { onSheet(null) },
                            onSave = { next, done -> onCommit(next, done) })
                    }
                    "reminders" -> {
                        val target = sheet.props as? Pair<*, *>
                        ReminderSettingsSheet(state, target?.first as? String, target?.second as? String,
                            onDismiss = { onSheet(null) },
                            onOpen = { type, id -> onSheet(ActiveSheet("reminders", type to id)) },
                            onSave = { next, done -> onCommit(next, done) })
                    }
                    "quickAdd" -> QuickAddSheet(
                        onDismiss = { onSheet(null) },
                        onSelectFlow = { flow -> onSheet(ActiveSheet(flow)) }
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
                        onDismiss = { onSheet(null); onFuelCreateError(null) },
                        onSave = { request, msg, onDone ->
                            fuelWorkflow.create(request, revision) { outcome ->
                                if (scope.isActive && outcome == FuelSaveOutcome.SAVED) {
                                    onFuelCreateError(null)
                                    onToast(ToastData(
                                        id = "fuel_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            onUndo(revision + 1)
                                        }
                                    ))
                                    onDone()
                                    onSheet(null)
                                } else if (scope.isActive) onFuelCreateError(when (outcome) {
                                    FuelSaveOutcome.CONFLICT, FuelSaveOutcome.INVALID -> FuelCreateError.CONFLICT
                                    else -> FuelCreateError.SAVE_FAILED
                                })
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
                                onDismiss = { onSheet(null); onFuelEditError(null) },
                                onSave = { _, _, _ -> },
                                onEdit = { request, message ->
                                    fuelWorkflow.edit(request, target.revision) { outcome ->
                                            if (scope.isActive) {
                                                if (outcome == FuelSaveOutcome.SAVED) {
                                                    onToast(ToastData(id = "fuel_edit_${request.id}", message = message, undoAction = { onUndo(target.revision + 1) }))
                                                    onSheet(null)
                                                    onFuelEditError(null)
                                                } else onFuelEditError(if (outcome == FuelSaveOutcome.FAILED) failureText else conflictText)
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
                            onDismiss = { onSheet(null) },
                            onSave = { nextState, msg, onDone ->
                                onCommit(nextState) { ok ->
                                    if (ok) {
                                        onToast(ToastData(
                                            id = "svc_${Clock.System.now().toEpochMilliseconds()}",
                                            message = msg,
                                            undoAction = {
                                                onUndo(revision + 1)
                                            }
                                        ))
                                        onDone()
                                        onSheet(null)
                                    }
                                }
                            }
                        )
                    }
                    "odometer" -> OdometerSheet(
                        state = state,
                        onDismiss = { onSheet(null) },
                        onSave = { nextState, msg, onDone ->
                            onCommit(nextState) { ok ->
                                if (ok) {
                                    onToast(ToastData(
                                        id = "odo_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            onUndo(revision + 1)
                                        }
                                    ))
                                    onDone()
                                    onSheet(null)
                                }
                            }
                        }
                    )
                    "docAdd" -> DocAddSheet(
                        state = state,
                        onDismiss = { onSheet(null) },
                        onSave = { nextState, msg, onDone ->
                            onCommit(nextState) { ok ->
                                if (ok) {
                                    onToast(ToastData(
                                        id = "doc_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            onUndo(revision + 1)
                                        }
                                    ))
                                    onDone()
                                    onSheet(null)
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
                                onDismiss = { onSheet(null) },
                                onUpdateReminder = { enabled ->
                                    val updated = state.documents.map { if (it.id == doc.id) it.copy(reminder = enabled) else it }
                                    onCommit(state.copy(documents = updated), null)
                                },
                                onReminderSettings = { onSheet(ActiveSheet("reminders", "document" to doc.id)) },
                                warningDays = doc.reminderWarningDays ?: state.reminderPolicy.warningDays,
                                privacyEnabled = state.privacy.enabled,
                                onInspect = { onInspect(doc.id) },
                                onOpenPrivacy = { onSheet(ActiveSheet("privacy")) },
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
                                onCommit(state.copy(privacy = next), null)
                            }
                        },
                        onLockNow = { onLockNow() },
                        onDismiss = { onSheet(null) }
                    )
                    "paste" -> PasteSheet(
                        onDismiss = { onSheet(null) },
                        onSelectFlow = { flow, payload ->
                            onSheet(ActiveSheet(flow, payload))
                        },
                        language = state.language
                    )
                    "backup", "backup-restore" -> BackupSheet(
                        state = state,
                        stateRevision = revision,
                        backupOperations = backupOperations,
                        vaultRevision = { session.snapshots.value.revision },
                        initialTab = if (sheet.name == "backup-restore" || sheet.props == BackupSheetTab.RESTORE) BackupSheetTab.RESTORE else BackupSheetTab.CREATE,
                        onDismiss = { onSheet(null) },
                        onBackupSaved = { timestamp ->
                            onCommit(state.copy(lastBackupEpochMillis = timestamp), null)
                        },
                        onRestore = { archive, password, expectedRevision, completed ->
                            backupRestore.restore(archive, password, expectedRevision) { result ->
                                completed(result)
                                if (result.isSuccess) {
                                    onSheet(null)
                                    onToast(null)
                                    onRestoreSuccess()
                                }
                            }
                        }
                    )
                    "recovery" -> RecoverySheet(recovery, revision = { session.snapshots.value.revision }, onDismiss = { onSheet(null) })
                }
}
}
}
