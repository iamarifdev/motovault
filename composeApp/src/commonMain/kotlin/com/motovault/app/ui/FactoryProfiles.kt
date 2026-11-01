package com.motovault.app.ui

import com.motovault.app.domain.maintenance.MaintenanceProfile
import com.motovault.app.domain.maintenance.MaintenanceProfiles
import com.motovault.app.domain.maintenance.MaintenanceScheduleSpec
import com.motovault.app.domain.maintenance.SchedulePhase
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Presentation adapter for the source-backed profiles owned by
 * domain.maintenance. The UI only decides how a ride maps onto a profile.
 */
data class FactoryProfile(
    val id: String, val modelId: String, val year: Int, val market: RideMarket,
    val modelCodes: Set<String>, val manualUrl: String, val title: String,
    val applicability: String, val specs: List<MaintenanceScheduleSpec>
) {
    fun matches(ride: Ride) = ride.modelId == modelId && ride.year == year && ride.market == market &&
        ride.modelCode.trim().uppercase() in modelCodes

    fun rules(baseline: Double?, date: LocalDate, certainty: BaselineCertainty = BaselineCertainty.CONFIRMED): List<ComponentRule> =
        specs.filter { spec -> spec.phase == SchedulePhase.RECURRING || baseline == null || baseline < 1000.0 }
            .map { spec -> spec.toRule(baseline, date, certainty) }
}

private fun MaintenanceScheduleSpec.toRule(baseline: Double?, date: LocalDate, certainty: BaselineCertainty) =
    ComponentRule(
        id = recordId(), vehicleId = "primary", name = name,
        specification = if (provenance.isManualBacked) {
            "${provenance.displayReference}. ${provenance.applicability}"
        } else {
            "Owner-defined schedule. Confirm against your manual or dealer."
        },
        intervalKm = intervalKm, intervalDays = intervalDays,
        lastServicedOdometerKm = if (phase == SchedulePhase.RUNNING_IN) 0.0 else (baseline ?: 0.0),
        lastServicedEpochDays = if (phase == SchedulePhase.RUNNING_IN) 0L else date.toEpochDays().toLong(),
        isCritical = isCritical, baselineCertainty = certainty,
        action = action, provenance = provenance, phase = phase, oneTime = oneTime, catalogueId = catalogueId
    )

object FactoryProfiles {
    private fun MaintenanceProfile.market(): RideMarket = runCatching { RideMarket.valueOf(marketCode) }.getOrDefault(RideMarket.UNKNOWN)

    val profiles = MaintenanceProfiles.profiles.map { profile ->
        FactoryProfile(
            id = profile.id, modelId = profile.modelId, year = profile.year, market = profile.market(),
            modelCodes = profile.modelCodes, manualUrl = profile.manualUrl, title = profile.manualTitle,
            applicability = profile.applicability, specs = profile.schedules
        )
    }

    fun find(id: String?) = profiles.firstOrNull { it.id == id }
    fun available(modelId: String?, year: Int?, market: RideMarket) = profiles.filter {
        it.modelId == modelId && it.year == year && it.market == market
    }

    // These are owner-entered paperwork values, not inferred model homologations.
    fun emissions(market: RideMarket): List<RideEmission> = listOf(RideEmission.UNKNOWN) + when (market) {
        RideMarket.INDIA -> listOf(RideEmission.BS4, RideEmission.BS6)
        RideMarket.EUROPE -> listOf(RideEmission.EURO4, RideEmission.EURO5, RideEmission.EURO5_PLUS)
        else -> RideEmission.entries.filter { it != RideEmission.UNKNOWN }
    }

    fun validate(ride: Ride) {
        require(ride.year in 1980..2100) { "Enter a valid release year." }
        require(ride.emission in emissions(ride.market)) { "Confirm the emission standard on your paperwork for this market." }
        ride.factoryProfileId?.let {
            require(find(it)?.matches(ride) == true) { "The manual must match your model, year, market and model code." }
        }
    }
}
