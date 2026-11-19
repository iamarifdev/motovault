package com.motovault.app.domain.capture

/** Pre-normalizes printed/handwritten service cash-memo OCR lines before item & price parsing. */
object ServiceBillOcrParser {
    private val leadingIndexRegex = Regex("^\\s*[0-9০-৯]+[.)]\\s*")
    private val dotLeadersRegex = Regex("[.\\-_·]{2,}")
    private val slashDashPriceRegex = Regex("([0-9০-৯]+(?:,[0-9০-৯]{2,3})*(?:\\.[0-9০-৯]+)?)\\s*/-\\.?")
    private val currencyRegex = Regex("৳|(?<![a-z])(?:bdt|tk|taka)(?![a-z])|টাকা", RegexOption.IGNORE_CASE)
    private val trailingAmountRegex = Regex("^(.*\\S)\\s+([0-9০-৯]+(?:,[0-9০-৯]{2,3})*(?:\\.[0-9০-৯]+)?)\\s*$")
    private val memoHeaderRegex = Regex("ক্যাশ\\s*মেমো|মেমো|রশিদ|cash\\s*memo|invoice|receipt|\\bbill\\b", RegexOption.IGNORE_CASE)

    fun parse(ocrText: String): CapturedService {
        if (ocrText.isBlank()) return CapturedService(emptyList(), emptyList())
        val normalizedLines = ocrText.lines()
            .map { raw -> normalizeLine(raw) }
            .filter { it.isNotEmpty() }
        return CaptureServiceParser.parse(normalizedLines.joinToString("\n"))
    }

    private fun normalizeLine(raw: String): String {
        var line = raw.trim()
        if (line.isEmpty()) return ""
        line = leadingIndexRegex.replace(line, "")
        line = dotLeadersRegex.replace(line, " ")
        line = slashDashPriceRegex.replace(line) { "${it.groupValues[1]} টাকা" }
        line = line.replace(Regex("\\s+"), " ").trim()
        if (memoHeaderRegex.containsMatchIn(line) && !currencyRegex.containsMatchIn(line) && trailingAmountRegex.matchEntire(line) == null) {
            return ""
        }
        if (!currencyRegex.containsMatchIn(line)) {
            val match = trailingAmountRegex.matchEntire(line)
            if (match != null) {
                val head = match.groupValues[1]
                val num = match.groupValues[2]
                if (CaptureServiceParser.parse(head).lines.isNotEmpty()) {
                    line = "$head $num টাকা"
                }
            }
        }
        return line
    }
}
