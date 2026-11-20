package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.ServiceLineItem
import com.motovault.app.domain.model.VisitRecordState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.history_discount
import motovault.composeapp.generated.resources.history_discount_amount
import motovault.composeapp.generated.resources.history_fuel_octane
import motovault.composeapp.generated.resources.history_fuel_petrol
import motovault.composeapp.generated.resources.history_full_tank_short
import motovault.composeapp.generated.resources.history_kv_note
import motovault.composeapp.generated.resources.history_kv_price
import motovault.composeapp.generated.resources.history_kv_pump
import motovault.composeapp.generated.resources.history_kv_service_charge
import motovault.composeapp.generated.resources.history_kv_tank
import motovault.composeapp.generated.resources.history_kv_workshop
import motovault.composeapp.generated.resources.history_line_with_detail
import motovault.composeapp.generated.resources.history_line_with_qty
import motovault.composeapp.generated.resources.history_partial_tank_short
import motovault.composeapp.generated.resources.history_price_fuel
import motovault.composeapp.generated.resources.history_price_unknown
import motovault.composeapp.generated.resources.history_tag_issue
import motovault.composeapp.generated.resources.history_tank_missed_suffix
import motovault.composeapp.generated.resources.history_workshop_with_phone
import motovault.composeapp.generated.resources.service_visit_cancelled
import motovault.composeapp.generated.resources.history_tag_quote
import motovault.composeapp.generated.resources.history_tag_warranty_months
import motovault.composeapp.generated.resources.service_warranty_badge

@Composable
internal fun HistoryLogKv(state: VaultState, log: RideLog, f: Fmt) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (log.isFuel) {
            HistoryFuelKvRows(log, f)
        } else {
            HistoryServiceKvRows(state, log, f)
        }
        if (log.notes.isNotBlank()) {
            HistoryKvRow(appStringResource(Res.string.history_kv_note), log.notes)
        }
    }
}

@Composable
private fun HistoryFuelKvRows(log: RideLog, f: Fmt) {
    log.fuelPrice?.let { price ->
        val fuelLabel = appStringResource(if (log.fuelType == "petrol") Res.string.history_fuel_petrol else Res.string.history_fuel_octane)
        HistoryKvRow(
            appStringResource(Res.string.history_kv_price),
            appStringResource(Res.string.history_price_fuel, f.money(price), fuelLabel)
        )
    }
    val baseTank = appStringResource(if (log.fullTank) Res.string.history_full_tank_short else Res.string.history_partial_tank_short)
    val tankText = if (log.missedFuelFill) appStringResource(Res.string.history_tank_missed_suffix, baseTank) else baseTank
    HistoryKvRow(appStringResource(Res.string.history_kv_tank), tankText)
    if (log.locationLabel.isNotBlank()) {
        HistoryKvRow(appStringResource(Res.string.history_kv_pump), log.locationLabel)
    }
}

@Composable
private fun HistoryServiceKvRows(state: VaultState, log: RideLog, f: Fmt) {
    log.serviceLines.forEach { line ->
        HistoryKvRow(formatServiceLineTitle(line, f), formatServiceLinePrice(line, f))
    }
    log.serviceLabourPoisha?.takeIf { it > 0 }?.let { labour ->
        HistoryKvRow(appStringResource(Res.string.history_kv_service_charge), f.money(labour / 100.0))
    }
    log.serviceDiscountPoisha?.takeIf { it > 0 }?.let { discount ->
        HistoryKvRow(
            appStringResource(Res.string.history_discount),
            appStringResource(Res.string.history_discount_amount, f.money(discount / 100.0))
        )
    }
    if (log.workshop.isNotBlank()) {
        val phone = state.workshopContacts.firstOrNull { c ->
            (log.workshopId != null && c.id == log.workshopId) || c.name == log.workshop
        }?.phone.orEmpty()
        val value = if (phone.isNotBlank()) {
            appStringResource(Res.string.history_workshop_with_phone, log.workshop, phone)
        } else {
            log.workshop
        }
        HistoryKvRow(appStringResource(Res.string.history_kv_workshop), value)
    }
}

@Composable
private fun formatServiceLineTitle(line: ServiceLineItem, f: Fmt): String {
    val base = serviceLineLabel(line)
    val detail = line.detail?.trim().orEmpty()
    val named = if (detail.isNotEmpty()) appStringResource(Res.string.history_line_with_detail, base, detail) else base
    return if (line.quantity > 1) appStringResource(Res.string.history_line_with_qty, named, f.d(line.quantity.toInt().toString())) else named
}

@Composable
private fun formatServiceLinePrice(line: ServiceLineItem, f: Fmt): String =
    line.unitPricePoisha?.let { f.money(it / 100.0 * line.quantity) }
        ?: appStringResource(Res.string.history_price_unknown)

@Composable
private fun HistoryKvRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HistoryServiceBadges(state: VaultState, log: RideLog, f: Fmt) {
    val statusLabel = when (log.visitState) {
        // v4 tags an open quote as "কোটেশন"; cancelled stays visible for older records.
        VisitRecordState.QUOTATION.code, VisitRecordState.APPROVED.code -> appStringResource(Res.string.history_tag_quote)
        VisitRecordState.CANCELLED.code -> appStringResource(Res.string.service_visit_cancelled)
        else -> null
    }
    val caseTitle = log.repairCaseId?.let { id -> state.repairCases.firstOrNull { it.id == id }?.title }
    val warranty = state.warranties.firstOrNull { it.visitLogId == log.id }
    val warrantyMonths = inferWarrantyMonths(log, state.warranties)
    if (statusLabel == null && caseTitle == null && warranty == null && warrantyMonths == 0) return

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        statusLabel?.let { HistoryBadgeTag(it) }
        caseTitle?.let { HistoryBadgeTag(appStringResource(Res.string.history_tag_issue, it)) }
        if (warrantyMonths > 0) {
            HistoryBadgeTag(appStringResource(Res.string.history_tag_warranty_months, f.d(warrantyMonths.toString())))
        } else warranty?.let { w ->
            // A warranty shorter than v4's 6-month choice has no month label, so show its end date.
            val formatted = runCatching { LocalDate.parse(w.expiryDate) }.getOrNull()?.let { f.date(it) } ?: w.expiryDate
            HistoryBadgeTag(appStringResource(Res.string.service_warranty_badge, formatted))
        }
    }
}

@Composable
private fun HistoryBadgeTag(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(BrandTintColor)
            .border(1.dp, BrandLineColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp)
    )
}
