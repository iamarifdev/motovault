@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package com.motovault.app.ui.redesign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.UIKit.*

private object InspectionScreenBrightness {
    var leases = 0
    var original = 0.5
    var active = true
    fun apply() {
        val requested = if (leases > 0 && active) 1.0 else original
        UIScreen.mainScreen.brightness = requested
        if (kotlin.native.Platform.isDebugBinary && NSProcessInfo.processInfo.arguments.contains("--motovault-runtime-probe")) {
            println("MOTOVAULT_BRIGHTNESS requested=$requested observed=${UIScreen.mainScreen.brightness} leases=$leases active=$active")
        }
    }
}

@Composable
actual fun InspectionBrightness() {
    DisposableEffect(Unit) {
        val state = InspectionScreenBrightness
        var disposed = false
        if (state.leases == 0) {
            state.original = UIScreen.mainScreen.brightness
            state.active = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
        }
        state.leases++
        state.apply()
        val center = NSNotificationCenter.defaultCenter
        val background = center.addObserverForName(UIApplicationWillResignActiveNotification, null, NSOperationQueue.mainQueue) {
            if (!disposed) { state.active = false; state.apply() }
        }
        val foreground = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) {
            if (!disposed) { state.active = true; state.apply() }
        }
        onDispose {
            disposed = true
            center.removeObserver(background)
            center.removeObserver(foreground)
            state.leases--
            state.apply()
        }
    }
}
