package com.motovault.app.ui

import com.motovault.app.domain.capture.CaptureDraftParser
import com.motovault.app.domain.capture.CaptureServiceParser
import com.motovault.app.domain.capture.CaptureTextNumbers
import com.motovault.app.domain.ledger.Money

data class ParsedLine(val item: String, val qty: Double, val price: Double?, val source: String)
data class ParseResult(val lines: List<ParsedLine>, val unmatched: List<String>)

/** Compatibility boundary for existing service/fuel drafts. Parsing has one domain owner. */
object SmartParse {
    val FUEL_WORDS = CaptureDraftParser.fuelWords
    fun parseServiceText(input: String): ParseResult {
        val result = CaptureServiceParser.parse(input)
        return ParseResult(result.lines.map { ParsedLine(requireNotNull(it.line.catalogueId), it.line.quantity,
            it.line.unitPricePoisha?.let { amount -> Money.taka(amount.toLong()) }, it.source) }, result.unmatched)
    }
    fun parseAmount(input: String): Double? {
        val result = CaptureTextNumbers.money(input)
        return if (result.malformed) null else result.candidates.singleOrNull()?.value?.let(Money::taka)
    }
}
