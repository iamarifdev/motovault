package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.privacy.VaultAuthAction
import com.motovault.app.ui.backup.BackupSheet
import com.motovault.app.ui.backup.BackupSheetTab
import com.motovault.app.ui.backup.CloudBackupSheet
import com.motovault.app.ui.backup.RecoverySheet
import com.motovault.app.ui.privacy.PrivacySheet

@Composable
internal fun RenderSettingsSheet(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    val state = ctx.state
    when (sheet.name) {
        "settings" -> (sheet.props as? SettingsSection)?.let { section ->
            SettingsSheet(
                state = state,
                section = section,
                onUpdateState = { next -> ctx.onCommit(next, null) },
                onOpenSheet = { name -> ctx.onSheet(ActiveSheet(name)) },
                onDismiss = { ctx.onSheet(null) }
            )
        }
        "reminders" -> {
            val target = sheet.props as? Pair<*, *>
            ReminderSettingsSheet(
                state = state,
                type = target?.first as? String,
                id = target?.second as? String,
                onDismiss = { ctx.onSheet(null) },
                onOpen = { type, id -> ctx.onSheet(ActiveSheet("reminders", type to id)) },
                onSave = { next, done -> ctx.onCommit(next, done) }
            )
        }
        "privacy" -> PrivacyRoute(ctx)
        "backup", "backup-restore" -> BackupRoute(ctx)
        "recovery" -> RecoverySheet(
            recovery = ctx.recovery,
            revision = { ctx.session.snapshots.value.revision },
            onDismiss = { ctx.onSheet(null) }
        )
        "cloud-backup" -> CloudBackupSheet(
            onOpenLocalBackup = { ctx.onSheet(ActiveSheet("backup", BackupSheetTab.CREATE)) },
            onDismiss = { ctx.onSheet(null) }
        )
    }
}

@Composable
private fun PrivacyRoute(ctx: SheetRouteContext) {
    val state = ctx.state
    PrivacySheet(
        state = state,
        onUpdatePrivacy = { next ->
            val request = if (!state.privacy.enabled && next.enabled) {
                ctx.accessPolicy.beginAuthentication(VaultAuthAction.ENABLE)
            } else if (state.privacy.enabled && !next.enabled) {
                ctx.accessPolicy.beginAuthentication(VaultAuthAction.DISABLE)
            } else null
            val grant = request?.let { ctx.accessPolicy.completeAuthentication(it, true) }
            val committed = ctx.accessPolicy.settingsCommitted(next, grant)
            if (committed) {
                ctx.onCommit(state.copy(privacy = next), null)
            }
        },
        onLockNow = ctx.onLockNow,
        onDismiss = { ctx.onSheet(null) }
    )
}

@Composable
private fun BackupRoute(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    val state = ctx.state
    val initialTab = if (sheet.name == "backup-restore" || sheet.props == BackupSheetTab.RESTORE) {
        BackupSheetTab.RESTORE
    } else {
        BackupSheetTab.CREATE
    }
    BackupSheet(
        state = state,
        stateRevision = ctx.revision,
        backupOperations = ctx.backupOperations,
        vaultRevision = { ctx.session.snapshots.value.revision },
        initialTab = initialTab,
        onDismiss = { ctx.onSheet(null) },
        onBackupSaved = { timestamp ->
            ctx.onCommit(state.copy(lastBackupEpochMillis = timestamp), null)
        },
        onRestore = { archive, password, expectedRevision, completed ->
            ctx.backupRestore.restore(archive, password, expectedRevision) { result ->
                completed(result)
                if (result.isSuccess) {
                    ctx.onSheet(null)
                    ctx.onToast(null)
                    ctx.onRestoreSuccess()
                }
            }
        },
        onOpenRecovery = { ctx.onSheet(ActiveSheet("recovery")) }
    )
}
