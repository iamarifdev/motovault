package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.model.FuelDefaults
import kotlin.math.roundToLong

data class SpokenFuelResult(val amountTaka: Long, val fuelType: String?)

/** Extracts spoken Taka amount and optional octane/petrol keyword from a transcript. */
object SpokenFuelParser {
    private val petrolPattern = Regex("পেট্রোল|petrol")
    private val octanePattern = Regex("অকটেন|octane")

    fun parse(transcript: String): SpokenFuelResult? {
        val money = CaptureTextNumbers.money(transcript)
        val amount = if (!money.malformed) {
            money.candidates.singleOrNull()?.value?.let { Money.taka(it).roundToLong() }
        } else null
        val parsed = amount ?: SpokenNumberParser.parse(transcript)
        if (parsed == null || parsed <= 0L) return null
        val normalized = NormalizedNumbers.ascii(transcript).lowercase()
        val fuelType = when {
            petrolPattern.containsMatchIn(normalized) -> FuelDefaults.PETROL
            octanePattern.containsMatchIn(normalized) -> FuelDefaults.OCTANE
            else -> null
        }
        return SpokenFuelResult(amountTaka = parsed, fuelType = fuelType)
    }
}
