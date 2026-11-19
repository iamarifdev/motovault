package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
internal actual fun rememberPlatformHaptic(enabled: Boolean): VaultHaptic {
    val selection = remember { UISelectionFeedbackGenerator() }
    val notification = remember { UINotificationFeedbackGenerator() }
    return remember(enabled, selection, notification) {
        VaultHaptic(
            enabled = enabled,
            performer = { kind ->
                when (kind) {
                    HapticKind.TICK -> selection.selectionChanged()
                    HapticKind.SUCCESS -> notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
                    HapticKind.WARNING -> notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
                    HapticKind.ERROR -> notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
                }
            }
        )
    }
}
