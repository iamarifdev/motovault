package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RidingSeason { MONSOON, WINTER_DUST, HEAT }

@Serializable
enum class WeatherCacheState { OFFLINE_ONLY, FRESH, EXPIRED }

@Serializable
data class WeatherCacheSnapshot(
    val regionCode: String = "BD-DAC",
    val summaryCode: String? = null,
    val updatedAtEpochMillis: Long = 0L
) {
    fun stateAt(nowEpochMillis: Long, ttlHours: Int = 6): WeatherCacheState {
        if (updatedAtEpochMillis <= 0L) return WeatherCacheState.OFFLINE_ONLY
        val ageMillis = (nowEpochMillis - updatedAtEpochMillis).coerceAtLeast(0L)
        return if (ageMillis <= ttlHours * 3_600_000L) WeatherCacheState.FRESH else WeatherCacheState.EXPIRED
    }
}

@Serializable
data class BreakdownContact(
    val id: String,
    val name: String,
    val phone: String,
    val roleCode: String = "mechanic"
)
