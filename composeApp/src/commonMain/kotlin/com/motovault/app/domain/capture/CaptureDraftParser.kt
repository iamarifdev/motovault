package com.motovault.app.domain.capture

import com.motovault.app.domain.input.NormalizedNumbers

object CaptureDraftParser {
    val fuelWords = Regex("পাম্প|ফিলিং|filling|fuel|ফুয়েল|পেট্রোল|অকটেন|octane|petrol", RegexOption.IGNORE_CASE)

    fun identityText(text: String): String = NormalizedNumbers.ascii(text).trim().lowercase().replace(Regex("\\s+"), " ")

    fun parse(text: String, source: CaptureSource, id: String, baselineOdometer: Double,
        hint: CaptureIntent = CaptureIntent.UNKNOWN, acceptedIds: Set<String> = emptySet()
    ): CaptureDraft {
        val issues = mutableSetOf<CaptureIssue>()
        if (text.isBlank()) issues += CaptureIssue.EMPTY
        if (text.length > MAX_CAPTURE_TEXT_LENGTH) issues += CaptureIssue.TOO_LONG
        if (source == CaptureSource.SHARE && id in acceptedIds) issues += CaptureIssue.DUPLICATE_SHARE
        val bounded = text.take(MAX_CAPTURE_TEXT_LENGTH)
        val money = CaptureTextNumbers.money(bounded)
        val amounts = if (source == CaptureSource.MANUAL && hint in listOf(CaptureIntent.FUEL, CaptureIntent.EXPENSE)) {
            listOfNotNull(NormalizedNumbers.poisha(bounded)?.let { CaptureCandidate(it, bounded, CaptureConfidence.MANUAL) })
        } else money.candidates
        if (money.malformed) issues += CaptureIssue.MALFORMED_AMOUNT
        if (amounts.size > 1) issues += CaptureIssue.AMBIGUOUS_AMOUNT
        val odometers = CaptureTextNumbers.odometers(bounded, baselineOdometer, source == CaptureSource.PHOTO || hint == CaptureIntent.ODOMETER)
        issues += odometers.issues
        val service = CaptureServiceParser.parse(bounded)
        val intent = when {
            hint != CaptureIntent.UNKNOWN -> hint
            service.lines.isNotEmpty() -> CaptureIntent.SERVICE
            odometers.candidates.isNotEmpty() && amounts.isEmpty() -> CaptureIntent.ODOMETER
            amounts.isNotEmpty() && fuelWords.containsMatchIn(bounded) -> CaptureIntent.FUEL
            amounts.isNotEmpty() -> CaptureIntent.EXPENSE
            else -> CaptureIntent.UNKNOWN
        }
        if (amounts.isEmpty() && odometers.candidates.isEmpty() && service.lines.isEmpty()) issues += CaptureIssue.NO_FIELDS
        return CaptureDraft(id, source, bounded, intent, amounts, odometers.candidates, service, issues)
    }
}
