package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.capture.VoiceCaptureState
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

private const val MAX_SMART_INPUT_LENGTH = 600
private val VOICE_BAR_DELAYS_MS = listOf(0, 150, 300)

private class SmartInputVoiceController(
    val listening: Boolean,
    val errorRes: StringResource?,
    val disclosureOpen: Boolean,
    val toggleMic: () -> Unit,
    val cancelVoice: () -> Unit,
    val confirmProvider: () -> Unit,
    val dismissProvider: () -> Unit,
    val clearError: () -> Unit
)

@Composable
internal fun ServiceSmartInputPanel(
    open: Boolean,
    onApply: (List<ParsedLine>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val parsed = remember(text) { SmartParse.parseServiceText(text) }
    val voice = rememberSmartInputVoiceController(
        onTranscript = { cleaned ->
            val combined = if (text.isBlank()) cleaned else "$text, $cleaned"
            text = combined.take(MAX_SMART_INPUT_LENGTH)
        }
    )

    LaunchedEffect(open) {
        if (!open) {
            voice.cancelVoice()
            voice.dismissProvider()
            voice.clearError()
            text = ""
        }
    }

    AnimatedVisibility(visible = open) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmartInputEditorBox(text, focused, { focused = it }) { next ->
                if (next.length <= MAX_SMART_INPUT_LENGTH) text = next
            }
            if (voice.disclosureOpen) {
                VoiceDisclosureCard(onConfirm = voice.confirmProvider, onCancel = voice.dismissProvider)
            }
            voice.errorRes?.let { res ->
                Text(text = appStringResource(res), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold)
            }
            SmartInputControlsRow(
                listening = voice.listening,
                parsedCount = parsed.lines.size,
                onMicClick = voice.toggleMic,
                onCancelVoice = voice.cancelVoice,
                onApply = { onApply(parsed.lines); text = ""; voice.clearError() }
            )
            if (parsed.lines.isNotEmpty()) SmartParsedList(parsed.lines)
            if (parsed.unmatched.isNotEmpty()) {
                Text(
                    text = appStringResource(Res.string.service_smart_unmatched, parsed.unmatched.joinToString(", ")),
                    fontSize = 13.sp,
                    color = WarningColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun rememberSmartInputVoiceController(
    onTranscript: (String) -> Unit
): SmartInputVoiceController {
    val haptic = currentHaptic
    var voiceStatus by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var voiceErrorRes by remember { mutableStateOf<StringResource?>(null) }
    var disclosureOpen by remember { mutableStateOf(false) }

    val voice = rememberLocalFirstVoice(
        onNeedsDisclosure = { disclosureOpen = true },
        onState = { next ->
            voiceStatus = next
            voiceErrorRes = when (next) {
                VoiceCaptureState.FAILED, VoiceCaptureState.NO_SPEECH, VoiceCaptureState.DENIED ->
                    Res.string.capture_voice_failed
                VoiceCaptureState.UNAVAILABLE, VoiceCaptureState.LOCAL_UNAVAILABLE ->
                    Res.string.capture_voice_unavailable
                else -> null
            }
        },
        onText = { transcript ->
            val cleaned = transcript.trim()
            if (cleaned.isNotEmpty()) {
                onTranscript(cleaned)
                haptic.perform(HapticKind.SUCCESS)
            }
        }
    )
    DisposableEffect(Unit) { onDispose { voice.cancel() } }
    val listening = voiceStatus in setOf(
        VoiceCaptureState.REQUESTING,
        VoiceCaptureState.LISTENING,
        VoiceCaptureState.PROCESSING
    )
    return SmartInputVoiceController(
        listening = listening,
        errorRes = voiceErrorRes,
        disclosureOpen = disclosureOpen,
        toggleMic = {
            when {
                listening -> voice.stop()
                voice.localAvailable -> { voiceErrorRes = null; voice.start(true) }
                voice.providerAvailable -> { voiceErrorRes = null; if (VoiceProviderConsent.granted) voice.start(false) else disclosureOpen = !disclosureOpen }
                else -> voiceErrorRes = Res.string.capture_voice_unavailable
            }
        },
        cancelVoice = { voice.cancel(); voiceErrorRes = null },
        confirmProvider = { disclosureOpen = false; voice.startProviderWithConsent() },
        dismissProvider = { disclosureOpen = false },
        clearError = { voiceErrorRes = null }
    )
}

@Composable
private fun SmartInputEditorBox(
    text: String,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit
) {
    val label = appStringResource(Res.string.service_smart_label)
    val placeholder = appStringResource(Res.string.service_example)
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                if (text.isEmpty()) Text(text = placeholder, fontSize = 14.sp, color = TextTertiary)
                inner()
            }
        )
    }
}

@Composable
private fun SmartInputControlsRow(
    listening: Boolean,
    parsedCount: Int,
    onMicClick: () -> Unit,
    onCancelVoice: () -> Unit,
    onApply: () -> Unit
) {
    val f = currentFmt
    val applyText = if (parsedCount > 0) {
        appStringResource(Res.string.service_add_count, f.d(parsedCount.toString()))
    } else {
        appStringResource(Res.string.service_add_action)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VaultButton(
            text = appStringResource(if (listening) Res.string.capture_voice_stop else Res.string.service_smart_speak),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            icon = if (listening) "fi_mic-off.svg" else "fi_mic.svg",
            onClick = onMicClick
        )
        if (listening) {
            VoiceBarsIndicator()
            VaultButton(
                text = appStringResource(Res.string.capture_voice_cancel),
                kind = ButtonKind.SECONDARY,
                size = ButtonSize.SM,
                onClick = onCancelVoice
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        VaultButton(text = applyText, size = ButtonSize.SM, enabled = parsedCount > 0, onClick = onApply)
    }
}

@Composable
private fun VoiceBarsIndicator() {
    val reducedMotion = isReducedMotion
    val label = appStringResource(Res.string.service_smart_listening)
    Row(
        modifier = Modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VOICE_BAR_DELAYS_MS.forEach { delayMs ->
            val scaleY = if (reducedMotion) {
                0.8f
            } else {
                val transition = rememberInfiniteTransition(label = "voiceBar$delayMs")
                val animated by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(350, delayMillis = delayMs), RepeatMode.Reverse),
                    label = "voiceBarScale$delayMs"
                )
                animated
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .graphicsLayer { this.scaleY = scaleY }
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandColor)
            )
        }
    }
}

@Composable
private fun SmartParsedList(lines: List<ParsedLine>) {
    val f = currentFmt
    val shape = RoundedCornerShape(12.dp)
    val listDesc = appStringResource(Res.string.service_smart_parsed_desc)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
            .semantics { contentDescription = listDesc }
    ) {
        lines.forEachIndexed { index, line ->
            if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
            val catalogItem = catalogItemOf(line.item)
            val itemName = if (catalogItem != null) catalogLabel(catalogItem) else line.item
            val qtyPrefix = if (line.qty > 1.0) {
                appStringResource(Res.string.service_smart_qty_prefix, f.d(line.qty.toInt().toString()))
            } else {
                ""
            }
            val priceText = line.price?.let(f::money) ?: appStringResource(Res.string.service_smart_price_unknown)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = itemName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = qtyPrefix + priceText, fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}
