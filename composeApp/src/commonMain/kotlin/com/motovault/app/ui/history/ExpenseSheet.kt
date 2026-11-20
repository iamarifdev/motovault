package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.ledger.CostCategory
import com.motovault.app.domain.ledger.CostLedger
import com.motovault.app.domain.ledger.ExpenseDraft
import com.motovault.app.domain.ledger.ExpenseRules
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.vault.saveExpense
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import kotlin.math.roundToLong

private const val MAX_EXPENSE_UI_NOTE_LENGTH = 80

@Composable
internal fun ExpenseSheet(
    state: VaultState,
    existing: RideLog?,
    onDismiss: () -> Unit,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit,
    initialAmount: Long? = null,
    onToastMessage: ((String) -> Unit)? = null
) {
    val f = currentFmt
    val haptic = currentHaptic
    val currentDay = today()
    val yesterday = remember(currentDay) { LocalDate.fromEpochDays(currentDay.toEpochDays() - 1) }

    val initialText = (existing?.let { CostLedger.amount(it) } ?: initialAmount)
        ?.let { Money.taka(it).roundToLong().toString() }.orEmpty()
    val entry = rememberNumberEntry(mapOf("amount" to initialText), "amount")
    var category by remember { mutableStateOf(existing?.let(CostLedger::category) ?: CostCategory.CUSTOM) }
    var dateKey by remember {
        mutableStateOf(
            when {
                existing == null || existing.date == currentDay.toString() -> EntryDateKey.TODAY
                existing.date == yesterday.toString() -> EntryDateKey.YESTERDAY
                else -> EntryDateKey.CUSTOM
            }
        )
    }
    var customDate by remember { mutableStateOf(existing?.date ?: currentDay.toString()) }
    var note by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var typingNote by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val rawAmount = entry.values["amount"].orEmpty()
    val parsedTaka = NormalizedNumbers.parse(rawAmount)
    val amountPoisha = Money.parse(NormalizedNumbers.ascii(rawAmount))
    val amountTaka = amountPoisha?.let(Money::taka) ?: parsedTaka ?: 0.0
    val parsedDate = when (dateKey) {
        EntryDateKey.TODAY -> currentDay
        EntryDateKey.YESTERDAY -> yesterday
        EntryDateKey.CUSTOM -> runCatching { LocalDate.parse(toAscii(customDate)) }.getOrNull()
    }
    val draft = ExpenseDraft(category, amountPoisha, parsedDate, note, existing?.id)
    val reasonText = expenseValidationMessage(parsedTaka, amountPoisha, parsedDate, currentDay, draft)
    val toastMsg = expenseToastMessage(existing != null, f.money(amountTaka))

    Sheet(
        title = appStringResource(if (existing == null) Res.string.cost_add else Res.string.cost_edit),
        onDismiss = onDismiss,
        compact = true,
        footer = {
            ExpenseFooter(reasonText, amountTaka, failed, saving, typingNote, entry::press) {
                val next = runCatching { state.saveExpense(draft, currentDay) }.getOrNull()
                if (next == null) { failed = true; haptic.perform(HapticKind.ERROR); return@ExpenseFooter }
                saving = true
                onSave(next) { ok ->
                    saving = false; failed = !ok
                    if (ok) { haptic.perform(HapticKind.SUCCESS); onToastMessage?.invoke(toastMsg); onDismiss() }
                    else haptic.perform(HapticKind.ERROR)
                }
            }
        }
    ) {
        ExpenseSheetBody(
            category = category,
            rawAmount = rawAmount,
            typingNote = typingNote,
            dateKey = dateKey,
            customDate = customDate,
            note = note,
            onCategory = { category = it },
            onStopTypingNote = { typingNote = false },
            onDateKey = { dateKey = it },
            onCustomDate = { customDate = it },
            onNoteFocus = { typingNote = it },
            onNoteChange = { if (it.length <= MAX_EXPENSE_UI_NOTE_LENGTH) note = it }
        )
    }
}

@Composable
private fun ExpenseSheetBody(
    category: CostCategory,
    rawAmount: String,
    typingNote: Boolean,
    dateKey: EntryDateKey,
    customDate: String,
    note: String,
    onCategory: (CostCategory) -> Unit,
    onStopTypingNote: () -> Unit,
    onDateKey: (EntryDateKey) -> Unit,
    onCustomDate: (String) -> Unit,
    onNoteFocus: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val f = currentFmt
    val focusManager = LocalFocusManager.current
    ChipLine(contentDescription = appStringResource(Res.string.expense_category_desc)) {
        ExpenseRules.entryCategories.forEach { item ->
            EntryChip(text = costCategoryLabel(item), active = category == item, onClick = { onCategory(item) })
        }
    }
    EntryAmountCard(
        caption = appStringResource(Res.string.expense_amount_question),
        valueText = f.editableNumber(rawAmount),
        isPlaceholder = rawAmount.isEmpty(),
        active = !typingNote,
        amountDescription = appStringResource(Res.string.expense_amount_question),
        leadingUnit = appStringResource(Res.string.fuel_currency_symbol),
        onClick = { focusManager.clearFocus(); onStopTypingNote() }
    )
    ChipLine(contentDescription = appStringResource(Res.string.cost_date)) {
        DateChips(dateKey = dateKey, customDate = customDate, onSelectKey = onDateKey, onCustomDateChange = onCustomDate)
    }
    ExpenseNoteField(note = note, focused = typingNote, onFocusChange = onNoteFocus, onValueChange = onNoteChange)
}

@Composable
private fun ExpenseNoteField(
    note: String,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val noteDescription = appStringResource(Res.string.cost_note)
    val placeholder = appStringResource(Res.string.expense_note_placeholder)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = note,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            keyboardOptions = FieldKeyboards.sentence,
            keyboardActions = rememberFieldKeyboardActions(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .semantics { contentDescription = noteDescription },
            decorationBox = { inner ->
                if (note.isEmpty()) Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                inner()
            }
        )
    }
}

@Composable
private fun ExpenseFooter(
    reasonText: String?,
    amountTaka: Double,
    failed: Boolean,
    saving: Boolean,
    typingNote: Boolean,
    onKey: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val f = currentFmt
    val saveLabel = appStringResource(Res.string.fuel_save_amount, f.money(amountTaka))
    if (failed) {
        Text(
            text = appStringResource(Res.string.cost_failed),
            color = ErrorColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    VaultButton(
        text = reasonText ?: saveLabel,
        contentDescription = reasonText ?: saveLabel,
        full = true,
        enabled = reasonText == null && !saving,
        onClick = onSaveClick
    )
    if (!typingNote) {
        VaultKeypad(onKey = onKey, decimal = false)
    }
}

@Composable
private fun expenseValidationMessage(
    parsedTaka: Double?,
    amountPoisha: Long?,
    parsedDate: LocalDate?,
    today: LocalDate,
    draft: ExpenseDraft
): String? = when {
    parsedTaka == null || parsedTaka <= 0.0 -> appStringResource(Res.string.fuel_enter_amount)
    amountPoisha == null -> appStringResource(Res.string.cost_invalid)
    parsedDate == null -> appStringResource(Res.string.fuel_invalid_date)
    parsedDate > today -> appStringResource(Res.string.fuel_future_date)
    ExpenseRules.validate(draft, today) != null -> appStringResource(Res.string.cost_invalid)
    else -> null
}

@Composable
private fun expenseToastMessage(isEdit: Boolean, moneyText: String): String =
    if (isEdit) appStringResource(Res.string.expense_updated)
    else appStringResource(Res.string.expense_added, moneyText)
