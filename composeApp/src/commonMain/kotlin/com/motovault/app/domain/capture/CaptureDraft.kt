package com.motovault.app.domain.capture

import com.motovault.app.domain.model.ServiceLineItem

const val MAX_CAPTURE_TEXT_LENGTH = 10_000
const val MAX_CAPTURE_SEGMENTS = 20
const val MAX_CAPTURE_CANDIDATES = 12
const val MAX_CAPTURE_ODOMETER_KM = 9_999_999.0
const val MAX_CAPTURE_RECEIPTS = 50

enum class CaptureSource { MANUAL, PASTE, PHOTO, SHARE, VOICE }
enum class CaptureIntent { UNKNOWN, FUEL, SERVICE, ODOMETER, EXPENSE }
enum class CaptureConfidence { MANUAL, LABELED, UNLABELED, AMBIGUOUS }
enum class CaptureIssue {
    EMPTY, TOO_LONG, MALFORMED_AMOUNT, AMBIGUOUS_AMOUNT, AMBIGUOUS_ODOMETER,
    ODOMETER_REGRESSION, INVALID_ODOMETER, DUPLICATE_SHARE, NO_FIELDS
}
data class CaptureCandidate<T>(val value: T, val excerpt: String, val confidence: CaptureConfidence)
data class CapturedServiceLine(val source: String, val line: ServiceLineItem)
data class CapturedService(val lines: List<CapturedServiceLine>, val unmatched: List<String>)

/** Acquisition is untrusted. No candidate is a saved record or an automatic confirmation. */
data class CaptureDraft(
    val id: String,
    val source: CaptureSource,
    val text: String,
    val intent: CaptureIntent,
    val amounts: List<CaptureCandidate<Long>>,
    val odometers: List<CaptureCandidate<Double>>,
    val service: CapturedService,
    val issues: Set<CaptureIssue>,
    val targetBikeId: String = "primary"
) {
    val requiresChoice get() = CaptureIssue.AMBIGUOUS_AMOUNT in issues || CaptureIssue.AMBIGUOUS_ODOMETER in issues
    val canReview get() = issues.none { it in setOf(CaptureIssue.EMPTY, CaptureIssue.TOO_LONG, CaptureIssue.DUPLICATE_SHARE) }
}

object CaptureReceipts {
    private val fingerprint = Regex("[a-f0-9]{64}")
    fun accept(previous: List<String>, id: String): List<String> {
        require(fingerprint.matches(id))
        require(id !in previous)
        return (listOf(id) + previous).take(MAX_CAPTURE_RECEIPTS)
    }
}
