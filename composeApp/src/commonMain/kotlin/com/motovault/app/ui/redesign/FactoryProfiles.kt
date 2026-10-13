package com.motovault.app.ui.redesign

import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class RideMarket(val label: String) {
    UNKNOWN("Not sure / other market"), INDIA("India"), EUROPE("Europe / UK"), BANGLADESH("Bangladesh")
}

@Serializable
enum class RideEmission(val label: String) {
    UNKNOWN("Not confirmed"), BS4("BS4"), BS6("BS6"), EURO4("Euro 4"), EURO5("Euro 5"), EURO5_PLUS("Euro 5+")
}

data class FactoryProfile(
    val id: String, val modelId: String, val year: Int, val market: RideMarket,
    val modelCodes: Set<String>, val manualUrl: String, val title: String
) {
    fun matches(ride: Ride) = ride.modelId == modelId && ride.year == year && ride.market == market &&
        ride.modelCode.trim().uppercase() in modelCodes

    fun rules(baseline: Double, date: LocalDate): List<ComponentRule> = listOf(
        // The first running-in service is separate from the recurring oil interval.
        ComponentRule(recordId(), "primary", "Engine oil", "$title, section 10.2. Oil + filter + screen. First service at 1,000 km; manual interval 7,500 km / 12 months. This app uses a 365-day reminder for the time interval. Confirm local conditions with your dealer.",
            7500.0, 365, baseline, date.toEpochDays().toLong()),
        ComponentRule(recordId(), "primary", "Chain", "Owner-defined chain-care reminder. Inspect before riding and after wet or dusty use; this is not a factory replacement interval.",
            500.0, 30, baseline, date.toEpochDays().toLong(), isCritical = false)
    )
}

object FactoryProfiles {
    val profiles = listOf(FactoryProfile(
        "ktm-125-duke-2023-eu-3214785", "ktm-duke-125-2023", 2023, RideMarket.EUROPE,
        setOf("F4003W1", "F4003W2", "F4003W5", "F4003W6", "F4022W3", "F4022W4", "F4022W5", "F4022W6"),
        "https://www.ktmshop.se/bike-manuals/23_3214785_en_OM.pdf", "KTM 125 DUKE 2023 manual 3214785en"
    ))

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
