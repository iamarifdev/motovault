package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.*

/** Input aliases are language data, never localized record identities. */
object CaptureServiceParser {
    private val aliases = listOf(
        "oil filter" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "তেলের ফিল্টার" to "oil_filter",
        "engine oil" to "engine_oil", "ইঞ্জিন অয়েল" to "engine_oil", "ইঞ্জিন অয়েল" to "engine_oil", "ইঞ্জিন তেল" to "engine_oil", "mobil" to "engine_oil",
        "অয়েল" to "engine_oil", "অয়েল" to "engine_oil", "ফিল্টার" to "oil_filter",
        "chain sprocket" to "chain_sprocket", "chain" to "chain_sprocket", "চেইন" to "chain_sprocket",
        "air filter" to "air_filter", "এয়ার ফিল্টার" to "air_filter", "এয়ার ফিল্টার" to "air_filter",
        "spark plug" to "spark_plug", "স্পার্ক প্লাগ" to "spark_plug", "ব্রেক প্যাড" to "brake_pad",
        "brake pad" to "brake_pad", "brake shoe" to "brake_pad", "ব্রেক" to "brake_pad",
        "brake fluid" to "brake_fluid", "ব্রেক ফ্লুইড" to "brake_fluid",
        "coolant" to "coolant", "কুল্যান্ট" to "coolant", "clutch cable" to "clutch_cable", "ক্লাচ কেবল" to "clutch_cable",
        "tyre" to "tyre", "tire" to "tyre", "টায়ার" to "tyre", "টায়ার" to "tyre", "battery" to "battery", "ব্যাটারি" to "battery",
        "general service" to "general_service", "সাধারণ সার্ভিস" to "general_service", "সার্ভিস" to "general_service"
    ).sortedByDescending { it.first.length }
    private val quantity = Regex("(?:^|\\s)([0-9]+(?:\\.[0-9]+)?)\\s*(?:x|টি|টা|পিস)(?=\\s|$)", RegexOption.IGNORE_CASE)
    private const val MAX_SERVICE_QUANTITY = 10_000.0

    fun parse(input: String): CapturedService {
        val lines = mutableListOf<CapturedServiceLine>()
        val unmatched = mutableListOf<String>()
        input.split(Regex("[\\n;।]|,(?![0-9০-৯٠-٩])")).map(String::trim).filter(String::isNotBlank)
            .take(MAX_CAPTURE_SEGMENTS).forEach { source ->
                val text = NormalizedNumbers.ascii(source).lowercase()
                val id = aliases.firstOrNull { (alias, _) ->
                    CaptureWordBoundaries.aliasIn(text, alias)
                }?.second
                val item = id?.let(BangladeshServiceCatalogue::find)
                if (item == null) { unmatched += source; return@forEach }
                val amount = CaptureTextNumbers.money(source).takeUnless { it.malformed }?.candidates?.singleOrNull()?.value
                    ?.takeIf { it <= Int.MAX_VALUE }?.toInt()
                val qty = quantity.find(text)?.groupValues?.get(1)?.let(NormalizedNumbers::parse)
                    ?.takeIf { it > 0 && it <= MAX_SERVICE_QUANTITY } ?: 1.0
                lines += CapturedServiceLine(source, ServiceLineItem(catalogueId = item.id, action = item.action,
                    quantity = qty, unitBn = "", unitPricePoisha = amount,
                    oil = if (item.id == "engine_oil") EngineOilMetadata("mineral", purchaseBasis = "bottle") else null))
            }
        return CapturedService(lines, unmatched)
    }
}
