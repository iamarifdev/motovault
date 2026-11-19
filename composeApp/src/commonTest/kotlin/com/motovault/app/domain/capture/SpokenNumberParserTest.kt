package com.motovault.app.domain.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpokenNumberParserTest {
    @Test fun parsesBanglaSpokenWordsAndScales() {
        assertEquals(12450L, SpokenNumberParser.parse("বারো হাজার চারশো পঞ্চাশ"))
        assertEquals(12350L, SpokenNumberParser.parse("১২ হাজার ৩৫০"))
        assertEquals(500L, SpokenNumberParser.parse("পাঁচশো টাকা"))
        assertEquals(105000L, SpokenNumberParser.parse("এক লাখ পাঁচ হাজার কিমি"))
        assertEquals(4280L, SpokenNumberParser.parse("চার হাজার দুইশো আশি"))
    }

    @Test fun parsesEnglishAndMixedNumbersAndReturnsNullWhenAbsent() {
        assertEquals(12450L, SpokenNumberParser.parse("twelve thousand four hundred fifty"))
        assertEquals(4280L, SpokenNumberParser.parse("৪২৮০ কিমি"))
        assertEquals(3000L, SpokenNumberParser.parse("৩ হাজারটা"))
        assertNull(SpokenNumberParser.parse("hello motovault"))
        assertNull(SpokenNumberParser.parse("কোনো সংখ্যা নেই"))
    }
}
