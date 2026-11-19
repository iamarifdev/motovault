package com.motovault.app.domain.capture

import com.motovault.app.domain.model.FuelDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PumpMeterOcrParserTest {
    @Test
    fun parsesLcdDispenseDisplayAndDerivesUnitPrice() {
        val reading = PumpMeterOcrParser.parse("TK 500.00\n3.85 L OCT")
        assertNotNull(reading)
        assertEquals(500.0, reading.amountTaka)
        assertEquals(3.85, reading.litres)
        assertEquals(129.87, reading.pricePerLitre)
        assertEquals(FuelDefaults.OCTANE, reading.fuelType)
    }

    @Test
    fun derivesLitresFromRateAndAmountAndHandlesBanglaText() {
        val rated = PumpMeterOcrParser.parse("RATE 130\nAMOUNT 650\nPETROL")
        assertNotNull(rated)
        assertEquals(650.0, rated.amountTaka)
        assertEquals(5.0, rated.litres)
        assertEquals(130.0, rated.pricePerLitre)
        assertEquals(FuelDefaults.PETROL, rated.fuelType)

        val bangla = PumpMeterOcrParser.parse("৫০০ টাকা\n৩.৮৫ লিটার অকটেন")
        assertNotNull(bangla)
        assertEquals(500.0, bangla.amountTaka)
        assertEquals(3.85, bangla.litres)
        assertEquals(FuelDefaults.OCTANE, bangla.fuelType)
    }

    @Test
    fun returnsNullForBlankOrUnrecognizedText() {
        assertNull(PumpMeterOcrParser.parse(""))
        assertNull(PumpMeterOcrParser.parse("WELCOME TO PUMP STATION"))
    }
}
