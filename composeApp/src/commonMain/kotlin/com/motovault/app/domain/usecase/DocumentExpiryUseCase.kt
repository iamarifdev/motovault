package com.motovault.app.domain.usecase

import com.motovault.app.domain.model.GovernmentDocument

enum class DocumentExpiryStatus {
    VALID,
    EXPIRING_SOON,
    EXPIRED
}

data class DocumentStatusResult(
    val documentId: String,
    val documentTitle: String,
    val status: DocumentExpiryStatus,
    val daysRemaining: Int,
    val requiresImmediateAttention: Boolean,
    val alertNotificationTitle: String?,
    val alertNotificationBody: String?
)

class DocumentExpiryUseCase {

    fun evaluate(document: GovernmentDocument, currentEpochDays: Long, expiryEpochDays: Long): DocumentStatusResult {
        val daysRemaining = (expiryEpochDays - currentEpochDays).toInt()

        val status = when {
            daysRemaining <= 0 -> DocumentExpiryStatus.EXPIRED
            daysRemaining <= 30 -> DocumentExpiryStatus.EXPIRING_SOON
            else -> DocumentExpiryStatus.VALID
        }

        val (notifTitle, notifBody) = when (status) {
            DocumentExpiryStatus.EXPIRED -> {
                "🚨 Document Expired: ${document.type.title}" to
                        "${document.type.title} (${document.documentNumber}) is expired! Renew immediately to avoid police penalty."
            }
            DocumentExpiryStatus.EXPIRING_SOON -> {
                "⚠️ Renewal Due in $daysRemaining Days: ${document.type.title}" to
                        "${document.type.title} (${document.documentNumber}) expires on ${document.expiryDateFormatted}. Schedule your BRTA renewal."
            }
            DocumentExpiryStatus.VALID -> null to null
        }

        return DocumentStatusResult(
            documentId = document.id,
            documentTitle = document.type.title,
            status = status,
            daysRemaining = daysRemaining,
            requiresImmediateAttention = status != DocumentExpiryStatus.VALID,
            alertNotificationTitle = notifTitle,
            alertNotificationBody = notifBody
        )
    }
}
