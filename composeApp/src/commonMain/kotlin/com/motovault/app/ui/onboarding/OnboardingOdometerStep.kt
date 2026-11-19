package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.capture.VoiceCaptureState
import com.motovault.app.domain.capture.SpokenNumberParser
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.capture_voice_unavailable
import motovault.composeapp.generated.resources.odo_mic_start
import motovault.composeapp.generated.resources.odo_voice_parsed
import motovault.composeapp.generated.resources.odo_voice_retry
import motovault.composeapp.generated.resources.odo_voice_unparsed
import motovault.composeapp.generated.resources.onboarding_camera_btn
import motovault.composeapp.generated.resources.onboarding_mic_listening
import motovault.composeapp.generated.resources.onboarding_odo_or_skip
import motovault.composeapp.generated.resources.onboarding_odometer_title
import motovault.composeapp.generated.resources.onboarding_service_timing_label
import motovault.composeapp.generated.resources.onboarding_skip_odo
import motovault.composeapp.generated.resources.onboarding_start_btn
import motovault.composeapp.generated.resources.onboarding_timing_months
import motovault.composeapp.generated.resources.onboarding_timing_recent
import motovault.composeapp.generated.resources.onboarding_timing_unknown
import motovault.composeapp.generated.resources.onboarding_unit_km
import kotlin.math.roundToLong

private sealed interface OnboardingVoiceNote {
    data class Parsed(val transcript: String, val km: Double) : OnboardingVoiceNote
    data class Unparsed(val transcript: String) : OnboardingVoiceNote
    data object Retry : OnboardingVoiceNote
    data object Unavailable : OnboardingVoiceNote
}

@Composable
internal fun OnboardingOdometerStep(
    odoRaw: String,
    odoVal: Double,
    serviceTiming: OnboardingServiceTiming,
    capture: OdometerCaptureState,
    onSetOdo: (String) -> Unit,
    onKeyPress: (String) -> Unit,
    onServiceTiming: (OnboardingServiceTiming) -> Unit,
    onFinish: (Double) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var voiceStatus by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var voiceNote by remember { mutableStateOf<OnboardingVoiceNote?>(null) }
    var disclosureOpen by remember { mutableStateOf(false) }

    val voice = rememberLocalFirstVoice(
        onNeedsDisclosure = { disclosureOpen = true },
        onState = { next ->
            voiceStatus = next
            when (next) {
                VoiceCaptureState.FAILED, VoiceCaptureState.NO_SPEECH, VoiceCaptureState.DENIED -> voiceNote = OnboardingVoiceNote.Retry
                VoiceCaptureState.UNAVAILABLE, VoiceCaptureState.LOCAL_UNAVAILABLE -> voiceNote = OnboardingVoiceNote.Unavailable
                else -> {}
            }
        },
        onText = { transcript ->
            val spoken = SpokenNumberParser.parse(transcript)
            if (spoken == null || spoken <= 0L || spoken >= MAX_SPOKEN_ODOMETER_KM) {
                voiceNote = OnboardingVoiceNote.Unparsed(transcript)
            } else {
                onSetOdo(spoken.toString())
                voiceNote = OnboardingVoiceNote.Parsed(transcript, spoken.toDouble())
                haptic.perform(HapticKind.SUCCESS)
            }
        }
    )
    val voiceActive = voiceStatus in setOf(VoiceCaptureState.REQUESTING, VoiceCaptureState.LISTENING, VoiceCaptureState.PROCESSING)
    DisposableEffect(Unit) { onDispose { voice.cancel() } }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = appStringResource(Res.string.onboarding_odometer_title), fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        OnboardingOdoHero(raw = odoRaw, odoVal = odoVal, f = f)
        OnboardingCaptureBar(
            voiceActive = voiceActive,
            onCameraClick = {
                if (voiceActive) voice.cancel()
                disclosureOpen = false
                if (capture.open) capture.close() else capture.start()
            },
            onMicClick = {
                if (voiceActive) { voice.stop(); return@OnboardingCaptureBar }
                capture.close()
                voiceNote = null
                when {
                    voice.localAvailable -> voice.start(true)
                    voice.providerAvailable -> if (VoiceProviderConsent.granted) voice.start(false) else disclosureOpen = !disclosureOpen
                    else -> voiceNote = OnboardingVoiceNote.Unavailable
                }
            }
        )
        if (disclosureOpen) {
            VoiceDisclosureCard(
                onConfirm = { disclosureOpen = false; voice.startProviderWithConsent() },
                onCancel = { disclosureOpen = false }
            )
        }
        voiceNote?.let { OnboardingVoiceBanner(note = it, f = f) }
        AnimatedVisibility(visible = capture.open) {
            OdometerCameraCard(capture = capture, onApply = { km ->
                haptic.perform(HapticKind.SUCCESS)
                onSetOdo(km.roundToLong().toString())
                capture.close()
            })
        }
        OnboardingServiceTimingSection(serviceTiming = serviceTiming, onSelect = onServiceTiming)
        VaultButton(
            text = appStringResource(if (odoVal <= 0.0) Res.string.onboarding_odo_or_skip else Res.string.onboarding_start_btn),
            full = true,
            enabled = odoVal > 0.0 && odoVal <= MAX_ONBOARDING_ODOMETER_KM && !capture.open,
            onClick = { onFinish(odoVal) }
        )
        Text(
            text = appStringResource(Res.string.onboarding_skip_odo),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onFinish(0.0) }.padding(vertical = 4.dp)
        )
        if (!capture.open) {
            VaultKeypad(onKey = onKeyPress, decimal = false, compact = false)
        }
    }
}

