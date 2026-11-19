package com.motovault.app.domain.workshop

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.ServiceLineItem
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.model.today
import com.motovault.app.domain.vault.VaultSettings
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.storage.toBackupPreferences
import com.motovault.app.storage.toVaultSettings
import com.motovault.app.ui.create
import com.motovault.app.ui.saveQuickService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceVisitUpdateTest {
    private val currentDay = today()
    private val earlierDay = currentDay.minus(DatePeriod(days = 10))
    private val baselineDay = currentDay.minus(DatePeriod(days = 30))

    private fun baseState(): VaultState {
        val oilRule = ComponentRule(
            id = "r-oil",
            vehicleId = "primary",
            name = "Engine oil",
            specification = "10W-40",
            intervalKm = 1000.0,
            intervalDays = 60,
            lastServicedOdometerKm = 4000.0,
            lastServicedEpochDays = baselineDay.toEpochDays().toLong(),
            baselineCertainty = BaselineCertainty.CONFIRMED,
            reminder = true
        )
        return VaultState.create(Ride("yamaha", "R15", 2023, 5500.0, id = "primary"))
            .copy(rules = listOf(oilRule))
    }

    @Test
    fun editPaidVisitPreservesIdentityMetadataMiscAndAcceptsCustomAndLenientOil() {
        val initial = baseState().recordWorkshopVisit(
            log = RideLog(
                id = "visit-1",
                title = "Engine oil",
                detail = "Engine oil",
                date = earlierDay.toString(),
                odometer = 4500.0,
                kind = "service",
                cost = 975.0,
                workshop = "Dhaka Garage",
                receiptBase64 = "synthetic-receipt",
                serviceLabourPoisha = 10000,
                serviceMiscellaneousPoisha = 7500,
                serviceTotalPoisha = 97500,
                serviceLines = listOf(
                    ServiceLineItem(catalogueId = "engine_oil", action = "replace", unitPricePoisha = 80000)
                )
            ),
            selectedRuleId = "r-oil",
            visitState = VisitRecordState.PAID,
            caseTitle = "Engine vibration",
            warrantyMonths = 6,
            workshopPhone = "01711111111",
            saveQuickServiceFn = VaultState::saveQuickService
        )

        val oldLog = initial.logs.single()
        val updatedInput = RideLog(
            title = "Engine oil, Mirror",
            detail = "Engine oil, Mirror",
            date = earlierDay.toString(),
            odometer = 4550.0,
            kind = "service",
            cost = 1225.0,
            workshop = "Dhaka Garage",
            serviceLabourPoisha = 10000,
            serviceMiscellaneousPoisha = null,
            serviceTotalPoisha = 122500,
            serviceLines = listOf(
                ServiceLineItem(catalogueId = "engine_oil", action = "replace", unitPricePoisha = 90000, oil = null),
                ServiceLineItem(catalogueId = null, customName = "Mirror", action = "service", unitPricePoisha = 15000)
            )
        )

        val edited = initial.updateServiceVisit(
            existingId = oldLog.id,
            updatedLog = updatedInput,
            visitState = VisitRecordState.PAID,
            caseTitle = "Engine vibration",
            warrantyMonths = 6,
            workshopPhone = "01711111111",
            saveQuickServiceFn = VaultState::saveQuickService,
            today = currentDay
        )

        val saved = edited.logs.single()
        assertEquals(oldLog.id, saved.id)
        assertEquals("primary", saved.vehicleId)
        assertEquals(oldLog.workshopId, saved.workshopId)
        assertEquals(oldLog.repairCaseId, saved.repairCaseId)
        assertEquals(7500, saved.serviceMiscellaneousPoisha)
        assertEquals("synthetic-receipt", saved.receiptBase64)
        assertNull(saved.serviceLines[1].catalogueId)
        assertEquals("Mirror", saved.serviceLines[1].customName)
        assertEquals(4550.0, edited.rules.single().lastServicedOdometerKm)
        assertEquals(900.0, edited.lastItemPrices["engine_oil"])

        val decoded = VaultState.decode(VaultState.encode(edited))
        assertNotNull(decoded)
        assertEquals(saved.serviceLines, decoded.logs.single().serviceLines)
        assertEquals(7500, decoded.logs.single().serviceMiscellaneousPoisha)
    }

    @Test
    fun quoteToPaidTransitionAndSupersededBaselineEditsPreserveTimelineConsistency() {
        val quote = RideLog(
            id = "visit-q",
            title = "Engine oil",
            detail = "Engine oil",
            date = earlierDay.toString(),
            odometer = 4600.0,
            kind = "service",
            cost = 900.0,
            workshop = "Mirpur Auto",
            serviceLines = listOf(ServiceLineItem(catalogueId = "engine_oil", action = "replace", unitPricePoisha = 90000))
        )
        val withQuote = baseState().recordWorkshopVisit(
            log = quote,
            selectedRuleId = "r-oil",
            visitState = VisitRecordState.QUOTATION,
            saveQuickServiceFn = VaultState::saveQuickService
        )
        assertEquals(4000.0, withQuote.rules.single().lastServicedOdometerKm)

        val paid = withQuote.updateServiceVisit(
            existingId = "visit-q",
            updatedLog = quote,
            visitState = VisitRecordState.PAID,
            warrantyMonths = 12,
            saveQuickServiceFn = VaultState::saveQuickService,
            today = currentDay
        )
        assertEquals(VisitRecordState.PAID.code, paid.logs.single().visitState)
        assertEquals(4600.0, paid.rules.single().lastServicedOdometerKm)
        assertEquals(1, paid.warranties.size)
        assertEquals(paid.warranties.map { it.id }, paid.logs.single().warrantyIds)

        val newerLog = RideLog(
            id = "visit-newer",
            title = "Engine oil",
            detail = "Engine oil",
            date = currentDay.toString(),
            odometer = 5200.0,
            kind = "service",
            cost = 950.0,
            serviceLines = listOf(ServiceLineItem(catalogueId = "engine_oil", action = "replace", unitPricePoisha = 95000))
        )
        val withTwoVisits = paid.recordWorkshopVisit(
            log = newerLog,
            selectedRuleId = "r-oil",
            visitState = VisitRecordState.PAID,
            saveQuickServiceFn = VaultState::saveQuickService
        )
        assertEquals(5200.0, withTwoVisits.rules.single().lastServicedOdometerKm)

        val editedOlder = withTwoVisits.updateServiceVisit(
            existingId = "visit-q",
            updatedLog = quote.copy(odometer = 4650.0, cost = 920.0),
            visitState = VisitRecordState.PAID,
            warrantyMonths = 12,
            saveQuickServiceFn = VaultState::saveQuickService,
            today = currentDay
        )
        assertEquals(5200.0, editedOlder.rules.single().lastServicedOdometerKm)
        assertTrue(editedOlder.logs.any { it.id == "visit-q" && it.odometer == 4650.0 })

        val backupRestored = VaultSettings.from(editedOlder).toBackupPreferences().toVaultSettings().applyTo(VaultState())
        assertTrue(backupRestored.logs.isEmpty()) // Settings adapter round-trips contacts safely
        assertEquals(editedOlder.workshopContacts.size, backupRestored.workshopContacts.size)
    }
}
