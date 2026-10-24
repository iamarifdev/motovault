package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import kotlin.test.*

class IntervalHonestyTest {
    private val now = LocalDate.parse("2026-10-24")
    private fun rule() = ComponentRule("chain", "primary", "Chain", "Owner-defined interval", 500.0, 30,
        1000.0, now.toEpochDays().toLong() - 10, baselineCertainty = BaselineCertainty.CONFIRMED)
    private fun state() = VaultState(ride = Ride("yamaha", "Synthetic", 2023, 1200.0), rules = listOf(rule()))

    @Test fun whicheverLimitFirstIsAnIntervalAndRetainsOverdueDistance() {
        val distance = WearCalculator.wearOf(rule(), 1600.0, now.toEpochDays().toLong())
        assertEquals(WearStatus.OVERDUE, distance.status)
        assertEquals(-100.0, distance.remainingKm)
        assertEquals("km", distance.limit)
        val time = WearCalculator.wearOf(rule().copy(lastServicedEpochDays = now.toEpochDays().toLong() - 31), 1000.0, now.toEpochDays().toLong())
        assertEquals(WearStatus.OVERDUE, time.status)
        assertEquals(-1, time.remainingDays)
        assertEquals("days", time.limit)
    }

    @Test fun unknownEstimatedInvalidAndFutureBaselinesAreNeverHealthyMeasurements() {
        listOf(rule().copy(baselineCertainty = BaselineCertainty.UNKNOWN),
            rule().copy(baselineCertainty = BaselineCertainty.OWNER_ESTIMATE),
            rule().copy(lastServicedOdometerKm = 2000.0),
            rule().copy(lastServicedEpochDays = now.toEpochDays().toLong() + 1),
            rule().copy(lastServicedOdometerKm = Double.NaN)).forEach {
            assertEquals(WearStatus.UNKNOWN, WearCalculator.wearOf(it, 1200.0, now.toEpochDays().toLong()).status)
        }
        assertFails { state().saveSchedule(rule().copy(lastServicedEpochDays = -1)) }
        assertFails { state().saveSchedule(rule().copy(lastServicedEpochDays = today().toEpochDays().toLong() + 1)) }
    }

    @Test fun legacyCertaintyRequiresAMatchingLinkedServiceRecord() {
        val unknown = state().copy(rules = listOf(rule().copy(baselineCertainty = BaselineCertainty.UNKNOWN)))
        val log = RideLog("Owner label preserved", "", LocalDate.fromEpochDays(rule().lastServicedEpochDays.toInt()).toString(), 1000.0,
            kind = "service", componentId = rule().id)
        assertEquals(BaselineCertainty.UNKNOWN, unknown.confirmedRules().single().baselineCertainty)
        assertEquals(BaselineCertainty.CONFIRMED, unknown.copy(logs = listOf(log)).confirmedRules().single().baselineCertainty)
        assertEquals(BaselineCertainty.UNKNOWN, unknown.copy(logs = listOf(log.copy(odometer = 999.0))).confirmedRules().single().baselineCertainty)
    }

    @Test fun reviewedQuickServiceUsesStableScheduleIdAndEstablishesCertainty() {
        val unknown = state().copy(rules = listOf(rule().copy(baselineCertainty = BaselineCertainty.UNKNOWN)))
        val log = RideLog("Service", "", now.toString(), 1200.0, kind = "service",
            serviceLines = listOf(ServiceLineItem(catalogueId = "chain_sprocket", quantity = 1.0)))
        val next = unknown.saveQuickService(log, "chain")
        assertEquals(listOf("chain"), next.logs.single().scheduleIds)
        assertEquals(BaselineCertainty.CONFIRMED, next.rules.single().baselineCertainty)
        assertEquals(1200.0, next.rules.single().lastServicedOdometerKm)
        assertEquals(now.toString(), next.ride!!.odometerConfirmedDate)
        assertFails { next.saveQuickService(log.copy(id = "older", odometer = 1100.0), "chain") }
    }

    @Test fun estimatesNeverMutateTheConfirmedMileageAndMissingDateHasNoInventedStaleness() {
        val state = state()
        assertEquals(1200.0, WearCalculator.estimateOdometer(state.ride!!, emptyList(), now))
        val dated = state.ride!!.copy(odometerConfirmedDate = "2026-10-20")
        assertTrue(WearCalculator.estimateOdometer(dated, emptyList(), now) > dated.odometer)
        assertEquals(1200.0, dated.odometer)
        assertEquals("2026-10-20", dated.odometerConfirmedDate)
    }

    @Test fun nativeBanglaScheduleLabelsResolveWithoutRewritingOwnerText() {
        val name = "ইঞ্জিন অয়েল ও ফিল্টার"
        assertEquals("engine_oil", BangladeshServiceCatalogue.itemForScheduleName(name).id)
        assertEquals("air_filter", BangladeshServiceCatalogue.itemForScheduleName("এয়ার ফিল্টার").id)
        assertEquals(name, ComponentRule("test", "primary", name, "", 500.0, 30).name)
    }
}
