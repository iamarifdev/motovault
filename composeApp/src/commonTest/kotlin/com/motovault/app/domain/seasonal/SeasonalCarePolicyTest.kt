package com.motovault.app.domain.seasonal

import com.motovault.app.domain.model.GeoCapture
import com.motovault.app.domain.model.RidingSeason
import com.motovault.app.domain.model.WeatherCacheSnapshot
import com.motovault.app.domain.model.WeatherCacheState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonalCarePolicyTest {

    @Test
    fun resolvesBangladeshSeasonsAcrossAllMonths() {
        for (month in 6..10) assertEquals(RidingSeason.MONSOON, SeasonalCarePolicy.seasonForMonth(month))
        for (month in 3..5) assertEquals(RidingSeason.HEAT, SeasonalCarePolicy.seasonForMonth(month))
        listOf(11, 12, 1, 2).forEach { month ->
            assertEquals(RidingSeason.WINTER_DUST, SeasonalCarePolicy.seasonForMonth(month))
        }
    }

    @Test
    fun togglesChecklistItemsAndDismissedSeasonsByBikeAndYear() {
        assertEquals(3, SeasonalCarePolicy.checklistFor(RidingSeason.MONSOON).size)
        assertEquals(3, SeasonalCarePolicy.checklistFor(RidingSeason.WINTER_DUST).size)
        assertEquals(3, SeasonalCarePolicy.checklistFor(RidingSeason.HEAT).size)

        val checked = SeasonalCarePolicy.toggleChecklistItem(emptySet(), "monsoon_chain")
        assertTrue("monsoon_chain" in checked)
        assertTrue("monsoon_chain" !in SeasonalCarePolicy.toggleChecklistItem(checked, "monsoon_chain"))

        val dismissed = SeasonalCarePolicy.toggleDismissedSeason(emptySet(), RidingSeason.MONSOON, 2026, "bike-1")
        assertTrue(SeasonalCarePolicy.isDismissed(dismissed, RidingSeason.MONSOON, 2026, "bike-1"))
        assertFalse(SeasonalCarePolicy.isDismissed(dismissed, RidingSeason.MONSOON, 2026, "bike-2"))
        assertFalse(SeasonalCarePolicy.isDismissed(dismissed, RidingSeason.HEAT, 2026, "bike-1"))

        val reenabled = SeasonalCarePolicy.toggleDismissedSeason(dismissed, RidingSeason.MONSOON, 2026, "bike-1")
        assertFalse(SeasonalCarePolicy.isDismissed(reenabled, RidingSeason.MONSOON, 2026, "bike-1"))
    }

    @Test
    fun evaluatesWeatherCacheOfflineFreshAndExpiredStates() {
        assertEquals(WeatherCacheState.OFFLINE_ONLY, WeatherCacheSnapshot().stateAt(1_000_000L))
        val cached = WeatherCacheSnapshot(regionCode = "BD-DAC", summaryCode = "rain", updatedAtEpochMillis = 10_000L)
        assertEquals(WeatherCacheState.FRESH, cached.stateAt(10_000L + 5L * 3_600_000L))
        assertEquals(WeatherCacheState.EXPIRED, cached.stateAt(10_000L + 7L * 3_600_000L))
    }

    @Test
    fun formatsBreakdownMessageWithAndWithoutOptionalGps() {
        val offlineMsg = SeasonalCarePolicy.formatBreakdownShareText("R15 V4", 12340.0, "Chain snapped near Banani", null)
        assertTrue("R15 V4 (12340 km)" in offlineMsg)
        assertTrue("Chain snapped near Banani" in offlineMsg)
        assertFalse("GPS:" in offlineMsg)

        val geo = GeoCapture(23.7937, 90.4066, 12.4, 50_000L, "gps")
        val gpsMsg = SeasonalCarePolicy.formatBreakdownShareText("R15 V4", 12340.0, "Flat rear tire", geo)
        assertTrue("GPS: 23.7937, 90.4066 (±12m)" in gpsMsg)
    }
}
