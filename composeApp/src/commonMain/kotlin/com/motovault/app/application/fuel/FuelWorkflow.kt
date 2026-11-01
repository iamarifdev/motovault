package com.motovault.app.application.fuel

import com.motovault.app.application.VaultWriter
import kotlinx.coroutines.CancellationException

internal enum class FuelSaveOutcome { SAVED, CONFLICT, INVALID, FAILED }

/** Coordinates fuel mutations through the one revision-aware vault writer. */
internal class FuelWorkflow<T>(
    private val writer: VaultWriter<T>,
    private val builder: FuelEntryBuilder<T>,
    private val editor: FuelEntryEditor<T>
) {
    fun create(request: FuelEntryRequest, expectedRevision: Long, completed: (FuelSaveOutcome) -> Unit) =
        save(expectedRevision, { builder.build(it, request) }, completed)

    fun edit(request: FuelEditRequest, expectedRevision: Long, completed: (FuelSaveOutcome) -> Unit) =
        save(expectedRevision, { editor.edit(it, request) }, completed)

    private fun save(expectedRevision: Long, prepare: (T) -> T, completed: (FuelSaveOutcome) -> Unit) {
        val snapshot = writer.snapshots.value
        if (snapshot.revision != expectedRevision || snapshot.saving || snapshot.loading || snapshot.restorationFailed) {
            completed(FuelSaveOutcome.CONFLICT)
            return
        }
        val next = try {
            prepare(snapshot.state)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            completed(FuelSaveOutcome.INVALID)
            return
        } catch (_: Exception) {
            completed(FuelSaveOutcome.FAILED)
            return
        }
        writer.commit(next, expectedRevision) { saved ->
            completed(when {
                saved -> FuelSaveOutcome.SAVED
                writer.snapshots.value.revision != expectedRevision -> FuelSaveOutcome.CONFLICT
                else -> FuelSaveOutcome.FAILED
            })
        }
    }
}
