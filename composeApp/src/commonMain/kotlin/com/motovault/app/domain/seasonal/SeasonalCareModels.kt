package com.motovault.app.domain.seasonal

import com.motovault.app.domain.model.GeoCapture
import com.motovault.app.domain.model.RidingSeason
import kotlin.math.roundToLong

const val BMD_OFFICIAL_URL = "https://live8.bmd.gov.bd/"
const val NATIONAL_EMERGENCY_NUMBER = "999"
const val WEATHER_CACHE_TTL_HOURS = 6

data class SeasonalCareItem(
    val id: String,
    val season: RidingSeason
)

object SeasonalCarePolicy {
    fun seasonForMonth(monthNumber: Int): RidingSeason = when (monthNumber) {
        in 6..10 -> RidingSeason.MONSOON
        in 3..5 -> RidingSeason.HEAT
        else -> RidingSeason.WINTER_DUST
    }

    fun checklistFor(season: RidingSeason): List<SeasonalCareItem> = when (season) {
        RidingSeason.MONSOON -> listOf(
            SeasonalCareItem("monsoon_chain", season),
            SeasonalCareItem("monsoon_brakes", season),
            SeasonalCareItem("monsoon_electrical", season)
        )
        RidingSeason.WINTER_DUST -> listOf(
            SeasonalCareItem("winter_air_filter", season),
            SeasonalCareItem("winter_battery", season),
            SeasonalCareItem("winter_visibility", season)
        )
        RidingSeason.HEAT -> listOf(
            SeasonalCareItem("heat_coolant_oil", season),
            SeasonalCareItem("heat_tire_pressure", season),
            SeasonalCareItem("heat_brake_fluid", season)
        )
    }

    fun dismissKey(season: RidingSeason, year: Int, bikeId: String?): String =
        "${bikeId ?: "default"}:${season.name}:$year"

    fun isDismissed(
        dismissedSeasons: Set<String>,
        season: RidingSeason,
        year: Int,
        bikeId: String?
    ): Boolean = dismissKey(season, year, bikeId) in dismissedSeasons

    fun toggleChecklistItem(checkedIds: Set<String>, itemId: String): Set<String> =
        if (itemId in checkedIds) checkedIds - itemId else checkedIds + itemId

    fun toggleDismissedSeason(
        dismissedSeasons: Set<String>,
        season: RidingSeason,
        year: Int,
        bikeId: String?
    ): Set<String> {
        val key = dismissKey(season, year, bikeId)
        return if (key in dismissedSeasons) dismissedSeasons - key else dismissedSeasons + key
    }

    fun formatBreakdownShareText(
        bikeLabel: String,
        odometerKm: Double?,
        note: String,
        geo: GeoCapture?
    ): String {
        val parts = mutableListOf<String>()
        val cleanBike = bikeLabel.trim().ifEmpty { "MotoVault Bike" }
        parts += if (odometerKm != null && odometerKm.isFinite()) {
            "$cleanBike (${odometerKm.roundToLong()} km)"
        } else {
            cleanBike
        }
        val cleanNote = note.trim()
        if (cleanNote.isNotEmpty()) parts += cleanNote
        if (geo != null && geo.isValid()) {
            val lat = (geo.latitude * 100_000.0).roundToLong() / 100_000.0
            val lon = (geo.longitude * 100_000.0).roundToLong() / 100_000.0
            val acc = geo.accuracyMeters.roundToLong().coerceAtLeast(0L)
            parts += "GPS: $lat, $lon (±${acc}m)"
        }
        return parts.joinToString("\n")
    }
}
