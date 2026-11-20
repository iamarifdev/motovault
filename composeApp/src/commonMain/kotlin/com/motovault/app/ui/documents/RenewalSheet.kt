@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.documents.RenewalChecklists
import com.motovault.app.domain.documents.RenewalGuide
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.MAX_RENEWAL_FEE_POISHA
import com.motovault.app.domain.model.MAX_RENEWAL_NOTES_LENGTH
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.stableKind
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.renewal_back
import motovault.composeapp.generated.resources.renewal_expiry_after_today
import motovault.composeapp.generated.resources.renewal_expiry_order
import motovault.composeapp.generated.resources.renewal_expiry_required
import motovault.composeapp.generated.resources.renewal_fee_hint
import motovault.composeapp.generated.resources.renewal_fee_invalid
import motovault.composeapp.generated.resources.renewal_fee_optional
import motovault.composeapp.generated.resources.renewal_fee_placeholder
import motovault.composeapp.generated.resources.renewal_new_expiry
import motovault.composeapp.generated.resources.renewal_no_expiry
import motovault.composeapp.generated.resources.renewal_notes
import motovault.composeapp.generated.resources.renewal_notes_hint
import motovault.composeapp.generated.resources.renewal_previous_summary
import motovault.composeapp.generated.resources.renewal_receipt_add
import motovault.composeapp.generated.resources.renewal_receipt_failed
import motovault.composeapp.generated.resources.renewal_receipt_hint
import motovault.composeapp.generated.resources.renewal_receipt_optional
import motovault.composeapp.generated.resources.renewal_receipt_remove
import motovault.composeapp.generated.resources.renewal_receipt_view
import motovault.composeapp.generated.resources.renewal_save
import motovault.composeapp.generated.resources.renewal_save_failed
import motovault.composeapp.generated.resources.renewal_title
import motovault.composeapp.generated.resources.renewal_years_later
import kotlin.math.roundToInt

private val RENEWAL_YEAR_PRESETS = listOf(1, 2, 5)

@Composable
internal fun RenewalSheet(
    state: VaultState,
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onChecklist: (String) -> Unit,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit,
    initialReceiptId: String? = null
) {
    val guide = RenewalChecklists.of(doc.stableKind()) ?: return
    val haptic = currentHaptic
    val todayDate = today()
    val currentExpiry = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
    // Renewing a long-expired document must still land after today (the domain requires it).
    val baseDate = currentExpiry?.takeIf { it > todayDate } ?: todayDate

    var expiryText by rememberSaveable(doc.id) { mutableStateOf(LocalDate.fromEpochDays(baseDate.toEpochDays() + 365).toString()) }
    var feeText by rememberSaveable(doc.id) { mutableStateOf("") }
    var notes by rememberSaveable(doc.id) { mutableStateOf("") }
    var stagedReceipt by rememberSaveable(doc.id) { mutableStateOf(initialReceiptId) }
    var savedReceipt by remember { mutableStateOf<String?>(null) }
    var guideOpen by rememberSaveable(doc.id) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var linkFailed by remember { mutableStateOf(false) }

    DisposableEffect(doc.id) {
        onDispose {
            val staged = stagedReceipt
            if (staged != null && staged != savedReceipt) runCatching { removeDocumentAssets(setOf(staged)) }
        }
    }

    val eval = evaluateRenewalInput(expiryText, feeText, currentExpiry, todayDate)
    val saveFailed = appStringResource(Res.string.renewal_save_failed)
    val buttonLabel = eval.expiryError ?: eval.feeError ?: appStringResource(Res.string.renewal_save)

    Sheet(
        title = appStringResource(Res.string.renewal_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            VaultButton(
                text = buttonLabel,
                full = true,
                enabled = !busy && eval.expiryError == null && eval.feeError == null,
                onClick = {
                    submitRenewal(state, doc, guide, eval, stagedReceipt, notes, todayDate, saveFailed, onSave, { savedReceipt = it }) { nextBusy, nextError, saved ->
                        busy = nextBusy; error = nextError
                        if (saved != null) { savedReceipt = saved; haptic.perform(HapticKind.SUCCESS) }
                        else if (nextError != null) haptic.perform(HapticKind.ERROR)
                    }
                }
            )
        }
    ) {
        RenewalSheetBody(
            doc = doc, guide = guide, currentExpiry = currentExpiry, baseDate = baseDate,
            expiryText = expiryText, eval = eval, feeText = feeText, stagedReceipt = stagedReceipt,
            notes = notes, guideOpen = guideOpen, error = error, busy = busy, linkFailed = linkFailed,
            onExpiryChange = { expiryText = it }, onFeeChange = { feeText = it },
            onReceiptChange = { stagedReceipt = it }, onClearError = { error = null },
            onReceiptError = { error = it }, onNotesChange = { notes = it },
            onToggleGuide = { guideOpen = !guideOpen }, onChecklist = onChecklist,
            onLinkFailed = { linkFailed = true }, onDismiss = onDismiss
        )
    }
}

