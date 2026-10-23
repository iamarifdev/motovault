package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

enum class HapticKind {
    TICK,
    SUCCESS,
    WARNING,
    ERROR
}

class VaultHaptic(private val feedback: HapticFeedback?, private val enabled: Boolean) {
    fun perform(kind: HapticKind) {
        if (!enabled || feedback == null) return
        when (kind) {
            HapticKind.TICK -> feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            HapticKind.SUCCESS -> feedback.performHapticFeedback(HapticFeedbackType.LongPress)
            HapticKind.WARNING -> feedback.performHapticFeedback(HapticFeedbackType.LongPress)
            HapticKind.ERROR -> feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

val LocalVaultHaptic = staticCompositionLocalOf { VaultHaptic(null, true) }

val currentHaptic: VaultHaptic
    @Composable get() = LocalVaultHaptic.current
