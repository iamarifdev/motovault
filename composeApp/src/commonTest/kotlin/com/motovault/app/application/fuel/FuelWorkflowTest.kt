package com.motovault.app.application.fuel

import com.motovault.app.application.VaultCoordinator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FuelWorkflowTest {
    private val request = FuelEntryRequest(600.0, 5.0, 120.0, 1000.0, LocalDate(2025, 1, 1), true, "octane", "", "")
    private val edit = FuelEditRequest("id", 600.0, 5.0, 120.0, 1000.0, LocalDate(2025, 1, 1), true, "octane", "", null)

    @Test
    fun savesThroughWriterAndRejectsStaleRevisionBeforeBuilding() = runBlocking {
        var durable = 1
        var builds = 0
        val writer = VaultCoordinator(this, 0, restore = { durable }, write = { durable = it }, assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {})
        val flow = FuelWorkflow(writer, FuelEntryBuilder<Int> { state, _ -> builds++; state + 1 }, FuelEntryEditor<Int> { state, _ -> state + 10 })
        var result: FuelSaveOutcome? = null
        flow.create(request, 0) { result = it }
        assertEquals(FuelSaveOutcome.SAVED, result)
        assertEquals(2, durable)
        flow.edit(edit, 0) { result = it }
        assertEquals(FuelSaveOutcome.CONFLICT, result)
        assertEquals(2, durable)
        assertEquals(1, builds)
        flow.edit(edit, 1) { result = it }
        assertEquals(FuelSaveOutcome.SAVED, result)
        assertEquals(12, durable)
    }

    @Test
    fun rejectsInvalidAndFailedWritesWithoutChangingSnapshot() = runBlocking {
        var durable = 1
        val writer = VaultCoordinator(this, 0, restore = { durable }, write = { error("synthetic failure") }, assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {})
        val flow = FuelWorkflow(writer, FuelEntryBuilder<Int> { state, _ -> state + 1 }, FuelEntryEditor<Int> { _, _ -> throw IllegalArgumentException("invalid") })
        var result: FuelSaveOutcome? = null
        flow.edit(edit, 0) { result = it }
        assertEquals(FuelSaveOutcome.INVALID, result)
        flow.create(request, 0) { result = it }
        assertEquals(FuelSaveOutcome.FAILED, result)
        assertEquals(1, durable)
        assertEquals(0L, writer.snapshots.value.revision)
    }

    @Test
    fun pendingWriteRejectsAnotherSaveAndCancellationPropagates() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var durable = 1
        val writer = VaultCoordinator(this, 0, restore = { durable }, write = { next -> gate.await(); durable = next }, assetIds = { emptySet() }, cleanup = {}, recover = {}, reconcile = {})
        val flow = FuelWorkflow(writer, FuelEntryBuilder<Int> { state, _ -> state + 1 }, FuelEntryEditor<Int> { _, _ -> throw CancellationException("cancel") })
        assertFailsWith<CancellationException> { flow.edit(edit, 0) {} }
        var first: FuelSaveOutcome? = null
        var second: FuelSaveOutcome? = null
        flow.create(request, 0) { first = it }
        flow.create(request, 0) { second = it }
        assertEquals(FuelSaveOutcome.CONFLICT, second)
        gate.complete(Unit)
        yield()
        assertEquals(FuelSaveOutcome.SAVED, first)
        assertEquals(2, durable)
    }
}
