package com.motovault.app.domain.model

/** Legacy defaults retained for persisted compatibility, not current pump prices. */
object FuelDefaults {
    const val OCTANE = "octane"
    const val PETROL = "petrol"
    const val LEGACY_OCTANE_TAKA_PER_LITRE = 120.0
    const val LEGACY_PETROL_TAKA_PER_LITRE = 115.0

    fun legacyPrices(): Map<String, Double> = mapOf(
        OCTANE to LEGACY_OCTANE_TAKA_PER_LITRE,
        PETROL to LEGACY_PETROL_TAKA_PER_LITRE
    )
}
