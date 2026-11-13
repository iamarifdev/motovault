package com.motovault.app.domain.workshop

import com.motovault.app.domain.garage.addBike
import com.motovault.app.domain.garage.setBikeArchived
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.domain.ledger.CostCategory
import com.motovault.app.domain.ledger.CostLedger
import com.motovault.app.domain.ledger.CostPeriod
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.IssueResolutionState
import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.RepairCase
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.ServiceLineItem
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.model.WorkshopContact
import com.motovault.app.domain.model.today
import com.motovault.app.domain.vault.VaultSettings
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.storage.toBackupPreferences
import com.motovault.app.storage.toVaultSettings
import com.motovault.app.ui.create
import com.motovault.app.ui.reminderPlan
import com.motovault.app.ui.reminderTarget
import com.motovault.app.ui.saveQuickService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkshopDomainTest {

    private fun baseState(): VaultState {
        val oilRule = ComponentRule(
            id = "r-oil",
            vehicleId = "primary",
            name = "Engine oil",
            specification = "10W-40",
            intervalKm = 1000.0,
            intervalDays = 60,
            lastServicedOdometerKm = 4000.0,
            lastServicedEpochDays = 19000L,
            baselineCertainty = BaselineCertainty.CONFIRMED,
            reminder = true
        )
        return VaultState.create(Ride("yamaha", "R15", 2023, 5000.0, id = "primary"))
            .copy(rules = listOf(oilRule))
    }

    private fun serviceLog(
        id: String,
        date: String,
        odometer: Double,
        workshop: String = "Uttara Motors",
        cost: Double = 950.0,
        catalogueId: String = "engine_oil"
    ): RideLog = RideLog(
        id = id,
        title = "Service",
        detail = "Engine oil",
        date = date,
        odometer = odometer,
        kind = "service",
        cost = cost,
        workshop = workshop,
        serviceLines = listOf(
            ServiceLineItem(
                catalogueId = catalogueId,
                customName = "Engine oil",
                action = "replace",
                unitPricePoisha = (cost * 100).toInt()
            )
        )
    )

    @Test
    fun repeatedRepairsLinkToSingleCaseAndStayBikeScoped() {
        val d1 = today().minus(DatePeriod(days = 10)).toString()
        val d2 = today().toString()
        val s1 = baseState().recordWorkshopVisit(
            log = serviceLog("v1", d1, 4500.0, workshop = "Mirpur Garage", cost = 500.0, catalogueId = "fork-seal"),
            selectedRuleId = null,
            caseTitle = "Front fork oil leak",
            workshopPhone = "01711000001",
            saveQuickServiceFn = VaultState::saveQuickService
        )
        val s2 = s1.recordWorkshopVisit(
            log = serviceLog("v2", d2, 4800.0, workshop = "Mirpur Garage", cost = 700.0, catalogueId = "fork-seal"),
            selectedRuleId = null,
            caseTitle = "front fork oil leak",
            warrantyMonths = 6,
            warrantyKm = 3000.0,
            saveQuickServiceFn = VaultState::saveQuickService
        )

        assertEquals(1, s2.repairCases.size)
        assertEquals(listOf("v1", "v2"), s2.repairCases.single().visitLogIds)
        assertEquals(1, s2.warranties.size)
        assertEquals("01711000001", s2.workshopContacts.single().phone)

        val withBike2 = s2.addBike(Ride("honda", "Hornet", 2024, 1200.0, id = "bike-2"), emptyList())
        assertTrue(withBike2.repairCases.isEmpty())
        assertTrue(withBike2.warranties.isEmpty())
        assertEquals(1, withBike2.workshopContacts.size)

        val backToPrimary = withBike2.switchToBike("primary")
        assertEquals(1, backToPrimary.repairCases.size)
        assertEquals(1, backToPrimary.warranties.size)
    }

    @Test
    fun warrantyExpiresByDateOrMileageLimit() {
        val warranty = PartWarranty(
            id = "w1",
            visitLogId = "v1",
            partName = "Chain sprocket",
            startDate = "2026-01-01",
            expiryDate = "2026-07-01",
            mileageLimitKm = 5000.0,
            startOdometerKm = 10000.0
        )
        assertFalse(warranty.isExpired(LocalDate(2026, 6, 30), 14999.0))
        assertTrue(warranty.isExpired(LocalDate(2026, 7, 2), 12000.0))
        assertTrue(warranty.isExpired(LocalDate(2026, 5, 1), 15000.0))
    }

    @Test
    fun duplicateWorkshopNamesAndLegacyFreeTextMigration() {
        val legacyState = baseState().copy(
            workshops = listOf("Desh Motors"),
            logs = listOf(serviceLog("v-legacy", "2026-03-01", 4100.0, workshop = "Badda Pitstop"))
        )
        val migrated = legacyState.migratedWorkshopContacts()
        assertTrue(migrated.any { it.name == "Desh Motors" })
        assertTrue(migrated.any { it.name == "Badda Pitstop" })

        val (afterFirst, c1) = legacyState.upsertWorkshopContact("Royal Auto", "01710000001")
        val (afterSecond, c2) = afterFirst.upsertWorkshopContact("Royal Auto", "01810000002")
        assertNotNull(c1)
        assertNotNull(c2)
        assertNotEquals(c1.id, c2.id)
        assertEquals(2, afterSecond.workshopContacts.count { it.name == "Royal Auto" })
    }

    @Test
    fun quotationDoesNotUpdateScheduleUntilMarkedPaid() {
        val date = today().toString()
        val quoted = baseState().recordWorkshopVisit(
            log = serviceLog("v-quote", date, 5200.0),
            selectedRuleId = "r-oil",
            visitState = VisitRecordState.QUOTATION,
            caseTitle = "Clutch overhaul",
            saveQuickServiceFn = VaultState::saveQuickService
        )
        assertEquals("quote", quoted.logs.single().visitState)
        assertEquals(4000.0, quoted.rules.single().lastServicedOdometerKm)
        assertFalse(quoted.confirmedRules().isEmpty())

        val cancelled = quoted.transitionVisitState("v-quote", VisitRecordState.CANCELLED, VaultState::saveQuickService)
        assertEquals("cancelled", cancelled.logs.single().visitState)
        assertEquals(4000.0, cancelled.rules.single().lastServicedOdometerKm)

        val approved = quoted.transitionVisitState("v-quote", VisitRecordState.APPROVED, VaultState::saveQuickService)
        assertEquals("approved", approved.logs.single().visitState)
        assertEquals(4000.0, approved.rules.single().lastServicedOdometerKm)

        val paid = approved.transitionVisitState("v-quote", VisitRecordState.PAID, VaultState::saveQuickService)
        assertEquals("paid", paid.logs.single().visitState)
        assertEquals(5200.0, paid.rules.single().lastServicedOdometerKm)
    }

    @Test
    fun costLedgerExcludesNonPaidVisitsFromTotalsAndCoverage() {
        val d1 = today().minus(DatePeriod(days = 2)).toString()
        val d2 = today().toString()
        val logs = listOf(
            serviceLog("paid-1", d1, 5000.0, cost = 1000.0).copy(visitState = VisitRecordState.PAID.code),
            serviceLog("quote-1", d2, 5400.0, cost = 2500.0).copy(visitState = VisitRecordState.QUOTATION.code),
            serviceLog("cancel-1", d2, 5500.0, cost = 900.0).copy(visitState = VisitRecordState.CANCELLED.code)
        )
        val summary = CostLedger.summarize(logs, today(), CostPeriod.RECENT, CostCategory.SERVICE)
        assertEquals(1, summary.logs.size)
        assertEquals(100_000L, summary.totalPoisha)
    }

    @Test
    fun warrantyRemindersScheduleAndSuppressWhenBikeArchived() {
        val expiry = today().plus(DatePeriod(days = 20)).toString()
        val stateWithWarranty = baseState()
            .recordWorkshopVisit(
                log = serviceLog("vw", today().toString(), 5050.0, workshop = "Bashundhara Auto", cost = 800.0),
                selectedRuleId = null,
                warrantyMonths = 6,
                workshopPhone = "01799000001",
                saveQuickServiceFn = VaultState::saveQuickService
            )
            .let { s ->
                val adjusted = s.warranties.map { it.copy(id = "w-rem", expiryDate = expiry, reminder = true) }
                s.copy(warranties = adjusted)
            }
        val now = LocalDateTime(today().year, today().monthNumber, today().dayOfMonth, 8, 0)
        val plans = reminderPlan(stateWithWarranty, now, emptyList(), emptySet())
        val warrantyPlan = plans.firstOrNull { it.targetType == "warranty" && it.documentId == "w-rem" }
        assertNotNull(warrantyPlan)
        val target = reminderTarget(stateWithWarranty, warrantyPlan)
        assertNotNull(target)
        assertEquals("primary", target.vehicleId)

        val withBike2 = stateWithWarranty.addBike(Ride("honda", "CB", 2024, 1000.0, id = "bike-2"), emptyList())
        val archivedPrimary = withBike2.setBikeArchived("primary", archived = true)
        assertEquals(null, reminderTarget(archivedPrimary, warrantyPlan))
    }

    @Test
    fun backupPreferencesRoundTripPreservesWorkshopContactsRepairCasesAndWarranties() {
        val state = baseState()
            .recordWorkshopVisit(
                log = serviceLog("v-bk", today().toString(), 5100.0, workshop = "Tejgaon Hub", cost = 1100.0),
                selectedRuleId = null,
                caseTitle = "Carburetor tuning",
                warrantyMonths = 12,
                workshopPhone = "01888000001",
                saveQuickServiceFn = VaultState::saveQuickService
            )
            .addBike(Ride("honda", "Hornet", 2024, 2000.0, id = "bike-2"), emptyList())

        val prefs = VaultSettings.from(state).toBackupPreferences()
        val restored = prefs.toVaultSettings().applyTo(VaultState())
        assertEquals(1, restored.workshopContacts.size)
        assertEquals("01888000001", restored.workshopContacts.single().phone)
        assertEquals(1, restored.garageBikes.single().repairCases.size)
        assertEquals("Carburetor tuning", restored.garageBikes.single().repairCases.single().title)
        assertEquals(1, restored.garageBikes.single().warranties.size)
    }
}
