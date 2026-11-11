package com.motovault.app.domain.vault

import com.motovault.app.domain.ledger.ExpenseDraft
import com.motovault.app.domain.ledger.ExpenseRules
import kotlinx.datetime.LocalDate

/** Pure state transition. Only the vault coordinator may commit the result. */
fun VaultState.saveExpense(draft: ExpenseDraft, today: LocalDate): VaultState =
    copy(logs = ExpenseRules.save(logs, requireNotNull(ride).odometer, draft, today))
