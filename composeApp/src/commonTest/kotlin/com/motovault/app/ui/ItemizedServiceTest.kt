package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemizedServiceTest {
    private fun state(): VaultState {
        val initial = VaultState.create(Ride("ktm", "Indian · BS6", 2023, 4500.0))
        return initial.copy(rules = initial.rules.map { it.copy(lastServicedOdometerKm = 4200.0) })
    }

    @Test
    fun specificScheduleNamesDoNotFallIntoBroadCategories() {
        assertEquals("oil_filter", BangladeshServiceCatalogue.itemForScheduleName("Oil filter").id)
        assertEquals("oil_filter", BangladeshServiceCatalogue.itemForScheduleName("অয়েল ফিল্টার").id)
        assertEquals("brake_fluid", BangladeshServiceCatalogue.itemForScheduleName("Brake Fluid DOT 4").id)
        assertEquals("brake_fluid", BangladeshServiceCatalogue.itemForScheduleName("ব্রেক ফ্লুইড").id)
        assertTrue(BangladeshServiceCatalogue.scheduleNameMatchesItem("Brake Fluid DOT 4", "brake_fluid"))
        assertTrue(!BangladeshServiceCatalogue.scheduleNameMatchesItem("Brake Fluid DOT 4", "brake_pad"))
    }

    @Test
    fun legacyJsonWithoutItemizedFieldsStillDecodes() {
        val old = """{"ride":{"brandId":"ktm","edition":"Indian","year":2023,"odometer":4500.0},"logs":[{"title":"Service","detail":"Service recorded","date":"2026-01-01","odometer":4400.0,"kind":"service","cost":1250.0,"componentId":"old-rule"}]}"""
        val log = VaultState.decode(old)!!.logs.single()
        assertEquals(emptyList(), log.serviceLines)
        assertNull(log.serviceTotalPoisha)
        assertEquals(1250.0, log.cost)
        assertNull(log.location)
        assertEquals("", log.locationLabel)
        assertFalse(VaultState.decode(old)!!.autoLocation)
    }

    @Test
    fun requestedLocationIsStoredWithFuelAndServiceAndRoundTrips() {
        val capture = GeoCapture(23.8103, 90.4125, 12.0, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), "test")
        val withFuel = state().logFuel(4600.0, 5.0, 600.0, location = capture, locationLabel = "ধানমন্ডি")
        assertEquals(capture, withFuel.logs.first().location)
        assertEquals("ধানমন্ডি", withFuel.logs.first().locationLabel)

        val withService = withFuel.logServiceVisit(
            listOf(ServiceLineItem("air_filter", unitPricePoisha = 5000)), location = capture
        )
        val restored = VaultState.decode(VaultState.encode(withService))!!
        assertEquals(capture, restored.logs.first().location)
        assertEquals(capture, restored.logs.last().location)
        val stale = capture.copy(capturedAtEpochMillis = capture.capturedAtEpochMillis - 121_000)
        assertTrue(stale.isValid()) // Saved fixes remain usable while a long service form is completed.
        assertFalse(stale.isFresh()) // Freshness is checked when the native callback arrives.
        assertFailsWith<IllegalArgumentException> {
            state().logFuel(4600.0, 5.0, 600.0, location = capture.copy(latitude = 91.0))
        }
        assertTrue(VaultState.decode(VaultState.encode(state().copy(autoLocation = true)))!!.autoLocation)
    }

    @Test
    fun oilFieldsRoundTripWithItemizedVisit() {
        val oil = EngineOilMetadata("full_synthetic", "10W-40", "SN", "MA2", "Example 4T", "bottle", 1000, 1.0, 850)
        val original = state().logServiceVisit(
            listOf(ServiceLineItem("engine_oil", action = "replace", quantity = 1.0, unitPricePoisha = 95000, oil = oil)),
            labourPoisha = 10000
        )
        val decoded = VaultState.decode(VaultState.encode(original))!!
        assertEquals(oil, decoded.logs.first().serviceLines.single().oil)
        assertEquals(105000, decoded.logs.first().serviceTotalPoisha)
    }

    @Test
    fun unknownInputsRemainUnknownAndInvalidOilMetadataIsRejected() {
        val unknown = state().logServiceVisit(listOf(ServiceLineItem("air_filter", unitPricePoisha = null)), labourPoisha = null)
        assertNull(unknown.logs.first().serviceTotalPoisha)
        assertEquals(0.0, unknown.logs.first().cost) // Legacy field remains compatible; itemized consumers use nullable total.
        val blankAncillaries = state().logServiceVisit(
            listOf(ServiceLineItem("air_filter", unitPricePoisha = 5000)),
            labourPoisha = null, miscellaneousPoisha = null, discountPoisha = null
        )
        assertNull(blankAncillaries.logs.first().serviceTotalPoisha)
        assertFailsWith<IllegalArgumentException> {
            state().logServiceVisit(listOf(ServiceLineItem("engine_oil", oil = EngineOilMetadata("universal", "10W-40", purchaseBasis = "bottle"))))
        }
        assertFailsWith<IllegalArgumentException> {
            state().logServiceVisit(listOf(ServiceLineItem("engine_oil", oil = EngineOilMetadata("mineral", purchaseBasis = "bottle", packVolumeMl = 500, boughtQuantity = 1.0, usedQuantityMl = 600))))
        }
    }

    @Test
    fun explicitFreeIsZeroAndUnknownPriceStaysUnknown() {
        val free = ServiceLineItem("inspection", action = "inspect", unitPricePoisha = 0)
        val unknown = ServiceLineItem("inspection", action = "inspect", unitPricePoisha = null)
        assertEquals(0, free.lineTotalPoisha)
        assertNull(unknown.lineTotalPoisha)
        assertEquals(0, serviceTotalPoisha(listOf(free), 0, 0, 0))
        assertNull(serviceTotalPoisha(listOf(unknown), 0, 0, 0))
        assertEquals(10125, serviceTotalPoisha(listOf(ServiceLineItem("engine_oil", quantity = 1.5, unitPricePoisha = 6750)), 0, 0, 0))
    }

    @Test
    fun oneVisitCanUpdateSeveralChosenSchedulesAndProtectsEachBaseline() {
        val state = state()
        val first = state.rules.first { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, "engine_oil") }
        val second = state.rules.first { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, "air_filter") }
        val untouched = state.rules.first { it.id !in setOf(first.id, second.id) }
        val event = state.logServiceVisit(
            listOf(
                ServiceLineItem("engine_oil", unitPricePoisha = 70000, scheduleId = first.id, resetsScheduleBaseline = true, oil = EngineOilMetadata("mineral", purchaseBasis = "bottle")),
                ServiceLineItem("air_filter", unitPricePoisha = 25000, scheduleId = second.id, resetsScheduleBaseline = true)
            ),
            odometer = 4400.0
        )
        assertEquals(1, event.logs.size)
        assertEquals(setOf(first.id, second.id), event.logs.first().scheduleIds.toSet())
        assertEquals(4400.0, event.rules.first { it.id == first.id }.lastServicedOdometerKm)
        assertEquals(4400.0, event.rules.first { it.id == second.id }.lastServicedOdometerKm)
        assertEquals(untouched, event.rules.first { it.id == untouched.id })
        assertFailsWith<IllegalArgumentException> { event.deleteLog(event.logs.first()) }
        val adjusted = event.saveSchedule(event.rules.first { it.id == first.id }.copy(lastServicedOdometerKm = 4300.0))
        assertFailsWith<IllegalArgumentException> { adjusted.deleteLog(adjusted.logs.first()) }
    }

    @Test
    fun aServiceLineCannotResetAnUnrelatedSchedule() {
        val state = state()
        val chain = state.rules.first { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, "chain_sprocket") }
        assertFailsWith<IllegalArgumentException> {
            state.logServiceVisit(listOf(ServiceLineItem("engine_oil", scheduleId = chain.id, resetsScheduleBaseline = true, oil = EngineOilMetadata("mineral", purchaseBasis = "bottle"))))
        }
    }

    @Test
    fun inspectionDoesNotResetSchedulesOrClaimTheBaseline() {
        val state = state()
        val before = state.rules
        val event = state.logServiceVisit(listOf(ServiceLineItem("inspection", action = "inspect", scheduleId = null)))
        assertEquals(before, event.rules)
        assertTrue(event.logs.single().scheduleIds.isEmpty())
        assertEquals("পরীক্ষা", event.logs.single().title)
        assertTrue(event.deleteLog(event.logs.single()).logs.isEmpty())
    }

    @Test
    fun v2PassportExportPreservesItemizedRecordsAndAllowsLegacyState() {
        val legacy = state()
        assertNull(passportV1BlockReason(legacy))
        val itemized = legacy.logServiceVisit(listOf(ServiceLineItem("air_filter", unitPricePoisha = null)), labourPoisha = null)
        assertNull(passportV1BlockReason(itemized))
        val exported = ExportServicePassportUseCase().execute(itemized, passportDefaults(itemized).copy(owner = "Owner"), ownerConfirmed = true)
        assertEquals("motovault-service-passport-v2", exported.passport.schema)
        assertEquals("air_filter", exported.passport.services.single().items.single().catalogueId)
        assertNull(exported.passport.services.single().items.single().lineTotalPoisha)
        assertTrue(passportParagraphs(exported).any { "Total not fully priced" in it.text })
    }

    @Test
    fun smartEntryCreatesReviewableCandidatesAndKeepsUnknownText() {
        val draft = SmartServiceDraftParser.parse("Engine oil 1200 taka\nOil filter 350 taka\nunknown front fork noise")
        assertEquals(listOf("engine_oil", "oil_filter"), draft.suggestions.map { it.line.catalogueId })
        assertEquals(120000, draft.suggestions[0].line.unitPricePoisha)
        assertEquals(35000, draft.suggestions[1].line.unitPricePoisha)
        assertEquals(listOf("unknown front fork noise"), draft.unmatched)
        assertEquals("mineral", draft.suggestions[0].line.oil?.baseType)
        val bangla = SmartServiceDraftParser.parse("ইঞ্জিন অয়েল ১২০০ টাকা")
        assertEquals("engine_oil", bangla.suggestions.single().line.catalogueId)
        assertEquals(120000, bangla.suggestions.single().line.unitPricePoisha)
    }

    @Test
    fun editingAnItemizedVisitKeepsItsIdAndRecalculatesTheTotal() {
        val initial = state()
        val oilSchedule = initial.rules.first { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, "engine_oil") }
        val logged = initial.logServiceVisit(
            listOf(ServiceLineItem("engine_oil", unitPricePoisha = 90000, scheduleId = oilSchedule.id,
                resetsScheduleBaseline = true, oil = EngineOilMetadata("mineral", purchaseBasis = "bottle"))),
            odometer = 4400.0, labourPoisha = 10000, receiptBase64 = "receipt"
        )
        val old = logged.logs.first()
        val edited = logged.replaceServiceVisit(
            old.id,
            listOf(ServiceLineItem("engine_oil", unitPricePoisha = 85000, scheduleId = oilSchedule.id,
                resetsScheduleBaseline = true, oil = EngineOilMetadata("mineral", purchaseBasis = "bottle"))),
            old.odometer, kotlinx.datetime.LocalDate.parse(old.date), 10000, 0, 0, "Workshop", "Checked", old.location, "updated receipt"
        )
        assertEquals(1, edited.logs.size)
        assertEquals(old.id, edited.logs.single().id)
        assertEquals(95000, edited.logs.single().serviceTotalPoisha)
        assertEquals("updated receipt", edited.logs.single().receiptBase64)
        assertEquals(4400.0, edited.rules.first { it.id == oilSchedule.id }.lastServicedOdometerKm)
        val passport = ExportServicePassportUseCase().execute(edited, passportDefaults(edited).copy(owner = "Owner"), ownerConfirmed = true)
        assertFalse(canonicalPassport(passport.passport).contains("updated receipt"))
    }

    @Test
    fun documentRenewalFeesAreSeparateEventsAndStayOutOfPassport() {
        val base = state().copy(documents = listOf(VaultDocument("Tax token", "TT-1", "Owner", "")))
        val paid = base.logDocumentFee("Tax token", 125000, notes = "Renewal")
        assertTrue(paid.logs.single().isDocumentFee)
        assertEquals(1250.0, paid.logs.single().cost)
        val passport = ExportServicePassportUseCase().execute(paid, passportDefaults(paid).copy(owner = "Owner"), ownerConfirmed = true)
        assertTrue(passport.passport.services.isEmpty())
        assertEquals(1250.0, VaultState.decode(VaultState.encode(paid))!!.logs.single().cost)
    }

    @Test
    fun correctingFuelRecalculatesEconomyWithoutBreakingOrder() {
        val base = state()
        val first = base.logFuel(4300.0, 4.0, 500.0, kotlinx.datetime.LocalDate.parse("2026-01-01"), fullTank = true)
        val second = first.logFuel(4400.0, 5.0, 650.0, kotlinx.datetime.LocalDate.parse("2026-01-02"), fullTank = true)
        val old = second.logs.first()
        val corrected = second.replaceFuelLog(old.id, 4410.0, 5.0, 700.0,
            kotlinx.datetime.LocalDate.parse("2026-01-02"), true, null, "মিরপুর")
        assertEquals(old.id, corrected.logs.first { it.isFuel && it.odometer == 4410.0 }.id)
        assertEquals(22.0, corrected.economy)
        assertEquals("মিরপুর", corrected.logs.first { it.id == old.id }.locationLabel)
        assertFailsWith<IllegalArgumentException> {
            corrected.replaceFuelLog(old.id, 4200.0, 5.0, 700.0,
                kotlinx.datetime.LocalDate.parse("2026-01-02"), true, null, "")
        }
    }
}
