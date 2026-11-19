package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.motovault.app.application.capture.VoiceCaptureState

/** The rider agreed to the network speech provider once; the disclosure is not shown again this session. */
internal object VoiceProviderConsent {
    var granted by mutableStateOf(false)
}

/**
 * Local-first voice capture. When the on-device recogniser cannot handle Bangla, the network provider is
 * offered through [onNeedsDisclosure], or started directly when the rider already agreed.
 */
@Composable
internal fun rememberLocalFirstVoice(
    onState: (VoiceCaptureState) -> Unit,
    onText: (String) -> Unit,
    onNeedsDisclosure: () -> Unit
): VoiceCaptureActions {
    val latestState by rememberUpdatedState(onState)
    val latestDisclosure by rememberUpdatedState(onNeedsDisclosure)
    val holder = remember { arrayOfNulls<VoiceCaptureActions>(1) }
    val voice = rememberVoiceCapture(
        onState = { next ->
            val current = holder[0]
            if (next == VoiceCaptureState.LOCAL_UNAVAILABLE && current?.providerAvailable == true) {
                latestState(VoiceCaptureState.IDLE)
                if (VoiceProviderConsent.granted) current.start(false) else latestDisclosure()
            } else {
                latestState(next)
            }
        },
        onText = onText
    )
    holder[0] = voice
    return voice
}

/** Starts voice local-first; asks for the one-time disclosure before using the network provider. */
internal fun VoiceCaptureActions.startLocalFirst(onNeedsDisclosure: () -> Unit, onUnavailable: () -> Unit) {
    when {
        localAvailable -> start(true)
        providerAvailable -> if (VoiceProviderConsent.granted) start(false) else onNeedsDisclosure()
        else -> onUnavailable()
    }
}

/** Records consent and starts the network provider. */
internal fun VoiceCaptureActions.startProviderWithConsent() {
    VoiceProviderConsent.granted = true
    start(false)
}
