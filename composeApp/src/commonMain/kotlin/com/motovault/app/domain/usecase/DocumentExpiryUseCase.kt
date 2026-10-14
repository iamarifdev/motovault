package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.DOCUMENT_RENEWAL_WARNING_DAYS
import com.motovault.app.domain.model.DocumentExpiryStatus

class DocumentExpiryUseCase {
    fun evaluate(daysRemaining: Int?): DocumentExpiryStatus = when {
        daysRemaining == null -> DocumentExpiryStatus.NO_EXPIRY
        daysRemaining <= 0 -> DocumentExpiryStatus.EXPIRED
        daysRemaining <= DOCUMENT_RENEWAL_WARNING_DAYS -> DocumentExpiryStatus.EXPIRING_SOON
        else -> DocumentExpiryStatus.VALID
    }
}