@Composable
private fun RenewalSheetBody(
    doc: VaultDocument, guide: RenewalGuide, currentExpiry: LocalDate?, baseDate: LocalDate,
    expiryText: String, eval: RenewalEvaluation, feeText: String, stagedReceipt: String?,
    notes: String, guideOpen: Boolean, error: String?, busy: Boolean, linkFailed: Boolean,
    onExpiryChange: (String) -> Unit, onFeeChange: (String) -> Unit,
    onReceiptChange: (String?) -> Unit, onClearError: () -> Unit, onReceiptError: (String) -> Unit,
    onNotesChange: (String) -> Unit, onToggleGuide: () -> Unit, onChecklist: (String) -> Unit,
    onLinkFailed: () -> Unit, onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val receiptFailed = appStringResource(Res.string.renewal_receipt_failed)
    RenewalSummaryCard(doc, currentExpiry)
    error?.let { msg -> Text(text = msg, fontSize = 13.sp, color = WarningColor, fontWeight = FontWeight.SemiBold) }
    RenewalExpirySection(expiryText, eval.expiryError, baseDate, onExpiryChange)
    if (guide.feeCalculator) RenewalFeeField(feeText, eval.feeError) { if (it.length <= 12) onFeeChange(it) }
    RenewalReceiptSection(
        receipt = stagedReceipt,
        onPicked = { id -> stagedReceipt?.let { runCatching { removeDocumentAssets(setOf(it)) } }; onReceiptChange(id); onClearError() },
        onPickError = { onReceiptError(receiptFailed) },
        onRemove = { stagedReceipt?.let { runCatching { removeDocumentAssets(setOf(it)) } }; onReceiptChange(null) }
    )
    RenewalField(label = appStringResource(Res.string.renewal_notes)) {
        RenewalInput(notes, { if (it.length <= MAX_RENEWAL_NOTES_LENGTH) onNotesChange(it) },
            appStringResource(Res.string.renewal_notes_hint), numeric = false, attention = false,
            description = appStringResource(Res.string.renewal_notes))
    }
    RenewalGuideSection(guide, doc.checkedRenewalSteps, guideOpen, linkFailed, busy, onToggleGuide, onChecklist) { url ->
        runCatching { uriHandler.openUri(url) }.onFailure { onLinkFailed() }
    }
    RenewalBackLink(onDismiss)
}

private data class RenewalEvaluation(
    val expiryDate: LocalDate?,
    val feePoisha: Int?,
    val expiryError: String?,
    val feeError: String?
)

@Composable
private fun evaluateRenewalInput(expiryText: String, feeText: String, currentExpiry: LocalDate?, todayDate: LocalDate): RenewalEvaluation {
    val expiryDate = runCatching { LocalDate.parse(NormalizedNumbers.ascii(expiryText.trim())) }.getOrNull()
    val expiryError = when {
        expiryText.isBlank() || expiryDate == null -> appStringResource(Res.string.renewal_expiry_required)
        currentExpiry != null && expiryDate <= currentExpiry -> appStringResource(Res.string.renewal_expiry_order)
        expiryDate <= todayDate -> appStringResource(Res.string.renewal_expiry_after_today)
        else -> null
    }
    val trimmedFee = feeText.trim()
    val feeValue = trimmedFee.takeIf { it.isNotEmpty() }?.let { parseNum(it) }
    val feePoisha = feeValue?.takeIf { it.isFinite() && it >= 0 }?.let { (it * 100).roundToInt() }
        ?.takeIf { it in 0..MAX_RENEWAL_FEE_POISHA }
    val feeError = if (trimmedFee.isNotEmpty() && (feeValue == null || !feeValue.isFinite() || feeValue < 0 || feePoisha == null)) {
        appStringResource(Res.string.renewal_fee_invalid)
    } else null
    return RenewalEvaluation(expiryDate, feePoisha, expiryError, feeError)
}

