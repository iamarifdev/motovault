package com.motovault.app.domain.backup

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** V1 archive representation. Property names and defaults are part of the wire format. */
@Serializable
data class BackupDocumentPage(
    val originalId: String, val previewId: String, val mimeType: String,
    val width: Int, val height: Int, val side: String, val acceptedId: String? = null
)

@Serializable
data class BackupDocument(
    val title: String, val number: String, val holder: String, val expiry: String,
    val id: String = newBackupRecordId(), val issued: String = "",
    val frontPhoto: String? = null, val backPhoto: String? = null,
    val pages: List<BackupDocumentPage> = emptyList(), val originalPdf: String? = null,
    val pdfPageCount: Int = 0, val details: Map<String, String> = emptyMap(),
    val detailsConfirmed: Boolean = false,
    val extractionCandidates: Map<String, String> = emptyMap(), val capturedAt: String = "",
    val reminder: Boolean = false, val kind: String = "", val reminderWarningDays: Int? = null,
    val reminderSnoozedUntil: String? = null
) {
    fun assetIds(): Set<String> = pages.flatMap { listOfNotNull(it.originalId, it.previewId, it.acceptedId) }.toSet() + listOfNotNull(originalPdf)
}

// Mirrors the legacy default for old V1 payloads that do not contain an ID.
internal fun newBackupRecordId(): String = Clock.System.now().toEpochMilliseconds().toString() + "-" + Random.nextLong().toString()
