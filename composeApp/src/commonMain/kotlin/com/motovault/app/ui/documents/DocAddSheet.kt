package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.motovault.app.domain.vault.upsertDocumentByKind
import com.motovault.app.domain.garage.allDocumentAssetIds
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.DocumentKinds
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.isCardDocument
import com.motovault.app.domain.model.stableKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_add_save_failed
import motovault.composeapp.generated.resources.doc_add_saved
import motovault.composeapp.generated.resources.doc_add_saved_reminder
import motovault.composeapp.generated.resources.doc_add_scan_title
import motovault.composeapp.generated.resources.doc_add_type_title
import motovault.composeapp.generated.resources.doc_edit_saved
import motovault.composeapp.generated.resources.doc_edit_title

private enum class DocAddStep { TYPE, PHOTO, REVIEW }

@Stable
private class DocAddState(
    existing: VaultDocument?,
    todayDate: LocalDate,
    val protectedAssets: Set<String>
) {
    var step by mutableStateOf(if (existing != null) DocAddStep.REVIEW else DocAddStep.TYPE)
    var selectedKind by mutableStateOf(existing?.stableKind() ?: DocumentKinds.REGISTRATION)
    var captured by mutableStateOf<CapturedDocument?>(null)
    var captureBack by mutableStateOf(false)
    var photoBusy by mutableStateOf(false)
    var photoError by mutableStateOf<String?>(null)
    var committedAssets by mutableStateOf<Set<String>>(emptySet())
    var expiry by mutableStateOf(
        existing?.expiry?.takeIf { it.isNotBlank() }
            ?: LocalDate.fromEpochDays(todayDate.toEpochDays() + 365).toString()
    )
    var reminder by mutableStateOf(existing?.reminder ?: true)
    var number by mutableStateOf(existing?.number.orEmpty())
    var holder by mutableStateOf(existing?.holder.orEmpty())
    var showOptional by mutableStateOf(existing != null && (existing.number.isNotBlank() || existing.holder.isNotBlank()))
    var saveError by mutableStateOf<String?>(null)
}

@Composable
fun DocAddSheet(
    state: VaultState,
    existingId: String? = null,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val scope = rememberCoroutineScope()
    val todayDate = today()
    val leadDays = state.reminderPolicy.warningDays
    val existing = existingId?.let { id -> state.documents.firstOrNull { it.id == id } }
    val draft = remember(existingId) { DocAddState(existing, todayDate, state.allDocumentAssetIds()) }

    DisposableEffect(existingId) {
        onDispose {
            val pending = draft.captured?.assetIds().orEmpty() - draft.protectedAssets - draft.committedAssets
            if (pending.isNotEmpty()) runCatching { removeDocumentAssets(pending) }
        }
    }

    val kindInfo = docKindInfo(draft.selectedKind)
    val kindName = appStringResource(kindInfo.resource)
    val sheetTitle = when {
        existingId != null -> appStringResource(Res.string.doc_edit_title, kindName)
        draft.step == DocAddStep.TYPE -> appStringResource(Res.string.doc_add_type_title)
        draft.step == DocAddStep.PHOTO -> appStringResource(Res.string.doc_add_scan_title)
        else -> kindName
    }
    val saveFailedMessage = appStringResource(Res.string.doc_add_save_failed)
    val toastMessage = buildDocSavedToast(kindName, existingId != null, draft.reminder, draft.expiry, leadDays, f::date)

    Sheet(title = sheetTitle, onDismiss = onDismiss, detent = SheetDetent.TALL) {
        DocAddStepBody(
            state = state,
            existingId = existingId,
            existing = existing,
            draft = draft,
            kindInfo = kindInfo,
            kindName = kindName,
            leadDays = leadDays,
            todayDate = todayDate,
            saveFailedMessage = saveFailedMessage,
            toastMessage = toastMessage,
            scope = scope,
            onSuccessHaptic = { haptic.perform(HapticKind.SUCCESS) },
            onErrorHaptic = { haptic.perform(HapticKind.ERROR) },
            onSave = onSave
        )
    }
}

