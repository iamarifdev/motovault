package com.motovault.app.ui.maintenance

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.ServiceLineItem
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.model.today
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.ui.CATALOG_ITEMS
import com.motovault.app.ui.ServiceRowState
import com.motovault.app.ui.ServiceSaveMapper
import com.motovault.app.ui.ServiceValidationReason
import com.motovault.app.ui.calculateServiceTotals
import com.motovault.app.ui.create
import com.motovault.app.ui.dueServiceKeys
import com.motovault.app.ui.inferWarrantyMonths
import com.motovault.app.ui.initialServiceVisitState
import com.motovault.app.ui.isOpenQuote
import com.motovault.app.ui.toggledQuote
import com.motovault.app.ui.suggestedServiceKeys
import com.motovault.app.ui.validateServiceDraft
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceSaveMapperTest {
    private val currentDay = today()

    @Test
    fun dueAndSuggestedServiceKeysOrderDueThenPricedThenCatalogAndExcludePicked() {
        val oldDays = currentDay.minus(DatePeriod(days = 200)).toEpochDays().toLong()
        val recentDays = currentDay.minus(DatePeriod(days = 2)).toEpochDays().toLong()
        val ride = Ride("yamaha", "R15", 2023, 5000.0, id = "primary")
        val state = VaultState(
            ride = ride,
            rules = listOf(
                ComponentRule("r-oil", "primary", "Engine oil", "", 1000.0, 90, 4950.0, recentDays, baselineCertainty = BaselineCertainty.CONFIRMED),
                ComponentRule("r-air", "primary", "Air filter", "", 2000.0, 90, 2000.0, oldDays, baselineCertainty = BaselineCertainty.CONFIRMED),
                ComponentRule("r-plug", "primary", "Spark plug", "", 5000.0, 180, 0.0, 0L, baselineCertainty = BaselineCertainty.UNKNOWN)
            ),
            lastItemPrices = mapOf("tyre" to 3200.0, "battery" to 1800.0)
        )

        val due = dueServiceKeys(state, currentDay)
        assertEquals(listOf("air_filter"), due)

        val suggested = suggestedServiceKeys(due, state.lastItemPrices, setOf("tyre"))
        assertEquals(6, suggested.size)
        assertEquals("air_filter", suggested[0])
        assertEquals("battery", suggested[1])
        assertEquals(CATALOG_ITEMS.first().key, suggested[2])
        assertEquals(true, "tyre" !in suggested)
    }

    @Test
    fun totalsIncludeStoredMiscAndValidationFollowsV4Order() {
        val rows = listOf(
            ServiceRowState("engine_oil", "engine_oil", "Engine oil", qty = 2),
            ServiceRowState("custom-0", null, "Bolt", qty = 1)
        )
        val totals = calculateServiceTotals(
            rows = rows,
            values = mapOf("p:engine_oil" to "900", "labour" to "150", "discount" to "50"),
            existingMiscPoisha = 7500
        )
        assertEquals(1800.0, totals.subtotal)
        assertEquals(150.0, totals.labour)
        assertEquals(75.0, totals.misc)
        assertEquals(50.0, totals.discount)
        assertEquals(1975.0, totals.total)
        assertEquals(1, totals.unknownPriceCount)

        assertEquals(ServiceValidationReason.RECORD_MISSING, validateServiceDraft("x", false, 0, null, false, null, currentDay))
        assertEquals(ServiceValidationReason.NO_ROWS, validateServiceDraft(null, false, 0, null, false, null, currentDay))
        assertEquals(ServiceValidationReason.INVALID_ODOMETER, validateServiceDraft(null, false, 1, 0.0, false, null, currentDay))
        assertEquals(ServiceValidationReason.CONFIRM_ODOMETER, validateServiceDraft(null, false, 1, 1000.0, false, null, currentDay))
        assertEquals(ServiceValidationReason.INVALID_DATE, validateServiceDraft(null, false, 1, 1000.0, true, null, currentDay))
        val future = LocalDate.fromEpochDays(currentDay.toEpochDays() + 1)
        assertEquals(ServiceValidationReason.FUTURE_DATE, validateServiceDraft(null, false, 1, 1000.0, true, future, currentDay))
        assertNull(validateServiceDraft(null, false, 1, 1000.0, true, currentDay, currentDay))
    }

    @Test
    fun mapperSavesCreateCustomLinesAndQuoteToPaidEditWithMiscPreserved() {
        val initial = VaultState.create(Ride("yamaha", "R15", 2023, 5000.0, id = "primary"))
        val rows = listOf(
            ServiceRowState("engine_oil", "engine_oil", "Engine oil", qty = 1, detail = "10W-40"),
            ServiceRowState("custom-0", null, "Lever adjustment", qty = 1)
        )
        val created = ServiceSaveMapper.save(
            state = initial,
            existing = null,
            selectedRuleId = null,
            rows = rows,
            values = mapOf("p:engine_oil" to "900", "p:custom-0" to "100", "labour" to "50", "discount" to ""),
            date = currentDay,
            odoVal = 5000.0,
            workshop = "Mohakhali Motors",
            workshopPhone = "01700000000",
            visitState = VisitRecordState.QUOTATION,
            caseTitle = "Lever play",
            warrantyMonths = 0,
            today = currentDay
        )
        val createdLog = created.logs.single()
        assertEquals(VisitRecordState.QUOTATION.code, createdLog.visitState)
        assertNull(createdLog.serviceLines[1].catalogueId)
        assertEquals("Lever adjustment", createdLog.serviceLines[1].customName)
        assertEquals(900.0, created.lastItemPrices["engine_oil"])

        val seededMisc = created.copy(logs = listOf(createdLog.copy(serviceMiscellaneousPoisha = 5000)))
        val rebuiltRows = ServiceSaveMapper.buildInitialRows(seededMisc.logs.single(), null, null, mapOf("engine_oil" to "Engine oil"))
        val promoted = ServiceSaveMapper.save(
            state = seededMisc,
            existing = seededMisc.logs.single(),
            selectedRuleId = null,
            rows = rebuiltRows,
            values = mapOf("p:engine_oil" to "920", "p:custom-e1" to "100", "labour" to "50", "discount" to ""),
            date = currentDay,
            odoVal = 5000.0,
            workshop = "Mohakhali Motors",
            workshopPhone = "01700000000",
            visitState = VisitRecordState.PAID,
            caseTitle = "Lever play",
            warrantyMonths = 6,
            today = currentDay
        )
        val updatedLog = promoted.logs.single()
        assertEquals(VisitRecordState.PAID.code, updatedLog.visitState)
        assertEquals(5000, updatedLog.serviceMiscellaneousPoisha)
        assertEquals(112000, updatedLog.serviceTotalPoisha)
        assertEquals(6, inferWarrantyMonths(updatedLog, promoted.warranties))
        assertEquals(
            0,
            inferWarrantyMonths(RideLog("S", "", currentDay.toString(), 5000.0), emptyList<PartWarranty>())
        )
    }

    @Test
    fun editingKeepsCancelledAndApprovedVisitStates() {
        fun logWith(state: String) = RideLog("Service", "", "2026-01-01", 1000.0, kind = "service", visitState = state)
        assertEquals(VisitRecordState.CANCELLED, initialServiceVisitState(logWith(VisitRecordState.CANCELLED.code)))
        assertEquals(VisitRecordState.APPROVED, initialServiceVisitState(logWith(VisitRecordState.APPROVED.code)))
        assertEquals(VisitRecordState.PAID, initialServiceVisitState(null))
        assertTrue(VisitRecordState.APPROVED.isOpenQuote)
        assertEquals(VisitRecordState.PAID, VisitRecordState.APPROVED.toggledQuote())
        assertEquals(VisitRecordState.QUOTATION, VisitRecordState.CANCELLED.toggledQuote())
    }
}
