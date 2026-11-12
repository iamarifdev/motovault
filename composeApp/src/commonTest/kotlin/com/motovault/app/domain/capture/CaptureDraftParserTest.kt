package com.motovault.app.domain.capture

import kotlin.test.*

class CaptureDraftParserTest {
    private val id = "a".repeat(64)
    private fun parse(text: String, source: CaptureSource = CaptureSource.PASTE, hint: CaptureIntent = CaptureIntent.UNKNOWN) =
        CaptureDraftParser.parse(text, source, id, 12340.0, hint)

    @Test fun separatesPaidAmountMileageBalanceFeeAndPhoneNumbers() {
        val draft = parse("Payment Tk 1,250.50 for octane at 12,400 km. Charge Tk 5.00 Balance Tk 9,000.00 to 01700000000 TrxID ABC123")
        assertEquals(125050L, draft.amounts.single().value)
        assertEquals(12400.0, draft.odometers.single().value)
        assertEquals(CaptureIntent.FUEL, draft.intent)
        assertEquals(CaptureConfidence.LABELED, draft.amounts.single().confidence)
        assertTrue(draft.issues.isEmpty(), draft.issues.toString())
    }

    @Test fun supportsRealLanguageSyntheticBanglaAndArabicIndicInputs() {
        for (text in listOf("পেমেন্ট ৫০০.৫০ টাকা অকটেন, ওডোমিটার ১২৪০০ কিমি", "Payment Tk ٥٠٠.٥٠ petrol, odometer ١٢٤٠٠ km")) {
            val draft = parse(text)
            assertEquals(50050L, draft.amounts.single().value)
            assertEquals(12400.0, draft.odometers.single().value)
        }
    }

    @Test fun contradictoryTotalsRequireExplicitChoice() {
        val draft = parse("Total Tk 500.00, paid Tk 450.00 fuel")
        assertEquals(setOf(50000L, 45000L), draft.amounts.map { it.value }.toSet())
        assertTrue(CaptureIssue.AMBIGUOUS_AMOUNT in draft.issues)
        assertTrue(draft.requiresChoice)
    }

    @Test fun malformedAmountsCannotBecomeValidPartialMatches() {
        for (value in listOf("1,2", "1.2.3", "1e3", "-500", "500abc", "500..")) {
            val draft = parse("Fuel Tk $value")
            assertTrue(draft.amounts.isEmpty(), value)
            assertTrue(CaptureIssue.MALFORMED_AMOUNT in draft.issues, value)
        }
    }

    @Test fun unlabelledIdentifiersNeverBecomeMoneyOrMileage() {
        val draft = parse("Reference ABC123, phone 01700000000, date 2026-11-12")
        assertTrue(draft.amounts.isEmpty())
        assertTrue(draft.odometers.isEmpty())
        assertTrue(CaptureIssue.NO_FIELDS in draft.issues)
    }

    @Test fun photoOdoLabelWinsOverTripAndUnlabelledAmbiguityStaysVisible() {
        val labeled = parse("ODO 12400 TRIP 150.5", CaptureSource.PHOTO)
        assertEquals(12400.0, labeled.odometers.single().value)
        val ambiguous = parse("12400 12500", CaptureSource.PHOTO)
        assertEquals(2, ambiguous.odometers.size)
        assertTrue(CaptureIssue.AMBIGUOUS_ODOMETER in ambiguous.issues)
        assertEquals(CaptureConfidence.UNLABELED, ambiguous.odometers.first().confidence)
    }

    @Test fun rejectsOdometerRegressionsAndImplausibleReadings() {
        val backwards = parse("odometer 12000 km")
        assertTrue(backwards.odometers.isEmpty())
        assertTrue(CaptureIssue.ODOMETER_REGRESSION in backwards.issues)
        val excessive = parse("99999999", CaptureSource.PHOTO)
        assertTrue(excessive.odometers.isEmpty())
        assertTrue(CaptureIssue.INVALID_ODOMETER in excessive.issues)
    }

    @Test fun allAcquisitionSourcesUseTheSameDraftAndPreserveTheirOrigin() {
        CaptureSource.entries.forEach { source ->
            val draft = parse("Fuel Tk 500.50", source)
            assertEquals(source, draft.source)
            assertEquals(id, draft.id)
            assertEquals(50050L, draft.amounts.single().value)
        }
        val manual = parse("৫০০.৫০", CaptureSource.MANUAL, CaptureIntent.EXPENSE)
        assertEquals(50050L, manual.amounts.single().value)
        assertEquals(CaptureConfidence.MANUAL, manual.amounts.single().confidence)
    }

    @Test fun sharedDuplicateAndOversizedPayloadCannotPassReview() {
        val duplicate = CaptureDraftParser.parse("Fuel Tk 500", CaptureSource.SHARE, id, 12340.0, acceptedIds = setOf(id))
        assertFalse(duplicate.canReview)
        assertTrue(CaptureIssue.DUPLICATE_SHARE in duplicate.issues)
        assertFalse(parse("x".repeat(MAX_CAPTURE_TEXT_LENGTH + 1)).canReview)
        assertFalse(parse("").canReview)
    }

    @Test fun serviceSuggestionsUseStableIdsAndExactPoishaWithUnknownLinesRetained() {
        val service = CaptureServiceParser.parse("2 x Engine oil 1200.25 taka\nঅয়েল ফিল্টার ৩৫০ টাকা\nunknown fork noise")
        assertEquals(listOf("engine_oil", "oil_filter"), service.lines.map { it.line.catalogueId })
        assertEquals(2.0, service.lines.first().line.quantity)
        assertEquals(120025, service.lines.first().line.unitPricePoisha)
        assertEquals(listOf("unknown fork noise"), service.unmatched)
        assertNull(CaptureServiceParser.parse("engine oil Tk 1.2.3 and Tk 500").lines.single().line.unitPricePoisha)
        assertTrue(CaptureServiceParser.parse("সার্ভিসচার্জ Tk 5").lines.isEmpty())
    }
    @Test fun wordBoundariesRejectAttachedLettersAndMarksButAllowALaterStandaloneAlias() {
        assertTrue(CaptureServiceParser.parse("সার্ভিসচার্জ ৫০ টাকা").lines.isEmpty())
        assertEquals("general_service", CaptureServiceParser.parse("সার্ভিসচার্জ; সার্ভিস ৫০ টাকা").lines.single().line.catalogueId)
        assertTrue(CaptureTextNumbers.money("Tk500abc").malformed)
        assertTrue(CaptureTextNumbers.money("Tk500ক").malformed)
        assertTrue(CaptureTextNumbers.money("abc500 টাকা").candidates.isEmpty())
        assertEquals(50050L, CaptureTextNumbers.money("Paid Tk500.50. Balance Tk2,140.50").candidates.single().value)
    }
}