private fun submitRenewal(
    state: VaultState,
    doc: VaultDocument,
    guide: RenewalGuide,
    eval: RenewalEvaluation,
    stagedReceipt: String?,
    notes: String,
    todayDate: LocalDate,
    saveFailed: String,
    onSave: (VaultState, (Boolean) -> Unit) -> Unit,
    onReceiptCommitting: (String?) -> Unit,
    onStatus: (busy: Boolean, error: String?, savedReceipt: String?) -> Unit
) {
    val date = eval.expiryDate ?: return
    onStatus(true, null, null)
    val next = runCatching {
        state.renewDocument(
            documentId = doc.id,
            newExpiry = date,
            renewedOn = todayDate,
            feePoisha = eval.feePoisha,
            receiptId = stagedReceipt,
            notes = notes,
            sourceCheckedOn = runCatching { LocalDate.parse(guide.sourceCheckedOn) }.getOrNull()
        )
    }.getOrElse {
        onStatus(false, saveFailed, null)
        return
    }
    // Treat the receipt as owned by the save while it runs, so closing the sheet cannot delete it.
    onReceiptCommitting(stagedReceipt)
    onSave(next) { ok ->
        if (ok) onStatus(false, null, stagedReceipt) else { onReceiptCommitting(null); onStatus(false, saveFailed, null) }
    }
}

@Composable
private fun RenewalSummaryCard(doc: VaultDocument, currentExpiry: LocalDate?) {
    val f = currentFmt
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor2)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = documentDisplayName(doc.kind.ifBlank { doc.title }), fontSize = 13.sp, color = TextSecondary)
        Text(
            text = currentExpiry?.let { appStringResource(Res.string.renewal_previous_summary, f.dateFull(it)) }
                ?: appStringResource(Res.string.renewal_no_expiry),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun RenewalExpirySection(
    expiryText: String,
    expiryError: String?,
    baseDate: LocalDate,
    onExpiryChange: (String) -> Unit
) {
    val f = currentFmt
    RenewalField(label = appStringResource(Res.string.renewal_new_expiry), error = expiryError) {
        RenewalInput(
            value = expiryText,
            onChange = { if (it.length <= 10) onExpiryChange(it) },
            placeholder = "",
            numeric = true,
            attention = true,
            description = appStringResource(Res.string.renewal_new_expiry)
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RENEWAL_YEAR_PRESETS.forEach { years ->
            val target = LocalDate.fromEpochDays(baseDate.toEpochDays() + 365 * years).toString()
            VaultChip(
                text = appStringResource(Res.string.renewal_years_later, f.d(years.toString())),
                active = expiryText.trim() == target,
                onClick = { onExpiryChange(target) }
            )
        }
    }
}

@Composable
private fun RenewalFeeField(feeText: String, feeError: String?, onFeeChange: (String) -> Unit) {
    RenewalField(
        label = appStringResource(Res.string.renewal_fee_optional),
        hint = appStringResource(Res.string.renewal_fee_hint),
        error = feeError
    ) {
        RenewalInput(
            value = feeText,
            onChange = onFeeChange,
            placeholder = appStringResource(Res.string.renewal_fee_placeholder),
            numeric = true,
            attention = false,
            description = appStringResource(Res.string.renewal_fee_optional)
        )
    }
}

@Composable
private fun RenewalReceiptSection(
    receipt: String?,
    onPicked: (String) -> Unit,
    onPickError: () -> Unit,
    onRemove: () -> Unit
) {
    RenewalField(
        label = appStringResource(Res.string.renewal_receipt_optional),
        hint = appStringResource(Res.string.renewal_receipt_hint)
    ) {
        if (receipt == null) {
            PaymentReceiptPicker(
                label = appStringResource(Res.string.renewal_receipt_add),
                onPicked = onPicked,
                onError = onPickError
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OpenOriginalAssetButton(receipt, receiptMimeType(receipt), appStringResource(Res.string.renewal_receipt_view))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onRemove)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    V3Icon(path = "feather_icon/fi_trash-2.svg", description = null, modifier = Modifier.size(16.dp), tint = ErrorColor)
                    Text(text = appStringResource(Res.string.renewal_receipt_remove), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ErrorColor)
                }
            }
        }
    }
}

@Composable
private fun RenewalBackLink(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Text(text = appStringResource(Res.string.renewal_back), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
    }
}
