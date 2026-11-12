package com.motovault.app.domain.input

import com.motovault.app.domain.ledger.Money

/** Input syntax is independent of language and display digits. */
object NormalizedNumbers {
    private val groupedInteger = "(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})"
    private val number = Regex("[+-]?(?:$groupedInteger(?:\\.[0-9]*)?|\\.[0-9]+)")

    fun ascii(value: String): String = buildString(value.length) {
        value.forEach { ch -> append(when (ch) {
            in '০'..'৯' -> '0' + (ch - '০')
            in '٠'..'٩' -> '0' + (ch - '٠')
            '\u066b' -> '.'
            '\u066c' -> ','
            else -> ch
        }) }
    }

    fun canonical(value: String): String? {
        val text = ascii(value).trim()
        return text.takeIf { number.matches(it) }?.replace(",", "")
    }

    fun parse(value: String): Double? = canonical(value)?.toDoubleOrNull()?.takeIf { it.isFinite() }

    fun poisha(value: String): Long? = canonical(value)?.let(Money::parse)
}
