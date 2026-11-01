package com.motovault.app.ui.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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


internal class BackupCreateState {
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    var exportProgress by mutableStateOf(0f)
    var exportStatus by mutableStateOf(BackupProgress(BackupProgressStage.PREPARING))
    var isExporting by mutableStateOf(false)
    var generatedArchive by mutableStateOf<ByteArray?>(null)
    var createError by mutableStateOf<BackupSheetError?>(null)
    var saveSuccess by mutableStateOf(false)
}

@Composable
internal fun BackupCreateContent(
    state: VaultState,
    stateRevision: Long,
    backupOperations: VaultBackupOperations<VaultState>,
    ui: BackupCreateState,
    f: Fmt,
    createPasswordLabel: String,
    confirmPasswordLabel: String,
    vaultRevision: () -> Long,
    onBackupSaved: (Long) -> Unit
) {
    val haptic = currentHaptic
    val scope = rememberCoroutineScope()
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

                if (ui.generatedArchive == null) {
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
                                value = ui.password,
                                onValueChange = { ui.password = it; ui.createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (ui.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                value = ui.confirmPassword,
                                onValueChange = { ui.confirmPassword = it; ui.createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (ui.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = confirmPasswordLabel }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.clickable { ui.passwordVisible = !ui.passwordVisible },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = appStringResource(if (ui.passwordVisible) Res.string.backup_hide_password else Res.string.backup_show_password),
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
                    ui.createError?.let {
                        Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

                    // Action Button or Progress
                    if (ui.isExporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { ui.exportProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = BrandColor
                            )
                            Text(text = backupProgressText(ui.exportStatus), fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        VaultButton(
                            text = appStringResource(Res.string.backup_create_button),
                            kind = ButtonKind.PRIMARY,
                            full = true,
                            onClick = {
                                if (ui.password.length < BackupUseCase.MIN_PASSPHRASE_LENGTH) {
                                    ui.createError = BackupSheetError.PASSWORD_MINIMUM
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }
                                if (ui.password != ui.confirmPassword) {
                                    ui.createError = BackupSheetError.PASSWORD_MISMATCH
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }
                                if (vaultRevision() != stateRevision) {
                                    ui.createError = BackupSheetError.SNAPSHOT_CHANGED
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }

                                ui.createError = null
                                ui.isExporting = true
                                scope.launch {
                                    try {
                                        val bytes = backupOperations.create(
                                            state = state,
                                            revision = stateRevision,
                                            password = ui.password,
                                            progress = { p, status ->
                                                ui.exportProgress = p
                                                ui.exportStatus = status
                                            }
                                        )
                                        ui.generatedArchive = bytes
                                        haptic.perform(HapticKind.SUCCESS)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (e: Exception) {
                                        ui.createError = BackupSheetError.CREATE_FAILED
                                        haptic.perform(HapticKind.WARNING)
                                    } finally {
                                        ui.isExporting = false
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Archive Ready - Native Save / Share
                    val archive = ui.generatedArchive!!
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
                            ui.createError = null
                            ui.saveSuccess = true
                            haptic.perform(HapticKind.SUCCESS)
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            onBackupSaved(now)
                        },
                        onShareOpened = { ui.createError = null },
                        onError = { failure ->
                            ui.createError = when (failure) {
                                BackupNativeFailure.SAVE -> BackupSheetError.SAVE_FAILED
                                BackupNativeFailure.SHARE -> BackupSheetError.SHARE_FAILED
                            }
                            haptic.perform(HapticKind.WARNING)
                        }
                    )

                    ui.createError?.let {
                        Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

                    if (ui.saveSuccess) {
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

}