/** v4 voice accepts 0 < km < 1,000,000. */
private const val MAX_SPOKEN_ODOMETER_KM = 1_000_000L

/** Same upper bound as VaultState.create, so finishing can never throw. */
internal const val MAX_ONBOARDING_ODOMETER_KM = 9_999_999.0

@Composable
private fun OnboardingOdoHero(raw: String, odoVal: Double, f: Fmt) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = f.editableNumber(raw),
            fontSize = 52.sp,
            lineHeight = 60.sp,
            fontWeight = FontWeight.Bold,
            color = if (odoVal > 0.0) TextPrimary else TextTertiary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = appStringResource(Res.string.onboarding_unit_km),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun OnboardingCaptureBar(voiceActive: Boolean, onCameraClick: () -> Unit, onMicClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultButton(
            text = appStringResource(Res.string.onboarding_camera_btn),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            icon = "fi_camera.svg",
            full = true,
            onClick = onCameraClick,
            modifier = Modifier.weight(1f)
        )
        VaultButton(
            text = appStringResource(if (voiceActive) Res.string.onboarding_mic_listening else Res.string.odo_mic_start),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            icon = if (voiceActive) "fi_mic-off.svg" else "fi_mic.svg",
            full = true,
            onClick = onMicClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OnboardingVoiceBanner(note: OnboardingVoiceNote, f: Fmt) {
    val text = when (note) {
        is OnboardingVoiceNote.Parsed -> appStringResource(Res.string.odo_voice_parsed, note.transcript, f.km(note.km))
        is OnboardingVoiceNote.Unparsed -> appStringResource(Res.string.odo_voice_unparsed, note.transcript)
        OnboardingVoiceNote.Retry -> appStringResource(Res.string.odo_voice_retry)
        OnboardingVoiceNote.Unavailable -> appStringResource(Res.string.capture_voice_unavailable)
    }
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = BrandColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingServiceTimingSection(
    serviceTiming: OnboardingServiceTiming,
    onSelect: (OnboardingServiceTiming) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = appStringResource(Res.string.onboarding_service_timing_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultChip(
                text = appStringResource(Res.string.onboarding_timing_recent),
                onClick = { onSelect(OnboardingServiceTiming.RECENT) },
                active = serviceTiming == OnboardingServiceTiming.RECENT
            )
            VaultChip(
                text = appStringResource(Res.string.onboarding_timing_months),
                onClick = { onSelect(OnboardingServiceTiming.MONTHS) },
                active = serviceTiming == OnboardingServiceTiming.MONTHS
            )
            VaultChip(
                text = appStringResource(Res.string.onboarding_timing_unknown),
                onClick = { onSelect(OnboardingServiceTiming.UNKNOWN) },
                active = serviceTiming == OnboardingServiceTiming.UNKNOWN
            )
        }
    }
}
