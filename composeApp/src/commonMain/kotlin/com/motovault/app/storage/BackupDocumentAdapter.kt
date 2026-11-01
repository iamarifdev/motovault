package com.motovault.app.storage

import com.motovault.app.domain.backup.BackupDocument
import com.motovault.app.domain.backup.BackupDocumentPage
import com.motovault.app.domain.model.DocumentPage
import com.motovault.app.domain.model.VaultDocument

internal fun VaultDocument.toBackupDocument() = BackupDocument(
    title = title, number = number, holder = holder, expiry = expiry, id = id, issued = issued,
    frontPhoto = frontPhoto, backPhoto = backPhoto,
    pages = pages.map { BackupDocumentPage(it.originalId, it.previewId, it.mimeType, it.width, it.height, it.side, it.acceptedId) },
    originalPdf = originalPdf, pdfPageCount = pdfPageCount, details = details,
    detailsConfirmed = detailsConfirmed, extractionCandidates = extractionCandidates, capturedAt = capturedAt,
    reminder = reminder, kind = kind, reminderWarningDays = reminderWarningDays, reminderSnoozedUntil = reminderSnoozedUntil
)

internal fun BackupDocument.toVaultDocument() = VaultDocument(
    title = title, number = number, holder = holder, expiry = expiry, id = id, issued = issued,
    frontPhoto = frontPhoto, backPhoto = backPhoto,
    pages = pages.map { DocumentPage(it.originalId, it.previewId, it.mimeType, it.width, it.height, it.side, it.acceptedId) },
    originalPdf = originalPdf, pdfPageCount = pdfPageCount, details = details,
    detailsConfirmed = detailsConfirmed, extractionCandidates = extractionCandidates, capturedAt = capturedAt,
    reminder = reminder, kind = kind, reminderWarningDays = reminderWarningDays, reminderSnoozedUntil = reminderSnoozedUntil
)
