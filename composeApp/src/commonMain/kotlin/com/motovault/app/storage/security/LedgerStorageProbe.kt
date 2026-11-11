package com.motovault.app.storage.security

import com.motovault.app.domain.ledger.*
import com.motovault.app.domain.model.*
import com.motovault.app.domain.vault.*
import com.motovault.app.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** Fresh synthetic database only. Uses the production writer, Room and archive crypto on each host. */
internal suspend fun verifyLedgerStorageProbe(open: () -> VaultDatabase): String = coroutineScope {
    val date = LocalDate(2026, 11, 11)
    val legacy = VaultState(ride = Ride("yamaha", "Synthetic", 2026, 1000.0), logs = listOf(
        RideLog("", "", "2026-11-01", 1000.0, id = "legacy-service", kind = "service", cost = 1.005)))
    val db = open()
    val childScope = CoroutineScope(currentCoroutineContext() + SupervisorJob())
    try {
        db.vault().replace(legacy)
        val writer = VaultSession(childScope, restore = { requireNotNull(db.vault().read()) },
            write = { db.vault().replace(it) }, cleanup = {}, recover = {})
        writer.snapshots.first { !it.loading }
        val entered = legacy.saveExpense(ExpenseDraft(CostCategory.PARKING, 125050, date, "synthetic"), date)
            .copy(ledger = LedgerPreferences(200000, 10000).confirmPrice("octane", 121.5, date.toString()))
        check(commitLedger(writer, entered))
        val row = entered.logs.first()
        val edited = entered.saveExpense(ExpenseDraft(CostCategory.TOLL, 2501, LocalDate(2026, 10, 31), "corrected", row.id), date)
        check(commitLedger(writer, edited))
        check(CostLedger.summarize(requireNotNull(db.vault().read()).logs, date, CostPeriod.MONTH).totalPoisha == 101L)
        check(commitLedger(writer, edited.deleteLog(edited.logs.first())))
        val undone = CompletableDeferred<Boolean>()
        writer.undo(writer.snapshots.value.revision) { undone.complete(it) }
        check(undone.await())
        val durable = requireNotNull(db.vault().read())
        check(durable == edited)
        check(CostLedger.summarize(durable.logs, date, CostPeriod.ALL).totalPoisha == 2602L)
        val adapter = BackupOperationsAdapter(::documentSha256) { error("No fixture assets") }
        val archive = adapter.create(durable, writer.snapshots.value.revision, "synthetic ledger password") { _, _ -> }
        val payload = adapter.preview(durable, archive, "synthetic ledger password").payload
        check(payload.logs.map { it.toRideLog() } == durable.logs)
        check(payload.preferences.toVaultSettings().ledger == durable.ledger)
        check(VaultState.decode(VaultState.encode(durable))?.logs == durable.logs)
    } finally { childScope.cancel(); db.close() }
    val reopened = open()
    try {
        val state = requireNotNull(reopened.vault().read())
        check(state.logs.first().costPoisha == 2501L)
        check(state.ledger.monthlyBudgetPoisha == 200000L)
        check(state.ledger.confirmedPrices["octane"]?.takaPerLitre == 121.5)
    } finally { reopened.close() }
    "native-ledger-legacy-correction-delete-undo-room-reopen-and-encrypted-backup"
}

private suspend fun commitLedger(writer: com.motovault.app.application.VaultWriter<VaultState>, next: VaultState): Boolean {
    val completed = CompletableDeferred<Boolean>()
    writer.commit(next, writer.snapshots.value.revision) { completed.complete(it) }
    return completed.await()
}
