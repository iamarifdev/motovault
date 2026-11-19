package com.motovault.app.domain.vault

import com.motovault.app.domain.model.DocumentKinds
import com.motovault.app.domain.model.DocumentPage
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.stableKind
import kotlinx.datetime.LocalDate

/**
 * Creates or updates a document by [existingId] or matching [kind] and ownership scope.
 * Adding the same document kind preserves its identity, scanned pages, and renewal history.
 */
fun VaultState.upsertDocumentByKind(
    kind: String,
    expiry: String,
    reminder: Boolean,
    number: String = "",
    holder: String = "",
    pages: List<DocumentPage> = emptyList(),
    originalPdf: String? = null,
    pdfPageCount: Int = 0,
    existingId: String? = null,
    capturedAt: String = ""
): VaultState {
    val canonicalKind = DocumentKinds.fromStored(kind, kind) ?: error("Unknown document kind.")
    val canonicalTitle = DocumentKinds.canonicalTitle(canonicalKind)
    val trimmedExpiry = expiry.trim()
    require(trimmedExpiry.isNotEmpty()) { "Enter an expiry date." }
    LocalDate.parse(trimmedExpiry)

    val isOwner = canonicalKind == DocumentKinds.LICENCE
    val targetVehicleId = if (isOwner) null else ride?.id
    val existing = if (existingId != null) {
        documents.firstOrNull { it.id == existingId } ?: error("Document not found.")
    } else {
        findScopedDocumentByKind(canonicalKind, isOwner, targetVehicleId)
    }

    val trimmedNumber = number.trim()
    val trimmedHolder = holder.trim()
    val nextDocument = if (existing != null) {
        mergeExistingDocument(
            existing = existing,
            canonicalKind = canonicalKind,
            canonicalTitle = canonicalTitle,
            trimmedExpiry = trimmedExpiry,
            reminder = reminder,
            trimmedNumber = trimmedNumber,
            trimmedHolder = trimmedHolder,
            pages = pages,
            originalPdf = originalPdf,
            pdfPageCount = pdfPageCount,
            capturedAt = capturedAt,
            keepBlankIdentity = existingId == null,
            resolvedVehicleId = if (isOwner) null else (existing.vehicleId ?: targetVehicleId)
        )
    } else {
        VaultDocument(
            title = canonicalTitle,
            number = trimmedNumber,
            holder = trimmedHolder,
            expiry = trimmedExpiry,
            pages = pages,
            originalPdf = originalPdf,
            pdfPageCount = pdfPageCount,
            detailsConfirmed = trimmedNumber.isNotBlank() || trimmedHolder.isNotBlank(),
            capturedAt = capturedAt,
            reminder = reminder,
            kind = canonicalKind,
            vehicleId = targetVehicleId
        )
    }
    return saveDocument(nextDocument)
}

/** Removes [documentId] from the active document list. */
fun VaultState.deleteDocument(documentId: String): VaultState =
    copy(documents = documents.filterNot { it.id == documentId })

private fun VaultState.findScopedDocumentByKind(
    canonicalKind: String,
    isOwner: Boolean,
    targetVehicleId: String?
): VaultDocument? = documents.firstOrNull { doc ->
    doc.stableKind() == canonicalKind && if (isOwner) {
        doc.isOwnerScoped
    } else {
        doc.vehicleId == null || doc.vehicleId == targetVehicleId
    }
}

private fun mergeExistingDocument(
    existing: VaultDocument,
    canonicalKind: String,
    canonicalTitle: String,
    trimmedExpiry: String,
    reminder: Boolean,
    trimmedNumber: String,
    trimmedHolder: String,
    pages: List<DocumentPage>,
    originalPdf: String?,
    pdfPageCount: Int,
    capturedAt: String,
    keepBlankIdentity: Boolean,
    resolvedVehicleId: String?
): VaultDocument {
    val resolvedNumber = if (keepBlankIdentity) trimmedNumber.ifBlank { existing.number } else trimmedNumber
    val resolvedHolder = if (keepBlankIdentity) trimmedHolder.ifBlank { existing.holder } else trimmedHolder
    val replaceAssets = pages.isNotEmpty() || originalPdf != null
    return existing.copy(
        title = canonicalTitle,
        number = resolvedNumber,
        holder = resolvedHolder,
        expiry = trimmedExpiry,
        pages = if (replaceAssets) pages else existing.pages,
        originalPdf = if (replaceAssets) originalPdf else existing.originalPdf,
        pdfPageCount = if (replaceAssets) pdfPageCount else existing.pdfPageCount,
        capturedAt = if (replaceAssets) capturedAt.ifBlank { existing.capturedAt } else existing.capturedAt,
        detailsConfirmed = existing.detailsConfirmed || resolvedNumber.isNotBlank() || resolvedHolder.isNotBlank(),
        reminder = reminder,
        kind = canonicalKind,
        vehicleId = resolvedVehicleId
    )
}
