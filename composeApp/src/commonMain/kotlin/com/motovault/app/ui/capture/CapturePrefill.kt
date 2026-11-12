package com.motovault.app.ui

import com.motovault.app.domain.capture.*
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.vault.acceptCapture

internal data class CapturePrefill(
    val receipt: String?, val amount: Long?, val odometer: Double?, val rows: List<ParsedLine>
)
internal fun CaptureDraft.prefill(amount: Long?, odometer: Double?) = CapturePrefill(
    id.takeIf { source == CaptureSource.SHARE }, amount, odometer,
    service.lines.map { ParsedLine(requireNotNull(it.line.catalogueId), it.line.quantity,
        it.line.unitPricePoisha?.let { amount -> Money.taka(amount.toLong()) }, it.source) }
)
internal fun VaultState.acceptPrefill(prefill: CapturePrefill?): VaultState = prefill?.receipt?.let(::acceptCapture) ?: this
