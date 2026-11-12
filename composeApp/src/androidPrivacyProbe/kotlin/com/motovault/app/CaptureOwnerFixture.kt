package com.motovault.app

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.ui.*

/** Consumer-disposal harness, not an authentication or owner-data fixture. */
@Composable
internal fun CaptureOwnerFixture(onCapture: (OdometerCaptureState) -> Unit, onConsumer: (Boolean) -> Unit) {
    val capture = rememberOdometerCaptureState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val state = remember { VaultState(ride = Ride("yamaha", "Synthetic", 2026, 1000.0)) }
    SideEffect { onCapture(capture) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) visible = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) visible = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    AppResourceEnvironment(state.language) {
        VaultTheme("yamaha", Appearance.LIGHT, reduceMotion = true, digits = state.digits, language = state.language) {
            Surface(Modifier.fillMaxSize(), color = CanvasColor) {
                if (visible) {
                    DisposableEffect(Unit) { onConsumer(true); onDispose { onConsumer(false) } }
                    CompositionLocalProvider(LocalOdometerCapture provides capture) {
                        OdometerSheet(state, onDismiss = capture::close, onSave = { _, _, _ -> })
                    }
                }
            }
        }
    }
}
