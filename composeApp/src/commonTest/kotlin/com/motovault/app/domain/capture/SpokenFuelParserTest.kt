package com.motovault.app.domain.capture

import com.motovault.app.domain.model.FuelDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpokenFuelParserTest {
    @Test
    fun parsesAmountAndOptionalFuelTypeFromBanglaAndMixedTranscripts() {
        assertEquals(
            SpokenFuelResult(500L, FuelDefaults.OCTANE),
            SpokenFuelParser.parse("পাঁচশো টাকা অকটেন")
        )
        assertEquals(
            SpokenFuelResult(500L, FuelDefaults.PETROL),
            SpokenFuelParser.parse("500 petrol")
        )
        assertEquals(
            SpokenFuelResult(500L, FuelDefaults.PETROL),
            SpokenFuelParser.parse("৫০০ টাকা পেট্রোল")
        )
        assertEquals(
            SpokenFuelResult(300L, null),
            SpokenFuelParser.parse("৩০০ টাকা")
        )
    }

    @Test
    fun rejectsEmptyZeroAndNonNumericTranscripts() {
        assertNull(SpokenFuelParser.parse("হ্যালো"))
        assertNull(SpokenFuelParser.parse("০ টাকা"))
        assertNull(SpokenFuelParser.parse(""))
    }
}
