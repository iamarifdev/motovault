package com.motovault.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.ledger.WorkLedger
import com.motovault.app.domain.ledger.WorkLedgerIssue
import com.motovault.app.domain.ledger.WorkShiftRecord
import com.motovault.app.ui.*
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.*

@Composable
internal fun WorkLedgerHistoryCard(
    state: VaultState,
    onOpenWorkSheet: () -> Unit
) {
    val f = currentFmt
    val bikeId = state.ride?.id
    val summary = remember(state.logs, state.ledger, bikeId) {
        WorkLedger.reconcile(
            logs = state.logs,
            shifts = state.ledger.workShifts,
            bikeId = bikeId,
            allowancePoisha = state.ledger.maintenanceAllowancePoisha
        )
    }
    val subtitle = if (state.ledger.workModeEnabled) {
        appStringResource(
            Res.string.work_ledger_card_subtitle_on,
            f.n(summary.shiftCount.toLong()),
            f.money(Money.taka(summary.netAfterAllocatedExpensePoisha))
        )
    } else {
        appStringResource(Res.string.work_ledger_card_subtitle_off)
    }

    Panel {
        Text(
            text = appStringResource(Res.string.work_ledger_card_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
        VaultButton(
            text = appStringResource(Res.string.work_ledger_open_action),
            size = ButtonSize.SM,
            kind = ButtonKind.SECONDARY,
            onClick = onOpenWorkSheet
        )
    }
}

@Composable
internal fun WorkLedgerSheet(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onDismiss: () -> Unit
) {
    val bikeId = state.ride?.id ?: "primary"
    val summary = remember(state.logs, state.ledger, bikeId) {
        WorkLedger.reconcile(
            logs = state.logs,
            shifts = state.ledger.workShifts,
            bikeId = bikeId,
            allowancePoisha = state.ledger.maintenanceAllowancePoisha
        )
    }
    var grossInput by remember { mutableStateOf("") }
    var deductionInput by remember { mutableStateOf("") }
    var workKmInput by remember { mutableStateOf("") }
    var dayKmInput by remember { mutableStateOf("") }

    val parsedGross = Money.parse(grossInput.trim())
    val parsedDeduction = deductionInput.trim().takeIf { it.isNotEmpty() }?.let(Money::parse) ?: 0L
    val canSaveShift = parsedGross != null && state.ledger.workModeEnabled

    Sheet(
        title = appStringResource(Res.string.work_ledger_sheet_title),
        onDismiss = onDismiss,
        footer = {
            if (state.ledger.workModeEnabled) {
                VaultButton(
                    text = appStringResource(Res.string.work_ledger_save_shift),
                    full = true,
                    enabled = canSaveShift,
                    onClick = {
                        val gross = parsedGross ?: return@VaultButton
                        val shift = WorkShiftRecord(
                            id = "ws_${Clock.System.now().toEpochMilliseconds()}",
                            bikeId = bikeId,
                            date = today().toString(),
                            grossEarningsPoisha = gross,
                            deductionsPoisha = parsedDeduction.coerceAtMost(gross),
                            workKm = workKmInput.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 },
                            totalDayKm = dayKmInput.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
                        )
                        val updatedLedger = state.ledger.copy(workShifts = listOf(shift) + state.ledger.workShifts)
                        onUpdateState(state.copy(ledger = updatedLedger))
                        grossInput = ""; deductionInput = ""; workKmInput = ""; dayKmInput = ""
                    }
                )
            }
        }
    ) {
        Panel {
            VaultSwitch(
                title = appStringResource(Res.string.work_ledger_enable_toggle),
                subtitle = appStringResource(Res.string.work_ledger_enable_desc),
                checked = state.ledger.workModeEnabled,
                onCheckedChange = { enabled ->
                    onUpdateState(state.copy(ledger = state.ledger.copy(workModeEnabled = enabled)))
                }
            )
        }
        WorkSummaryPanel(summary)
        if (state.ledger.workModeEnabled) {
            WorkEntryFields(
                gross = grossInput,
                deduction = deductionInput,
                workKm = workKmInput,
                dayKm = dayKmInput,
                onGross = { grossInput = it },
                onDeduction = { deductionInput = it },
                onWorkKm = { workKmInput = it },
                onDayKm = { dayKmInput = it }
            )
        }
    }
}

@Composable
private fun WorkSummaryPanel(summary: com.motovault.app.domain.ledger.WorkReconciledSummary) {
    val f = currentFmt
    Panel {
        Text(text = appStringResource(Res.string.work_ledger_summary_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            text = appStringResource(Res.string.work_ledger_net_label, f.money(Money.taka(summary.netAfterAllocatedExpensePoisha))),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor
        )
        Text(
            text = appStringResource(
                Res.string.work_ledger_allocated_label,
                f.money(Money.taka(summary.allocatedExpensePoisha)),
                f.money(Money.taka(summary.actualTotalExpensePoisha))
            ),
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = appStringResource(Res.string.work_ledger_allowance_label, f.money(Money.taka(summary.allowanceReservePoisha))),
            fontSize = 12.sp,
            color = TextSecondary
        )
        if (summary.pendingPayoutPoisha > 0L) {
            Text(
                text = appStringResource(Res.string.work_ledger_pending_label, f.money(Money.taka(summary.pendingPayoutPoisha))),
                fontSize = 12.sp,
                color = WarningColor
            )
        }
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
    Panel {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(label = appStringResource(Res.string.work_ledger_gross_field), value = gross, onChange = onGross, numeric = true)
            Field(label = appStringResource(Res.string.work_ledger_deduction_field), value = deduction, onChange = onDeduction, numeric = true)
            Field(label = appStringResource(Res.string.work_ledger_work_km_field), value = workKm, onChange = onWorkKm, numeric = true)
            Field(label = appStringResource(Res.string.work_ledger_day_km_field), value = dayKm, onChange = onDayKm, numeric = true)
        }
    }
}
