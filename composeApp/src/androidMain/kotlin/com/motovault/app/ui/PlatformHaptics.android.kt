package com.motovault.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberPlatformHaptic(enabled: Boolean): VaultHaptic {
    val view = LocalView.current
    return remember(view, enabled) {
        VaultHaptic(
            enabled = enabled,
            performer = { kind ->
                val constant = when (kind) {
                    HapticKind.TICK -> HapticFeedbackConstants.CLOCK_TICK
                    HapticKind.SUCCESS -> if (Build.VERSION.SDK_INT >= 30) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                    HapticKind.WARNING -> HapticFeedbackConstants.LONG_PRESS
                    HapticKind.ERROR -> if (Build.VERSION.SDK_INT >= 30) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                }
                view.performHapticFeedback(constant)
            }
        )
    }
}
