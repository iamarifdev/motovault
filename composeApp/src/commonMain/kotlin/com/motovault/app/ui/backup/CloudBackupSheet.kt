package com.motovault.app.ui.backup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.motovault.app.ui.*
import motovault.composeapp.generated.resources.*

@Composable
internal fun CloudBackupSheet(
    onOpenLocalBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    Sheet(
        title = appStringResource(Res.string.cloud_backup_sheet_title),
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.cloud_backup_open_local_action),
                full = true,
                onClick = {
                    onDismiss()
                    onOpenLocalBackup()
                }
            )
        }
    ) {
        Panel {
            Text(
                text = appStringResource(Res.string.cloud_backup_status_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Text(
                text = appStringResource(Res.string.cloud_backup_status_disconnected),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Panel {
            Text(
                text = appStringResource(Res.string.cloud_backup_key_policy),
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = appStringResource(Res.string.cloud_backup_staged_note),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
