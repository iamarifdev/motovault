package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BaselineCertainty { UNKNOWN, OWNER_ESTIMATE, CONFIRMED }

enum class WearHealthStatus {
    HEALTHY,      // 0 - 74%
    SERVICE_SOON, // 75 - 99%
    OVERDUE       // 100%+
}

@Serializable
data class ComponentRule(
    val id: String,
    val vehicleId: String,
    val name: String,
    val specification: String,
    val intervalKm: Double,
    val intervalDays: Int,
    val lastServicedOdometerKm: Double = 0.0,
    val lastServicedEpochDays: Long = 0L,
    val isCritical: Boolean = true,
    val reminder: Boolean = true,
    val reminderSnoozedUntil: String? = null,
    val baselineCertainty: BaselineCertainty = BaselineCertainty.UNKNOWN
) {
    fun calculateWearPercentage(currentOdometerKm: Double, currentEpochDays: Long): Int {
        val deltaDistance = (currentOdometerKm - lastServicedOdometerKm).coerceAtLeast(0.0)
        val distanceRatio = if (intervalKm > 0) deltaDistance / intervalKm else 0.0

        val deltaDays = (currentEpochDays - lastServicedEpochDays).coerceAtLeast(0L).toDouble()
        val timeRatio = if (intervalDays > 0) deltaDays / intervalDays else 0.0

        val maxRatio = maxOf(distanceRatio, timeRatio)
        return (maxRatio * 100).toInt()
    }

    fun getHealthStatus(currentOdometerKm: Double, currentEpochDays: Long): WearHealthStatus {
        val percent = calculateWearPercentage(currentOdometerKm, currentEpochDays)
        return when {
            percent >= 100 -> WearHealthStatus.OVERDUE
            percent >= 75 -> WearHealthStatus.SERVICE_SOON
            else -> WearHealthStatus.HEALTHY
        }
    }

    fun remainingKm(currentOdometerKm: Double): Double {
        val elapsed = (currentOdometerKm - lastServicedOdometerKm).coerceAtLeast(0.0)
        return (intervalKm - elapsed).coerceAtLeast(0.0)
    }
}
