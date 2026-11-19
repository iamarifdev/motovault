package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.vault_doc_date_invalid
import motovault.composeapp.generated.resources.vault_doc_days_left
import motovault.composeapp.generated.resources.vault_doc_expired_days
import motovault.composeapp.generated.resources.vault_doc_no_expiry
import motovault.composeapp.generated.resources.vault_doc_valid

enum class DocExpiryStatus { OVERDUE, SOON, OK, NONE }

data class DocExpiryInfo(val daysLeft: Int?, val status: DocExpiryStatus)

private val DarkOkBg = Color(0xFF153E24)
private val DarkOkFg = Color(0xFF86EFAC)
private val DarkSoonBg = Color(0xFF4E380B)
private val DarkSoonFg = Color(0xFFFDE68A)
private val DarkOverdueBg = Color(0xFF591B1D)
private val DarkOverdueFg = Color(0xFFFECACA)

fun documentExpiryInfo(doc: VaultDocument, leadDays: Int = 30): DocExpiryInfo {
    val days = doc.daysRemaining ?: return DocExpiryInfo(null, DocExpiryStatus.NONE)
    val warnThreshold = doc.reminderWarningDays ?: leadDays
    val status = when {
        days < 0 -> DocExpiryStatus.OVERDUE
        days <= warnThreshold -> DocExpiryStatus.SOON
        else -> DocExpiryStatus.OK
    }
    return DocExpiryInfo(days, status)
}

@Composable
fun ExpiryChip(
    doc: VaultDocument,
    modifier: Modifier = Modifier,
    warningDays: Int = doc.reminderWarningDays ?: 30
) {
    val f = currentFmt
    val palette = LocalVaultPalette.current
    val info = documentExpiryInfo(doc, warningDays)
    val days = info.daysLeft

    val icon = when (info.status) {
        DocExpiryStatus.OVERDUE -> "fi_triangle-alert.svg"
        DocExpiryStatus.SOON -> "fi_timer.svg"
        DocExpiryStatus.OK -> "fi_circle-check.svg"
        DocExpiryStatus.NONE -> "fi_circle-check.svg"
    }
    val text = when {
        days == null -> appStringResource(
            if (doc.expiry.isBlank()) Res.string.vault_doc_no_expiry else Res.string.vault_doc_date_invalid
        )
        info.status == DocExpiryStatus.OVERDUE -> appStringResource(Res.string.vault_doc_expired_days, f.days(-days))
        info.status == DocExpiryStatus.SOON -> appStringResource(Res.string.vault_doc_days_left, f.days(days))
        else -> appStringResource(Res.string.vault_doc_valid)
    }
    val (bgColor, fgColor) = if (palette.dark) {
        when (info.status) {
            DocExpiryStatus.OVERDUE -> DarkOverdueBg to DarkOverdueFg
            DocExpiryStatus.SOON -> DarkSoonBg to DarkSoonFg
            DocExpiryStatus.OK -> DarkOkBg to DarkOkFg
            DocExpiryStatus.NONE -> palette.text2.copy(alpha = 0.14f) to palette.text2
        }
    } else {
        val base = when (info.status) {
            DocExpiryStatus.OVERDUE -> palette.err
            DocExpiryStatus.SOON -> palette.warn
            DocExpiryStatus.OK -> palette.ok
            DocExpiryStatus.NONE -> palette.text2
        }
        base.copy(alpha = 0.14f) to base
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V3Icon(
            path = "feather_icon/$icon",
            description = null,
            modifier = Modifier.size(14.dp),
            tint = fgColor
        )
        Text(
            text = text,
            color = fgColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
