package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

/** Append-only renewal history; the event stores stable IDs and the display is resolved here. */
@Composable
internal fun RenewalHistory(document: VaultDocument) {
    val f = currentFmt
    SectionTitle(appStringResource(Res.string.renewal_history))
    if (document.renewals.isEmpty()) {
        Text(appStringResource(Res.string.renewal_history_empty), fontSize = 12.sp, color = TextSecondary)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.renewals.forEach { renewal ->
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SurfaceColor2)
                    .border(1.dp, BorderColor, RoundedCornerShape(13.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(displayDate(renewal.newExpiry, f::dateFull), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        renewal.feePoisha?.let { f.money(it / 100.0) } ?: appStringResource(Res.string.renewal_history_no_fee),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary
                    )
                }
                Text(
                    appStringResource(Res.string.renewal_history_previous, displayDate(renewal.previousExpiry, f::dateFull)),
                    fontSize = 12.sp, color = TextSecondary
                )
                Text(
                    appStringResource(Res.string.renewal_history_on, displayDate(renewal.renewedOn, f::dateFull)),
                    fontSize = 12.sp, color = TextSecondary
                )
                if (renewal.receiptId != null) {
                    Text(appStringResource(Res.string.renewal_history_receipt_attached), fontSize = 12.sp, color = HealthyColor)
                }
                renewal.notes.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary) }
            }
        }
    }
}

private fun displayDate(value: String, format: (LocalDate) -> String): String =
    value.takeIf { it.isNotBlank() }?.let { runCatching { format(LocalDate.parse(it)) }.getOrNull() } ?: value.ifBlank { "—" }
