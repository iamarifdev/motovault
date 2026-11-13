package com.motovault.app.ui

import com.motovault.app.domain.capture.*
import com.motovault.app.domain.garage.mutateBike
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.vault.acceptCapture

internal data class CapturePrefill(
    val receipt: String?,
    val amount: Long?,
    val odometer: Double?,
    val rows: List<ParsedLine>,
    val targetBikeId: String? = null
)

internal fun CaptureDraft.prefill(
    amount: Long?,
    odometer: Double?,
    targetBikeId: String? = this.targetBikeId
) = CapturePrefill(
    id.takeIf { source == CaptureSource.SHARE }, amount, odometer,
    service.lines.map { ParsedLine(requireNotNull(it.line.catalogueId), it.line.quantity,
        it.line.unitPricePoisha?.let { amount -> Money.taka(amount.toLong()) }, it.source) },
    targetBikeId = targetBikeId
)

internal fun VaultState.acceptPrefill(prefill: CapturePrefill?): VaultState {
    if (prefill == null) return this
    return mutateBike(prefill.targetBikeId) { scoped ->
        prefill.receipt?.let(scoped::acceptCapture) ?: scoped
    }
}

internal fun VaultState.applyPrefillScopedMutation(
    prefill: CapturePrefill?,
    transform: (VaultState) -> VaultState
): VaultState = mutateBike(prefill?.targetBikeId, transform)
