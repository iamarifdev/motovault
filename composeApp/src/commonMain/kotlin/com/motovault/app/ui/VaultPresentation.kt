package com.motovault.app.ui

import com.motovault.app.domain.model.DocumentExpiryStatus
import com.motovault.app.domain.usecase.DocumentExpiryUseCase
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.VaultDocument

val Ride.selectedModel: BikeModel? get() = BikeCatalogue.find(modelId)?.takeIf { it.brandId == brandId }
    ?: if (modelName.isBlank()) BikeCatalogue.legacyModel(brandId) else null

val Ride.displayName: String get() = modelName.ifBlank { selectedModel?.name ?: "Your motorcycle" }

val VaultDocument.expiryStatus: DocumentExpiryStatus get() = DocumentExpiryUseCase().evaluate(daysRemaining)

val VaultDocument.expiryLabel: String get() {
    val days = daysRemaining
    return when (DocumentExpiryUseCase().evaluate(days)) {
        DocumentExpiryStatus.NO_EXPIRY -> "No expiry"
        DocumentExpiryStatus.EXPIRED -> "Expired"
        DocumentExpiryStatus.EXPIRING_SOON -> "$days days"
        DocumentExpiryStatus.VALID -> "Valid"
    }
}