@Composable
private fun DocAddStepBody(
    state: VaultState,
    existingId: String?,
    existing: VaultDocument?,
    draft: DocAddState,
    kindInfo: DocumentKindInfo,
    kindName: String,
    leadDays: Int,
    todayDate: LocalDate,
    saveFailedMessage: String,
    toastMessage: String,
    scope: CoroutineScope,
    onSuccessHaptic: () -> Unit,
    onErrorHaptic: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    when (draft.step) {
        DocAddStep.TYPE -> DocTypeStep(
            onPickKind = { picked ->
                draft.selectedKind = picked.kind
                draft.captured = null
                draft.captureBack = false
                draft.photoError = null
                draft.step = DocAddStep.PHOTO
            },
            onSkipPhoto = {
                draft.selectedKind = DocumentKinds.REGISTRATION
                draft.captured = null
                draft.captureBack = false
                draft.photoError = null
                draft.step = DocAddStep.REVIEW
            }
        )
        DocAddStep.PHOTO -> DocPhotoStep(
            docKind = kindInfo,
            captureBack = draft.captureBack,
            busy = draft.photoBusy,
            errorMessage = draft.photoError,
            onCaptureBusyChange = { draft.photoBusy = it },
            onCameraPageCaptured = { page ->
                val merged = if (draft.captureBack) {
                    CapturedDocument(listOfNotNull(draft.captured?.pages?.firstOrNull()) + page.copy(side = "Back"))
                } else {
                    CapturedDocument(listOf(page.copy(side = if (isCardDocument(draft.selectedKind)) "Front" else "Page 1")))
                }
                acceptCapturedDocument(draft, kindInfo, merged, scope)
            },
            onImported = { imported ->
                val merged = if (draft.captureBack && imported.pages.isNotEmpty()) {
                    CapturedDocument(listOfNotNull(draft.captured?.pages?.firstOrNull()) + imported.pages.first().copy(side = "Back"))
                } else {
                    imported
                }
                acceptCapturedDocument(draft, kindInfo, merged, scope)
            },
            onError = { msg -> draft.photoError = msg },
            onKeepFrontOnly = {
                draft.captureBack = false
                draft.step = DocAddStep.REVIEW
            },
            onSkipPhoto = {
                val stale = draft.captured?.assetIds().orEmpty() - draft.protectedAssets
                if (stale.isNotEmpty()) runCatching { removeDocumentAssets(stale) }
                draft.captured = null
                draft.captureBack = false
                draft.step = DocAddStep.REVIEW
            }
        )
        DocAddStep.REVIEW -> DocExpiryStep(
            kindName = kindName,
            isEdit = existingId != null,
            missingExisting = existingId != null && existing == null,
            existingSameKind = existingId == null && hasSameKindDocument(state, draft.selectedKind),
            hasPhoto = draft.captured != null,
            expiry = draft.expiry,
            reminder = draft.reminder,
            number = draft.number,
            holder = draft.holder,
            showOptional = draft.showOptional,
            leadDays = leadDays,
            today = todayDate,
            saveError = draft.saveError,
            onExpiryChange = { draft.expiry = it },
            onReminderChange = { draft.reminder = it },
            onToggleOptional = { draft.showOptional = !draft.showOptional },
            onNumberChange = { draft.number = it },
            onHolderChange = { draft.holder = it },
            onSave = {
                submitDocAddSave(state, draft, existingId, saveFailedMessage, toastMessage, onSuccessHaptic, onErrorHaptic, onSave)
            }
        )
    }
}

private fun hasSameKindDocument(state: VaultState, selectedKind: String): Boolean =
    state.documents.any { doc ->
        doc.stableKind() == selectedKind &&
            if (selectedKind == DocumentKinds.LICENCE) doc.isOwnerScoped
            else (doc.vehicleId == null || doc.vehicleId == state.ride?.id)
    }

private fun acceptCapturedDocument(
    draft: DocAddState,
    kindInfo: DocumentKindInfo,
    nextCaptured: CapturedDocument,
    scope: CoroutineScope
) {
    val stale = draft.captured?.assetIds().orEmpty() - nextCaptured.assetIds() - draft.protectedAssets
    if (stale.isNotEmpty()) runCatching { removeDocumentAssets(stale) }
    draft.captured = nextCaptured
    if (isCardDocument(draft.selectedKind) && !draft.captureBack && nextCaptured.pages.size == 1 && nextCaptured.originalPdf == null) {
        draft.captureBack = true
        return
    }
    draft.photoBusy = true
    scope.launch {
        try {
            val processed = if (nextCaptured.recognizedText.isBlank()) recognizeDocument(nextCaptured) else nextCaptured
            draft.captured = processed
            val suggestions = DocumentTextReader.suggestions(kindInfo.title, processed.recognizedText)
            suggestions["expiry"]?.let { if (draft.expiry.isBlank()) draft.expiry = it }
            suggestions["number"]?.let { if (draft.number.isBlank()) draft.number = it }
            suggestions["holder"]?.let { if (draft.holder.isBlank()) draft.holder = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Keep manual entry ready when on-device OCR cannot extract text.
        } finally {
            draft.photoBusy = false
            draft.captureBack = false
            draft.step = DocAddStep.REVIEW
        }
    }
}

private fun submitDocAddSave(
    state: VaultState,
    draft: DocAddState,
    existingId: String?,
    saveFailedMessage: String,
    toastMessage: String,
    onSuccessHaptic: () -> Unit,
    onErrorHaptic: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    draft.saveError = null
    val nextState = runCatching {
        state.upsertDocumentByKind(
            kind = draft.selectedKind,
            expiry = NormalizedNumbers.ascii(draft.expiry),
            reminder = draft.reminder,
            number = draft.number,
            holder = draft.holder,
            pages = draft.captured?.pages.orEmpty(),
            originalPdf = draft.captured?.originalPdf,
            pdfPageCount = draft.captured?.pdfPageCount ?: 0,
            existingId = existingId,
            capturedAt = if (draft.captured != null) Clock.System.now().toString() else ""
        )
    }.getOrElse {
        draft.saveError = saveFailedMessage
        onErrorHaptic()
        return
    }
    val stagedAssets = draft.captured?.assetIds().orEmpty()
    onSuccessHaptic()
    onSave(nextState, toastMessage) {
        draft.committedAssets = stagedAssets
    }
}

@Composable
private fun buildDocSavedToast(
    kindName: String,
    isEdit: Boolean,
    reminder: Boolean,
    expiry: String,
    leadDays: Int,
    formatDate: (LocalDate) -> String
): String {
    if (isEdit) return appStringResource(Res.string.doc_edit_saved, kindName)
    val parsed = runCatching { LocalDate.parse(NormalizedNumbers.ascii(expiry.trim())) }.getOrNull()
    return if (reminder && parsed != null) {
        val remindDate = LocalDate.fromEpochDays(parsed.toEpochDays() - leadDays)
        appStringResource(Res.string.doc_add_saved_reminder, kindName, formatDate(remindDate))
    } else {
        appStringResource(Res.string.doc_add_saved, kindName)
    }
}
