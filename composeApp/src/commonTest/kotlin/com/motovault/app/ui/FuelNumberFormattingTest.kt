package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuelNumberFormattingTest {
    @Test
    fun numericInputUsesTheSameCanonicalValueForSupportedDigitScripts() {
        listOf("১,২৩৪.৫০", "١,٢٣٤.٥٠", "1,234.50").forEach {
            assertEquals(1234.5, parseNum(it))
        }
        assertNull(parseNum("."))
    }

    @Test
    fun displayDigitPreferenceDoesNotChangeTheNumericValue() {
        val amount = 123456.5
        assertEquals("১,২৩,৪৫৬.৫০", Fmt(Digits.BN).n(amount, 2))
        assertEquals("1,23,456.50", Fmt(Digits.EN).n(amount, 2))
        assertEquals(amount, parseNum(Fmt(Digits.BN).n(amount, 2)))
        assertEquals(amount, parseNum(Fmt(Digits.EN).n(amount, 2)))
    }

    @Test
    fun editableFuelAmountRetainsFractionAndDecimalWhileTyping() {
        val bengali = Fmt(Digits.BN)
        assertEquals("১,২৩৪.৫০", bengali.editableNumber("1234.50"))
        assertEquals("১২.", bengali.editableNumber("12."))
        assertEquals("12.50", Fmt(Digits.EN).editableNumber("১২.৫০"))
        assertEquals("০", bengali.editableNumber(""))
    }
}
