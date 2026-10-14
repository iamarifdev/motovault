package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationCaptureTest {
    private val now = 1_800_000_000_000L

    @Test
    fun acceptsCoordinatesAtGeographicBoundsAndFreshAccurateFixes() {
        assertTrue(capture(latitude = -90.0, longitude = -180.0).isValid())
        assertTrue(capture(latitude = 90.0, longitude = 180.0).isValid())
        assertTrue(capture().isFresh(now))
    }

    @Test
    fun rejectsInvalidCoordinatesAccuracyTimestampAndSource() {
        assertFalse(capture(latitude = 90.01).isValid())
        assertFalse(capture(longitude = -180.01).isValid())
        assertFalse(capture(accuracy = -1.0).isValid())
        assertFalse(capture(accuracy = Double.NaN).isValid())
        assertFalse(capture(timestamp = 0).isValid())
        assertFalse(capture(source = " ").isValid())
        assertFalse(capture(latitude = Double.POSITIVE_INFINITY).isValid())
    }

    @Test
    fun rejectsStaleAndImplausiblyFutureFixes() {
        assertFalse(capture(timestamp = now - 120_001).isFresh(now))
        assertTrue(capture(timestamp = now - 120_000).isFresh(now))
        assertTrue(capture(timestamp = now + 30_000).isFresh(now))
        assertFalse(capture(timestamp = now + 30_001).isFresh(now))
    }

    private fun capture(
        latitude: Double = 23.8103,
        longitude: Double = 90.4125,
        accuracy: Double = 5.0,
        timestamp: Long = now,
        source: String = "simulated_foreground_gps"
    ) = GeoCapture(latitude, longitude, accuracy, timestamp, source)
}
