package com.motovault.app.domain.ledger

import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.recordId
import kotlinx.datetime.LocalDate

const val MAX_EXPENSE_NOTE_LENGTH = 500
enum class ExpenseIssue { AMOUNT, DATE, CATEGORY, NOTE, MISSING }
data class ExpenseDraft(val category: CostCategory, val amountPoisha: Long?, val date: LocalDate?, val note: String, val id: String? = null)

object ExpenseRules {
    val entryCategories = listOf(CostCategory.PARKING, CostCategory.TOLL, CostCategory.CUSTOM)

    fun validate(draft: ExpenseDraft, today: LocalDate): ExpenseIssue? = when {
        draft.category !in entryCategories -> ExpenseIssue.CATEGORY
        draft.amountPoisha == null || draft.amountPoisha !in 1..MAX_COST_POISHA -> ExpenseIssue.AMOUNT
        draft.date == null || draft.date > today -> ExpenseIssue.DATE
        draft.note.length > MAX_EXPENSE_NOTE_LENGTH -> ExpenseIssue.NOTE
        else -> null
    }

    fun save(logs: List<RideLog>, odometer: Double, draft: ExpenseDraft, today: LocalDate): List<RideLog> {
        require(validate(draft, today) == null)
        val previous = draft.id?.let { id -> logs.singleOrNull { it.id == id && it.kind == "expense" } }
        require(draft.id == null || previous != null)
        val row = RideLog(title = "", detail = "", date = requireNotNull(draft.date).toString(),
            odometer = previous?.odometer ?: odometer,
            id = previous?.id ?: recordId(), kind = "expense", cost = Money.taka(requireNotNull(draft.amountPoisha)),
            notes = draft.note.trim(), costPoisha = draft.amountPoisha, expenseCategory = draft.category.code)
        return if (previous == null) listOf(row) + logs else logs.map { if (it.id == row.id) row else it }
    }
}
