package com.motovault.app.ui.backup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.RecoverySummary
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.VaultWriteConflict
import com.motovault.app.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*

private sealed interface RecoveryPage {
    data object Loading : RecoveryPage
    data class List(val entries: kotlin.collections.List<RecoverySummary>) : RecoveryPage
    data class Review(val entry: RecoverySummary, val revision: Long) : RecoveryPage
    data object Working : RecoveryPage
    data object LoadFailed : RecoveryPage
    data class Failed(val stale: Boolean) : RecoveryPage
    data object Success : RecoveryPage
}

@Composable
private fun recoveryDate(entry: RecoverySummary): String {
    val fmt = currentFmt
    return entry.createdAtEpochMillis?.let { millis ->
        runCatching { fmt.date(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date) }.getOrNull()
    } ?: appStringResource(Res.string.recovery_unknown_date)
}

@Composable
internal fun RecoverySheet(recovery: VaultRecovery, revision: () -> Long, onDismiss: () -> Unit) {
    var page by remember { mutableStateOf<RecoveryPage>(RecoveryPage.Loading) }
    var reload by remember { mutableIntStateOf(0) }
    val fmt = currentFmt
    LaunchedEffect(reload) {
        page = RecoveryPage.Loading
        try { page = RecoveryPage.List(recovery.list()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { page = RecoveryPage.LoadFailed }
    }
    fun reloadList() { reload++ }
    Sheet(title = appStringResource(Res.string.recovery_title), onDismiss = onDismiss,
        detent = SheetDetent.TALL, dismissEnabled = page != RecoveryPage.Working) {
        when (val current = page) {
            RecoveryPage.Loading -> Text(appStringResource(Res.string.recovery_loading), color = TextSecondary)
            is RecoveryPage.List -> {
                Text(appStringResource(Res.string.recovery_description), color = TextSecondary)
                if (current.entries.isEmpty()) Text(appStringResource(Res.string.recovery_empty), color = TextSecondary)
                current.entries.forEach { entry ->
                    Column(Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(entry.bikeName ?: appStringResource(Res.string.recovery_unknown_bike), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(recoveryDate(entry), color = TextSecondary)
                        Text(appStringResource(Res.string.recovery_counts, fmt.n(entry.recordCount.toLong()), fmt.n(entry.documentCount.toLong())), color = TextSecondary)
                        VaultButton(text = appStringResource(Res.string.recovery_review), kind = ButtonKind.SECONDARY, full = true,
                            onClick = { page = RecoveryPage.Review(entry, revision()) })
                    }
                }
            }
            is RecoveryPage.Review -> {
                Text(appStringResource(Res.string.recovery_confirm_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(current.entry.bikeName ?: appStringResource(Res.string.recovery_unknown_bike), color = TextPrimary)
                Text(recoveryDate(current.entry), color = TextSecondary)
                Text(appStringResource(Res.string.recovery_counts, fmt.n(current.entry.recordCount.toLong()), fmt.n(current.entry.documentCount.toLong())), color = TextSecondary)
                Text(appStringResource(Res.string.recovery_confirm_body), color = TextSecondary)
                Text(appStringResource(Res.string.recovery_device_notice), color = TextSecondary)
                VaultButton(text = appStringResource(Res.string.recovery_confirm), full = true, onClick = {
                    page = RecoveryPage.Working
                    recovery.restore(current.entry.id, current.revision) { result ->
                        page = if (result.isSuccess) RecoveryPage.Success else RecoveryPage.Failed(result.exceptionOrNull() is VaultWriteConflict)
                    }
                })
                VaultButton(text = appStringResource(Res.string.recovery_cancel), kind = ButtonKind.SECONDARY, full = true, onClick = ::reloadList)
            }
            RecoveryPage.Working -> Text(appStringResource(Res.string.recovery_saving), color = TextSecondary)
            RecoveryPage.LoadFailed -> {
                Text(appStringResource(Res.string.recovery_load_failed), color = ErrorColor)
                VaultButton(text = appStringResource(Res.string.recovery_retry), kind = ButtonKind.SECONDARY, full = true, onClick = ::reloadList)
            }
            is RecoveryPage.Failed -> {
                Text(appStringResource(if (current.stale) Res.string.recovery_stale else Res.string.recovery_failed), color = ErrorColor)
                VaultButton(text = appStringResource(Res.string.recovery_back), kind = ButtonKind.SECONDARY, full = true, onClick = ::reloadList)
            }
            RecoveryPage.Success -> {
                Text(appStringResource(Res.string.recovery_success), color = TextPrimary)
                VaultButton(text = appStringResource(Res.string.recovery_back), kind = ButtonKind.SECONDARY, full = true, onClick = ::reloadList)
            }
        }
    }
}
