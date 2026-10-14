package com.motovault.app.ui

/** Local, deliberately small parser. Every result remains a draft until the owner reviews and saves it. */
data class SmartServiceSuggestion(val source: String, val line: ServiceLineItem)
data class SmartServiceDraft(val suggestions: List<SmartServiceSuggestion>, val unmatched: List<String>)

object SmartServiceDraftParser {
    private val aliases = listOf(
        "oil filter" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "engine oil" to "engine_oil",
        "ইঞ্জিন অয়েল" to "engine_oil", "ইঞ্জিন অয়েল" to "engine_oil", "chain sprocket" to "chain_sprocket", "chain" to "chain_sprocket",
        "চেইন" to "chain_sprocket", "air filter" to "air_filter", "এয়ার ফিল্টার" to "air_filter",
        "spark plug" to "spark_plug", "স্পার্ক প্লাগ" to "spark_plug", "brake pad" to "brake_pad",
        "brake shoe" to "brake_pad", "ব্রেক প্যাড" to "brake_pad", "brake fluid" to "brake_fluid",
        "ব্রেক ফ্লুইড" to "brake_fluid", "coolant" to "coolant", "কুল্যান্ট" to "coolant",
        "clutch cable" to "clutch_cable", "ক্লাচ কেবল" to "clutch_cable", "tyre" to "tyre",
        "tire" to "tyre", "টায়ার" to "tyre", "battery" to "battery", "ব্যাটারি" to "battery",
        "general service" to "general_service", "সাধারণ সার্ভিস" to "general_service"
    ).sortedByDescending { it.first.length }
    private val pricePattern = Regex("(?:৳|bdt\\s*|tk\\s*|taka\\s*|টাকা\\s*)([0-9]+(?:\\.[0-9]{1,2})?)|([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:৳|bdt|tk|taka|টাকা)", RegexOption.IGNORE_CASE)
    private val quantityPattern = Regex("(?:^|\\s)([0-9]+(?:\\.[0-9]+)?)\\s*x\\s*", RegexOption.IGNORE_CASE)

    private fun asciiDigits(value: String) = buildString(value.length) {
        value.forEach { char -> append(when (char) {
            in '০'..'৯' -> '0' + (char - '০')
            in '٠'..'٩' -> '0' + (char - '٠')
            else -> char
        }) }
    }

    fun parse(input: String): SmartServiceDraft {
        val suggestions = mutableListOf<SmartServiceSuggestion>()
        val unmatched = mutableListOf<String>()
        input.lineSequence().flatMap { it.split(';').asSequence() }.map(String::trim).filter(String::isNotBlank).take(20).forEach { source ->
            val normalized = asciiDigits(source).lowercase()
            val id = aliases.firstOrNull { (alias, _) -> normalized.contains(alias) }?.second
            val item = id?.let(BangladeshServiceCatalogue::find)
            if (item == null) {
                unmatched += source
                return@forEach
            }
            val amount = pricePattern.find(normalized)?.let { match ->
                (match.groupValues[1].ifBlank { match.groupValues[2] }).toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it >= 0 && it <= Int.MAX_VALUE / 100.0 }
                    ?.let { (it * 100.0).toLong().takeIf { poisha -> poisha <= Int.MAX_VALUE }?.toInt() }
            }
            val quantity = quantityPattern.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
                ?.takeIf { it.isFinite() && it > 0 && it <= 10000 } ?: 1.0
            suggestions += SmartServiceSuggestion(source, ServiceLineItem(
                catalogueId = item.id, action = item.action, quantity = quantity,
                unitBn = item.unitBn, unitPricePoisha = amount,
                oil = if (item.id == "engine_oil") EngineOilMetadata("mineral", purchaseBasis = "bottle") else null
            ))
        }
        return SmartServiceDraft(suggestions, unmatched)
    }
}
