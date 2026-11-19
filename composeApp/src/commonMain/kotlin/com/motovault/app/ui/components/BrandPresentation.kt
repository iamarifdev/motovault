package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.model.ComponentRule
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.component_air_filter
import motovault.composeapp.generated.resources.component_brake_fluid
import motovault.composeapp.generated.resources.component_brake_pads
import motovault.composeapp.generated.resources.component_chain
import motovault.composeapp.generated.resources.component_coolant
import motovault.composeapp.generated.resources.component_oil
import motovault.composeapp.generated.resources.component_spark_plug
import motovault.composeapp.generated.resources.logo_bajaj
import motovault.composeapp.generated.resources.logo_cfmoto
import motovault.composeapp.generated.resources.logo_enfield
import motovault.composeapp.generated.resources.logo_hero
import motovault.composeapp.generated.resources.logo_honda
import motovault.composeapp.generated.resources.logo_ktm
import motovault.composeapp.generated.resources.logo_suzuki
import motovault.composeapp.generated.resources.logo_tvs
import motovault.composeapp.generated.resources.logo_yamaha
import org.jetbrains.compose.resources.DrawableResource

fun componentSuffix(rule: ComponentRule): String {
    val knownSuffix = listOf("air_filter", "spark_plug", "brake_pads", "brake_fluid", "chain", "oil", "coolant")
        .firstOrNull { rule.id.endsWith("_$it") || rule.id.endsWith("-$it") }
    if (knownSuffix != null) return knownSuffix
    return when (rule.id) {
        "r-air" -> "air_filter"
        "r-spark" -> "spark_plug"
        "r-brakes" -> "brake_pads"
        else -> when (rule.name.trim().lowercase()) {
            "chain", "drive chain clean & lube", "চেইন পরিষ্কার ও লুব্রিকেন্ট" -> "chain"
            "engine oil", "engine oil & oil filter", "ইঞ্জিন অয়েল ও ফিল্টার" -> "oil"
            "air filter", "এয়ার ফিল্টার" -> "air_filter"
            "spark plug", "স্পার্ক প্লাগ" -> "spark_plug"
            "brake pads (front & rear)", "brake pads", "ব্রেক প্যাড" -> "brake_pads"
            "engine coolant" -> "coolant"
            "brake fluid (dot 4)" -> "brake_fluid"
            else -> rule.id.substringAfterLast('_')
        }
    }
}

@Composable
fun componentLabel(rule: ComponentRule): String {
    rule.catalogueId?.let(BangladeshServiceCatalogue::find)?.let { item ->
        return catalogueLabel(item.id)
    }
    val suffix = componentSuffix(rule)
    val bengaliName = when (suffix) {
        "chain" -> appStringResource(Res.string.component_chain)
        "oil" -> appStringResource(Res.string.component_oil)
        "air_filter" -> appStringResource(Res.string.component_air_filter)
        "spark_plug" -> appStringResource(Res.string.component_spark_plug)
        "brake_pads" -> appStringResource(Res.string.component_brake_pads)
        "coolant" -> appStringResource(Res.string.component_coolant)
        "brake_fluid" -> appStringResource(Res.string.component_brake_fluid)
        else -> null
    }
    return bengaliName ?: rule.name
}

@Composable
fun componentShortLabel(rule: ComponentRule): String {
    val suffix = componentSuffix(rule)
    val catalogItem = CATALOG_ITEMS.firstOrNull { it.key == rule.catalogueId }
        ?: CATALOG_ITEMS.firstOrNull { it.ruleSuffix == suffix }
    return catalogItem?.let { catalogShortLabel(it) } ?: componentLabel(rule)
}

fun BrandWorld.logo(): DrawableResource = when (id) {
    "ktm" -> Res.drawable.logo_ktm
    "cfmoto" -> Res.drawable.logo_cfmoto
    "yamaha" -> Res.drawable.logo_yamaha
    "honda" -> Res.drawable.logo_honda
    "suzuki" -> Res.drawable.logo_suzuki
    "bajaj" -> Res.drawable.logo_bajaj
    "tvs" -> Res.drawable.logo_tvs
    "hero" -> Res.drawable.logo_hero
    else -> Res.drawable.logo_enfield
}
