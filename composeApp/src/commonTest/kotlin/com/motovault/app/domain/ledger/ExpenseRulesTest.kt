package com.motovault.app.domain.ledger

import com.motovault.app.domain.vault.saveExpense

import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ExpenseRulesTest {
    private val today = LocalDate(2026, 11, 11)
    private val initial = VaultState(ride = Ride("yamaha", "BD", 2026, 1000.0))

    @Test fun editMovesCostAcrossMonthBoundaryAndDeletePreservesOtherRecords() {
        val saved = initial.saveExpense(ExpenseDraft(CostCategory.PARKING, 125050, today, "paid"), today)
        val id = saved.logs.single().id
        val corrected = saved.saveExpense(ExpenseDraft(CostCategory.TOLL, 2501, LocalDate(2026, 10, 31), "corrected", id), today)
        assertEquals(id, corrected.logs.single().id)
        assertEquals(2501, corrected.logs.single().costPoisha)
        assertEquals(0, CostLedger.summarize(corrected.logs, today, CostPeriod.MONTH).totalPoisha)
        assertEquals(2501, CostLedger.summarize(corrected.logs, today, CostPeriod.ALL).totalPoisha)
        assertTrue(corrected.deleteLog(corrected.logs.single()).logs.isEmpty())
    }

    @Test fun rejectsRefundsInvalidDatesUnknownCategoriesAndMissingEdits() {
        val valid = ExpenseDraft(CostCategory.CUSTOM, 100, today, "")
        assertEquals(ExpenseIssue.AMOUNT, ExpenseRules.validate(valid.copy(amountPoisha = -1), today))
        assertEquals(ExpenseIssue.DATE, ExpenseRules.validate(valid.copy(date = LocalDate(2026, 11, 12)), today))
        assertEquals(ExpenseIssue.CATEGORY, ExpenseRules.validate(valid.copy(category = CostCategory.FUEL), today))
        assertEquals(ExpenseIssue.NOTE, ExpenseRules.validate(valid.copy(note = "x".repeat(MAX_EXPENSE_NOTE_LENGTH + 1)), today))
        assertFailsWith<IllegalArgumentException> { initial.saveExpense(valid.copy(id = "missing"), today) }
    }

    @Test fun budgetsAreOptionalPlansAndOldPriceEditsDoNotReplaceNewerConfirmation() {
        val prefs = LedgerPreferences(monthlyBudgetPoisha = 100000, maintenanceAllowancePoisha = 10000)
            .confirmPrice("octane", 121.50, "2026-11-10")
        assertEquals(prefs, prefs.confirmPrice("octane", 120.0, "2026-11-01"))
        assertEquals(122.75, prefs.confirmPrice("octane", 122.75, "2026-11-11").confirmedPrices["octane"]?.takaPerLitre)
        assertEquals(0, CostLedger.summarize(initial.copy(ledger = prefs).logs, today, CostPeriod.MONTH).totalPoisha)
        assertFailsWith<IllegalArgumentException> { LedgerPreferences(monthlyBudgetPoisha = -1) }
    }
}
