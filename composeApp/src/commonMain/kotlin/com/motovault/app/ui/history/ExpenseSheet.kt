package com.motovault.app.ui

import com.motovault.app.domain.vault.saveExpense

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.*
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
internal fun ExpenseSheet(state: VaultState, existing: RideLog?, onDismiss: () -> Unit,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit, initialAmount: Long? = null
) {
    val f = currentFmt
    val entry = rememberNumberEntry(mapOf("amount" to ((existing?.let { CostLedger.amount(it) } ?: initialAmount)?.let { Money.decimal(it) } ?: "")), "amount")
    var category by remember { mutableStateOf(existing?.let(CostLedger::category) ?: CostCategory.PARKING) }
    var date by remember { mutableStateOf(existing?.date ?: today().toString()) }
    var note by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val draft = ExpenseDraft(category, Money.parse(toAscii(entry.values["amount"].orEmpty())),
        runCatching { LocalDate.parse(toAscii(date)) }.getOrNull(), note, existing?.id)
    val valid = ExpenseRules.validate(draft, today()) == null
    Sheet(title = appStringResource(if (existing == null) Res.string.cost_add else Res.string.cost_edit), onDismiss = onDismiss,
        footer = {
            VaultButton(appStringResource(Res.string.cost_save), full = true, enabled = valid && !saving, onClick = {
                val next = runCatching { state.saveExpense(draft, today()) }.getOrNull()
                if (next == null) failed = true else {
                    saving = true
                    onSave(next) { ok -> saving = false; failed = !ok; if (ok) onDismiss() }
                }
            })
            VaultKeypad(entry::press)
        }) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseRules.entryCategories.forEach { VaultChip(costCategoryLabel(it), active = category == it, onClick = { category = it }) }
        }
        CostNumberField(appStringResource(Res.string.cost_amount), entry, "amount")
        CostDateControls(date) { date = it }
        Field(appStringResource(Res.string.cost_note), note, { note = it })
        if (!valid) Text(appStringResource(Res.string.cost_invalid), color = ErrorColor, fontSize = 13.sp)
        if (failed) Text(appStringResource(Res.string.cost_failed), color = ErrorColor, fontSize = 13.sp)
    }
}

@Composable
internal fun CostNumberField(label: String, entry: NumberEntryState, field: String) {
    val value = entry.values[field].orEmpty()
    val display = if (value.isBlank()) appStringResource(Res.string.cost_empty_amount) else currentFmt.d(value)
    VaultChip(text = appStringResource(Res.string.cost_field_value, label, display), active = entry.activeField == field,
        onClick = { entry.setActive(field) }, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun CostDateControls(date: String, onChange: (String) -> Unit) {
    val fmt = currentFmt
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultChip(appStringResource(Res.string.date_today), active = toAscii(date) == today().toString(), onClick = { onChange(today().toString()) })
        val yesterday = LocalDate.fromEpochDays(today().toEpochDays() - 1).toString()
        VaultChip(appStringResource(Res.string.date_yesterday), active = toAscii(date) == yesterday, onClick = { onChange(yesterday) })
    }
    Field(appStringResource(Res.string.cost_date), fmt.d(date), { onChange(toAscii(it)) })
}
