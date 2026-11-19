package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers

/** Parses spoken integers in Bangla words, English words, Bangla/Arabic-Indic digits, or mixed transcripts. */
object SpokenNumberParser {
    private val banglaWords1To99 = listOf(
        "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
        "এগারো", "বারো", "তেরো", "চোদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ", "বিশ",
        "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "উনত্রিশ", "ত্রিশ",
        "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "উনচল্লিশ", "চল্লিশ",
        "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "উনপঞ্চাশ", "পঞ্চাশ",
        "একান্ন", "বাহান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "উনষাট", "ষাট",
        "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "উনসত্তর", "সত্তর",
        "একাত্তর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "উনআশি", "আশি",
        "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "আটাশি", "উননব্বই", "নব্বই",
        "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই"
    )

    private val values: Map<String, Long> = buildMap {
        banglaWords1To99.forEachIndexed { index, word -> put(word, (index + 1).toLong()) }
        put("শূন্য", 0L); put("দু", 2L); put("ছ", 6L); put("ছয়", 6L); put("নই", 9L); put("নয়", 9L)
        put("এগার", 11L); put("বার", 12L); put("তের", 13L); put("পনের", 15L); put("ষোল", 16L)
        put("সতের", 17L); put("আঠার", 18L)
        put("zero", 0L); put("one", 1L); put("two", 2L); put("three", 3L); put("four", 4L)
        put("five", 5L); put("six", 6L); put("seven", 7L); put("eight", 8L); put("nine", 9L)
        put("ten", 10L); put("eleven", 11L); put("twelve", 12L); put("thirteen", 13L); put("fourteen", 14L)
        put("fifteen", 15L); put("sixteen", 16L); put("seventeen", 17L); put("eighteen", 18L); put("nineteen", 19L)
        put("twenty", 20L); put("thirty", 30L); put("forty", 40L); put("fifty", 50L)
        put("sixty", 60L); put("seventy", 70L); put("eighty", 80L); put("ninety", 90L)
    }

    private val hundredSuffixes = listOf("শো", "শত", "শ")
    private val scales = mapOf(
        "হাজার" to 1000L, "thousand" to 1000L,
        "লাখ" to 100_000L, "লক্ষ" to 100_000L, "lakh" to 100_000L, "lac" to 100_000L
    )
    private val unitSuffixRegex = Regex("(টা|টি|টাকার|টাকা|কিমি|km|tk|bdt)$")

    private fun parseSmallToken(token: String): Long? {
        if (token.all { it in '0'..'9' } && token.isNotEmpty()) return token.toLongOrNull()
        values[token]?.let { return it }
        if (token == "hundred") return 100L
        for (suffix in hundredSuffixes) {
            if (!token.endsWith(suffix)) continue
            val head = token.dropLast(suffix.length)
            if (head.isEmpty() || head == "এক") return 100L
            val multiplier = values[head]
            if (multiplier != null && multiplier in 1L..9L) return multiplier * 100L
        }
        return null
    }

    fun parse(input: String): Long? {
        val cleaned = NormalizedNumbers.ascii(input).lowercase().replace(",", "")
        val tokens = cleaned.split(Regex("[\\s\\-]+")).filter { it.isNotBlank() }
        var total = 0L
        var current = 0L
        var found = false
        for (raw in tokens) {
            val token = raw.replace(unitSuffixRegex, "")
            if (token.isEmpty()) continue
            val scale = scales[token]
            if (scale != null) {
                total += (if (current == 0L) 1L else current) * scale
                current = 0L
                found = true
                continue
            }
            if (token == "hundred") {
                current = (if (current == 0L) 1L else current) * 100L
                found = true
                continue
            }
            val value = parseSmallToken(token) ?: continue
            current += value
            found = true
        }
        return if (found) total + current else null
    }
}
