package com.motovault.app.ui

import com.motovault.app.domain.capture.CaptureServiceParser

data class SmartServiceSuggestion(val source: String, val line: ServiceLineItem)
data class SmartServiceDraft(val suggestions: List<SmartServiceSuggestion>, val unmatched: List<String>)

/** Legacy call sites retain their typed suggestions and explicit review. */
object SmartServiceDraftParser {
    fun parse(input: String): SmartServiceDraft {
        val result = CaptureServiceParser.parse(input)
        return SmartServiceDraft(result.lines.map { SmartServiceSuggestion(it.source, it.line) }, result.unmatched)
    }
}
