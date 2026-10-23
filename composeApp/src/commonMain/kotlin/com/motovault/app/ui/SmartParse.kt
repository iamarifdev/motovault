package com.motovault.app.ui

data class ParsedLine(
    val item: String, // e.g. "engine_oil", "oil_filter", "air_filter", "spark_plug", "chain_sprocket", "brake_pad", "coolant", "tyre", "battery", "general_service"
    val qty: Double,
    val price: Double?,
    val source: String
)

data class ParseResult(
    val lines: List<ParsedLine>,
    val unmatched: List<String>
)

object SmartParse {
    private val ALIASES: List<Pair<String, String>> = listOf(
        "oil filter" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "অয়েল ফিল্টার" to "oil_filter", "তেলের ফিল্টার" to "oil_filter", "ফিল্টার" to "oil_filter",
        "engine oil" to "engine_oil", "ইঞ্জিন অয়েল" to "engine_oil", "ইঞ্জিন অয়েল" to "engine_oil", "ইঞ্জিন তেল" to "engine_oil", "অয়েল" to "engine_oil", "অয়েল" to "engine_oil", "mobil" to "engine_oil",
        "chain sprocket" to "chain_sprocket", "chain" to "chain_sprocket", "চেইন ও স্প্রকেট" to "chain_sprocket", "চেইন" to "chain_sprocket",
        "air filter" to "air_filter", "এয়ার ফিল্টার" to "air_filter", "এয়ার ফিল্টার" to "air_filter",
        "spark plug" to "spark_plug", "স্পার্ক প্লাগ" to "spark_plug", "স্পার্ক" to "spark_plug",
        "brake pad" to "brake_pad", "brake" to "brake_pad", "ব্রেক প্যাড" to "brake_pad", "ব্রেক" to "brake_pad",
        "coolant" to "coolant", "কুল্যান্ট" to "coolant",
        "tyre" to "tyre", "tire" to "tyre", "টায়ার" to "tyre", "টায়ার" to "tyre",
        "battery" to "battery", "ব্যাটারি" to "battery",
        "general service" to "general_service", "সার্ভিস" to "general_service"
    ).sortedByDescending { it.first.length }

    private val PRICE_REGEX = Regex("""(?:৳|bdt\s*|tk\s*|taka\s*|টাকা\s*)(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:৳|bdt|tk|taka|টাকা|তক)""", RegexOption.IGNORE_CASE)
    private val QTY_REGEX = Regex("""(?:^|\s)(\d+(?:\.\d+)?)\s*(?:x|টি|টা|পিস)\b""", RegexOption.IGNORE_CASE)
    val FUEL_WORDS = Regex("""(পাম্প|ফিলিং|filling|fuel|পেট্রোল|অকটেন|octane|petrol)""", RegexOption.IGNORE_CASE)

    fun parseServiceText(input: String): ParseResult {
        val lines = mutableListOf<ParsedLine>()
        val unmatched = mutableListOf<String>()
        val segments = input.split(Regex("""[\n;,।]""")).map { it.trim() }.filter { it.isNotEmpty() }.take(20)

        for (source in segments) {
            val text = toAscii(source).lowercase()
            val hit = ALIASES.firstOrNull { (alias, _) -> text.contains(alias) }
            if (hit == null) {
                unmatched.add(source)
                continue
            }
            val priceMatch = PRICE_REGEX.find(text)
            val price = priceMatch?.let { m ->
                val g1 = m.groups[1]?.value
                val g2 = m.groups[2]?.value
                (g1 ?: g2)?.toDoubleOrNull()
            }
            val qtyMatch = QTY_REGEX.find(text)
            val qty = qtyMatch?.groups?.get(1)?.value?.toDoubleOrNull() ?: 1.0

            lines.add(
                ParsedLine(
                    item = hit.second,
                    qty = qty,
                    price = price,
                    source = source
                )
            )
        }
        return ParseResult(lines, unmatched)
    }

    fun parseAmount(input: String): Double? {
        val t = toAscii(input).replace(",", "")
        val match = PRICE_REGEX.find(t)
        return match?.let { m ->
            val g1 = m.groups[1]?.value
            val g2 = m.groups[2]?.value
            (g1 ?: g2)?.toDoubleOrNull()
        }
    }
}
