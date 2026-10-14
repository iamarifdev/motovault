package com.motovault.app

import com.motovault.app.storage.VaultSession
import com.motovault.app.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlin.test.*

class VaultSessionTest {
    private fun initial() = VaultState.create(Ride("ktm", "Owner", 2023, 1000.0))

    @Test fun reminderFailureCannotInvalidateRestoreOrCommittedSave() = runBlocking {
        val original = initial()
        var durable = original
        var reminders = 0
        val session = VaultSession(this, { durable }, { durable = it }, {}, {}, {
            reminders++
            error("native scheduling unavailable")
        })
        assertFalse(session.snapshots.value.loading)
        assertFalse(session.snapshots.value.restorationFailed)
        val next = original.copy(appearance = Appearance.DARK)
        var success: Boolean? = null
        session.commit(next, 0) { success = it }
        assertEquals(true, success)
        assertEquals(next, durable)
        assertEquals(next, session.snapshots.value.state)
        assertNull(session.snapshots.value.error)
        assertEquals(2, reminders)
    }

    @Test fun failedWriteNeverSchedulesUncommittedDocument() = runBlocking {
        val original = initial()
        val reconciled = mutableListOf<List<VaultDocument>>()
        val session = VaultSession(this, { original }, { error("disk full") }, {}, {}, { reconciled += it })
        val next = original.copy(documents = listOf(VaultDocument("Tax token", "TEST", "Synthetic", "2027-01-01")))
        session.commit(next, 0) { assertFalse(it) }
        assertEquals(listOf(emptyList()), reconciled)
        assertEquals(original, session.snapshots.value.state)
    }

    @Test fun recreatedObserverJoinsOutstandingSaveAndCannotReplaceFromOldSnapshot() = runBlocking {
        val original = initial()
        var durable = original
        var restores = 0
        val finishWrite = CompletableDeferred<Unit>()
        var delayWrite = true
        val session = VaultSession(this, { restores++; durable }, { next ->
            if (delayWrite) finishWrite.await()
            durable = next
        }, {}, {})
        val oldUi = launch(start = CoroutineStart.UNDISPATCHED) { session.snapshots.collect {} }
        val withFuel = original.logFuel(1000.0, 5.0)
        var saved: Boolean? = null
        session.commit(withFuel, 0) { saved = it }
        assertTrue(session.snapshots.value.saving)
        oldUi.cancelAndJoin()
        val newUi = launch(start = CoroutineStart.UNDISPATCHED) { session.snapshots.collect {} }
        assertTrue(session.snapshots.value.saving)
        assertEquals(1, restores)
        finishWrite.complete(Unit)
        val completed = session.snapshots.first { it.revision == 1L }
        assertEquals(withFuel, completed.state)
        assertEquals(true, saved)
        var staleAccepted: Boolean? = null
        session.commit(original.copy(appearance = Appearance.DARK), 0) { staleAccepted = it }
        assertEquals(false, staleAccepted)
        assertEquals(withFuel, durable)
        delayWrite = false
        val next = completed.state.copy(appearance = Appearance.DARK)
        session.commit(next, completed.revision) { assertTrue(it) }
        assertEquals(next, session.snapshots.first { it.revision == 2L }.state)
        assertEquals(withFuel.logs, durable.logs)
        newUi.cancelAndJoin()
    }

    @Test fun failedSaveKeepsStateRevisionAndAssetsForRetry() = runBlocking {
        val original = initial()
        var fail = true
        var cleanups = 0
        val session = VaultSession(this, { original }, { if (fail) error("disk full") }, { cleanups++ }, {})
        val next = original.logFuel(1000.0, 5.0)
        session.commit(next, 0) { assertFalse(it) }
        assertEquals(original, session.snapshots.value.state)
        assertEquals(0L, session.snapshots.value.revision)
        assertFalse(session.snapshots.value.saving)
        assertNotNull(session.snapshots.value.error)
        assertEquals(0, cleanups)
        fail = false
        session.commit(next, 0) { assertTrue(it) }
        assertEquals(next, session.snapshots.value.state)
        assertEquals(1, cleanups)
    }

    @Test fun recoveryFinishesBeforeWritesAndFailedRestoreNeverSweeps() = runBlocking {
        val finishRecovery = CompletableDeferred<Unit>()
        val original = initial()
        var writes = 0
        val session = VaultSession(this, { original }, { writes++ }, {}, { finishRecovery.await() })
        assertTrue(session.snapshots.value.loading)
        session.commit(original, 0) { assertFalse(it) }
        assertEquals(0, writes)
        finishRecovery.complete(Unit)
        assertEquals(original, session.snapshots.first { !it.loading }.state)
        var swept = false
        val broken = VaultSession(this, { error("corrupt vault") }, { writes++ }, {}, { swept = true })
        assertTrue(broken.snapshots.value.restorationFailed)
        assertFalse(swept)
        broken.commit(original, 0) { assertFalse(it) }
        assertEquals(0, writes)
    }
}
