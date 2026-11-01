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

private enum class BackupSheetError {
    PASSWORD_MINIMUM, PASSWORD_MISMATCH, CREATE_FAILED, SAVE_FAILED, SHARE_FAILED, OPEN_FAILED,
    PASSWORD_REQUIRED, PASSWORD_OR_FILE_INVALID, VALIDATION_FAILED,
    REVALIDATE, REVISION_CHANGED, SNAPSHOT_CHANGED, RESTORE_FAILED
}

@Composable
private fun backupErrorText(error: BackupSheetError): String = when (error) {
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
private fun backupProgressText(progress: BackupProgress): String = when (progress.stage) {
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
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportStatus by remember { mutableStateOf(BackupProgress(BackupProgressStage.PREPARING)) }
    var isExporting by remember { mutableStateOf(false) }
    var generatedArchive by remember { mutableStateOf<ByteArray?>(null) }
    var createError by remember { mutableStateOf<BackupSheetError?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    // --- RESTORE BACKUP STATE ---
    var pickedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pickedFileName by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var restorePasswordVisible by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var restorePreview by remember { mutableStateOf<BackupPreview?>(null) }
    var reviewedInputs by remember { mutableStateOf<RestoreReviewInputs?>(null) }
    var restoreError by remember { mutableStateOf<BackupSheetError?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreSuccess by remember { mutableStateOf(false) }
    val busy = isRestoring || isExporting || isValidating

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
                // Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandTintColor)
                        .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_shield.svg", null, modifier = Modifier.size(18.dp), tint = BrandColor)
                            Text(appStringResource(Res.string.backup_secure_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = appStringResource(Res.string.backup_secure_description),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Summary of data being backed up
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(appStringResource(Res.string.backup_includes), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(appStringResource(Res.string.backup_bike_label), fontSize = 13.sp, color = TextPrimary)
                        Text(state.ride?.displayName ?: appStringResource(Res.string.backup_no_bike), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(appStringResource(Res.string.backup_odometer_label), fontSize = 13.sp, color = TextPrimary)
                        Text(f.km(state.ride?.odometer ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(appStringResource(Res.string.backup_records_label), fontSize = 13.sp, color = TextPrimary)
                        Text(appPluralStringResource(Res.plurals.backup_record_count, state.logs.size, f.n(state.logs.size.toLong())), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(appStringResource(Res.string.backup_documents_label), fontSize = 13.sp, color = TextPrimary)
                        Text(appPluralStringResource(Res.plurals.backup_document_count, state.documents.size, f.n(state.documents.size.toLong())), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                if (generatedArchive == null) {
                    // Password Inputs
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(createPasswordLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = password,
                                onValueChange = { password = it; createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = createPasswordLabel }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(confirmPasswordLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = confirmPasswordLabel }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = appStringResource(if (passwordVisible) Res.string.backup_hide_password else Res.string.backup_show_password),
                            fontSize = 12.sp,
                            color = BrandColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Security Warning Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningColor.copy(alpha = 0.1f))
                            .border(1.dp, WarningColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_alert-triangle.svg", null, modifier = Modifier.size(16.dp), tint = WarningColor)
                            Text(
                                text = appStringResource(Res.string.backup_password_warning),
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Error text if any
                    createError?.let {
                        Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

                    // Action Button or Progress
                    if (isExporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { exportProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = BrandColor
                            )
                            Text(text = backupProgressText(exportStatus), fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        VaultButton(
                            text = appStringResource(Res.string.backup_create_button),
                            kind = ButtonKind.PRIMARY,
                            full = true,
                            onClick = {
                                if (password.length < BackupUseCase.MIN_PASSPHRASE_LENGTH) {
                                    createError = BackupSheetError.PASSWORD_MINIMUM
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }
                                if (password != confirmPassword) {
                                    createError = BackupSheetError.PASSWORD_MISMATCH
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }
                                if (vaultRevision() != stateRevision) {
                                    createError = BackupSheetError.SNAPSHOT_CHANGED
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }

                                createError = null
                                isExporting = true
                                scope.launch {
                                    try {
                                        val bytes = backupOperations.create(
                                            state = state,
                                            revision = stateRevision,
                                            password = password,
                                            progress = { p, status ->
                                                exportProgress = p
                                                exportStatus = status
                                            }
                                        )
                                        generatedArchive = bytes
                                        haptic.perform(HapticKind.SUCCESS)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (e: Exception) {
                                        createError = BackupSheetError.CREATE_FAILED
                                        haptic.perform(HapticKind.WARNING)
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Archive Ready - Native Save / Share
                    val archive = generatedArchive!!
                    val filename = "motovault-backup-${today()}.mva"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HealthyColor.copy(alpha = 0.12f))
                            .border(1.dp, HealthyColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_check-circle.svg", null, modifier = Modifier.size(20.dp), tint = HealthyColor)
                            Text(appStringResource(Res.string.backup_ready_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = appStringResource(Res.string.backup_ready_size, appStringResource(Res.string.backup_size_kib, f.n((archive.size / BYTES_PER_KIB).coerceAtLeast(1).toLong()))),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    BackupNativeActions(
                        archiveBytes = archive,
                        suggestedFilename = filename,
                        onSaved = {
                            createError = null
                            saveSuccess = true
                            haptic.perform(HapticKind.SUCCESS)
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            onBackupSaved(now)
                        },
                        onShareOpened = { createError = null },
                        onError = { failure ->
                            createError = when (failure) {
                                BackupNativeFailure.SAVE -> BackupSheetError.SAVE_FAILED
                                BackupNativeFailure.SHARE -> BackupSheetError.SHARE_FAILED
                            }
                            haptic.perform(HapticKind.WARNING)
                        }
                    )

                    createError?.let {
                        Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

                    if (saveSuccess) {
                        Text(
                            text = appStringResource(Res.string.backup_saved),
                            fontSize = 13.sp,
                            color = HealthyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            // ==========================================
            // TAB 2: RESTORE BACKUP
            // ==========================================
            } else {
                // Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandTintColor)
                        .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(appStringResource(Res.string.restore_intro_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = appStringResource(Res.string.restore_intro_description),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // File Picker Card
                if (pickedFileBytes == null) {
                    BackupFilePicker(
                        onFileSelected = { bytes, name ->
                            pickedFileBytes = bytes
                            pickedFileName = name
                            restoreError = null
                            restorePreview = null
                            haptic.perform(HapticKind.TICK)
                        },
                        onError = {
                            restoreError = BackupSheetError.OPEN_FAILED
                            haptic.perform(HapticKind.WARNING)
                        }
                    ) { openPicker ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(14.dp))
                                .clickable { openPicker() }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                V3Icon("feather_icon/fi_upload-cloud.svg", null, modifier = Modifier.size(32.dp), tint = BrandColor)
                                Text(appStringResource(Res.string.restore_choose_file), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(appStringResource(Res.string.restore_choose_source), fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    // File Chosen Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pickedFileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(appStringResource(Res.string.backup_size_kib, f.n(((pickedFileBytes?.size ?: 0) / BYTES_PER_KIB).coerceAtLeast(1).toLong())), fontSize = 12.sp, color = TextSecondary)
                        }
                        BackupFilePicker(
                            onFileSelected = { bytes, name ->
                                if (isRestoring || isValidating) return@BackupFilePicker
                                pickedFileBytes = bytes
                                pickedFileName = name
                                restorePreview = null
                                reviewedInputs = null
                                restoreError = null
                            },
                            onError = { restoreError = BackupSheetError.OPEN_FAILED }
                        ) { openPicker ->
                            VaultButton(
                                text = appStringResource(Res.string.restore_change_file),
                                kind = ButtonKind.SECONDARY,
                                size = ButtonSize.SM,
                                enabled = !busy,
                                onClick = { if (!isRestoring && !isValidating) openPicker() }
                            )
                        }
                    }

                    if (restorePreview == null) {
                        // Enter Password to Validate
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(restorePasswordLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceColor2)
                                    .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = restorePassword,
                                    enabled = !isValidating,
                                    onValueChange = { if (!isValidating) { restorePassword = it; restoreError = null } },
                                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(BrandColor),
                                    visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = restorePasswordLabel }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.clickable { restorePasswordVisible = !restorePasswordVisible },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = appStringResource(if (restorePasswordVisible) Res.string.backup_hide_password else Res.string.backup_show_password),
                                fontSize = 12.sp,
                                color = BrandColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isValidating) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandColor)
                                Spacer(Modifier.width(10.dp))
                                Text(appStringResource(Res.string.restore_validating), fontSize = 13.sp, color = TextSecondary)
                            }
                        } else {
                            VaultButton(
                                text = appStringResource(Res.string.restore_validate),
                                kind = ButtonKind.PRIMARY,
                                full = true,
                                onClick = {
                                    if (restorePassword.isBlank()) {
                                        restoreError = BackupSheetError.PASSWORD_REQUIRED
                                        haptic.perform(HapticKind.WARNING)
                                        return@VaultButton
                                    }
                                    if (vaultRevision() != stateRevision) {
                                        restoreError = BackupSheetError.REVALIDATE
                                        haptic.perform(HapticKind.WARNING)
                                        return@VaultButton
                                    }
                                    isValidating = true
                                    restoreError = null
                                    val previewRevision = stateRevision
                                    val previewState = state
                                    val reviewInputs = RestoreReviewInputs(requireNotNull(pickedFileBytes), restorePassword, previewRevision)
                                    scope.launch {
                                        try {
                                            val preview = backupOperations.preview(previewState, reviewInputs.archive, reviewInputs.password)
                                            if (reviewInputs.matches(pickedFileBytes, restorePassword)) {
                                                restorePreview = preview
                                                reviewedInputs = reviewInputs
                                                haptic.perform(HapticKind.SUCCESS)
                                            } else {
                                                restoreError = BackupSheetError.REVALIDATE
                                            }
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (e: BackupSecurityException) {
                                            restoreError = BackupSheetError.PASSWORD_OR_FILE_INVALID
                                            haptic.perform(HapticKind.WARNING)
                                        } catch (e: Exception) {
                                            restoreError = BackupSheetError.VALIDATION_FAILED
                                            haptic.perform(HapticKind.WARNING)
                                        } finally {
                                            isValidating = false
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        // ==========================================
                        // RESTORE PREVIEW & CONFLICT CONFIRMATION
                        // ==========================================
                        val preview = restorePreview!!
                        val backupDate = runCatching {
                            Instant.fromEpochMilliseconds(preview.manifest.createdAtEpochMillis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                        }.getOrDefault("")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(appStringResource(Res.string.restore_preview_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(appStringResource(Res.string.backup_bike_label), fontSize = 13.sp, color = TextPrimary)
                                Text(preview.verifiedSummary.bikeDisplayName ?: appStringResource(Res.string.backup_no_bike), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(appStringResource(Res.string.restore_odometer), fontSize = 13.sp, color = TextPrimary)
                                Text(f.km(preview.verifiedSummary.odometer ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(appStringResource(Res.string.restore_record_count_label), fontSize = 13.sp, color = TextPrimary)
                                Text(appPluralStringResource(Res.plurals.backup_record_count, preview.verifiedSummary.logCount, f.n(preview.verifiedSummary.logCount.toLong())), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(appStringResource(Res.string.restore_document_count_label), fontSize = 13.sp, color = TextPrimary)
                                val documents = appPluralStringResource(Res.plurals.backup_document_count, preview.verifiedSummary.documentCount, f.n(preview.verifiedSummary.documentCount.toLong()))
                                val files = appPluralStringResource(Res.plurals.backup_file_count, preview.validatedAssetCount, f.n(preview.validatedAssetCount.toLong()))
                                Text(appStringResource(Res.string.restore_document_files, documents, files), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(appStringResource(Res.string.restore_backup_date), fontSize = 13.sp, color = TextPrimary)
                                Text(f.d(backupDate), fontSize = 13.sp, color = TextSecondary)
                            }
                        }

                        // Destructive Overwrite Warning Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorColor.copy(alpha = 0.1f))
                                .border(1.dp, ErrorColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                V3Icon("feather_icon/fi_alert-triangle.svg", null, modifier = Modifier.size(18.dp), tint = ErrorColor)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(appStringResource(Res.string.restore_replace_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                    Text(
                                        text = appStringResource(Res.string.restore_replace_description),
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Restore Action Buttons
                        if (isRestoring) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ErrorColor)
                                Spacer(Modifier.width(10.dp))
                                Text(appStringResource(Res.string.restore_progress), fontSize = 13.sp, color = TextSecondary)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VaultButton(
                                    text = appStringResource(Res.string.recovery_cancel),
                                    kind = ButtonKind.SECONDARY,
                                    full = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        restorePreview = null
                                        reviewedInputs = null
                                    }
                                )
                                VaultButton(
                                    text = appStringResource(Res.string.restore_confirm),
                                    kind = ButtonKind.PRIMARY,
                                    full = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        isRestoring = true
                                        restoreError = null
                                        val review = reviewedInputs
                                        if (review == null || !review.matches(pickedFileBytes, restorePassword)) {
                                            isRestoring = false
                                            restoreError = BackupSheetError.REVALIDATE
                                            return@VaultButton
                                        }
                                        onRestore(review.archive, review.password, review.revision) { result ->
                                            isRestoring = false
                                            result.fold(
                                                onSuccess = {
                                                    restoreSuccess = true
                                                    haptic.perform(HapticKind.SUCCESS)
                                                },
                                                onFailure = { failure ->
                                                    restoreError = if (failure is com.motovault.app.application.VaultWriteConflict)
                                                        BackupSheetError.REVISION_CHANGED
                                                    else BackupSheetError.RESTORE_FAILED
                                                    haptic.perform(HapticKind.WARNING)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Error Display for Restore
                restoreError?.let {
                    Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                }

                if (restoreSuccess) {
                    Text(
                        text = appStringResource(Res.string.restore_success),
                        fontSize = 14.sp,
                        color = HealthyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
