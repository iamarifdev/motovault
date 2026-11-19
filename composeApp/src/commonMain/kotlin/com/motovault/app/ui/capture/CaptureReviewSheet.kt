@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.application.capture.VoiceCaptureState
import com.motovault.app.domain.capture.CaptureCandidate
import com.motovault.app.domain.capture.CaptureDraft
import com.motovault.app.domain.capture.CaptureDraftParser
import com.motovault.app.domain.capture.CaptureIssue
import com.motovault.app.domain.capture.CaptureSource
import com.motovault.app.domain.capture.MAX_CAPTURE_TEXT_LENGTH
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.capture_amount_choose
import motovault.composeapp.generated.resources.capture_bad_amount
import motovault.composeapp.generated.resources.capture_duplicate
import motovault.composeapp.generated.resources.capture_text_label
import motovault.composeapp.generated.resources.capture_unmatched
import motovault.composeapp.generated.resources.capture_voice_cancel
import motovault.composeapp.generated.resources.capture_voice_failed
import motovault.composeapp.generated.resources.capture_voice_unavailable
import motovault.composeapp.generated.resources.odo_mic_start
import motovault.composeapp.generated.resources.paste_v4_fuel_found
import motovault.composeapp.generated.resources.paste_v4_lead
import motovault.composeapp.generated.resources.paste_v4_listening
import motovault.composeapp.generated.resources.paste_v4_none
import motovault.composeapp.generated.resources.paste_v4_open_fuel
import motovault.composeapp.generated.resources.paste_v4_open_service
import motovault.composeapp.generated.resources.paste_v4_placeholder
import motovault.composeapp.generated.resources.paste_v4_service_summary
import motovault.composeapp.generated.resources.paste_v4_title
import org.jetbrains.compose.resources.StringResource

private class PasteVoiceController(
    val listening: Boolean,
    val errorRes: StringResource?,
    val disclosureOpen: Boolean,
    val toggleMic: () -> Unit,
    val cancelVoice: () -> Unit,
    val confirmProvider: () -> Unit,
    val dismissProvider: () -> Unit
)

@Composable
internal fun CaptureReviewSheet(
    input: CaptureInput,
    state: VaultState,
    factory: CaptureDraftFactory,
    onDismiss: () -> Unit,
    onContinue: (String, CapturePrefill) -> Unit
) {
    val haptic = currentHaptic
    var text by remember(input) { mutableStateOf(input.text.take(MAX_CAPTURE_TEXT_LENGTH)) }
    var source by remember(input) { mutableStateOf(input.source) }
    val draft = remember(text, source, state.captureReceipts, state.ride?.odometer) {
        factory.create(
            text = text,
            source = source,
            baselineOdometer = state.ride?.odometer ?: 0.0,
            acceptedIds = state.captureReceipts.toSet(),
            originalSharedText = input.text.takeIf { source == CaptureSource.SHARE }
        )
    }
    var selectedAmount by remember(draft.id) { mutableStateOf<Long?>(null) }
    val voice = rememberPasteVoiceController { transcript ->
        text = transcript.take(MAX_CAPTURE_TEXT_LENGTH)
        if (source != CaptureSource.SHARE) source = CaptureSource.VOICE
        haptic.perform(HapticKind.SUCCESS)
    }

    Sheet(title = appStringResource(Res.string.paste_v4_title), onDismiss = onDismiss) {
        Text(
            text = appStringResource(Res.string.paste_v4_lead),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary
        )
        PasteVoiceRow(voice = voice)
        PasteTextArea(
            text = text,
            onTextChange = { next ->
                if (next.length <= MAX_CAPTURE_TEXT_LENGTH) text = next
            }
        )
        PasteWarningsSection(
            draft = draft,
            selectedAmount = selectedAmount,
            onSelectAmount = { selectedAmount = it }
        )
        PasteFoundSection(
            text = text,
            state = state,
            draft = draft,
            chosenAmount = selectedAmount ?: draft.amounts.singleOrNull()?.value,
            onOpenFlow = { flow, prefill ->
                haptic.perform(HapticKind.SUCCESS)
                onContinue(flow, prefill)
            }
        )
    }
}

@Composable
private fun rememberPasteVoiceController(onTranscript: (String) -> Unit): PasteVoiceController {
    var status by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var errorRes by remember { mutableStateOf<StringResource?>(null) }
    var disclosureOpen by remember { mutableStateOf(false) }

    val voice = rememberLocalFirstVoice(
        onNeedsDisclosure = { disclosureOpen = true },
        onState = { next ->
            status = next
            errorRes = when (next) {
                VoiceCaptureState.FAILED, VoiceCaptureState.NO_SPEECH, VoiceCaptureState.DENIED ->
                    Res.string.capture_voice_failed
                VoiceCaptureState.UNAVAILABLE, VoiceCaptureState.LOCAL_UNAVAILABLE ->
                    Res.string.capture_voice_unavailable
                else -> null
            }
        },
        onText = { transcript ->
            val cleaned = transcript.trim()
            if (cleaned.isNotEmpty()) onTranscript(cleaned)
        }
    )
    DisposableEffect(Unit) { onDispose { voice.cancel() } }
    val listening = status in setOf(
        VoiceCaptureState.REQUESTING,
        VoiceCaptureState.LISTENING,
        VoiceCaptureState.PROCESSING
    )
    return PasteVoiceController(
        listening = listening,
        errorRes = errorRes,
        disclosureOpen = disclosureOpen,
        toggleMic = {
            when {
                listening -> voice.stop()
                voice.localAvailable -> { errorRes = null; voice.start(true) }
                voice.providerAvailable -> { errorRes = null; if (VoiceProviderConsent.granted) voice.start(false) else disclosureOpen = !disclosureOpen }
                else -> errorRes = Res.string.capture_voice_unavailable
            }
        },
        cancelVoice = { voice.cancel(); errorRes = null },
        confirmProvider = { disclosureOpen = false; voice.startProviderWithConsent() },
        dismissProvider = { disclosureOpen = false }
    )
}

