package com.motovault.app.ui.backup

import com.motovault.app.application.VaultBackupOperations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.backup.*
import com.motovault.app.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.backup_revalidate
import motovault.composeapp.generated.resources.backup_revision_changed
import motovault.composeapp.generated.resources.backup_restore_failed
import motovault.composeapp.generated.resources.backup_progress_preparing
import motovault.composeapp.generated.resources.backup_progress_checking
import motovault.composeapp.generated.resources.backup_progress_key
import motovault.composeapp.generated.resources.backup_progress_encrypting_assets
import motovault.composeapp.generated.resources.backup_progress_encrypted_assets
import motovault.composeapp.generated.resources.backup_progress_encrypting_vault
import motovault.composeapp.generated.resources.backup_progress_writing
import motovault.composeapp.generated.resources.backup_progress_ready
import motovault.composeapp.generated.resources.backup_password_minimum
import motovault.composeapp.generated.resources.backup_password_mismatch
import motovault.composeapp.generated.resources.backup_create_failed
import motovault.composeapp.generated.resources.backup_password_required
import motovault.composeapp.generated.resources.backup_password_or_file_invalid
import motovault.composeapp.generated.resources.backup_validation_failed
import motovault.composeapp.generated.resources.backup_save_file_failed
import motovault.composeapp.generated.resources.backup_open_file_failed
import motovault.composeapp.generated.resources.backup_sheet_title
import motovault.composeapp.generated.resources.backup_tab_create
import motovault.composeapp.generated.resources.backup_tab_restore
import motovault.composeapp.generated.resources.backup_secure_title
import motovault.composeapp.generated.resources.backup_secure_description
import motovault.composeapp.generated.resources.backup_includes
import motovault.composeapp.generated.resources.backup_bike_label
import motovault.composeapp.generated.resources.backup_no_bike
import motovault.composeapp.generated.resources.backup_odometer_label
import motovault.composeapp.generated.resources.backup_records_label
import motovault.composeapp.generated.resources.backup_documents_label
import motovault.composeapp.generated.resources.backup_password_label
import motovault.composeapp.generated.resources.backup_confirm_password_label
import motovault.composeapp.generated.resources.backup_show_password
import motovault.composeapp.generated.resources.backup_hide_password
import motovault.composeapp.generated.resources.backup_password_warning
import motovault.composeapp.generated.resources.backup_create_button
import motovault.composeapp.generated.resources.backup_record_count
import motovault.composeapp.generated.resources.backup_document_count
import motovault.composeapp.generated.resources.backup_file_count
import motovault.composeapp.generated.resources.backup_size_kib
import motovault.composeapp.generated.resources.restore_intro_title
import motovault.composeapp.generated.resources.restore_intro_description
import motovault.composeapp.generated.resources.restore_choose_file
import motovault.composeapp.generated.resources.restore_choose_source
import motovault.composeapp.generated.resources.restore_change_file
import motovault.composeapp.generated.resources.restore_enter_password
import motovault.composeapp.generated.resources.restore_validating
import motovault.composeapp.generated.resources.restore_validate
import motovault.composeapp.generated.resources.restore_preview_title
import motovault.composeapp.generated.resources.restore_odometer
import motovault.composeapp.generated.resources.restore_record_count_label
import motovault.composeapp.generated.resources.restore_document_count_label
import motovault.composeapp.generated.resources.restore_document_files
import motovault.composeapp.generated.resources.restore_backup_date
import motovault.composeapp.generated.resources.restore_replace_title
import motovault.composeapp.generated.resources.restore_replace_description
import motovault.composeapp.generated.resources.restore_progress
import motovault.composeapp.generated.resources.restore_confirm
import motovault.composeapp.generated.resources.restore_success
import motovault.composeapp.generated.resources.recovery_cancel
import motovault.composeapp.generated.resources.backup_ready_title
import motovault.composeapp.generated.resources.backup_ready_size
import motovault.composeapp.generated.resources.backup_saved
import motovault.composeapp.generated.resources.backup_share_failed
import motovault.composeapp.generated.resources.backup_snapshot_changed

private const val BYTES_PER_KIB = 1024

internal enum class BackupSheetError {
    PASSWORD_MINIMUM, PASSWORD_MISMATCH, CREATE_FAILED, SAVE_FAILED, SHARE_FAILED, OPEN_FAILED,
    PASSWORD_REQUIRED, PASSWORD_OR_FILE_INVALID, VALIDATION_FAILED,
    REVALIDATE, REVISION_CHANGED, SNAPSHOT_CHANGED, RESTORE_FAILED
}

