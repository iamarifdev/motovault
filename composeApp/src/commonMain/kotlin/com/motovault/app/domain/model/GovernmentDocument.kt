package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

const val DOCUMENT_RENEWAL_WARNING_DAYS = 60

@Serializable
enum class GovernmentDocumentType(val title: String) {
    BRTA_REGISTRATION("BRTA Vehicle Registration"),
    DRIVING_LICENSE("BRTA Smart Driving License"),
    TAX_TOKEN("BRTA Tax Token"),
    INSURANCE("Motor Vehicle Insurance"),
    FITNESS("Fitness Certificate")
}

@Serializable
data class GovernmentDocument(
    val id: String,
    val vehicleId: String? = null,
    val type: GovernmentDocumentType,
    val documentNumber: String,
    val holderName: String,
    val issueDateFormatted: String,
    val expiryDateFormatted: String,
    val daysUntilExpiry: Int,
    val bloodGroup: String? = null,
    val authorizedClasses: String? = null,
    val chassisNumber: String? = null,
    val engineNumber: String? = null,
    val frontImageUri: String? = null,
    val backImageUri: String? = null,
    val isBiometricLocked: Boolean = true
) {
    val isExpired: Boolean get() = daysUntilExpiry <= 0
    val isExpiringSoon: Boolean get() = daysUntilExpiry in 1..DOCUMENT_RENEWAL_WARNING_DAYS
}
