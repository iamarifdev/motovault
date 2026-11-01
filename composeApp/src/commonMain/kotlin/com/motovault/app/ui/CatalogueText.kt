package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.model.BangladeshServiceCatalogue
import com.motovault.app.domain.model.ServiceLineItem
import motovault.composeapp.generated.resources.*

@Composable
internal fun catalogueLabel(id: String): String = when (id) {
    "engine_oil" -> appStringResource(Res.string.catalogue_engine_oil)
    "oil_filter" -> appStringResource(Res.string.catalogue_oil_filter)
    "air_filter" -> appStringResource(Res.string.catalogue_air_filter)
    "spark_plug" -> appStringResource(Res.string.catalogue_spark_plug)
    "chain_sprocket" -> appStringResource(Res.string.catalogue_chain_sprocket)
    "brake_pad" -> appStringResource(Res.string.catalogue_brake_pad)
    "brake_fluid" -> appStringResource(Res.string.catalogue_brake_fluid)
    "coolant" -> appStringResource(Res.string.catalogue_coolant)
    "clutch_cable" -> appStringResource(Res.string.catalogue_clutch_cable)
    "tyre" -> appStringResource(Res.string.catalogue_tyre)
    "battery" -> appStringResource(Res.string.catalogue_battery)
    "chain_lube" -> appStringResource(Res.string.catalogue_chain_lube)
    "chain_adjust" -> appStringResource(Res.string.catalogue_chain_adjust)
    "brake_hose" -> appStringResource(Res.string.catalogue_brake_hose)
    "fork_oil" -> appStringResource(Res.string.catalogue_fork_oil)
    "valve_clearance" -> appStringResource(Res.string.catalogue_valve_clearance)
    "tyre_brake_inspect" -> appStringResource(Res.string.catalogue_tyre_brake_inspect)
    "first_service" -> appStringResource(Res.string.catalogue_first_service)
    "general_service" -> appStringResource(Res.string.catalogue_general_service)
    "inspection" -> appStringResource(Res.string.catalogue_inspection)
    "cleaning" -> appStringResource(Res.string.catalogue_cleaning)
    "adjustment" -> appStringResource(Res.string.catalogue_adjustment)
    else -> id
}

@Composable
internal fun catalogueUnit(id: String): String = when (id) {
    "engine_oil", "fork_oil" -> appStringResource(Res.string.unit_litre)
    "brake_fluid", "coolant" -> appStringResource(Res.string.unit_ml)
    "chain_sprocket", "brake_pad" -> appStringResource(Res.string.unit_set)
    "chain_lube", "chain_adjust", "valve_clearance", "tyre_brake_inspect", "first_service",
    "general_service", "inspection", "cleaning", "adjustment" -> appStringResource(Res.string.unit_bar)
    else -> appStringResource(Res.string.unit_piece)
}

@Composable
internal fun serviceLineLabel(line: ServiceLineItem): String =
    line.catalogueId?.let { catalogueLabel(it) } ?: line.displayName

@Composable
internal fun serviceCatalogueLabels(): Map<String, String> =
    BangladeshServiceCatalogue.items.associate { it.id to catalogueLabel(it.id) }
