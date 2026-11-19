package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.model.FuelDefaults
import kotlin.math.round

data class PumpMeterReading(
    val amountTaka: Double?,
    val litres: Double?,
    val pricePerLitre: Double?,
    val fuelType: String?
)

/** Extracts sale amount, litres, unit price, and fuel type from pump-meter OCR text. */
object PumpMeterOcrParser {
    private const val NUM = "([0-9]+(?:,[0-9]{2,3})*(?:\\.[0-9]+)?)"
    private const val HS = "[ \\t]*"
    private val petrolRegex = Regex("পেট্রোল|petrol", RegexOption.IGNORE_CASE)
    private val octaneRegex = Regex("অকটেন|octane|\\boct\\b", RegexOption.IGNORE_CASE)

    private val priceLabeledRegex = Regex(
        "(?:rate|price|unit${HS}price|per${HS}l(?:tr|itre|iter)?|দাম|দর)$HS[:=]?$HS(?:৳|tk|bdt|rs)?$HS$NUM",
        RegexOption.IGNORE_CASE
    )
    private val priceSlashRegex = Regex(
        "(?:৳|tk|bdt|rs)?$HS$NUM$HS/$HS(?:l(?:tr|itre|iter)?|লিটার|লি)",
        RegexOption.IGNORE_CASE
    )
    private val litreLabeledRegex = Regex(
        "(?:volume|vol|qty|lit(?:re|er)s?|ltr|লিটার)$HS[:=]?$HS$NUM",
        RegexOption.IGNORE_CASE
    )
    private val litreSuffixRegex = Regex(
        "$NUM$HS(?:litres|liters|litre|liter|ltr|l|লিটার)(?![a-z])",
        RegexOption.IGNORE_CASE
    )
    private val amountLabeledRegex = Regex(
        "(?:amount|amt|sale|total|taka|টাকা|মোট)$HS[:=]?$HS(?:৳|tk|bdt|rs)?$HS$NUM",
        RegexOption.IGNORE_CASE
    )

    fun parse(ocrText: String): PumpMeterReading? {
        if (ocrText.isBlank()) return null
        val text = NormalizedNumbers.ascii(ocrText)
        val fuelType = when {
            petrolRegex.containsMatchIn(text) -> FuelDefaults.PETROL
            octaneRegex.containsMatchIn(text) -> FuelDefaults.OCTANE
            else -> null
        }

        val priceMatches = (priceLabeledRegex.findAll(text) + priceSlashRegex.findAll(text)).toList()
        var price = priceMatches.firstNotNullOfOrNull { match ->
            NormalizedNumbers.parse(match.groupValues[1])?.takeIf { it in 20.0..500.0 }
        }
        val excludedRanges = priceMatches.map { it.range }

        var litres = findLitres(text, excludedRanges)
        var amount = findAmount(text, excludedRanges)

        if (amount == null && litres != null && price != null) {
            amount = round2(litres * price).takeIf { it in 1.0..100_000.0 }
        } else if (litres == null && amount != null && price != null && price > 0.0) {
            litres = round2(amount / price).takeIf { it in 0.05..100.0 }
        } else if (price == null && amount != null && litres != null && litres > 0.0) {
            price = round2(amount / litres).takeIf { it in 20.0..500.0 }
        }

        if (amount == null && litres == null) return null
        return PumpMeterReading(amount, litres, price, fuelType)
    }

    private fun findLitres(text: String, excluded: List<IntRange>): Double? {
        val labeled = litreLabeledRegex.findAll(text).mapNotNull { match ->
            if (overlapsAny(match.range, excluded)) null
            else NormalizedNumbers.parse(match.groupValues[1])?.takeIf { it in 0.05..100.0 }
        }.firstOrNull()
        if (labeled != null) return labeled

        return litreSuffixRegex.findAll(text).mapNotNull { match ->
            val prev = if (match.range.first > 0) text[match.range.first - 1] else ' '
            if (prev == '/' || overlapsAny(match.range, excluded)) null
            else NormalizedNumbers.parse(match.groupValues[1])?.takeIf { it in 0.05..100.0 }
        }.firstOrNull()
    }

    private fun findAmount(text: String, excluded: List<IntRange>): Double? {
        val labeled = amountLabeledRegex.findAll(text).mapNotNull { match ->
            if (overlapsAny(match.range, excluded) || isFollowedByPerLitre(text, match.range.last)) null
            else NormalizedNumbers.parse(match.groupValues[1])?.takeIf { it in 1.0..100_000.0 }
        }.firstOrNull()
        if (labeled != null) return labeled

        val stripped = buildString(text.length) {
            text.forEachIndexed { idx, ch ->
                append(if (excluded.any { idx in it }) ' ' else ch)
            }
        }
        val money = CaptureTextNumbers.money(stripped)
        if (!money.malformed) {
            val candidate = money.candidates.singleOrNull()?.value?.let(Money::taka)
            if (candidate != null && candidate in 1.0..100_000.0) return candidate
        }
        return null
    }

    private fun isFollowedByPerLitre(text: String, endIndex: Int): Boolean {
        val tail = text.substring(minOf(text.length, endIndex + 1)).trimStart()
        return tail.startsWith("/l", ignoreCase = true) || tail.startsWith("/লি")
    }

    private fun overlapsAny(range: IntRange, others: List<IntRange>): Boolean =
        others.any { other -> range.first <= other.last && other.first <= range.last }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
