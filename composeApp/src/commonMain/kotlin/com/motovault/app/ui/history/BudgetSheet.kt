package com.motovault.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.Money
import motovault.composeapp.generated.resources.*

@Composable
internal fun BudgetSheet(state: VaultState, onDismiss: () -> Unit, onSave: (VaultState, (Boolean) -> Unit) -> Unit) {
    val entry = rememberNumberEntry(mapOf(
        "budget" to state.ledger.monthlyBudgetPoisha?.let { Money.decimal(it) }.orEmpty(),
        "allowance" to state.ledger.maintenanceAllowancePoisha?.let { Money.decimal(it) }.orEmpty()), "budget")
    val rawBudget = toAscii(entry.values["budget"].orEmpty())
    val rawAllowance = toAscii(entry.values["allowance"].orEmpty())
    val budget = Money.parse(rawBudget)
    val allowance = Money.parse(rawAllowance)
    val valid = (rawBudget.isBlank() || budget != null) && (rawAllowance.isBlank() || allowance != null)
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    Sheet(title = appStringResource(Res.string.cost_budget), onDismiss = onDismiss, footer = {
        VaultButton(appStringResource(Res.string.cost_save), full = true, enabled = valid && !saving, onClick = {
            saving = true
            onSave(state.copy(ledger = state.ledger.copy(monthlyBudgetPoisha = budget, maintenanceAllowancePoisha = allowance))) {
                saving = false; failed = !it; if (it) onDismiss()
            }
        })
        VaultKeypad(entry::press)
    }) {
        CostNumberField(appStringResource(Res.string.cost_budget), entry, "budget")
        CostNumberField(appStringResource(Res.string.cost_allowance), entry, "allowance")
        MonthlyBudgetSummary(state)
        Text(appStringResource(Res.string.cost_plan_only), color = TextSecondary, fontSize = 13.sp)
        if (!valid) Text(appStringResource(Res.string.cost_invalid), color = ErrorColor, fontSize = 13.sp)
        if (failed) Text(appStringResource(Res.string.cost_failed), color = ErrorColor, fontSize = 13.sp)
    }
}

@Composable
internal fun CostDetailsSheet(state: VaultState, selection: CostDetailsSelection, onDismiss: () -> Unit) {
    val f = currentFmt
    val summary = com.motovault.app.domain.ledger.CostLedger.summarize(state.logs, today(), selection.period, selection.category)
    Sheet(title = appStringResource(Res.string.cost_details), onDismiss = onDismiss) {
        Text(costPeriodLabel(selection.period), color = TextSecondary)
        com.motovault.app.domain.ledger.CostCategory.entries.filter { selection.category == null || it == selection.category }.forEach { category ->
            Text(appStringResource(Res.string.cost_field_value, costCategoryLabel(category),
                if (summary.totalPoisha == null) appStringResource(Res.string.cost_unavailable)
                else costMoney(summary.categories[category] ?: 0)), color = TextPrimary)
        }
        Text(appStringResource(Res.string.cost_incomplete), color = TextSecondary, fontSize = 13.sp)
        CostCoverage(summary)
        state.ledger.maintenanceAllowancePoisha?.let {
            Text(appStringResource(Res.string.cost_field_value, appStringResource(Res.string.cost_allowance), costMoney(it)), color = TextSecondary)
        }
        Text(appStringResource(Res.string.cost_plan_only), color = TextSecondary, fontSize = 13.sp)
    }
}
