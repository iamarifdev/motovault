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


internal class BackupRestoreState {
    var pickedFileBytes by mutableStateOf<ByteArray?>(null)
    var pickedFileName by mutableStateOf("")
    var restorePassword by mutableStateOf("")
    var restorePasswordVisible by mutableStateOf(false)
    var isValidating by mutableStateOf(false)
    var restorePreview by mutableStateOf<BackupPreview?>(null)
    var reviewedInputs by mutableStateOf<RestoreReviewInputs?>(null)
    var restoreError by mutableStateOf<BackupSheetError?>(null)
    var isRestoring by mutableStateOf(false)
    var restoreSuccess by mutableStateOf(false)
}

@Composable
internal fun BackupRestoreContent(
    state: VaultState,
    stateRevision: Long,
    backupOperations: VaultBackupOperations<VaultState>,
    ui: BackupRestoreState,
    f: Fmt,
    restorePasswordLabel: String,
    vaultRevision: () -> Long,
    busy: Boolean,
    onRestore: (ByteArray, String, Long, (Result<Unit>) -> Unit) -> Unit
) {
    val haptic = currentHaptic
    val scope = rememberCoroutineScope()

                BackupRestoreIntro()
                BackupRestoreFileSection(ui, busy, f)

                    if (ui.restorePreview == null) {
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
                                    value = ui.restorePassword,
                                    enabled = !ui.isValidating,
                                    onValueChange = { if (!ui.isValidating) { ui.restorePassword = it; ui.restoreError = null } },
                                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(BrandColor),
                                    visualTransformation = if (ui.restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = restorePasswordLabel }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.clickable { ui.restorePasswordVisible = !ui.restorePasswordVisible },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = appStringResource(if (ui.restorePasswordVisible) Res.string.backup_hide_password else Res.string.backup_show_password),
                                fontSize = 12.sp,
                                color = BrandColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (ui.isValidating) {
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
                                    if (ui.restorePassword.isBlank()) {
                                        ui.restoreError = BackupSheetError.PASSWORD_REQUIRED
                                        haptic.perform(HapticKind.WARNING)
                                        return@VaultButton
                                    }
                                    if (vaultRevision() != stateRevision) {
                                        ui.restoreError = BackupSheetError.REVALIDATE
                                        haptic.perform(HapticKind.WARNING)
                                        return@VaultButton
                                    }
                                    ui.isValidating = true
                                    ui.restoreError = null
                                    val previewRevision = stateRevision
                                    val previewState = state
                                    val reviewInputs = RestoreReviewInputs(requireNotNull(ui.pickedFileBytes), ui.restorePassword, previewRevision)
                                    scope.launch {
                                        try {
                                            val preview = backupOperations.preview(previewState, reviewInputs.archive, reviewInputs.password)
                                            if (reviewInputs.matches(ui.pickedFileBytes, ui.restorePassword)) {
                                                ui.restorePreview = preview
                                                ui.reviewedInputs = reviewInputs
                                                haptic.perform(HapticKind.SUCCESS)
                                            } else {
                                                ui.restoreError = BackupSheetError.REVALIDATE
                                            }
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (e: BackupSecurityException) {
                                            ui.restoreError = BackupSheetError.PASSWORD_OR_FILE_INVALID
                                            haptic.perform(HapticKind.WARNING)
                                        } catch (e: Exception) {
                                            ui.restoreError = BackupSheetError.VALIDATION_FAILED
                                            haptic.perform(HapticKind.WARNING)
                                        } finally {
                                            ui.isValidating = false
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        // ==========================================
                        // RESTORE PREVIEW & CONFLICT CONFIRMATION
                        // ==========================================
                        val preview = ui.restorePreview!!
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
                        if (ui.isRestoring) {
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
                                        ui.restorePreview = null
                                        ui.reviewedInputs = null
                                    }
                                )
                                VaultButton(
                                    text = appStringResource(Res.string.restore_confirm),
                                    kind = ButtonKind.PRIMARY,
                                    full = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        ui.isRestoring = true
                                        ui.restoreError = null
                                        val review = ui.reviewedInputs
                                        if (review == null || !review.matches(ui.pickedFileBytes, ui.restorePassword)) {
                                            ui.isRestoring = false
                                            ui.restoreError = BackupSheetError.REVALIDATE
                                            return@VaultButton
                                        }
                                        onRestore(review.archive, review.password, review.revision) { result ->
                                            ui.isRestoring = false
                                            result.fold(
                                                onSuccess = {
                                                    ui.restoreSuccess = true
                                                    haptic.perform(HapticKind.SUCCESS)
                                                },
                                                onFailure = { failure ->
                                                    ui.restoreError = if (failure is com.motovault.app.application.VaultWriteConflict)
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

                // Error Display for Restore
                ui.restoreError?.let {
                    Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                }

                if (ui.restoreSuccess) {
                    Text(
                        text = appStringResource(Res.string.restore_success),
                        fontSize = 14.sp,
                        color = HealthyColor,
                        fontWeight = FontWeight.Bold
                    )
                }

}
