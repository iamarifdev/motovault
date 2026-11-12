package com.motovault.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.application.capture.*

@Composable
internal actual fun rememberVoiceCapture(onState: (VoiceCaptureState) -> Unit, onText: (String) -> Unit): VoiceCaptureActions {
    val bridge = IosVoiceCaptureRegistry.bridge
    val stateCallback by rememberUpdatedState(onState)
    val textCallback by rememberUpdatedState(onText)
    val lifecycle = LocalLifecycleOwner.current
    val listener = remember {
        object : VoiceCaptureListener {
            override fun state(value: VoiceCaptureState) { stateCallback(value) }
            override fun transcript(value: String) { textCallback(value) }
        }
    }
    DisposableEffect(bridge, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { bridge?.cancel(); stateCallback(VoiceCaptureState.IDLE) } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); bridge?.cancel() }
    }
    return VoiceCaptureActions(bridge?.supportsLocal() == true, bridge?.available() == true,
        start = { local -> if (bridge == null) stateCallback(VoiceCaptureState.UNAVAILABLE) else bridge.start(local, listener) },
        stop = { bridge?.stop() }, cancel = { bridge?.cancel(); stateCallback(VoiceCaptureState.IDLE) })
}
