package com.motovault.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.ledger.POISHA_PER_TAKA
import com.motovault.app.domain.ledger.WorkLedger
import com.motovault.app.domain.ledger.WorkLedgerIssue
import com.motovault.app.domain.ledger.WorkReconciledSummary
import com.motovault.app.domain.ledger.WorkShiftRecord
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.Field
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.Panel
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.SheetDetent
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.VaultSwitch
import com.motovault.app.ui.WarningColor
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import com.motovault.app.ui.currentHaptic
import com.motovault.app.ui.parseNum
import com.motovault.app.ui.today
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.work_ledger_allocated_label
import motovault.composeapp.generated.resources.work_ledger_day_km_field
import motovault.composeapp.generated.resources.work_ledger_deduction_field
import motovault.composeapp.generated.resources.work_ledger_enable_desc
import motovault.composeapp.generated.resources.work_ledger_enable_toggle
import motovault.composeapp.generated.resources.work_ledger_err_day_km
import motovault.composeapp.generated.resources.work_ledger_err_deduction
import motovault.composeapp.generated.resources.work_ledger_err_gross
import motovault.composeapp.generated.resources.work_ledger_err_work_km
import motovault.composeapp.generated.resources.work_ledger_gross_field
import motovault.composeapp.generated.resources.work_ledger_incomplete_mileage
import motovault.composeapp.generated.resources.work_ledger_net_label
import motovault.composeapp.generated.resources.work_ledger_save_shift
import motovault.composeapp.generated.resources.work_ledger_saved_shifts_count
import motovault.composeapp.generated.resources.work_ledger_sheet_title
import motovault.composeapp.generated.resources.work_ledger_shift_saved
import motovault.composeapp.generated.resources.work_ledger_summary_title
import motovault.composeapp.generated.resources.work_ledger_work_km_field
import kotlin.math.roundToLong

