package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VisitRecordState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
internal fun HistoryServiceMetaSection(
    state: VaultState,
    log: RideLog,
    f: Fmt,
    onTransitionVisit: ((String, VisitRecordState) -> Unit)? = null
) {
    val caseTitle = log.repairCaseId?.let { id -> state.repairCases.firstOrNull { it.id == id }?.title }
    val warranties = state.warranties.filter { it.visitLogId == log.id }
    val statusLabel = when (log.visitState) {
        VisitRecordState.QUOTATION.code -> appStringResource(Res.string.service_visit_quote)
        VisitRecordState.APPROVED.code -> appStringResource(Res.string.service_visit_approved)
        VisitRecordState.CANCELLED.code -> appStringResource(Res.string.service_visit_cancelled)
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statusLabel?.let { ServiceMetaBadge(it) }
            caseTitle?.let { ServiceMetaBadge(it) }
            warranties.firstOrNull()?.let { w ->
                val formatted = runCatching { LocalDate.parse(w.expiryDate) }.getOrNull()?.let { f.date(it) } ?: w.expiryDate
                ServiceMetaBadge(appStringResource(Res.string.service_warranty_badge, formatted))
            }
        }
        val canTransition = onTransitionVisit != null &&
            (log.visitState == VisitRecordState.QUOTATION.code || log.visitState == VisitRecordState.APPROVED.code)
        if (canTransition) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { onTransitionVisit?.invoke(log.id, VisitRecordState.PAID) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(appStringResource(Res.string.service_quote_approve_paid), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { onTransitionVisit?.invoke(log.id, VisitRecordState.CANCELLED) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(appStringResource(Res.string.service_quote_cancel), fontSize = 13.sp, color = ErrorColor)
                }
            }
        }
    }
}

@Composable
private fun ServiceMetaBadge(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = BrandColor,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(BrandTintColor)
            .border(1.dp, BrandColor.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
