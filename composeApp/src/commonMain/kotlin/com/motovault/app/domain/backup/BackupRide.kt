package com.motovault.app.domain.backup

import kotlinx.serialization.Serializable

@Serializable
enum class BackupRideMarket { UNKNOWN, INDIA, EUROPE, BANGLADESH }

@Serializable
enum class BackupRideEmission { UNKNOWN, BS4, BS6, EURO4, EURO5, EURO5_PLUS }

/** The names and defaults in this type are the version-one archive format. */
@Serializable
data class BackupRide(
    val brandId: String, val edition: String, val year: Int, val odometer: Double,
    val modelId: String? = null, val modelName: String = "", val registration: String = "",
    val market: BackupRideMarket = BackupRideMarket.UNKNOWN, val emission: BackupRideEmission = BackupRideEmission.UNKNOWN,
    val modelCode: String = "", val factoryProfileId: String? = null,
    val odometerConfirmedDate: String? = null
)
