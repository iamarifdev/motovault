package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class Digits(val label: String) {
    BN("১২৩ বাংলা"),
    EN("123 English")
}

private const val BN_DIGITS_STR = "০১২৩৪৫৬৭৮৯"

/** Accepts Bengali, Arabic-Indic, or Latin digits. All numeric inputs normalise through this. */
fun toAscii(s: String): String = s.map { ch ->
    when (ch) {
        in '০'..'৯' -> ('0'.code + (ch.code - '০'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
        else -> ch
    }
}.joinToString("")

fun toBn(s: String): String = s.map { ch ->
    if (ch in '0'..'9') BN_DIGITS_STR[ch.code - '0'.code] else ch
}.joinToString("")

fun parseNum(s: String): Double? {
    val t = toAscii(s).replace(",", "").trim()
    if (t.isEmpty() || t == ".") return null
    return t.toDoubleOrNull()
}

/** Formats integer using Bangladesh / Indian Lakh grouping (e.g. 1,23,456). */
fun formatLakh(value: Long): String {
    val sign = if (value < 0) "-" else ""
    val s = kotlin.math.abs(value).toString()
    if (s.length <= 3) return sign + s
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
    return sign + groupedRest + "," + last3
}

fun formatLakh(value: Double, dec: Int = 0): String {
    if (dec == 0) return formatLakh(kotlin.math.round(value).toLong())
    var factor = 1.0
    repeat(dec) { factor *= 10.0 }
    val rounded = kotlin.math.round(value * factor) / factor
    val parts = rounded.toString().split('.')
    val whole = formatLakh(parts[0].toLong())
    val frac = if (parts.size > 1) parts[1].padEnd(dec, '0').take(dec) else "".padEnd(dec, '0')
    return "$whole.$frac"
}

val MONTHS_BN = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

val MONTHS_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

class Fmt(val digits: Digits) {
    fun d(s: String): String = if (digits == Digits.BN) toBn(s) else toAscii(s)

    fun n(x: Long): String = d(formatLakh(x))

    fun n(x: Double, dec: Int = 0): String = d(formatLakh(x, dec))

    fun raw(s: String): String = d(s)

    fun money(x: Double): String = "৳" + d(formatLakh(kotlin.math.round(x).toLong()))

    fun km(x: Double): String = d(formatLakh(kotlin.math.round(x).toLong())) + if (digits == Digits.BN) " কিমি" else " km"

    fun liters(x: Double): String = d(formatLakh(x, 2)) + if (digits == Digits.BN) " লিটার" else " L"

    fun days(x: Int): String = d(x.toString()) + if (digits == Digits.BN) " দিন" else " days"

    fun date(d: LocalDate): String {
        val months = if (digits == Digits.BN) MONTHS_BN else MONTHS_EN
        return "${d(d.dayOfMonth.toString())} ${months[d.monthNumber - 1]}"
    }

    fun dateFull(d: LocalDate): String {
        val months = if (digits == Digits.BN) MONTHS_BN else MONTHS_EN
        return "${d(d.dayOfMonth.toString())} ${months[d.monthNumber - 1]} ${d(d.year.toString())}"
    }

    fun month(d: LocalDate): String {
        val months = if (digits == Digits.BN) MONTHS_BN else MONTHS_EN
        return "${months[d.monthNumber - 1]} ${d(d.year.toString())}"
    }
}

val LocalFmt = staticCompositionLocalOf { Fmt(Digits.BN) }

val currentFmt: Fmt
    @Composable get() = LocalFmt.current
