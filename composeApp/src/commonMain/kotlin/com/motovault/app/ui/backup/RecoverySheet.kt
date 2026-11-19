package com.motovault.app.ui.backup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.RecoverySummary
import com.motovault.app.application.VaultRecovery
import com.motovault.app.application.VaultWriteConflict
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.ErrorColor
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.SheetDetent
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.recovery_back
import motovault.composeapp.generated.resources.recovery_cancel
import motovault.composeapp.generated.resources.recovery_confirm
import motovault.composeapp.generated.resources.recovery_confirm_body
import motovault.composeapp.generated.resources.recovery_confirm_title
import motovault.composeapp.generated.resources.recovery_counts
import motovault.composeapp.generated.resources.recovery_description
import motovault.composeapp.generated.resources.recovery_device_notice
import motovault.composeapp.generated.resources.recovery_empty
import motovault.composeapp.generated.resources.recovery_failed
import motovault.composeapp.generated.resources.recovery_load_failed
import motovault.composeapp.generated.resources.recovery_loading
import motovault.composeapp.generated.resources.recovery_retry
import motovault.composeapp.generated.resources.recovery_retry_review
import motovault.composeapp.generated.resources.recovery_review
import motovault.composeapp.generated.resources.recovery_saving
import motovault.composeapp.generated.resources.recovery_stale
import motovault.composeapp.generated.resources.recovery_success
import motovault.composeapp.generated.resources.recovery_title
import motovault.composeapp.generated.resources.recovery_unknown_bike
import motovault.composeapp.generated.resources.recovery_unknown_date

private sealed interface RecoveryPage {
    data object Loading : RecoveryPage
    data class List(val entries: kotlin.collections.List<RecoverySummary>) : RecoveryPage
    data class Review(val entry: RecoverySummary, val revision: Long) : RecoveryPage
    data object Working : RecoveryPage
    data object LoadFailed : RecoveryPage
    data class Failed(val entry: RecoverySummary, val stale: Boolean) : RecoveryPage
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
    LaunchedEffect(reload) {
        page = RecoveryPage.Loading
        try {
            page = RecoveryPage.List(recovery.list())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            page = RecoveryPage.LoadFailed
        }
    }
    fun reloadList() { reload++ }

    Sheet(
        title = appStringResource(Res.string.recovery_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        dismissEnabled = page != RecoveryPage.Working,
        footer = recoveryFooter(page, recovery, revision, onPage = { page = it }, onReload = ::reloadList)
    ) {
        RecoverySheetBody(page = page, revision = revision, onPage = { page = it })
    }
}

private fun recoveryFooter(
    page: RecoveryPage,
    recovery: VaultRecovery,
    revision: () -> Long,
    onPage: (RecoveryPage) -> Unit,
    onReload: () -> Unit
): (@Composable () -> Unit)? = when (page) {
    is RecoveryPage.Review -> {
        {
            VaultButton(text = appStringResource(Res.string.recovery_confirm), full = true, onClick = {
                onPage(RecoveryPage.Working)
                recovery.restore(page.entry.id, page.revision) { result ->
                    onPage(if (result.isSuccess) RecoveryPage.Success else RecoveryPage.Failed(page.entry, result.exceptionOrNull() is VaultWriteConflict))
                }
            })
            VaultButton(text = appStringResource(Res.string.recovery_cancel), kind = ButtonKind.SECONDARY, full = true, onClick = onReload)
        }
    }
    is RecoveryPage.Failed -> {
        {
            VaultButton(text = appStringResource(Res.string.recovery_retry_review), full = true, onClick = {
                if (page.stale) onReload() else onPage(RecoveryPage.Review(page.entry, revision()))
            })
        }
    }
    RecoveryPage.LoadFailed -> {
        { VaultButton(text = appStringResource(Res.string.recovery_retry), full = true, onClick = onReload) }
    }
    RecoveryPage.Success -> {
        { VaultButton(text = appStringResource(Res.string.recovery_back), kind = ButtonKind.SECONDARY, full = true, onClick = onReload) }
    }
    RecoveryPage.Loading, is RecoveryPage.List, RecoveryPage.Working -> null
}

@Composable
private fun RecoverySheetBody(page: RecoveryPage, revision: () -> Long, onPage: (RecoveryPage) -> Unit) {
    val fmt = currentFmt
    when (page) {
        RecoveryPage.Loading -> Text(appStringResource(Res.string.recovery_loading), color = TextSecondary)
        is RecoveryPage.List -> {
            Text(appStringResource(Res.string.recovery_description), color = TextSecondary)
            if (page.entries.isEmpty()) Text(appStringResource(Res.string.recovery_empty), color = TextSecondary)
            page.entries.forEach { entry ->
                Column(
                    Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(entry.bikeName ?: appStringResource(Res.string.recovery_unknown_bike), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(recoveryDate(entry), color = TextSecondary)
                    Text(appStringResource(Res.string.recovery_counts, fmt.n(entry.recordCount.toLong()), fmt.n(entry.documentCount.toLong())), color = TextSecondary)
                    VaultButton(text = appStringResource(Res.string.recovery_review), kind = ButtonKind.SECONDARY, full = true,
                        onClick = { onPage(RecoveryPage.Review(entry, revision())) })
                }
            }
        }
        is RecoveryPage.Review -> {
            Text(appStringResource(Res.string.recovery_confirm_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(page.entry.bikeName ?: appStringResource(Res.string.recovery_unknown_bike), color = TextPrimary)
            Text(recoveryDate(page.entry), color = TextSecondary)
            Text(appStringResource(Res.string.recovery_counts, fmt.n(page.entry.recordCount.toLong()), fmt.n(page.entry.documentCount.toLong())), color = TextSecondary)
            Text(appStringResource(Res.string.recovery_confirm_body), color = TextSecondary)
            Text(appStringResource(Res.string.recovery_device_notice), color = TextSecondary)
        }
        RecoveryPage.Working -> Text(appStringResource(Res.string.recovery_saving), color = TextSecondary)
        RecoveryPage.LoadFailed -> Text(appStringResource(Res.string.recovery_load_failed), color = ErrorColor)
        is RecoveryPage.Failed -> Text(appStringResource(if (page.stale) Res.string.recovery_stale else Res.string.recovery_failed), color = ErrorColor)
        RecoveryPage.Success -> Text(appStringResource(Res.string.recovery_success), color = TextPrimary)
    }
}
