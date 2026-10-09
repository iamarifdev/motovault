package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PowertrainType {
    ICE,
    EV,
    HYBRID
}

@Serializable
enum class VehicleCategory {
    MOTORCYCLE,
    CAR,
    SCOOTER
}

@Serializable
enum class Brand(
    val id: String,
    val displayName: String,
    val isAvailable: Boolean,
    val defaultAccentHex: Long
) {
    KTM("ktm", "KTM", true, 0xFFFF6600),
    YAMAHA("yamaha", "Yamaha", false, 0xFF0066FF),
    HONDA("honda", "Honda", false, 0xFFE60012),
    ROYAL_ENFIELD("re", "Royal Enfield", false, 0xFFD4A373),
    KAWASAKI("kawasaki", "Kawasaki", false, 0xFF00E676),
    ATHER("ather", "Ather Energy", false, 0xFF00F2FE),
    OLA("ola", "Ola Electric", false, 0xFF00C49F),
    REVOLT("revolt", "Revolt Motors", false, 0xFFFF3366)
}

@Serializable
enum class VehicleEdition(val displayName: String, val subtitle: String) {
    INDIAN("Indian Edition", "Bajaj Auto, Chakan (BS4 / BS6 Specs)"),
    EUROPEAN("European Edition", "Mattighofen / EU Homologated (Euro 4 / Euro 5)")
}

@Serializable
enum class EmissionStandard(
    val code: String,
    val displayName: String,
    val edition: VehicleEdition,
    val minYear: Int,
    val maxYear: Int,
    val fuelTankCapacityLiters: Double
) {
    BS4("BS4", "BS4 (Underbelly Exhaust, 10.2L Tank)", VehicleEdition.INDIAN, 2018, 2020, 10.2),
    BS6("BS6", "BS6 / OBD-2 (Side Exhaust, Split Trellis, 13.5L Tank)", VehicleEdition.INDIAN, 2020, 2026, 13.5),
    EURO4("EURO4", "Euro 4 (Full Color TFT, 13.4L Tank)", VehicleEdition.EUROPEAN, 2017, 2020, 13.4),
    EURO5("EURO5", "Euro 5 / 5+ (OBD-II, Euro 5 Tuning)", VehicleEdition.EUROPEAN, 2021, 2026, 13.4);

    companion object {
        fun forEdition(edition: VehicleEdition): List<EmissionStandard> {
            return entries.filter { it.edition == edition }
        }
    }
}
