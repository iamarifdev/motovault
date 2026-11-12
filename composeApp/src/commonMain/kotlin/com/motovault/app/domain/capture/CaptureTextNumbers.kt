package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers

data class CapturedMoney(val candidates: List<CaptureCandidate<Long>>, val malformed: Boolean)
data class CapturedOdometers(val candidates: List<CaptureCandidate<Double>>, val issues: Set<CaptureIssue>)

object CaptureTextNumbers {
    private const val CONTEXT_LENGTH = 40
    private const val NUMBER = "[+-]?[0-9][0-9,.eE+-]*"
    private const val CURRENCY = "(?:৳|(?<![a-z])(?:bdt|tk|taka)(?![a-z])|টাকা)"
    private val before = Regex("$CURRENCY\\s*($NUMBER)", RegexOption.IGNORE_CASE)
    private val after = Regex("($NUMBER)\\s*$CURRENCY", RegexOption.IGNORE_CASE)
    private val labels = Regex("balance|available|charge|fee|ব্যালেন্স|চার্জ|ফি|paid|payment|amount|total|মোট|পেমেন্ট|পরিশোধ|দিয়েছি|দিয়েছি", RegexOption.IGNORE_CASE)
    private val excluded = setOf("balance", "available", "charge", "fee", "ব্যালেন্স", "চার্জ", "ফি")
    private val odoBefore = Regex("(?:odometer|(?<![a-z])odo|ওডোমিটার|মিটার)\\s*[:=]?\\s*($NUMBER)", RegexOption.IGNORE_CASE)
    private val odoAfter = Regex("($NUMBER)\\s*(?:km|কিমি|কি\\.মি\\.)(?![a-z])", RegexOption.IGNORE_CASE)
    private val numbers = Regex(NUMBER)

    fun money(input: String): CapturedMoney {
        val text = NormalizedNumbers.ascii(input).lowercase()
        var malformed = false
        val matches = (before.findAll(text).filter { CaptureWordBoundaries.tokenEnd(text, it.range.last) }.toList() +
            after.findAll(text).filter { CaptureWordBoundaries.tokenStart(text, it.range.first) && CaptureWordBoundaries.tokenEnd(text, it.range.last) }.toList())
            .distinctBy { it.range }
        if (matches.isEmpty() && Regex("$CURRENCY\\s*[+-]?[0-9]", RegexOption.IGNORE_CASE).containsMatchIn(text)) malformed = true
        val found = matches.mapNotNull { match ->
                val label = labels.findAll(text.substring(maxOf(0, match.range.first - CONTEXT_LENGTH), match.range.first)).lastOrNull()?.value
                if (label in excluded) return@mapNotNull null
                val amount = NormalizedNumbers.poisha(match.groupValues[1].removeSuffix(".").removeSuffix(","))
                if (amount == null) { malformed = true; return@mapNotNull null }
                CaptureCandidate(amount, match.value, CaptureConfidence.LABELED) to (label != null)
            }
        val preferred = found.filter { it.second }.ifEmpty { found }
        return CapturedMoney(preferred.map { it.first }.distinctBy { it.value }.take(MAX_CAPTURE_CANDIDATES), malformed)
    }

    fun odometers(input: String, baseline: Double, allowUnlabeled: Boolean): CapturedOdometers {
        val text = NormalizedNumbers.ascii(input)
        val labeled = (odoBefore.findAll(text).toList() + odoAfter.findAll(text).toList()).map { it.groupValues[1] to it.value }
        val candidates = if (labeled.isEmpty() && allowUnlabeled) numbers.findAll(text).map { it.value to it.value }.toList() else labeled
        val issues = mutableSetOf<CaptureIssue>()
        val valid = candidates.mapNotNull { (number, excerpt) ->
            val km = NormalizedNumbers.parse(number.removeSuffix(".").removeSuffix(","))
            when {
                km == null || km !in 0.0..MAX_CAPTURE_ODOMETER_KM -> { issues += CaptureIssue.INVALID_ODOMETER; null }
                km < baseline -> { issues += CaptureIssue.ODOMETER_REGRESSION; null }
                else -> CaptureCandidate(km, excerpt, if (labeled.isEmpty()) CaptureConfidence.UNLABELED else CaptureConfidence.LABELED)
            }
        }.distinctBy { it.value }.take(MAX_CAPTURE_CANDIDATES)
        if (valid.size > 1) issues += CaptureIssue.AMBIGUOUS_ODOMETER
        return CapturedOdometers(valid, issues)
    }
}
