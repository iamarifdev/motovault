package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.application.capture.VoiceCaptureState

internal class VoiceCaptureActions(
    val localAvailable: Boolean, val providerAvailable: Boolean,
    val start: (localOnly: Boolean) -> Unit, val stop: () -> Unit, val cancel: () -> Unit
)
@Composable
internal expect fun rememberVoiceCapture(onState: (VoiceCaptureState) -> Unit, onText: (String) -> Unit): VoiceCaptureActions
