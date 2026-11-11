package com.motovault.app.domain.ledger

import kotlin.math.floor

const val POISHA_PER_TAKA = 100L
const val MAX_COST_POISHA = 10_000_000_000L
private const val LEGACY_ROUNDING_TOLERANCE = 0.000001

object Money {
    fun fromLegacy(taka: Double): Long? {
        if (!taka.isFinite() || taka < 0 || taka > MAX_COST_POISHA.toDouble() / POISHA_PER_TAKA) return null
        return floor(taka * POISHA_PER_TAKA + 0.5 + LEGACY_ROUNDING_TOLERANCE).toLong()
            .takeIf { it <= MAX_COST_POISHA }
    }

    /** Canonical decimal text; input-digit normalization belongs at the presentation boundary. */
    fun parse(canonical: String): Long? {
        if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(canonical)) return null
        val whole = canonical.substringBefore('.').toLongOrNull() ?: return null
        if (whole > MAX_COST_POISHA / POISHA_PER_TAKA) return null
        val fraction = canonical.substringAfter('.', "").padEnd(2, '0').toLong()
        return (whole * POISHA_PER_TAKA + fraction).takeIf { it <= MAX_COST_POISHA }
    }

    fun decimal(poisha: Long): String = "${poisha / POISHA_PER_TAKA}.${(poisha % POISHA_PER_TAKA).toString().padStart(2, '0')}"

    fun taka(poisha: Long) = poisha.toDouble() / POISHA_PER_TAKA
}
