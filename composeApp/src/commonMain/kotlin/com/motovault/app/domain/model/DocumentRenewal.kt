package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

/** Named business limits, not a fee schedule. The fee itself is always owner-entered. */
const val MAX_RENEWAL_FEE_POISHA = 10_000_000
const val MAX_RENEWAL_NOTES_LENGTH = 500

/**
 * One reviewed renewal for a document. The event keeps the previous and the new expiry,
 * the amount actually paid, the receipt asset and the resulting document revision.
 * Originals stay in place; a renewal never overwrites the only stored copy.
 */
@Serializable
data class DocumentRenewal(
    val documentId: String,
    val kind: String,
    val previousExpiry: String,
    val newExpiry: String,
    val renewedOn: String,
    val documentRevision: Int,
    val id: String = recordId(),
    val feePoisha: Int? = null,
    val receiptId: String? = null,
    val notes: String = "",
    val sourceCheckedOn: String = "",
    val feeLogId: String? = null
)
