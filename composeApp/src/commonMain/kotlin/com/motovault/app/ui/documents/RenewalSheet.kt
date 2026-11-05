package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.documents.RENEWAL_FEE_CALCULATOR_URL
import com.motovault.app.domain.documents.RENEWAL_QR_VERIFICATION_URL
import com.motovault.app.domain.documents.RenewalChecklists
import com.motovault.app.domain.model.MAX_RENEWAL_FEE_POISHA
import com.motovault.app.domain.model.MAX_RENEWAL_NOTES_LENGTH
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.stableKind
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import kotlin.math.roundToInt

/**
 * One reviewed renewal. The checklist writes only stable step IDs, the fee is the amount the owner
 * actually paid, and the browser opens only after an explicit tap. Nothing is uploaded here.
 */
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
    val f = currentFmt
    val uriHandler = LocalUriHandler.current
    val todayDate = today()
    val currentExpiry = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
    val baseDate = currentExpiry ?: todayDate
    var expiryText by rememberSaveable(doc.id) {
        mutableStateOf(LocalDate.fromEpochDays(baseDate.toEpochDays() + 365).toString())
    }
    var feeText by rememberSaveable(doc.id) { mutableStateOf("") }
    var notes by rememberSaveable(doc.id) { mutableStateOf("") }
    // A resumed renewal can keep the receipt that was already staged for the same document.
    var stagedReceipt by rememberSaveable(doc.id) { mutableStateOf(initialReceiptId) }
    var savedReceipt by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var linkFailed by remember { mutableStateOf(false) }

    val expiryRequired = appStringResource(Res.string.renewal_expiry_required)
    val expiryOrder = appStringResource(Res.string.renewal_expiry_order)
    val feeInvalid = appStringResource(Res.string.renewal_fee_invalid)
    val saveFailed = appStringResource(Res.string.renewal_save_failed)
    val receiptFailed = appStringResource(Res.string.renewal_receipt_failed)

    DisposableEffect(doc.id) {
        onDispose {
            val staged = stagedReceipt
            if (staged != null && staged != savedReceipt) runCatching { removeDocumentAssets(setOf(staged)) }
        }
    }

    val expiryDate = runCatching { LocalDate.parse(expiryText) }.getOrNull()
    val expiryError = when {
        expiryText.isBlank() || expiryDate == null -> expiryRequired
        currentExpiry != null && expiryDate <= currentExpiry -> expiryOrder
        else -> null
    }
    val feeValue = feeText.trim().takeIf { it.isNotEmpty() }?.let { parseNum(it) }
    val feePoisha = feeValue?.takeIf { it.isFinite() && it >= 0 }?.let { (it * 100).roundToInt() }
        ?.takeIf { it in 0..MAX_RENEWAL_FEE_POISHA }
    val feeError = when {
        feeText.isBlank() -> null
        feeValue == null || !feeValue.isFinite() || feeValue < 0 || feePoisha == null -> feeInvalid
        else -> null
    }
    val canSave = !busy && expiryError == null && feeError == null

    fun openUrl(url: String) {
        runCatching { uriHandler.openUri(url) }.onFailure { linkFailed = true }
    }

    fun save() {
        val date = expiryDate ?: return
        if (!canSave) return
        error = null
        busy = true
        val next = runCatching {
            state.renewDocument(
                documentId = doc.id, newExpiry = date, renewedOn = todayDate, feePoisha = feePoisha,
                receiptId = stagedReceipt, notes = notes,
                sourceCheckedOn = runCatching { LocalDate.parse(guide.sourceCheckedOn) }.getOrNull()
            )
        }.getOrElse {
            busy = false
            error = saveFailed
            return
        }
        onSave(next) { ok ->
            busy = false
            if (ok) savedReceipt = stagedReceipt else error = saveFailed
        }
    }

    val checkedSteps = doc.checkedRenewalSteps
    val progress = if (guide.steps.isEmpty()) 0f else checkedSteps.count { it in guide.stepIds }.toFloat() / guide.steps.size

    Sheet(
        title = appStringResource(Res.string.renewal_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.renewal_save),
                full = true,
                enabled = canSave,
                onClick = { save() }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor2).border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(documentDisplayName(doc.title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                appStringResource(if (doc.expiry.isBlank()) Res.string.renewal_no_expiry else Res.string.renewal_expiry_summary,
                    currentExpiry?.let { f.dateFull(it) } ?: doc.expiry),
                fontSize = 13.sp, color = TextSecondary
            )
            Text(
                appStringResource(Res.string.renewal_source, f.dateFull(LocalDate.parse(guide.sourceCheckedOn))),
                fontSize = 12.sp, color = TextTertiary
            )
        }

        error?.let {
            Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = WarningColor,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(WarningColor.copy(alpha = .12f)).padding(11.dp))
        }
        Text(
            appStringResource(Res.string.renewal_progress, f.n(checkedSteps.count { it in guide.stepIds }.toLong()), f.n(guide.steps.size.toLong())),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
        )
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(BorderColor)) {
            Box(Modifier.fillMaxWidth(progress).height(6.dp).background(BrandColor))
        }

        guide.steps.forEach { step ->
            val copy = renewalStepText(step.id) ?: return@forEach
            val checked = step.id in checkedSteps
            Row(
                modifier = Modifier.fillMaxWidth()
                    .toggleable(value = checked, role = Role.Checkbox, enabled = !busy) {
                        onChecklist(step.id)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(copy.first, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(copy.second, fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
                }
            }
        }

        guide.links.forEach { link ->
            TextButton(onClick = { openUrl(link.url) }, contentPadding = PaddingValues(vertical = 4.dp)) {
                Text(renewalLinkLabel(link.id), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
            }
        }
        Text(appStringResource(Res.string.renewal_offline_note), fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
        if (linkFailed) Text(appStringResource(Res.string.renewal_link_failed), fontSize = 12.sp, lineHeight = 17.sp, color = WarningColor)

        if (guide.feeCalculator) {
            RenewalField(
                label = appStringResource(Res.string.renewal_fee_amount),
                hint = appStringResource(Res.string.renewal_fee_hint),
                error = feeError
            ) {
                RenewalInput(feeText, { if (it.length <= 12) feeText = it }, numeric = true,
                    description = appStringResource(Res.string.renewal_fee_amount))
            }
            TextButton(onClick = { openUrl(RENEWAL_FEE_CALCULATOR_URL) }, contentPadding = PaddingValues(vertical = 4.dp)) {
                Text(appStringResource(Res.string.renewal_fee_calculator), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
            }
        }
        if (guide.lateFineNote) {
            Text(
                appStringResource(Res.string.renewal_late_fine), fontSize = 12.sp, lineHeight = 17.sp, color = WarningColor,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(WarningColor.copy(alpha = .12f)).padding(11.dp)
            )
        }

        RenewalField(
            label = appStringResource(Res.string.renewal_receipt),
            hint = appStringResource(Res.string.renewal_receipt_hint)
        ) {
            val receipt = stagedReceipt
            if (receipt == null) {
                PaymentReceiptPicker(
                    label = appStringResource(Res.string.renewal_receipt_add),
                    onPicked = { id ->
                        stagedReceipt?.let { runCatching { removeDocumentAssets(setOf(it)) } }
                        stagedReceipt = id
                        error = null
                    },
                    onError = { error = receiptFailed }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenOriginalAssetButton(receipt, receiptMimeType(receipt), appStringResource(Res.string.renewal_receipt_view))
                    TextButton(onClick = {
                        runCatching { removeDocumentAssets(setOf(receipt)) }
                        stagedReceipt = null
                    }) { Text(appStringResource(Res.string.renewal_receipt_remove), fontSize = 13.sp, color = WarningColor) }
                }
            }
        }

        RenewalField(
            label = appStringResource(Res.string.renewal_new_expiry),
            error = expiryError
        ) {
            RenewalInput(expiryText, { if (it.length <= 10) expiryText = it }, numeric = true,
                description = appStringResource(Res.string.renewal_new_expiry))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 5).forEach { years ->
                val target = LocalDate.fromEpochDays(baseDate.toEpochDays() + 365 * years).toString()
                VaultChip(
                    text = appStringResource(Res.string.renewal_years_later, f.d(years.toString())),
                    active = expiryText == target,
                    onClick = { expiryText = target }
                )
            }
        }

        RenewalField(
            label = appStringResource(Res.string.renewal_notes),
            hint = appStringResource(Res.string.renewal_notes_hint)
        ) {
            RenewalInput(notes, { if (it.length <= MAX_RENEWAL_NOTES_LENGTH) notes = it }, numeric = false,
                description = appStringResource(Res.string.renewal_notes))
        }

        if (guide.qrVerification) {
            VaultButton(
                text = appStringResource(Res.string.renewal_verify),
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { openUrl(RENEWAL_QR_VERIFICATION_URL) }
            )
            Text(appStringResource(Res.string.renewal_verify_note), fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
        }
        VaultButton(
            text = appStringResource(Res.string.renewal_back),
            kind = ButtonKind.GHOST,
            full = true,
            onClick = onDismiss
        )
    }
}

@Composable
private fun RenewalField(label: String, hint: String? = null, error: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        content()
        val message = error ?: hint
        if (message != null) {
            Text(message, fontSize = 11.sp, lineHeight = 16.sp, color = if (error != null) WarningColor else TextSecondary)
        }
    }
}

@Composable
private fun RenewalInput(value: String, onChange: (String) -> Unit, numeric: Boolean, description: String) {
    val allowed = { next: String -> !numeric || next.all { it.isDigit() || it in ".,-/ " } }
    BasicTextField(
        value = value,
        onValueChange = { next -> if (allowed(next)) onChange(next) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
        cursorBrush = SolidColor(BrandColor),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 12.dp)
            .semantics { contentDescription = description }
    )
}

private fun receiptMimeType(id: String): String = when {
    id.endsWith(".png") -> "image/png"
    id.endsWith(".webp") -> "image/webp"
    else -> "image/jpeg"
}
