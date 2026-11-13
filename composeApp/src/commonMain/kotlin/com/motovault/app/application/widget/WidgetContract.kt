package com.motovault.app.application.widget

import com.motovault.app.domain.model.AppLanguage
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.Digits
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.WearHealthStatus
import com.motovault.app.domain.usecase.CalculateWearUseCase
import com.motovault.app.domain.vault.VaultState
import kotlinx.serialization.Serializable

private const val MILLIS_PER_HOUR = 3_600_000L
private const val DEFAULT_STALE_HOURS = 24
private val ALLOWED_SHEETS = setOf("fuel", "odometer", "garage")

@Serializable
enum class WidgetTaskStatus {
    OK,
    SOON,
    OVERDUE,
    UNKNOWN,
    NONE,
    LOCKED
}

@Serializable
data class WidgetSnapshot(
    val schemaVersion: Int = 1,
    val bikeId: String? = null,
    val bikeName: String = "",
    val odometerKm: Double? = null,
    val taskItemKey: String? = null,
    val taskStatus: WidgetTaskStatus = WidgetTaskStatus.NONE,
    val privacyRedacted: Boolean = false,
    val language: AppLanguage = AppLanguage.BN,
    val digits: Digits = Digits.BN,
    val updatedAtEpochMillis: Long = 0L,
    val staleAfterHours: Int = DEFAULT_STALE_HOURS
) {
    fun isStale(nowEpochMillis: Long): Boolean =
        updatedAtEpochMillis <= 0L || (nowEpochMillis - updatedAtEpochMillis) > staleAfterHours * MILLIS_PER_HOUR
}

@Serializable
data class WidgetDeepLink(
    val sheet: String,
    val bikeId: String? = null
) {
    fun toUri(): String {
        val cleanSheet = if (sheet in ALLOWED_SHEETS) sheet else "garage"
        val cleanBikeId = bikeId?.trim()?.takeIf { it.isNotEmpty() }
        return if (cleanBikeId == null) {
            "motovault://widget/action?sheet=$cleanSheet"
        } else {
            "motovault://widget/action?sheet=$cleanSheet&bikeId=${encodeQueryComponent(cleanBikeId)}"
        }
    }

    companion object {
        fun parse(uri: String?): WidgetDeepLink? {
            if (uri.isNullOrBlank()) return null
            val prefix = "motovault://widget/action"
            if (!uri.startsWith(prefix)) return null
            val query = uri.substringAfter('?', "")
            if (query.isBlank()) return null
            val params = query.split('&').mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null
                else part.substring(0, idx) to decodeQueryComponent(part.substring(idx + 1))
            }.toMap()
            val sheet = params["sheet"]?.lowercase()?.takeIf { it in ALLOWED_SHEETS } ?: return null
            val bikeId = params["bikeId"]?.trim()?.takeIf { it.isNotEmpty() }
            return WidgetDeepLink(sheet = sheet, bikeId = bikeId)
        }
    }
}

object WidgetSnapshotProjector {
    private val wearUseCase = CalculateWearUseCase()

    fun project(state: VaultState, todayEpochDays: Long, nowEpochMillis: Long): WidgetSnapshot {
        if (state.privacy.enabled) {
            return WidgetSnapshot(
                taskStatus = WidgetTaskStatus.LOCKED,
                privacyRedacted = true,
                language = state.language,
                digits = state.digits,
                updatedAtEpochMillis = nowEpochMillis
            )
        }
        val (selectedRide, selectedRules) = resolveBike(state)
            ?: return WidgetSnapshot(
                taskStatus = WidgetTaskStatus.NONE,
                language = state.language,
                digits = state.digits,
                updatedAtEpochMillis = nowEpochMillis
            )

        val confirmed = selectedRules.filter { it.baselineCertainty == BaselineCertainty.CONFIRMED }
        val (taskKey, status) = when {
            selectedRules.isEmpty() -> null to WidgetTaskStatus.NONE
            confirmed.isEmpty() -> selectedRules.first().id to WidgetTaskStatus.UNKNOWN
            else -> {
                val top = wearUseCase.executeAll(confirmed, selectedRide.odometer, todayEpochDays)
                    .maxByOrNull { it.wearPercentage }
                if (top == null) null to WidgetTaskStatus.NONE
                else {
                    val mapped = when (top.healthStatus) {
                        WearHealthStatus.OVERDUE -> WidgetTaskStatus.OVERDUE
                        WearHealthStatus.SERVICE_SOON -> WidgetTaskStatus.SOON
                        WearHealthStatus.HEALTHY -> WidgetTaskStatus.OK
                    }
                    top.componentId to mapped
                }
            }
        }

        return WidgetSnapshot(
            bikeId = selectedRide.id,
            bikeName = selectedRide.modelName.ifBlank { selectedRide.edition }.ifBlank { selectedRide.brandId },
            odometerKm = selectedRide.odometer,
            taskItemKey = taskKey,
            taskStatus = status,
            privacyRedacted = false,
            language = state.language,
            digits = state.digits,
            updatedAtEpochMillis = nowEpochMillis
        )
    }

    private fun resolveBike(state: VaultState): Pair<Ride, List<ComponentRule>>? {
        val activePool = buildList {
            state.ride?.takeUnless { it.archived }?.let { add(it to state.confirmedRules()) }
            state.garageBikes.filterNot { it.ride.archived }.forEach { record ->
                if (record.ride.id != state.ride?.id) add(record.ride to record.rules)
            }
        }
        val preferred = state.widgetBikeId?.let { id -> activePool.firstOrNull { it.first.id == id } }
        return preferred ?: activePool.firstOrNull()
    }
}

private fun encodeQueryComponent(value: String): String = buildString(value.length) {
    for (ch in value) {
        if (ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.') append(ch)
        else append('%').append(ch.code.toString(16).uppercase().padStart(2, '0'))
    }
}

private fun decodeQueryComponent(value: String): String = buildString(value.length) {
    var i = 0
    while (i < value.length) {
        if (value[i] == '%' && i + 2 < value.length) {
            val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
            if (hex != null) {
                append(hex.toChar())
                i += 3
                continue
            }
        }
        append(value[i])
        i++
    }
}
