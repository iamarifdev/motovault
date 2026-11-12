package com.motovault.app.domain.input

import kotlin.test.*

class NormalizedNumbersTest {
    @Test fun acceptsSupportedDigitsAndValidInternationalOrLakhGrouping() {
        listOf("১,২৩৪.৫০", "١,٢٣٤.٥٠", "1,234.50", "١٬٢٣٤٫٥٠").forEach {
            assertEquals(1234.5, NormalizedNumbers.parse(it))
            assertEquals(123450L, NormalizedNumbers.poisha(it))
        }
        assertEquals(1234567.89, NormalizedNumbers.parse("12,34,567.89"))
        assertEquals(1234567.89, NormalizedNumbers.parse("1,234,567.89"))
    }

    @Test fun rejectsMalformedGroupingExponentsAndNonFiniteValues() {
        listOf("1,2", "12,34", "1,23,45", "1,,000", "1.2.3", "NaN", "Infinity", "1e3", "1_000", ".").forEach {
            assertNull(NormalizedNumbers.parse(it), it)
        }
        listOf("1.005", "-1", "100000000.01", "1.").forEach { assertNull(NormalizedNumbers.poisha(it), it) }
    }

    @Test fun preservesInProgressDecimalAndLetsFieldRulesRejectNegativeNumbers() {
        assertEquals(12.0, NormalizedNumbers.parse("12."))
        assertEquals(0.5, NormalizedNumbers.parse(".5"))
        assertEquals(-500.0, NormalizedNumbers.parse("-৫০০"))
        assertNull(NormalizedNumbers.poisha("-৫০০"))
    }
}
