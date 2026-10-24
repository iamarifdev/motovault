package com.motovault.app.domain.model

const val DOCUMENT_RENEWAL_WARNING_DAYS = 30

enum class DocumentExpiryStatus {
    NO_EXPIRY,
    VALID,
    EXPIRING_SOON,
    EXPIRED
}
