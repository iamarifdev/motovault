package com.motovault.app.domain.backup

import kotlinx.serialization.Serializable
import com.motovault.app.domain.model.FuelDefaults

// These values are legacy archive defaults, not presentation strings.
private const val DEFAULT_SERVICE_ACTION = "replace"
private const val DEFAULT_SERVICE_UNIT = ""

@Serializable
data class BackupOilMetadata(
    val baseType: String = "", val saeGrade: String = "", val apiStandard: String = "",
    val jasoStandard: String = "", val productName: String = "", val purchaseBasis: String = "",
    val packVolumeMl: Int? = null, val boughtQuantity: Double? = null, val usedQuantityMl: Int? = null
)

@Serializable
data class BackupServiceLine(
    val catalogueId: String? = null, val customName: String = "", val action: String = DEFAULT_SERVICE_ACTION,
    val quantity: Double = 1.0, val unitBn: String = DEFAULT_SERVICE_UNIT, val unitPricePoisha: Int? = null,
    val scheduleId: String? = null, val resetsScheduleBaseline: Boolean = false,
    val oil: BackupOilMetadata? = null, val detail: String? = null
)

@Serializable
data class BackupLocation(
    val latitude: Double, val longitude: Double, val accuracyMeters: Double,
    val capturedAtEpochMillis: Long, val source: String
)

/** Legacy values remain unchanged, including defaults used when older fields are absent. */
@Serializable
data class BackupLog(
    val title: String, val detail: String, val date: String, val odometer: Double,
    val id: String = newBackupRecordId(), val kind: String = "", val liters: Double? = null,
    val fullTank: Boolean = true, val cost: Double = 0.0,
    val workshop: String = "", val notes: String = "", val componentId: String? = null,
    val serviceLines: List<BackupServiceLine> = emptyList(),
    val serviceLabourPoisha: Int? = null, val serviceMiscellaneousPoisha: Int? = null,
    val serviceDiscountPoisha: Int? = null, val serviceTotalPoisha: Int? = null,
    val scheduleIds: List<String> = emptyList(), val location: BackupLocation? = null,
    val locationLabel: String = "", val receiptBase64: String? = null,
    val fuelType: String = FuelDefaults.OCTANE, val fuelPrice: Double? = null
)
