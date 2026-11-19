package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import kotlinx.datetime.LocalDate

internal val ONBOARDING_BRAND_ORDER = listOf(
    "yamaha",
    "honda",
    "bajaj",
    "tvs",
    "suzuki",
    "hero",
    "ktm",
    "cfmoto",
    "enfield"
)

internal enum class OnboardingServiceTiming {
    RECENT,
    MONTHS,
    UNKNOWN
}

data class OnboardingResult(
    val brandId: String,
    val modelId: String?,
    val modelName: String,
    val year: Int,
    val cc: Int?,
    val odometer: Double,
    val baselineKm: Double?,
    val baselineDate: LocalDate,
    val baselineCertainty: BaselineCertainty
)

internal fun defaultYearFor(yearStart: Int, yearEnd: Int, currentYear: Int): Int {
    val low = minOf(yearStart, yearEnd)
    val high = maxOf(yearStart, yearEnd)
    return (currentYear - 2).coerceIn(low, high)
}

internal fun buildOnboardingResult(
    brandId: String,
    customMode: Boolean,
    customName: String,
    customCcInput: String,
    variant: BikeVariant?,
    odometer: Double,
    serviceTiming: OnboardingServiceTiming,
    today: LocalDate
): OnboardingResult? {
    val cleanOdo = odometer.coerceAtLeast(0.0)
    val modelId: String?
    val modelName: String
    val year: Int
    val cc: Int?

    if (customMode) {
        val trimmedName = customName.trim()
        if (trimmedName.isEmpty()) return null
        modelId = null
        modelName = trimmedName
        year = today.year - 1
        cc = parseNum(customCcInput)?.toInt()?.takeIf { it in 50..2500 }
    } else {
        if (variant == null) return null
        modelId = variant.modelId
        modelName = variant.name
        year = defaultYearFor(variant.yearStart, variant.yearEnd, today.year)
        cc = variant.cc
    }

    val baselineKm: Double?
    val baselineDate: LocalDate
    val baselineCertainty: BaselineCertainty

    if (cleanOdo > 0.0 && serviceTiming == OnboardingServiceTiming.RECENT) {
        baselineKm = (cleanOdo - 300.0).coerceAtLeast(0.0)
        baselineDate = LocalDate.fromEpochDays(today.toEpochDays() - 14)
        baselineCertainty = BaselineCertainty.OWNER_ESTIMATE
    } else if (cleanOdo > 0.0 && serviceTiming == OnboardingServiceTiming.MONTHS) {
        baselineKm = (cleanOdo - 1200.0).coerceAtLeast(0.0)
        baselineDate = LocalDate.fromEpochDays(today.toEpochDays() - 60)
        baselineCertainty = BaselineCertainty.OWNER_ESTIMATE
    } else {
        baselineKm = null
        baselineDate = today
        baselineCertainty = BaselineCertainty.UNKNOWN
    }

    return OnboardingResult(
        brandId = brandId,
        modelId = modelId,
        modelName = modelName,
        year = year,
        cc = cc,
        odometer = cleanOdo,
        baselineKm = baselineKm,
        baselineDate = baselineDate,
        baselineCertainty = baselineCertainty
    )
}
