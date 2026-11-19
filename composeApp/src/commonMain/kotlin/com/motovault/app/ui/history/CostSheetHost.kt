package com.motovault.app.ui

import androidx.compose.runtime.Composable

@Composable
internal fun CostSheetHost(
    sheet: ActiveSheet,
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit,
    onUndo: () -> Unit,
    onToast: (ToastData?) -> Unit
) {
    if (sheet.name == "expense") {
        ExpenseSheet(
            state = state,
            existing = sheet.props as? RideLog,
            onDismiss = onDismiss,
            onSave = onSave,
            initialAmount = (sheet.props as? CapturePrefill)?.amount,
            onToastMessage = { msg -> onToast(ToastData(id = "cost_saved", message = msg, undoAction = onUndo)) }
        )
    }
}
