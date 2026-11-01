package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.*

/** Root-shell strings kept outside the composition function to preserve its responsibility. */
internal data class VaultUiText(
    val undoFailedRetry: String,
    val undoFailedChanged: String,
    val privacyStartFailed: String,
    val unlockTitle: String,
    val unlockReason: String,
    val privacyVerifyFailed: String,
    val exitTitle: String,
    val exitReason: String,
    val privacyCancelled: String,
    val reminderGone: String,
    val defaultEdition: String,
    val recordDeleted: String
)

@Composable
internal fun vaultUiText(): VaultUiText = VaultUiText(
    undoFailedRetry = appStringResource(Res.string.toast_undo_failed_retry),
    undoFailedChanged = appStringResource(Res.string.toast_undo_failed_changed),
    privacyStartFailed = appStringResource(Res.string.privacy_auth_start_failed),
    unlockTitle = appStringResource(Res.string.privacy_unlock_title),
    unlockReason = appStringResource(Res.string.privacy_unlock_reason),
    privacyVerifyFailed = appStringResource(Res.string.privacy_verify_failed),
    exitTitle = appStringResource(Res.string.privacy_exit_title),
    exitReason = appStringResource(Res.string.privacy_exit_reason),
    privacyCancelled = appStringResource(Res.string.privacy_verify_cancelled),
    reminderGone = appStringResource(Res.string.toast_reminder_gone),
    defaultEdition = appStringResource(Res.string.ride_default_edition),
    recordDeleted = appStringResource(Res.string.toast_record_deleted)
)
