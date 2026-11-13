package com.motovault.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.domain.capture.*
import motovault.composeapp.generated.resources.*

@Composable
internal fun CaptureReviewSheet(input: CaptureInput, state: VaultState, factory: CaptureDraftFactory,
    onDismiss: () -> Unit, onContinue: (String, CapturePrefill) -> Unit
) {
    var text by remember(input) { mutableStateOf(input.text) }
    var source by remember(input) { mutableStateOf(input.source) }
    val draft = remember(text, source, state.captureReceipts, state.ride?.odometer) {
        factory.create(text, source, state.ride?.odometer ?: 0.0, acceptedIds = state.captureReceipts.toSet(),
            originalSharedText = input.text.takeIf { source == CaptureSource.SHARE })
    }
    var amount by remember(draft.id) { mutableStateOf<Long?>(null) }
    var odometer by remember(draft.id) { mutableStateOf<Double?>(null) }
    val chosenAmount = amount ?: draft.amounts.singleOrNull()?.value
    val chosenOdometer = odometer ?: draft.odometers.singleOrNull()?.value
    val label = appStringResource(Res.string.capture_text_label)
    Sheet(appStringResource(Res.string.capture_review_title), onDismiss) {
        Text(appStringResource(Res.string.capture_review_privacy), color = TextSecondary)
        OutlinedTextField(text, { if (it.length <= MAX_CAPTURE_TEXT_LENGTH) text = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }, label = { Text(label) })
        VoiceCaptureControls { transcript ->
            text = transcript
            if (source != CaptureSource.SHARE) source = CaptureSource.VOICE
        }
        if (CaptureIssue.DUPLICATE_SHARE in draft.issues) Text(appStringResource(Res.string.capture_duplicate), color = ErrorColor)
        if (CaptureIssue.MALFORMED_AMOUNT in draft.issues) Text(appStringResource(Res.string.capture_bad_amount), color = ErrorColor)
        if (draft.amounts.size > 1) {
            Text(appStringResource(Res.string.capture_amount_choose))
            draft.amounts.forEach { candidate -> VaultChip(costMoney(candidate.value),
                active = amount == candidate.value, onClick = { amount = candidate.value }) }
        }
        if (draft.odometers.size > 1) {
            Text(appStringResource(Res.string.capture_photo_choose))
            draft.odometers.forEach { candidate -> VaultChip(currentFmt.n(candidate.value, 1),
                active = odometer == candidate.value, onClick = { odometer = candidate.value }) }
        }
        if (draft.service.lines.isNotEmpty()) {
            draft.service.lines.forEach { line -> CATALOG_ITEMS.firstOrNull { it.key == line.line.catalogueId }?.let { Text(appStringResource(it.shortLabel)) } }
        }
        if (draft.service.unmatched.isNotEmpty()) Text(appStringResource(Res.string.capture_unmatched), color = TextSecondary)
        chosenAmount?.let { Text(costMoney(it)) }
        chosenOdometer?.let { Text(currentFmt.n(it, 1)) }
        val allowed = draft.canReview && CaptureIssue.MALFORMED_AMOUNT !in draft.issues
        val targets = listOf(
            Triple("fuel", Res.string.quickadd_fuel, chosenAmount != null),
            Triple("service", Res.string.quickadd_service, draft.service.lines.isNotEmpty()),
            Triple("odometer", Res.string.quickadd_odometer, chosenOdometer != null),
            Triple("expense", Res.string.cost_add, chosenAmount != null)
        )
        targets.forEach { (target, title, available) ->
            VaultButton(appStringResource(title), full = true, kind = ButtonKind.SECONDARY,
                enabled = allowed && available && state.ride != null &&
                    (draft.odometers.size <= 1 || chosenOdometer != null),
                onClick = { onContinue(target, draft.prefill(chosenAmount, chosenOdometer, state.ride?.id)) })
        }
    }
}
