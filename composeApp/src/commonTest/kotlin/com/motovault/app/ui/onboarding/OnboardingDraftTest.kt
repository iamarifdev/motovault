package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideEmission
import com.motovault.app.domain.model.RideMarket
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnboardingDraftTest {

    private val today = LocalDate(2026, 11, 19)

    @Test
    fun defaultYearForClampsToVariantRange() {
        assertEquals(2024, defaultYearFor(2022, 2026, currentYear = 2026))
        assertEquals(2022, defaultYearFor(2018, 2022, currentYear = 2026))
        assertEquals(2025, defaultYearFor(2025, 2026, currentYear = 2026))
    }

    @Test
    fun catalogueVariantMapsTimingBaselinesAndSkip() {
        val variant = BikeCatalogueFamilies.variantOf("yamaha-r15-v4")!!
        val recent = buildOnboardingResult(
            brandId = "yamaha",
            customMode = false,
            customName = "",
            customCcInput = "150",
            variant = variant,
            odometer = 2500.0,
            serviceTiming = OnboardingServiceTiming.RECENT,
            today = today
        )
        assertNotNull(recent)
        assertEquals("yamaha-r15-v4", recent.modelId)
        assertEquals("R15 V4", recent.modelName)
        assertEquals(2024, recent.year)
        assertEquals(155, recent.cc)
        assertEquals(2200.0, recent.baselineKm)
        assertEquals(LocalDate(2026, 11, 5), recent.baselineDate)
        assertEquals(BaselineCertainty.OWNER_ESTIMATE, recent.baselineCertainty)

        val months = buildOnboardingResult(
            brandId = "yamaha",
            customMode = false,
            customName = "",
            customCcInput = "150",
            variant = variant,
            odometer = 900.0,
            serviceTiming = OnboardingServiceTiming.MONTHS,
            today = today
        )!!
        assertEquals(0.0, months.baselineKm)
        assertEquals(LocalDate(2026, 9, 20), months.baselineDate)
        assertEquals(BaselineCertainty.OWNER_ESTIMATE, months.baselineCertainty)

        val unknown = buildOnboardingResult(
            brandId = "yamaha",
            customMode = false,
            customName = "",
            customCcInput = "150",
            variant = variant,
            odometer = 1200.0,
            serviceTiming = OnboardingServiceTiming.UNKNOWN,
            today = today
        )!!
        assertNull(unknown.baselineKm)
        assertEquals(today, unknown.baselineDate)
        assertEquals(BaselineCertainty.UNKNOWN, unknown.baselineCertainty)

        val skipped = buildOnboardingResult(
            brandId = "yamaha",
            customMode = false,
            customName = "",
            customCcInput = "150",
            variant = variant,
            odometer = 0.0,
            serviceTiming = OnboardingServiceTiming.RECENT,
            today = today
        )!!
        assertEquals(0.0, skipped.odometer)
        assertNull(skipped.baselineKm)
        assertEquals(BaselineCertainty.UNKNOWN, skipped.baselineCertainty)
    }

    @Test
    fun customBikeUsesNullModelIdSupportsBanglaCcAndCreatesValidVault() {
        assertNull(buildOnboardingResult("honda", true, "   ", "150", null, 500.0, OnboardingServiceTiming.UNKNOWN, today))
        assertNull(buildOnboardingResult("honda", false, "", "150", null, 500.0, OnboardingServiceTiming.UNKNOWN, today))

        val bnCc = buildOnboardingResult("honda", true, "  My Cafe Racer ", "১৬০", null, 800.0, OnboardingServiceTiming.UNKNOWN, today)!!
        assertNull(bnCc.modelId)
        assertEquals("My Cafe Racer", bnCc.modelName)
        assertEquals(2025, bnCc.year)
        assertEquals(160, bnCc.cc)

        val outOfRangeCc = buildOnboardingResult("honda", true, "Mini Bike", "9999", null, 100.0, OnboardingServiceTiming.UNKNOWN, today)!!
        assertNull(outOfRangeCc.cc)

        val created = VaultState.create(
            Ride(
                brandId = bnCc.brandId,
                edition = "Standard",
                year = bnCc.year,
                odometer = bnCc.odometer,
                modelId = bnCc.modelId,
                modelName = bnCc.modelName,
                market = RideMarket.UNKNOWN,
                emission = RideEmission.UNKNOWN,
                cc = bnCc.cc
            ),
            baseline = bnCc.baselineKm,
            baselineDate = bnCc.baselineDate,
            baselineCertainty = bnCc.baselineCertainty
        )
        assertEquals("My Cafe Racer", created.ride?.modelName)
        assertNull(created.ride?.modelId)
        assertEquals(160, created.ride?.cc)
    }
}
