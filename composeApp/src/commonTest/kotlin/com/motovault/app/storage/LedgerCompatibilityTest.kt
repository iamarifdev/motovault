package com.motovault.app.storage

import com.motovault.app.domain.backup.initializeBackupTestCrypto
import com.motovault.app.domain.ledger.*
import com.motovault.app.domain.model.*
import com.motovault.app.domain.vault.*
import com.motovault.app.storage.security.documentSha256
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class LedgerCompatibilityTest {
    @BeforeTest fun crypto() = initializeBackupTestCrypto()

    @Test fun oldJsonKeepsMoneyAndDefaultsWhileNewArchivePreservesIntegerAmountsAndPlans() = runBlocking {
        val old = requireNotNull(VaultState.decode("""{"ride":{"brandId":"yamaha","edition":"BD","year":2026,"odometer":1000.0},"logs":[{"title":"","detail":"","date":"2026-11-01","odometer":1000.0,"id":"old","cost":1.005}]}"""))
        assertEquals(1.005, old.logs.single().cost)
        assertNull(old.logs.single().costPoisha)
        assertEquals(LedgerPreferences(), old.ledger)
        val state = old.saveExpense(ExpenseDraft(CostCategory.CUSTOM, 12345, LocalDate(2026, 11, 2), "owner note"), LocalDate(2026, 11, 11))
            .copy(ledger = LedgerPreferences(100000, 5000).confirmPrice("petrol", 115.75, "2026-11-02"))
        assertEquals(state, VaultState.decode(VaultState.encode(state)))
        val adapter = BackupOperationsAdapter(::documentSha256) { error("No assets") }
        val archive = adapter.create(state, 4, "synthetic password") { _, _ -> }
        val payload = adapter.preview(state, archive, "synthetic password").payload
        assertEquals(state.logs, payload.logs.map { it.toRideLog() })
        assertEquals(VaultSettings.from(state), payload.preferences.toVaultSettings())
        assertEquals(12446L, CostLedger.summarize(payload.logs.map { it.toRideLog() }, LocalDate(2026, 11, 11), CostPeriod.ALL).totalPoisha)
    }
}
