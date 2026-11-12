package com.motovault.app.domain.vault

import com.motovault.app.domain.capture.CaptureReceipts

/** Apply only to the same aggregate that is durably saved; failed/cancelled drafts consume no ID. */
fun VaultState.acceptCapture(id: String): VaultState = copy(captureReceipts = CaptureReceipts.accept(captureReceipts, id))
