package com.motovault.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.application.capture.ShareCaptureInbox
import com.motovault.app.application.capture.IosSharedCaptureRegistry
import com.motovault.app.domain.capture.CaptureSource

internal data class CaptureInput(val text: String = "", val source: CaptureSource = CaptureSource.PASTE)

@Composable
internal fun CaptureIntake(enabled: Boolean, onOpen: (CaptureInput) -> Unit) {
    val pending by ShareCaptureInbox.pending.collectAsState()
    val lifecycle = LocalLifecycleOwner.current
    var resume by remember { mutableStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resume++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled, resume) {
        if (enabled && ShareCaptureInbox.pending.value == null) {
            IosSharedCaptureRegistry.bridge?.takeText()?.let { ShareCaptureInbox.offer(it) }
        }
    }
    LaunchedEffect(pending, enabled) {
        val text = pending ?: return@LaunchedEffect
        if (enabled && ShareCaptureInbox.take(text)) onOpen(CaptureInput(text, CaptureSource.SHARE))
    }
}
