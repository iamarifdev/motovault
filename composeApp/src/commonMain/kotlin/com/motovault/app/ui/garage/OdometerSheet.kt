package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.capture.VoiceCaptureState
import com.motovault.app.domain.capture.MAX_CAPTURE_ODOMETER_KM
import com.motovault.app.domain.capture.SpokenNumberParser
import com.motovault.app.domain.model.Ride
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.capture_odo_invalid
import motovault.composeapp.generated.resources.capture_photo_action
import motovault.composeapp.generated.resources.capture_voice_unavailable
import motovault.composeapp.generated.resources.garage_unit_km
import motovault.composeapp.generated.resources.odo_current
import motovault.composeapp.generated.resources.odo_error_below_current
import motovault.composeapp.generated.resources.odo_estimated
import motovault.composeapp.generated.resources.odo_fix
import motovault.composeapp.generated.resources.odo_last_updated_days
import motovault.composeapp.generated.resources.odo_last_updated_today
import motovault.composeapp.generated.resources.odo_mic_start
import motovault.composeapp.generated.resources.odo_mic_stop
import motovault.composeapp.generated.resources.odo_ok_save
import motovault.composeapp.generated.resources.odo_save
import motovault.composeapp.generated.resources.odo_saved
import motovault.composeapp.generated.resources.odo_title
import motovault.composeapp.generated.resources.odo_today_estimate
import motovault.composeapp.generated.resources.odo_typed
import motovault.composeapp.generated.resources.odo_voice_parsed
import motovault.composeapp.generated.resources.odo_voice_retry
import motovault.composeapp.generated.resources.odo_voice_unparsed
import motovault.composeapp.generated.resources.odometer_bump_chip

private val ODO_BUMPS = listOf(10, 50, 100, 500)

private sealed interface OdoVoiceBanner {
    data class Parsed(val transcript: String, val km: Double) : OdoVoiceBanner
    data class Unparsed(val transcript: String) : OdoVoiceBanner
    data object Retry : OdoVoiceBanner
    data object Unavailable : OdoVoiceBanner
}

@Composable
fun OdometerSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit,
    initialOdometer: Double? = null
) {
    val ride = state.ride ?: return
    val f = currentFmt
    val haptic = currentHaptic
    val currentDay = today()
    val estimate = remember(ride, state.logs, currentDay) { WearCalculator.estimateOdometer(ride, state.logs, currentDay) }
    val entry = rememberNumberEntry(
        initialValues = mapOf("odo" to (initialOdometer?.toLong()?.toString() ?: estimate.toLong().toString())),
        firstField = "odo"
    )
    var touched by remember { mutableStateOf(initialOdometer != null) }
    var voiceStatus by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var voiceBanner by remember { mutableStateOf<OdoVoiceBanner?>(null) }
    var providerDisclosureOpen by remember { mutableStateOf(false) }

    val capture = LocalOdometerCapture.current ?: rememberOdometerCaptureState()
    val voice = rememberLocalFirstVoice(
        onNeedsDisclosure = { providerDisclosureOpen = true },
        onState = { next ->
            voiceStatus = next
            when (next) {
                VoiceCaptureState.FAILED, VoiceCaptureState.NO_SPEECH, VoiceCaptureState.DENIED -> voiceBanner = OdoVoiceBanner.Retry
                VoiceCaptureState.UNAVAILABLE, VoiceCaptureState.LOCAL_UNAVAILABLE -> voiceBanner = OdoVoiceBanner.Unavailable
                else -> {}
            }
        },
        onText = { transcript ->
            val spoken = SpokenNumberParser.parse(transcript)
            if (spoken == null || spoken <= 0L) {
                voiceBanner = OdoVoiceBanner.Unparsed(transcript)
            } else {
                entry.set("odo", spoken.toString())
                touched = true
                voiceBanner = OdoVoiceBanner.Parsed(transcript, spoken.toDouble())
                haptic.perform(HapticKind.SUCCESS)
            }
        }
    )
    val voiceActive = voiceStatus in setOf(VoiceCaptureState.REQUESTING, VoiceCaptureState.LISTENING, VoiceCaptureState.PROCESSING)
    DisposableEffect(Unit) { onDispose { voice.cancel() } }

    val parsedOdo = parseNum(entry.values["odo"].orEmpty())
    val odoVal = parsedOdo ?: 0.0
    val badError = when {
        parsedOdo == null || odoVal !in 0.0..MAX_CAPTURE_ODOMETER_KM -> appStringResource(Res.string.capture_odo_invalid)
        odoVal < ride.odometer -> appStringResource(Res.string.odo_error_below_current, f.n(ride.odometer.toLong()))
        else -> null
    }
    val savedMessage = appStringResource(Res.string.odo_saved, f.km(odoVal))

    Sheet(
        title = appStringResource(Res.string.odo_title),
        onDismiss = onDismiss,
        actions = {
            OdometerHeaderActions(
                cameraOpen = capture.open,
                voiceActive = voiceActive,
                onCameraToggle = {
                    if (voiceActive) voice.cancel()
                    providerDisclosureOpen = false
                    if (capture.open) capture.close() else capture.start()
                },
                onMicClick = {
                    if (voiceActive) { voice.stop(); return@OdometerHeaderActions }
                    capture.close()
                    voiceBanner = null
                    when {
                        voice.localAvailable -> voice.start(true)
                        voice.providerAvailable -> if (VoiceProviderConsent.granted) voice.start(false) else providerDisclosureOpen = !providerDisclosureOpen
                        else -> voiceBanner = OdoVoiceBanner.Unavailable
                    }
                }
            )
        },
        footer = {
            val buttonText = when {
                badError != null -> appStringResource(Res.string.odo_fix)
                touched -> appStringResource(Res.string.odo_save)
                else -> appStringResource(Res.string.odo_ok_save)
            }
            VaultButton(
                text = buttonText, icon = "fi_check.svg", full = true, enabled = badError == null && !capture.open,
                onClick = { onSave(state.copy(ride = ride.copy(odometer = odoVal, odometerConfirmedDate = currentDay.toString())), savedMessage) {} }
            )
            if (!capture.open) VaultKeypad(onKey = { k -> touched = true; entry.press(k) }, decimal = false)
        }
    ) {
        OdometerSheetBody(
            ride = ride, odoVal = odoVal, estimate = estimate, touched = touched, currentDay = currentDay,
            badError = badError, voiceBanner = voiceBanner, providerDisclosureOpen = providerDisclosureOpen,
            capture = capture, f = f,
            onBump = { bump -> entry.set("odo", (odoVal.toLong() + bump).toString()); touched = true },
            onResetEstimate = { entry.set("odo", estimate.toLong().toString()); touched = false },
            onConfirmVoiceProvider = { providerDisclosureOpen = false; voice.startProviderWithConsent() },
            onCancelVoiceProvider = { providerDisclosureOpen = false },
            onApplyPhoto = { value -> entry.set("odo", value.toLong().toString()); touched = true; capture.close() }
        )
    }
}

