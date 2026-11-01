package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

data class ServiceCatalogItem(
    val key: String,
    val label: StringResource,
    val shortLabel: StringResource,
    val unit: StringResource,
    val ruleSuffix: String?
)

val CATALOG_ITEMS = listOf(
    ServiceCatalogItem("engine_oil", Res.string.catalog_engine_oil, Res.string.catalog_engine_oil_short, Res.string.unit_bottle, "oil"),
    ServiceCatalogItem("oil_filter", Res.string.catalog_oil_filter, Res.string.catalog_oil_filter_short, Res.string.unit_piece, "oil"),
    ServiceCatalogItem("air_filter", Res.string.catalog_air_filter, Res.string.catalog_air_filter_short, Res.string.unit_piece, "air_filter"),
    ServiceCatalogItem("spark_plug", Res.string.catalog_spark_plug, Res.string.catalog_spark_plug_short, Res.string.unit_piece, "spark_plug"),
    ServiceCatalogItem("chain_sprocket", Res.string.catalog_chain_sprocket, Res.string.catalog_chain_sprocket_short, Res.string.unit_set, "chain"),
    ServiceCatalogItem("brake_pad", Res.string.catalog_brake_pad, Res.string.catalog_brake_pad_short, Res.string.unit_set, "brake_pads"),
    ServiceCatalogItem("coolant", Res.string.catalog_coolant, Res.string.catalog_coolant_short, Res.string.unit_bottle, "coolant"),
    ServiceCatalogItem("tyre", Res.string.catalog_tyre, Res.string.catalog_tyre_short, Res.string.unit_piece, null),
    ServiceCatalogItem("battery", Res.string.catalog_battery, Res.string.catalog_battery_short, Res.string.unit_piece, null),
    ServiceCatalogItem("general_service", Res.string.catalog_general_service, Res.string.catalog_general_service_short, Res.string.unit_bar, null)
)

fun catalogItemOf(key: String): ServiceCatalogItem? = CATALOG_ITEMS.firstOrNull { it.key == key }

@Composable
fun catalogLabel(item: ServiceCatalogItem): String = appStringResource(item.label)

@Composable
fun catalogShortLabel(item: ServiceCatalogItem): String = appStringResource(item.shortLabel)

@Composable
fun catalogUnit(item: ServiceCatalogItem): String = appStringResource(item.unit)

val VISCOSITY_GRADES = listOf("10W-30", "10W-40", "20W-40", "20W-50")

data class DocumentKindInfo(
    val kind: String,
    val title: String,
    val card: Boolean,
    val resource: StringResource = Res.string.doc_registration
)

val DOCUMENT_KINDS = listOf(
    DocumentKindInfo("registration", "Registration certificate", true, Res.string.doc_smart_card),
    DocumentKindInfo("licence", "Driving licence", true, Res.string.doc_driving_license),
    DocumentKindInfo("tax", "Tax token", false, Res.string.doc_tax_token),
    DocumentKindInfo("insurance", "Insurance certificate", false, Res.string.doc_insurance),
    DocumentKindInfo("fitness", "Fitness certificate", false, Res.string.doc_fitness)
)

fun docKindInfo(kind: String): DocumentKindInfo = DOCUMENT_KINDS.firstOrNull { it.kind == kind }
    ?: DOCUMENT_KINDS.firstOrNull { it.title.equals(kind, ignoreCase = true) }
    ?: DocumentKindInfo(kind, kind, false, documentResourceOf(kind) ?: Res.string.doc_registration)

@Composable
fun docDisplayName(kindOrTitle: String): String {
    val res = documentResourceOf(kindOrTitle)
    return if (res != null) appStringResource(res) else kindOrTitle
}
