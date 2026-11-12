package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.*

@Composable
internal fun CostSheetHost(sheet: ActiveSheet, state: VaultState, onDismiss: () -> Unit,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit, onUndo: () -> Unit, onToast: (ToastData?) -> Unit
) {
    val saved = appStringResource(if (sheet.name == "budget") Res.string.cost_budget_saved else Res.string.cost_saved)
    val save: (VaultState, (Boolean) -> Unit) -> Unit = { next, done ->
        onSave(next) { ok ->
            if (ok) onToast(ToastData(id = "cost_saved", message = saved, undoAction = onUndo))
            done(ok)
        }
    }
    when (sheet.name) {
        "expense" -> ExpenseSheet(state, sheet.props as? RideLog, onDismiss, save,
            initialAmount = (sheet.props as? CapturePrefill)?.amount)
        "budget" -> BudgetSheet(state, onDismiss, save)
        "cost-details" -> CostDetailsSheet(state, sheet.props as? CostDetailsSelection ?: CostDetailsSelection(), onDismiss)
    }
}

internal data class CostDetailsSelection(
    val period: com.motovault.app.domain.ledger.CostPeriod = com.motovault.app.domain.ledger.CostPeriod.ALL,
    val category: com.motovault.app.domain.ledger.CostCategory? = null
)