@Composable
private fun PasteVoiceRow(voice: PasteVoiceController) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VaultButton(
            text = appStringResource(if (voice.listening) Res.string.paste_v4_listening else Res.string.odo_mic_start),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            icon = if (voice.listening) "fi_mic-off.svg" else "fi_mic.svg",
            onClick = voice.toggleMic
        )
        if (voice.listening) {
            VaultButton(
                text = appStringResource(Res.string.capture_voice_cancel),
                kind = ButtonKind.SECONDARY,
                size = ButtonSize.SM,
                onClick = voice.cancelVoice
            )
        }
    }
    if (voice.disclosureOpen) {
        VoiceDisclosureCard(onConfirm = voice.confirmProvider, onCancel = voice.dismissProvider)
    }
    voice.errorRes?.let { res ->
        Text(text = appStringResource(res), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ErrorColor)
    }
}

@Composable
private fun PasteTextArea(text: String, onTextChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val label = appStringResource(Res.string.capture_text_label)
    val placeholder = appStringResource(Res.string.paste_v4_placeholder)
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                if (text.isEmpty()) Text(text = placeholder, fontSize = 14.sp, color = TextTertiary)
                inner()
            }
        )
    }
}

@Composable
private fun PasteWarningsSection(
    draft: CaptureDraft,
    selectedAmount: Long?,
    onSelectAmount: (Long) -> Unit
) {
    if (CaptureIssue.DUPLICATE_SHARE in draft.issues) {
        Text(text = appStringResource(Res.string.capture_duplicate), fontSize = 13.sp, color = ErrorColor)
    }
    if (CaptureIssue.MALFORMED_AMOUNT in draft.issues) {
        Text(text = appStringResource(Res.string.capture_bad_amount), fontSize = 13.sp, color = ErrorColor)
    }
    if (draft.amounts.size > 1) {
        Text(text = appStringResource(Res.string.capture_amount_choose), fontSize = 13.sp, color = TextSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            draft.amounts.forEach { candidate: CaptureCandidate<Long> ->
                VaultChip(
                    text = costMoney(candidate.value),
                    active = selectedAmount == candidate.value,
                    onClick = { onSelectAmount(candidate.value) }
                )
            }
        }
    }
}

@Composable
private fun PasteFoundSection(
    text: String,
    state: VaultState,
    draft: CaptureDraft,
    chosenAmount: Long?,
    onOpenFlow: (String, CapturePrefill) -> Unit
) {
    val allowed = draft.canReview && CaptureIssue.MALFORMED_AMOUNT !in draft.issues && state.ride != null
    val fuelAmount = chosenAmount?.takeIf {
        allowed && CaptureDraftParser.fuelWords.containsMatchIn(text) && draft.service.lines.isEmpty()
    }
    val hasService = allowed && draft.service.lines.isNotEmpty()

    if (fuelAmount != null) {
        PasteFoundCard(
            iconPath = "feather_icon/fi_fuel.svg",
            message = appStringResource(Res.string.paste_v4_fuel_found, costMoney(fuelAmount)),
            actionLabel = appStringResource(Res.string.paste_v4_open_fuel),
            onAction = { onOpenFlow("fuel", draft.prefill(fuelAmount, null, state.ride?.id)) }
        )
    }
    if (hasService) {
        val names = draft.service.lines.mapNotNull { captured ->
            CATALOG_ITEMS.firstOrNull { it.key == captured.line.catalogueId }?.shortLabel
        }.map { appStringResource(it) }.joinToString(", ")
        val serviceTotalPoisha = draft.service.lines.sumOf {
            ((it.line.unitPricePoisha ?: 0) * it.line.quantity).toLong()
        }
        val summary = if (serviceTotalPoisha > 0L) {
            appStringResource(Res.string.paste_v4_service_summary, names, costMoney(serviceTotalPoisha))
        } else {
            names
        }
        PasteFoundCard(
            iconPath = "feather_icon/fi_tool.svg",
            message = summary,
            actionLabel = appStringResource(Res.string.paste_v4_open_service),
            onAction = { onOpenFlow("service", draft.prefill(chosenAmount, null, state.ride?.id)) }
        )
        if (draft.service.unmatched.isNotEmpty()) {
            Text(text = appStringResource(Res.string.capture_unmatched), fontSize = 13.sp, color = TextSecondary)
        }
    }
    if (text.isNotBlank() && allowed && fuelAmount == null && !hasService && draft.amounts.size <= 1) {
        Text(text = appStringResource(Res.string.paste_v4_none), fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun PasteFoundCard(
    iconPath: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BrandTintColor)
            .border(1.dp, BrandLineColor, shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V3Icon(path = iconPath, description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextPrimary
        )
        VaultButton(text = actionLabel, size = ButtonSize.SM, onClick = onAction)
    }
}
