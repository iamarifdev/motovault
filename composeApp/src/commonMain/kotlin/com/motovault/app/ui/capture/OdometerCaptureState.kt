package com.motovault.app.ui

import androidx.compose.runtime.*

/** Root-owned, ephemeral acquisition. Private review may disappear while the native camera is open. */
internal class OdometerCaptureState {
    var open by mutableStateOf(false); private set
    var photo by mutableStateOf<OdometerPhoto?>(null); private set
    var error by mutableStateOf<OdometerCameraIssue?>(null); private set
    private var pending = false
    private var accepting = false
    var launchNative: () -> Unit = {}

    fun start() {
        if (pending) return
        open = true; photo = null; error = null
        pending = true; accepting = true
        try { launchNative() } catch (_: Exception) { failed(OdometerCameraIssue.FAILED) }
    }

    fun received(result: OdometerPhoto) {
        pending = false
        if (accepting) photo = result
    }

    fun failed(issue: OdometerCameraIssue) {
        pending = false
        if (!accepting) return
        if (issue == OdometerCameraIssue.CANCELLED) close() else error = issue
    }

    fun close() {
        open = false; photo = null; error = null; accepting = false
    }
}

internal val LocalOdometerCapture = staticCompositionLocalOf<OdometerCaptureState?> { null }

@Composable
internal fun rememberOdometerCaptureState(): OdometerCaptureState {
    val capture = remember { OdometerCaptureState() }
    val camera = rememberOdometerCamera(capture::received, capture::failed)
    SideEffect { capture.launchNative = camera }
    return capture
}
