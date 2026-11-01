package com.motovault.app.storage

import com.motovault.app.ui.DocumentPage
import com.motovault.app.ui.VaultDocument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupDocumentAdapterTest {
    @Test
    fun preservesAllDocumentFieldsAndWireRepresentation() {
        val document = VaultDocument(
            title = "Synthetic", number = "123", holder = "Example", expiry = "2030-01-01",
            id = "doc-1", issued = "2020-01-01", frontPhoto = "front", backPhoto = "back",
            pages = listOf(DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 100, 80, "Front", "accepted.jpg")),
            originalPdf = "original.pdf", pdfPageCount = 2, details = mapOf("class" to "A"),
            detailsConfirmed = true, extractionCandidates = mapOf("name" to "Example"), capturedAt = "2026-01-01",
            reminder = true, kind = "licence", reminderWarningDays = 15, reminderSnoozedUntil = "2029-12-01"
        )
        val backup = document.toBackupDocument()
        assertEquals(document, backup.toVaultDocument())
        assertEquals(Json.parseToJsonElement(Json.encodeToString(document)), Json.parseToJsonElement(Json.encodeToString(backup)))
        assertEquals(setOf("original.jpg", "preview.jpg", "accepted.jpg", "original.pdf"), backup.assetIds())
    }
}
