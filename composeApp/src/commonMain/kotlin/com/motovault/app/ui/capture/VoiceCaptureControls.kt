package com.motovault.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.motovault.app.application.capture.VoiceCaptureState
import motovault.composeapp.generated.resources.*

@Composable
internal fun VoiceCaptureControls(onText: (String) -> Unit) {
    var local by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    val voice = rememberVoiceCapture(onState = { status = it }, onText = onText)
    val active = status in setOf(VoiceCaptureState.REQUESTING, VoiceCaptureState.LISTENING, VoiceCaptureState.PROCESSING)
    Text(appStringResource(Res.string.capture_voice_privacy), color = TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultChip(appStringResource(Res.string.capture_voice_local), active = local, onClick = { if (!active) local = true })
        VaultChip(appStringResource(Res.string.capture_voice_provider), active = !local, onClick = { if (!active) local = false })
    }
    Text(appStringResource(if (local) Res.string.capture_voice_local_detail else Res.string.capture_voice_provider_detail), color = TextSecondary)
    val message = when (status) {
        VoiceCaptureState.REQUESTING -> Res.string.capture_voice_requesting
        VoiceCaptureState.LISTENING -> Res.string.capture_voice_listening
        VoiceCaptureState.PROCESSING -> Res.string.capture_voice_processing
        VoiceCaptureState.LOCAL_UNAVAILABLE -> Res.string.capture_voice_local_unavailable
        VoiceCaptureState.UNAVAILABLE -> Res.string.capture_voice_unavailable
        VoiceCaptureState.DENIED -> Res.string.capture_voice_denied
        VoiceCaptureState.FAILED -> Res.string.capture_voice_failed
        VoiceCaptureState.NO_SPEECH -> Res.string.capture_voice_empty
        VoiceCaptureState.IDLE -> null
    }
    message?.let { Text(appStringResource(it), color = TextSecondary) }
    if (active) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton(appStringResource(Res.string.capture_voice_stop), enabled = status == VoiceCaptureState.LISTENING, onClick = voice.stop)
            VaultButton(appStringResource(Res.string.capture_voice_cancel), kind = ButtonKind.SECONDARY, onClick = voice.cancel)
        }
    } else {
        val available = if (local) voice.localAvailable else voice.providerAvailable
        if (!available) Text(appStringResource(if (local) Res.string.capture_voice_local_unavailable else Res.string.capture_voice_unavailable), color = TextSecondary)
        VaultButton(appStringResource(Res.string.capture_voice_start), kind = ButtonKind.SECONDARY,
            enabled = available, onClick = { voice.start(local) })
    }
}
