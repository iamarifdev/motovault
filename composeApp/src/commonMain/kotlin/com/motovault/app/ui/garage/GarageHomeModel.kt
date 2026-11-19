package com.motovault.app.ui

import com.motovault.app.domain.model.VaultDocument
import kotlinx.datetime.LocalDate

enum class GarageHealth { OK, SOON, OVERDUE }

enum class GarageJobFilter { DUE, ALL }

sealed interface GarageJob {
    data class WearJob(val wear: Wear, val barProgress: Float) : GarageJob
    data class DocJob(val doc: VaultDocument, val daysLeft: Int) : GarageJob
}

sealed interface FuelNoteState {
    data class DaysWithoutFuel(val days: Int) : FuelNoteState
    data class LastFuelDate(val date: LocalDate) : FuelNoteState
    data object Empty : FuelNoteState
}

data class GarageHomeSnapshot(
    val wears: List<Wear>,
    val dueWears: List<Wear>,
    val renewals: List<GarageJob.DocJob>,
    val health: GarageHealth,
    val dueCount: Int,
    val dueJobs: List<GarageJob>,
    val allJobs: List<GarageJob>,
    val nextJob: Wear?,
    val economy: Double?,
    val estimate: Double,
    val fuelNote: FuelNoteState,
    val fuelWarningDot: Boolean,
    val docWarningDot: Boolean
) {
    fun effectiveFilter(requested: GarageJobFilter): GarageJobFilter =
        if (dueJobs.isEmpty()) GarageJobFilter.ALL else requested

    fun visibleJobs(requested: GarageJobFilter, showAll: Boolean): List<GarageJob> {
        val activeFilter = effectiveFilter(requested)
        val source = if (activeFilter == GarageJobFilter.DUE) dueJobs else allJobs
        return if (activeFilter == GarageJobFilter.ALL && !showAll) source.take(5) else source
    }

    fun hiddenExtraCount(requested: GarageJobFilter, showAll: Boolean): Int {
        val activeFilter = effectiveFilter(requested)
        return if (activeFilter == GarageJobFilter.ALL && !showAll && allJobs.size > 5) allJobs.size - 5 else 0
    }
}

fun buildGarageHomeSnapshot(state: VaultState, today: LocalDate): GarageHomeSnapshot? {
    val ride = state.ride ?: return null
    val wears = WearCalculator.byUrgency(state.confirmedRules(), ride.odometer, today.toEpochDays().toLong())
    val economy = WearCalculator.economyOf(state.logs)
    val estimate = WearCalculator.estimateOdometer(ride, state.logs, today)

    val lastFuel = state.logs.filter { it.isFuel }.sortedBy { it.odometer }.lastOrNull()
    val lastFuelDate = lastFuel?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() }
    val sinceFuelDays = lastFuelDate?.let { (today.toEpochDays() - it.toEpochDays()).toInt() }
    val fuelWarningDot = sinceFuelDays != null && sinceFuelDays >= 10
    val fuelNote = when {
        sinceFuelDays != null && sinceFuelDays >= 10 -> FuelNoteState.DaysWithoutFuel(sinceFuelDays)
        lastFuelDate != null -> FuelNoteState.LastFuelDate(lastFuelDate)
        else -> FuelNoteState.Empty
    }

    val defaultLead = state.reminderPolicy.warningDays
    val renewals = state.documents.mapNotNull { doc ->
        val expiry = doc.expiry.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return@mapNotNull null
        val left = (expiry.toEpochDays() - today.toEpochDays()).toInt()
        val lead = doc.reminderWarningDays ?: defaultLead
        if (left <= lead) GarageJob.DocJob(doc, left) else null
    }.sortedBy { it.daysLeft }

    val dueWears = wears.filter { it.status != WearStatus.OK }
    val overdueCount = wears.count { it.status == WearStatus.OVERDUE }
    val health = when {
        overdueCount > 0 || renewals.any { it.daysLeft < 0 } -> GarageHealth.OVERDUE
        dueWears.isNotEmpty() || renewals.isNotEmpty() -> GarageHealth.SOON
        else -> GarageHealth.OK
    }

    val toWearJob: (Wear) -> GarageJob.WearJob = { wear ->
        val progress = if (wear.status == WearStatus.UNKNOWN) 1f else wear.used.toFloat().coerceIn(0.08f, 1f)
        GarageJob.WearJob(wear, progress)
    }
    val dueJobs: List<GarageJob> = buildList {
        dueWears.filter { it.status == WearStatus.OVERDUE }.forEach { add(toWearJob(it)) }
        renewals.forEach { add(it) }
        dueWears.filter { it.status != WearStatus.OVERDUE }.forEach { add(toWearJob(it)) }
    }
    val allJobs: List<GarageJob> = dueJobs + wears.filter { it.status == WearStatus.OK }.map(toWearJob)

    return GarageHomeSnapshot(
        wears = wears,
        dueWears = dueWears,
        renewals = renewals,
        health = health,
        dueCount = dueWears.size + renewals.size,
        dueJobs = dueJobs,
        allJobs = allJobs,
        nextJob = dueWears.firstOrNull() ?: wears.firstOrNull(),
        economy = economy,
        estimate = estimate,
        fuelNote = fuelNote,
        fuelWarningDot = fuelWarningDot,
        docWarningDot = renewals.isNotEmpty()
    )
}
