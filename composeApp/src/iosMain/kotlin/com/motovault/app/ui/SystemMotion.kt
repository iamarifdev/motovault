package com.motovault.app.ui

import androidx.compose.runtime.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
import platform.UIKit.UIApplicationDidBecomeActiveNotification

@Composable
internal actual fun systemReducedMotion(): Boolean {
    var reduced by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(UIAccessibilityReduceMotionStatusDidChangeNotification, UIApplicationDidBecomeActiveNotification).map { name ->
            center.addObserverForName(name, null, NSOperationQueue.mainQueue) {
                reduced = UIAccessibilityIsReduceMotionEnabled()
            }
        }
        reduced = UIAccessibilityIsReduceMotionEnabled()
        onDispose { observers.forEach(center::removeObserver) }
    }
    return reduced
}