@Composable
internal fun WorkLedgerSheet(
    state: VaultState,
    onToggleWorkMode: (Boolean) -> Unit,
    onSaveShift: (VaultState, String) -> Unit,
    onDismiss: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val bikeId = state.ride?.id ?: "primary"
    val summary = remember(state.logs, state.ledger, bikeId) {
        WorkLedger.reconcile(state.logs, state.ledger.workShifts, bikeId, allowancePoisha = null)
    }
    var grossInput by remember { mutableStateOf("") }
    var deductionInput by remember { mutableStateOf("") }
    var workKmInput by remember { mutableStateOf("") }
    var dayKmInput by remember { mutableStateOf("") }

    val parsed = rememberWorkShiftDraft(grossInput, deductionInput, workKmInput, dayKmInput)
    val reasonText = workShiftReasonText(parsed)
    val savedToast = parsed?.let { appStringResource(Res.string.work_ledger_shift_saved, f.money(it.grossTaka)) }.orEmpty()

    Sheet(
        title = appStringResource(Res.string.work_ledger_sheet_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = if (state.ledger.workModeEnabled) {
            {
                VaultButton(
                    text = reasonText ?: appStringResource(Res.string.work_ledger_save_shift),
                    full = true,
                    enabled = reasonText == null && parsed != null,
                    onClick = {
                        val valid = parsed ?: return@VaultButton
                        val shift = WorkShiftRecord(
                            id = "ws_${Clock.System.now().toEpochMilliseconds()}",
                            bikeId = bikeId,
                            date = today().toString(),
                            grossEarningsPoisha = (valid.grossTaka * POISHA_PER_TAKA).roundToLong(),
                            deductionsPoisha = (valid.deductionTaka * POISHA_PER_TAKA).roundToLong(),
                            workKm = valid.workKm,
                            totalDayKm = valid.dayKm
                        )
                        val updatedLedger = state.ledger.copy(workShifts = listOf(shift) + state.ledger.workShifts)
                        haptic.perform(HapticKind.SUCCESS)
                        grossInput = ""; deductionInput = ""; workKmInput = ""; dayKmInput = ""
                        onSaveShift(state.copy(ledger = updatedLedger), savedToast)
                    }
                )
            }
        } else null
    ) {
        Panel {
            VaultSwitch(
                title = appStringResource(Res.string.work_ledger_enable_toggle),
                subtitle = appStringResource(Res.string.work_ledger_enable_desc),
                checked = state.ledger.workModeEnabled,
                onCheckedChange = onToggleWorkMode
            )
        }
        if (state.ledger.workModeEnabled) {
            WorkSummaryPanel(summary)
            WorkEntryFields(
                gross = grossInput, deduction = deductionInput, workKm = workKmInput, dayKm = dayKmInput,
                onGross = { grossInput = it }, onDeduction = { deductionInput = it },
                onWorkKm = { workKmInput = it }, onDayKm = { dayKmInput = it }
            )
        } else if (summary.shiftCount > 0) {
            Text(text = appStringResource(Res.string.work_ledger_saved_shifts_count, f.n(summary.shiftCount.toLong())), fontSize = 13.sp, color = TextSecondary)
        }
    }
}

private data class ParsedWorkShift(
    val grossTaka: Double,
    val deductionTaka: Double,
    val workKm: Double,
    val dayKm: Double,
    val errorCase: Int
)

private fun rememberWorkShiftDraft(gross: String, deduction: String, workKm: String, dayKm: String): ParsedWorkShift? {
    val g = parseNum(gross)
    val d = if (deduction.isBlank()) 0.0 else parseNum(deduction)
    val w = parseNum(workKm)
    val t = parseNum(dayKm)
    val err = when {
        g == null || g <= 0.0 -> 1
        d == null || d < 0.0 || d > g -> 2
        w == null || w < 0.0 -> 3
        t == null || t < w -> 4
        else -> 0
    }
    return if (err == 0 && g != null && d != null && w != null && t != null) {
        ParsedWorkShift(g, d, w, t, 0)
    } else {
        ParsedWorkShift(0.0, 0.0, 0.0, 0.0, err)
    }
}

@Composable
private fun workShiftReasonText(parsed: ParsedWorkShift?): String? = when (parsed?.errorCase) {
    1 -> appStringResource(Res.string.work_ledger_err_gross)
    2 -> appStringResource(Res.string.work_ledger_err_deduction)
    3 -> appStringResource(Res.string.work_ledger_err_work_km)
    4 -> appStringResource(Res.string.work_ledger_err_day_km)
    else -> null
}

@Composable
private fun WorkSummaryPanel(summary: WorkReconciledSummary) {
    val f = currentFmt
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = appStringResource(Res.string.work_ledger_summary_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = appStringResource(Res.string.work_ledger_net_label, f.money(Money.taka(summary.netAfterAllocatedExpensePoisha))), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
        Text(text = appStringResource(Res.string.work_ledger_allocated_label, f.money(Money.taka(summary.allocatedExpensePoisha)), f.money(Money.taka(summary.actualTotalExpensePoisha))), fontSize = 12.sp, color = TextSecondary)
        if (WorkLedgerIssue.INCOMPLETE_MILEAGE in summary.issues) {
            Text(text = appStringResource(Res.string.work_ledger_incomplete_mileage), fontSize = 12.sp, color = WarningColor)
        }
    }
}

@Composable
private fun WorkEntryFields(
    gross: String,
    deduction: String,
    workKm: String,
    dayKm: String,
    onGross: (String) -> Unit,
    onDeduction: (String) -> Unit,
    onWorkKm: (String) -> Unit,
    onDayKm: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(label = appStringResource(Res.string.work_ledger_gross_field), value = gross, onChange = onGross, numeric = true)
        Field(label = appStringResource(Res.string.work_ledger_deduction_field), value = deduction, onChange = onDeduction, numeric = true)
        Field(label = appStringResource(Res.string.work_ledger_work_km_field), value = workKm, onChange = onWorkKm, numeric = true)
        Field(label = appStringResource(Res.string.work_ledger_day_km_field), value = dayKm, onChange = onDayKm, numeric = true)
    }
}
