package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VisitRecordState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
internal fun ServiceRepairWarrantyControls(
    state: VaultState,
    visitState: VisitRecordState,
    onVisitState: (VisitRecordState) -> Unit,
    workshopPhone: String,
    onWorkshopPhone: (String) -> Unit,
    caseTitle: String,
    onCaseTitle: (String) -> Unit,
    warrantyMonths: Int,
    onWarrantyMonths: (Int) -> Unit,
    entry: NumberEntryState,
    keysOpen: Boolean,
    onFocusNumber: (String) -> Unit,
    f: Fmt
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultChip(
                text = appStringResource(Res.string.service_visit_paid),
                active = visitState == VisitRecordState.PAID,
                onClick = { onVisitState(VisitRecordState.PAID) }
            )
            VaultChip(
                text = appStringResource(Res.string.service_visit_quote),
                active = visitState == VisitRecordState.QUOTATION,
                onClick = { onVisitState(VisitRecordState.QUOTATION) }
            )
        }
        ServiceOptionalTextField(workshopPhone, appStringResource(Res.string.service_workshop_phone), onWorkshopPhone)
        if (state.repairCases.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.repairCases.take(4).forEach { rc ->
                    VaultChip(
                        text = rc.title,
                        active = caseTitle.equals(rc.title, ignoreCase = true),
                        onClick = { onCaseTitle(if (caseTitle.equals(rc.title, ignoreCase = true)) "" else rc.title) }
                    )
                }
            }
        }
        ServiceOptionalTextField(caseTitle, appStringResource(Res.string.service_repair_case_placeholder), onCaseTitle)
        if (visitState == VisitRecordState.PAID) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    0 to appStringResource(Res.string.service_warranty_none),
                    6 to appStringResource(Res.string.service_warranty_6m),
                    12 to appStringResource(Res.string.service_warranty_12m)
                ).forEach { (months, label) ->
                    VaultChip(text = label, active = warrantyMonths == months, onClick = { onWarrantyMonths(months) })
                }
            }
        }
        ServiceExtraChargesRow(entry = entry, keysOpen = keysOpen, onFocusNumber = onFocusNumber, f = f)
    }
}

@Composable
private fun ServiceExtraChargesRow(
    entry: NumberEntryState,
    keysOpen: Boolean,
    onFocusNumber: (String) -> Unit,
    f: Fmt
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "labour" to appStringResource(Res.string.service_labour),
            "misc" to appStringResource(Res.string.service_misc),
            "discount" to appStringResource(Res.string.service_discount)
        ).forEach { (k, label) ->
            val isActive = entry.activeField == k && keysOpen
            val v = parseNum(entry.values[k] ?: "")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) BrandTintColor else SurfaceColor)
                    .border(2.dp, if (isActive) BrandColor else BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onFocusNumber(k) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = label, fontSize = 12.sp, color = TextSecondary)
                Text(text = if (v != null) f.money(v) else "—", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun ServiceOptionalTextField(value: String, placeholder: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor2)
            .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = TextTertiary)
                inner()
            }
        )
    }
}

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
            .clip(RoundedCornerShape(8.dp))
            .background(BrandTintColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