@Composable
internal fun backupErrorText(error: BackupSheetError): String = when (error) {
    BackupSheetError.PASSWORD_MINIMUM -> appStringResource(Res.string.backup_password_minimum, currentFmt.n(BackupUseCase.MIN_PASSPHRASE_LENGTH.toLong()))
    BackupSheetError.PASSWORD_MISMATCH -> appStringResource(Res.string.backup_password_mismatch)
    BackupSheetError.CREATE_FAILED -> appStringResource(Res.string.backup_create_failed)
    BackupSheetError.SAVE_FAILED -> appStringResource(Res.string.backup_save_file_failed)
    BackupSheetError.SHARE_FAILED -> appStringResource(Res.string.backup_share_failed)
    BackupSheetError.OPEN_FAILED -> appStringResource(Res.string.backup_open_file_failed)
    BackupSheetError.PASSWORD_REQUIRED -> appStringResource(Res.string.backup_password_required)
    BackupSheetError.PASSWORD_OR_FILE_INVALID -> appStringResource(Res.string.backup_password_or_file_invalid)
    BackupSheetError.VALIDATION_FAILED -> appStringResource(Res.string.backup_validation_failed)
    BackupSheetError.REVALIDATE -> appStringResource(Res.string.backup_revalidate)
    BackupSheetError.REVISION_CHANGED -> appStringResource(Res.string.backup_revision_changed)
    BackupSheetError.SNAPSHOT_CHANGED -> appStringResource(Res.string.backup_snapshot_changed)
    BackupSheetError.RESTORE_FAILED -> appStringResource(Res.string.backup_restore_failed)
}

@Composable
internal fun backupProgressText(progress: BackupProgress): String = when (progress.stage) {
    BackupProgressStage.PREPARING -> appStringResource(Res.string.backup_progress_preparing)
    BackupProgressStage.CHECKING_ASSETS -> appStringResource(Res.string.backup_progress_checking)
    BackupProgressStage.DERIVING_KEY -> appStringResource(Res.string.backup_progress_key)
    BackupProgressStage.ENCRYPTING_ASSETS -> appStringResource(Res.string.backup_progress_encrypting_assets)
    BackupProgressStage.ENCRYPTED_ASSETS -> appStringResource(Res.string.backup_progress_encrypted_assets, currentFmt.n(progress.completed.toLong()), currentFmt.n(progress.total.toLong()))
    BackupProgressStage.ENCRYPTING_VAULT -> appStringResource(Res.string.backup_progress_encrypting_vault)
    BackupProgressStage.WRITING_ARCHIVE -> appStringResource(Res.string.backup_progress_writing)
    BackupProgressStage.READY -> appStringResource(Res.string.backup_progress_ready)
}

enum class BackupSheetTab {
    CREATE,
    RESTORE
}

@Composable
internal fun BackupSheet(
    state: VaultState,
    stateRevision: Long,
    backupOperations: VaultBackupOperations<VaultState>,
    vaultRevision: () -> Long,
    initialTab: BackupSheetTab = BackupSheetTab.CREATE,
    onDismiss: () -> Unit,
    onBackupSaved: (Long) -> Unit,
    onRestore: (ByteArray, String, Long, (Result<Unit>) -> Unit) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val scope = rememberCoroutineScope()
    val createPasswordLabel = appStringResource(Res.string.backup_password_label, f.n(BackupUseCase.MIN_PASSPHRASE_LENGTH.toLong()))
    val confirmPasswordLabel = appStringResource(Res.string.backup_confirm_password_label)
    val restorePasswordLabel = appStringResource(Res.string.restore_enter_password)

    var activeTab by remember { mutableStateOf(initialTab) }

    // --- CREATE BACKUP STATE ---
    val create = remember { BackupCreateState() }

    // --- RESTORE BACKUP STATE ---
    val restore = remember { BackupRestoreState() }
    val busy = restore.isRestoring || create.isExporting || restore.isValidating

    Sheet(
        title = appStringResource(Res.string.backup_sheet_title),
        onDismiss = onDismiss,
        dismissEnabled = !busy,
        detent = SheetDetent.TALL
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector
            VaultSegmented(
                options = listOf(
                    BackupSheetTab.CREATE to appStringResource(Res.string.backup_tab_create),
                    BackupSheetTab.RESTORE to appStringResource(Res.string.backup_tab_restore)
                ),
                selected = activeTab,
                enabled = !busy,
                onSelect = {
                    haptic.perform(HapticKind.TICK)
                    activeTab = it
                }
            )

            // ==========================================
            // TAB 1: CREATE BACKUP
            // ==========================================
            if (activeTab == BackupSheetTab.CREATE) {
                BackupCreateContent(
                    state = state,
                    stateRevision = stateRevision,
                    backupOperations = backupOperations,
                    ui = create,
                    f = f,
                    createPasswordLabel = createPasswordLabel,
                    confirmPasswordLabel = confirmPasswordLabel,
                    vaultRevision = vaultRevision,
                    onBackupSaved = onBackupSaved
                )
            } else {
                BackupRestoreContent(
                    state = state,
                    stateRevision = stateRevision,
                    backupOperations = backupOperations,
                    ui = restore,
                    f = f,
                    restorePasswordLabel = restorePasswordLabel,
                    vaultRevision = vaultRevision,
                    busy = busy,
                    onRestore = onRestore
                )
            }
        }
    }
}
