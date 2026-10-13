package com.motovault.app.ui.redesign

import kotlin.test.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class DocumentCaptureTest {
    @Test fun recoveryKeepsReferencesRecentFilesUnknownNamesAndNonFiles() {
        val now = 1800000000000L
        val old = now - DOCUMENT_ORPHAN_GRACE_MILLIS - 1
        val saved = "1790000000000--123.jpg"
        val orphan = "1790000000000-456.pdf"
        val fresh = "1790000000000-789.png"
        val link = "1790000000000-987.jpg"
        val future = "1790000000000-876.jpg"
        val files = listOf(DocumentAssetFile(saved, old), DocumentAssetFile(orphan, old),
            DocumentAssetFile(fresh, now), DocumentAssetFile("legacy-original.jpg", old),
            DocumentAssetFile(link, old, false), DocumentAssetFile(future, now + 1000),
            DocumentAssetFile("../1790000000000-123.jpg", old), DocumentAssetFile("1790000000000-222.jpg", 0))
        assertEquals(setOf(orphan), orphanDocumentAssets(files, setOf(saved), now))
    }
    @Test fun failedOrCancelledOperationRemovesOnlyItsOwnAllocations() = runBlocking {
        for (failure in listOf(IllegalStateException("bad second page"), CancellationException("disposed"))) {
            var removed = emptySet<String>()
            val operation = DocumentAssetOperation { removed = it }
            try {
                documentAssetOperation(operation) {
                    it.register("new-original.jpg")
                    it.register("new-preview.jpg")
                    throw failure
                }
                fail("Expected failure")
            } catch (caught: Exception) { assertSame(failure, caught) }
            assertEquals(setOf("new-original.jpg", "new-preview.jpg"), removed)
            assertFalse("saved-original.jpg" in removed)
        }
    }
    @Test fun successfulOperationTransfersAllFilesWithoutCleanup() = runBlocking {
        var removed = emptySet<String>()
        val operation = DocumentAssetOperation { removed = it }
        assertEquals("new.jpg", documentAssetOperation(operation) { it.register("new.jpg") })
        assertTrue(removed.isEmpty())
    }
    @Test fun retakingOnePaperPagePreservesOtherPagesAndSourcePdf() {
        val first = DocumentPage("first.jpg", "first-preview.jpg", "image/jpeg", 1000, 1400, "Page 1")
        val second = first.copy(originalId = "second.jpg", side = "Page 2")
        val document = CapturedDocument(listOf(first, second), "source.pdf", 2)
        val updated = document.replacePage(1, second.copy(originalId = "new.jpg", side = "Front"))
        assertEquals(first, updated.pages.first()); assertEquals("Page 2", updated.pages[1].side); assertEquals("source.pdf", updated.originalPdf)
    }
    @Test fun appendingPagesKeepsTheirOrderAndEnforcesTheLimit() {
        val page = DocumentPage("raw.jpg", "thumb.jpg", "image/jpeg", 1000, 1400, "Front")
        val document = CapturedDocument(listOf(page.copy(side = "Page 1")))
        assertEquals(listOf("Page 1", "Page 2"), document.appendPage(page, "Tax token").pages.map { it.side })
        assertFailsWith<IllegalArgumentException> { CapturedDocument(List(8) { page }).appendPage(page, "Tax token") }
    }
    @Test fun unseenOcrExtrasStayCandidatesRatherThanConfirmedDetails() {
        val values = mapOf("registration" to "TEST", "engine" to "UNREVIEWED", "chassis" to "UNREVIEWED")
        assertEquals(mapOf("registration" to "TEST"), reviewedDocumentDetails("Registration smart card", values, emptyMap(), false))
        assertEquals(values, reviewedDocumentDetails("Registration smart card", values, emptyMap(), true))
        assertEquals(mapOf("engine" to "PREVIOUS"), reviewedDocumentDetails("Driving licence", values, mapOf("engine" to "PREVIOUS"), false))
    }
    @Test fun rawAcceptedAndPreviewFilesAreAllRetainedByTheRecord() {
        val page = DocumentPage("raw.jpg", "thumb.jpg", "image/jpeg", 1600, 1000, "Front", "crop.jpg")
        assertEquals(setOf("raw.jpg", "thumb.jpg", "crop.jpg", "source.pdf"), CapturedDocument(listOf(page), "source.pdf").assetIds())
        val document = VaultDocument("Driving licence", "TEST", "Sample", "", pages = listOf(page))
        assertEquals(setOf("raw.jpg", "thumb.jpg", "crop.jpg"), document.copy(holder = "Corrected").assetIds())
    }
    @Test fun cropRejectsOutsideImageAndUnreadablySmallRegions() {
        assertFailsWith<IllegalArgumentException> { DocumentCrop(left = -.1f).checked() }
        assertFailsWith<IllegalArgumentException> { DocumentCrop(left = .9f, right = .95f).checked() }
        assertEquals(DocumentCrop(.1f, .2f, .8f, .9f), DocumentCrop(.1f, .2f, .8f, .9f).checked())
    }
    @Test fun qualityBlocksUnreadyGlareDarkAndBlurredCaptures() {
        assertFalse(CaptureQuality().canCapture)
        assertFalse(CaptureQuality(ready = true, glare = true).canCapture)
        assertFalse(CaptureQuality(ready = true, dark = true).canCapture)
        assertFalse(CaptureQuality(ready = true, blurred = true).canCapture)
        assertTrue(CaptureQuality(ready = true).canCapture)
    }
    @Test fun savedCropAndRecognitionMetadataSurviveSerialization() {
        val document = VaultDocument("Driving licence", "TEST", "Sample", "", pages = listOf(DocumentPage("raw.jpg", "preview.jpg", "image/jpeg", 1600, 1000, "Front", "accepted.jpg")), extractionCandidates = mapOf("number" to "TEST"), capturedAt = "2026-10-11T00:00:00Z")
        assertEquals(document, Json.decodeFromString<VaultDocument>(Json.encodeToString(VaultDocument.serializer(), document)))
    }
    @Test fun readsLabelledValuesWithoutGuessingAmbiguousDates() {
        val values = DocumentTextReader.suggestions("Driving licence", "Name: SAMPLE HOLDER\nLicence No: TEST-1234\nIssue Date: 03/04/2024\nExpiry Date: 25/12/2030")
        assertEquals("SAMPLE HOLDER", values["holder"])
        assertEquals("TEST-1234", values["number"])
        assertNull(values["issued"])
        assertEquals("2030-12-25", values["expiry"])
    }
    @Test fun neverUsesTheNextLabelAsAName() {
        assertNull(DocumentTextReader.suggestions("Driving licence", "Name\nBlood Group\nO+")["holder"])
    }
    @Test fun normalizesBanglaDigitsAndRejectsInvalidDates() {
        assertEquals("2030-12-25", DocumentTextReader.normalizeDate("২৫/১২/২০৩০"))
        assertNull(DocumentTextReader.normalizeDate("31/02/2030"))
    }
    @Test fun preventsPathsOutsideManagedStorage() {
        assertFailsWith<IllegalArgumentException> { validateDocumentAssetId("../private.jpg") }
        assertFailsWith<IllegalArgumentException> { validateDocumentAssetId("/tmp/card.jpg") }
        assertEquals("sample_original-123.jpg", validateDocumentAssetId("sample_original-123.jpg"))
    }
    @Test fun legacyRecordsRemainReadableAndEditingPreservesOriginals() {
        val legacy = Json.decodeFromString<VaultDocument>("""{"title":"Driving licence","number":"TEST","holder":"Sample","expiry":""}""")
        assertTrue(legacy.pages.isEmpty())
        val page = DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 1200, 800, "Front")
        val scanned = legacy.copy(pages = listOf(page), originalPdf = "original.pdf")
        val edited = scanned.copy(holder = "Corrected holder")
        assertEquals(scanned.pages, edited.pages)
        assertEquals(scanned.originalPdf, edited.originalPdf)
        assertEquals(2, documentPageLimit("Driving licence"))
        assertEquals(8, documentPageLimit("Insurance"))
    }
}
