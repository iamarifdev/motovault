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
@Composable
internal fun BackupPasswordSection(ui: BackupCreateState, createPasswordLabel: String, confirmPasswordLabel: String) {
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

                    // Error text if any
                    ui.createError?.let {
                        Text(text = backupErrorText(it), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

}
