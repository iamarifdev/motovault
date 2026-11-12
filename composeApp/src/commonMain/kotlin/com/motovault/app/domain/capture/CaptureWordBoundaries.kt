package com.motovault.app.domain.capture

/** Character categories work on both JVM and Native without regex property support. */
internal object CaptureWordBoundaries {
    private fun letterOrMark(value: Char?) = value != null && (value.isLetter() || value.category in setOf(
        CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK, CharCategory.ENCLOSING_MARK))
    fun aliasIn(text: String, alias: String): Boolean {
        var start = text.indexOf(alias)
        while (start >= 0) {
            if (!letterOrMark(text.getOrNull(start - 1)) && !letterOrMark(text.getOrNull(start + alias.length))) return true
            start = text.indexOf(alias, start + alias.length)
        }
        return false
    }
    fun tokenEnd(text: String, end: Int): Boolean = text.getOrNull(end + 1)?.let {
        !letterOrMark(it) && !it.isDigit() && it != '.' && it != ','
    } ?: true
    fun tokenStart(text: String, start: Int): Boolean = text.getOrNull(start - 1)?.let {
        !letterOrMark(it) && !it.isDigit() && it != '.' && it != ','
    } ?: true
}
