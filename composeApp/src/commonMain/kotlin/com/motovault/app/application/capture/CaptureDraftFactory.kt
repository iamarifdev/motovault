package com.motovault.app.application.capture

import com.motovault.app.domain.capture.*

internal fun interface CaptureFingerprint { fun digest(canonicalText: String): String }

/** Injected native hashing never gives an acquisition adapter permission to save. */
internal class CaptureDraftFactory(private val fingerprint: CaptureFingerprint) {
    fun create(text: String, source: CaptureSource, baselineOdometer: Double,
        hint: CaptureIntent = CaptureIntent.UNKNOWN, acceptedIds: Set<String> = emptySet(), originalSharedText: String? = null
    ): CaptureDraft {
        val boundedIdentity = CaptureDraftParser.identityText((originalSharedText ?: text).take(MAX_CAPTURE_TEXT_LENGTH))
        return CaptureDraftParser.parse(text, source, fingerprint.digest(boundedIdentity), baselineOdometer, hint, acceptedIds)
    }
}
