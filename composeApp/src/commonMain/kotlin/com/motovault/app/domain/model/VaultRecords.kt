package com.motovault.app.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class GeoCapture(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAtEpochMillis: Long,
    val source: String
) {
    fun isValid(): Boolean = latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters >= 0.0 && capturedAtEpochMillis > 0 && source.isNotBlank()

    fun isFresh(nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds()): Boolean =
        capturedAtEpochMillis in (nowEpochMillis - 120_000)..(nowEpochMillis + 30_000)
}

@Serializable
data class DocumentPage(
    val originalId: String, val previewId: String, val mimeType: String,
    val width: Int, val height: Int, val side: String,
    val acceptedId: String? = null
)

@Serializable
data class Ride(
    val brandId: String, val edition: String, val year: Int, val odometer: Double,
    val modelId: String? = null, val modelName: String = "", val registration: String = "",
    val market: RideMarket = RideMarket.UNKNOWN, val emission: RideEmission = RideEmission.UNKNOWN,
    val modelCode: String = "", val factoryProfileId: String? = null,
    val odometerConfirmedDate: String? = null
) {
    fun confirmedMileage(value: Double, date: LocalDate): Ride {
        require(value.isFinite() && value in 0.0..9999999.0 && date <= today())
        return if (value >= odometer) copy(odometer = value, odometerConfirmedDate = date.toString()) else this
    }
}

@Serializable
data class RideLog(
    val title: String, val detail: String, val date: String, val odometer: Double,
    val id: String = recordId(), val kind: String = "", val liters: Double? = null,
    val fullTank: Boolean = true, val cost: Double = 0.0,
    val workshop: String = "", val notes: String = "", val componentId: String? = null,
    val serviceLines: List<ServiceLineItem> = emptyList(),
    val serviceLabourPoisha: Int? = null, val serviceMiscellaneousPoisha: Int? = null,
    val serviceDiscountPoisha: Int? = null, val serviceTotalPoisha: Int? = null,
    val scheduleIds: List<String> = emptyList(), val location: GeoCapture? = null,
    val locationLabel: String = "", val receiptBase64: String? = null,
    val fuelType: String = "octane",
    val fuelPrice: Double? = null
) {
    val isFuel: Boolean get() = kind == "fuel" || title == "Fuel fill-up"
    val isDocumentFee: Boolean get() = kind == "document_fee"
    val fuelLiters: Double? get() = liters ?: if (isFuel) detail.substringBefore(" L").toDoubleOrNull() else null
}

@Serializable
data class VaultDocument(
    val title: String, val number: String, val holder: String, val expiry: String,
    val id: String = recordId(), val issued: String = "",
    val frontPhoto: String? = null, val backPhoto: String? = null,
    val pages: List<DocumentPage> = emptyList(), val originalPdf: String? = null,
    val pdfPageCount: Int = 0, val details: Map<String, String> = emptyMap(),
    val detailsConfirmed: Boolean = false,
    val extractionCandidates: Map<String, String> = emptyMap(), val capturedAt: String = "",
    val reminder: Boolean = false,
    val kind: String = "",
    val reminderWarningDays: Int? = null,
    val reminderSnoozedUntil: String? = null
) {
    val daysRemaining: Int? get() = expiry.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it).toEpochDays() - today().toEpochDays() }.getOrNull()
    }
}

fun VaultDocument.assetIds(): Set<String> =
    pages.flatMap { listOfNotNull(it.originalId, it.previewId, it.acceptedId) }.toSet() + listOfNotNull(originalPdf)

fun isCardDocument(type: String): Boolean = when (type) {
    "Registration smart card", "smart_card", "registration", "রেজিস্ট্রেশন স্মার্ট কার্ড", "রেজিস্ট্রেশন সার্টিফিকেট",
    "Driving licence", "Driving license", "licence", "license", "ড্রাইভিং লাইসেন্স" -> true
    else -> false
}

fun documentPageLimit(type: String) = if (isCardDocument(type)) 2 else 8

fun validateDocumentAssetId(id: String): String {
    require(id.length in 1..160 && Regex("[A-Za-z0-9_-]+\\.(jpg|jpeg|png|webp|pdf)").matches(id)) { "Invalid document file." }
    return id
}
