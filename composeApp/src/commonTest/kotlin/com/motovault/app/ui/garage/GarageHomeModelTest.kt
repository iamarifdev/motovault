package com.motovault.app.ui.garage

import com.motovault.app.domain.garage.addBike
import com.motovault.app.domain.garage.carouselBikes
import com.motovault.app.domain.garage.setBikeArchived
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.ui.FuelNoteState
import com.motovault.app.ui.GarageHealth
import com.motovault.app.ui.GarageHomeSnapshot
import com.motovault.app.ui.GarageJob
import com.motovault.app.ui.GarageJobFilter
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.WearStatus
import com.motovault.app.ui.buildGarageHomeSnapshot
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GarageHomeModelTest {
    private val today = LocalDate(2026, 11, 19)
    private val todayEpoch = today.toEpochDays().toLong()

    private fun rule(
        id: String,
        name: String,
        intervalKm: Double = 3000.0,
        lastKm: Double = 4000.0,
        lastEpoch: Long = todayEpoch - 5,
        certainty: BaselineCertainty = BaselineCertainty.CONFIRMED
    ) = ComponentRule(
        id = id,
        vehicleId = "primary",
        name = name,
        specification = "test",
        intervalKm = intervalKm,
        intervalDays = 180,
        lastServicedOdometerKm = lastKm,
        lastServicedEpochDays = lastEpoch,
        baselineCertainty = certainty
    )

    private fun doc(id: String, title: String, expiryDaysOffset: Int, warnDays: Int? = null) = VaultDocument(
        id = id,
        title = title,
        number = "",
        holder = "",
        expiry = LocalDate.fromEpochDays(today.toEpochDays() + expiryDaysOffset).toString(),
        reminderWarningDays = warnDays
    )

    @Test
    fun healthPillDistinguishesOkSoonAndOverdue() {
        val base = VaultState(
            ride = Ride("yamaha", "R15 V4", 2024, 5000.0, odometerConfirmedDate = today.toString()),
            rules = listOf(rule("r1", "Engine oil", intervalKm = 3000.0, lastKm = 4500.0))
        )
        val okSnap = assertNotNull(buildGarageHomeSnapshot(base, today))
        assertEquals(GarageHealth.OK, okSnap.health)
        assertEquals(0, okSnap.dueCount)
        assertEquals(GarageJobFilter.ALL, okSnap.effectiveFilter(GarageJobFilter.DUE))

        val unknownState = base.copy(rules = listOf(rule("r1", "Engine oil", certainty = BaselineCertainty.UNKNOWN)))
        val unknownSnap = assertNotNull(buildGarageHomeSnapshot(unknownState, today))
        assertEquals(GarageHealth.SOON, unknownSnap.health)
        assertEquals(1, unknownSnap.dueCount)
        assertEquals(1f, (unknownSnap.dueJobs.single() as GarageJob.WearJob).barProgress)

        val soonDocState = base.copy(documents = listOf(doc("d1", "Tax token", expiryDaysOffset = 10)))
        val soonDocSnap = assertNotNull(buildGarageHomeSnapshot(soonDocState, today))
        assertEquals(GarageHealth.SOON, soonDocSnap.health)
        assertTrue(soonDocSnap.docWarningDot)

        val expiredDocState = base.copy(documents = listOf(doc("d1", "Tax token", expiryDaysOffset = -2)))
        assertEquals(GarageHealth.OVERDUE, buildGarageHomeSnapshot(expiredDocState, today)!!.health)

        val overdueWearState = base.copy(rules = listOf(rule("r2", "Chain", intervalKm = 500.0, lastKm = 4400.0)))
        assertEquals(GarageHealth.OVERDUE, buildGarageHomeSnapshot(overdueWearState, today)!!.health)
    }

    @Test
    fun mergedJobsOrderMatchesOverdueWearsThenRenewalsThenUnknownSoonThenOk() {
        val rules = listOf(
            rule("ok", "Spark plug", intervalKm = 6000.0, lastKm = 4900.0),
            rule("soon", "Air filter", intervalKm = 1000.0, lastKm = 4200.0),
            rule("unk", "Brake pads", certainty = BaselineCertainty.UNKNOWN),
            rule("over", "Chain", intervalKm = 500.0, lastKm = 4400.0),
            rule("ok2", "Coolant", intervalKm = 10000.0, lastKm = 4500.0),
            rule("ok3", "Brake fluid", intervalKm = 10000.0, lastKm = 4500.0)
        )
        val docs = listOf(
            doc("d2", "Fitness certificate", expiryDaysOffset = 15),
            doc("d1", "Tax token", expiryDaysOffset = -3)
        )
        val state = VaultState(
            ride = Ride("ktm", "Duke 125", 2023, 5000.0, odometerConfirmedDate = today.toString()),
            rules = rules,
            documents = docs
        )
        val snap = assertNotNull(buildGarageHomeSnapshot(state, today))
        val dueKeys = snap.dueJobs.map {
            when (it) {
                is GarageJob.WearJob -> "w:${it.wear.rule.id}"
                is GarageJob.DocJob -> "d:${it.doc.id}"
            }
        }
        assertEquals(listOf("w:over", "d:d1", "d:d2", "w:unk", "w:soon"), dueKeys)
        assertEquals(8, snap.allJobs.size)
        assertEquals(5, snap.visibleJobs(GarageJobFilter.ALL, showAll = false).size)
        assertEquals(3, snap.hiddenExtraCount(GarageJobFilter.ALL, showAll = false))
        assertEquals(8, snap.visibleJobs(GarageJobFilter.ALL, showAll = true).size)
        assertEquals(0, snap.hiddenExtraCount(GarageJobFilter.ALL, showAll = true))
        val okProgress = (snap.allJobs.last() as GarageJob.WearJob).barProgress
        assertTrue(okProgress >= 0.08f && okProgress <= 1f)
        assertEquals("over", snap.nextJob?.rule?.id)
    }

    @Test
    fun fuelNoteAndTenDayWarningDotTrackLastFuelLog() {
        val emptyState = VaultState(ride = Ride("yamaha", "R15", 2024, 2000.0))
        val emptySnap = assertNotNull(buildGarageHomeSnapshot(emptyState, today))
        assertEquals(FuelNoteState.Empty, emptySnap.fuelNote)
        assertFalse(emptySnap.fuelWarningDot)

        val recentDate = LocalDate.fromEpochDays(today.toEpochDays() - 4)
        val recentState = emptyState.copy(
            logs = listOf(RideLog("Fuel fill-up", "5 L", recentDate.toString(), 2000.0, kind = "fuel", liters = 5.0))
        )
        val recentSnap = assertNotNull(buildGarageHomeSnapshot(recentState, today))
        assertEquals(FuelNoteState.LastFuelDate(recentDate), recentSnap.fuelNote)
        assertFalse(recentSnap.fuelWarningDot)

        val staleDate = LocalDate.fromEpochDays(today.toEpochDays() - 10)
        val staleState = emptyState.copy(
            logs = listOf(RideLog("Fuel fill-up", "5 L", staleDate.toString(), 2000.0, kind = "fuel", liters = 5.0))
        )
        val staleSnap = assertNotNull(buildGarageHomeSnapshot(staleState, today))
        assertEquals(FuelNoteState.DaysWithoutFuel(10), staleSnap.fuelNote)
        assertTrue(staleSnap.fuelWarningDot)
    }

    @Test
    fun carouselBikesKeepStableOrderAcrossSwitchesAndExcludeArchived() {
        val r1 = Ride("yamaha", "R15", 2024, 1000.0, id = "primary")
        val r2 = Ride("ktm", "Duke", 2023, 2000.0, id = "bike-b")
        val r3 = Ride("honda", "Hornet", 2025, 500.0, id = "bike-a")
        var state = VaultState(ride = r1).addBike(r2, emptyList()).addBike(r3, emptyList())
        assertEquals(listOf("primary", "bike-a", "bike-b"), state.carouselBikes().map { it.id })

        state = state.switchToBike("bike-b")
        assertEquals("bike-b", state.ride?.id)
        assertEquals(listOf("primary", "bike-a", "bike-b"), state.carouselBikes().map { it.id })

        state = state.setBikeArchived("bike-a", archived = true)
        assertEquals(listOf("primary", "bike-b"), state.carouselBikes().map { it.id })
    }

    @Test
    fun saveScheduleResetAndEditUpdateBaselineWithoutCreatingHistoryLogs() {
        val initialRule = rule("r1", "Engine oil", intervalKm = 3000.0, lastKm = 1000.0, certainty = BaselineCertainty.UNKNOWN)
        val initial = VaultState(ride = Ride("yamaha", "R15", 2024, 5200.0), rules = listOf(initialRule))

        val resetNext = initial.saveSchedule(
            initialRule.copy(
                lastServicedOdometerKm = 5200.0,
                lastServicedEpochDays = todayEpoch,
                baselineCertainty = BaselineCertainty.CONFIRMED
            )
        )
        assertTrue(resetNext.logs.isEmpty())
        assertEquals(BaselineCertainty.CONFIRMED, resetNext.rules.single().baselineCertainty)
        assertEquals(5200.0, resetNext.rules.single().lastServicedOdometerKm)
        assertEquals(todayEpoch, resetNext.rules.single().lastServicedEpochDays)

        val undoBaselineCheck = buildGarageHomeSnapshot(resetNext, today)!!
        assertEquals(WearStatus.OK, undoBaselineCheck.wears.single().status)
        assertEquals(GarageHealth.OK, undoBaselineCheck.health)

        val edited = initial.saveSchedule(
            initialRule.copy(
                lastServicedOdometerKm = 3500.0,
                lastServicedEpochDays = todayEpoch - 20,
                baselineCertainty = BaselineCertainty.CONFIRMED
            )
        )
        assertTrue(edited.logs.isEmpty())
        assertEquals(3500.0, edited.rules.single().lastServicedOdometerKm)
        assertEquals(todayEpoch - 20, edited.rules.single().lastServicedEpochDays)
    }
}
