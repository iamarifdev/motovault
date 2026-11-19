package com.motovault.app.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.SettingsCardStack
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.appStringResource
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.cloud_backup_key_policy
import motovault.composeapp.generated.resources.cloud_backup_open_local_action
import motovault.composeapp.generated.resources.cloud_backup_sheet_title
import motovault.composeapp.generated.resources.cloud_backup_staged_note
import motovault.composeapp.generated.resources.cloud_backup_status_disconnected
import motovault.composeapp.generated.resources.cloud_backup_status_label

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
        SettingsCardStack {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}