@Composable
private fun OdometerHeaderActions(
    cameraOpen: Boolean,
    voiceActive: Boolean,
    onCameraToggle: () -> Unit,
    onMicClick: () -> Unit
) {
    VaultIconButton(
        icon = "fi_camera.svg",
        label = appStringResource(Res.string.capture_photo_action),
        tone = if (cameraOpen) IconTone.TINT else IconTone.PLAIN,
        onClick = onCameraToggle
    )
    VaultIconButton(
        icon = if (voiceActive) "fi_mic-off.svg" else "fi_mic.svg",
        label = appStringResource(if (voiceActive) Res.string.odo_mic_stop else Res.string.odo_mic_start),
        tone = if (voiceActive) IconTone.TINT else IconTone.PLAIN,
        onClick = onMicClick
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OdometerSheetBody(
    ride: Ride,
    odoVal: Double,
    estimate: Double,
    touched: Boolean,
    currentDay: LocalDate,
    badError: String?,
    voiceBanner: OdoVoiceBanner?,
    providerDisclosureOpen: Boolean,
    capture: OdometerCaptureState,
    f: Fmt,
    onBump: (Int) -> Unit,
    onResetEstimate: () -> Unit,
    onConfirmVoiceProvider: () -> Unit,
    onCancelVoiceProvider: () -> Unit,
    onApplyPhoto: (Double) -> Unit
) {
    if (providerDisclosureOpen) {
        VoiceDisclosureCard(onConfirm = onConfirmVoiceProvider, onCancel = onCancelVoiceProvider)
    }
    if (voiceBanner != null) {
        val bannerText = when (voiceBanner) {
            is OdoVoiceBanner.Parsed -> appStringResource(Res.string.odo_voice_parsed, voiceBanner.transcript, f.km(voiceBanner.km))
            is OdoVoiceBanner.Unparsed -> appStringResource(Res.string.odo_voice_unparsed, voiceBanner.transcript)
            OdoVoiceBanner.Retry -> appStringResource(Res.string.odo_voice_retry)
            OdoVoiceBanner.Unavailable -> appStringResource(Res.string.capture_voice_unavailable)
        }
        Text(text = bannerText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandColor, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    OdometerHeroBlock(ride = ride, odoVal = odoVal, touched = touched, today = currentDay, f = f)
    if (badError != null) {
        Text(text = badError, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ODO_BUMPS.forEach { bump ->
            VaultChip(text = appStringResource(Res.string.odometer_bump_chip, f.d(bump.toString())), onClick = { onBump(bump) })
        }
        VaultChip(text = appStringResource(Res.string.odo_estimated), onClick = onResetEstimate)
    }
    if (capture.open) {
        val shape = RoundedCornerShape(16.dp)
        Column(modifier = Modifier.fillMaxWidth().clip(shape).background(SurfaceColor2).border(1.dp, BorderColor, shape).padding(12.dp)) {
            OdometerPhotoReviewBody(capture.photo, capture.error, ride.odometer, onApply = onApplyPhoto, onClose = capture::close, onRetry = capture::start)
        }
    }
}

@Composable
private fun OdometerHeroBlock(ride: Ride, odoVal: Double, touched: Boolean, today: LocalDate, f: Fmt) {
    val confirmedDate = ride.odometerConfirmedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val updatedSubline = if (confirmedDate != null) {
        val ago = (today.toEpochDays() - confirmedDate.toEpochDays()).coerceAtLeast(0)
        if (ago == 0) appStringResource(Res.string.odo_last_updated_today, f.km(ride.odometer))
        else appStringResource(Res.string.odo_last_updated_days, f.days(ago), f.km(ride.odometer))
    } else appStringResource(Res.string.odo_current, f.km(ride.odometer))

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = appStringResource(if (touched) Res.string.odo_typed else Res.string.odo_today_estimate), fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.Bottom) {
            CountUp(value = odoVal, format = { n -> f.n(n, if (odoVal % 1.0 == 0.0) 0 else 1) }, style = TextStyle(fontSize = 52.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold, color = TextPrimary))
            Spacer(Modifier.width(8.dp))
            Text(text = appStringResource(Res.string.garage_unit_km), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(bottom = 10.dp))
        }
        Text(text = updatedSubline, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}
