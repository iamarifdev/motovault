package com.motovault.app.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class VisitRecordState(val code: String) {
    PAID("paid"),
    QUOTATION("quote"),
    APPROVED("approved"),
    CANCELLED("cancelled")
}

enum class IssueResolutionState(val code: String) {
    OPEN("open"),
    MONITORED("monitored"),
    RESOLVED("resolved")
}

@Serializable
data class WorkshopContact(
    val id: String = recordId(),
    val name: String,
    val phone: String = "",
    val area: String = "",
    val notes: String = ""
)

@Serializable
data class RepairCase(
    val id: String = recordId(),
    val vehicleId: String = "primary",
    val title: String,
    val symptom: String = "",
    val status: IssueResolutionState = IssueResolutionState.OPEN,
    val createdDate: String,
    val visitLogIds: List<String> = emptyList()
)

@Serializable
data class PartWarranty(
    val id: String = recordId(),
    val vehicleId: String = "primary",
    val visitLogId: String,
    val catalogueId: String? = null,
    val partName: String,
    val startDate: String,
    val expiryDate: String,
    val mileageLimitKm: Double? = null,
    val startOdometerKm: Double = 0.0,
    val workshopId: String? = null,
    val reminder: Boolean = true,
    val claimNotes: String = ""
) {
    fun isExpired(today: LocalDate, currentOdometerKm: Double): Boolean {
        val expiredByDate = runCatching { LocalDate.parse(expiryDate) }.getOrNull()?.let { it < today } ?: false
        val expiredByKm = mileageLimitKm?.let { currentOdometerKm - startOdometerKm >= it } ?: false
        return expiredByDate || expiredByKm
    }
}
