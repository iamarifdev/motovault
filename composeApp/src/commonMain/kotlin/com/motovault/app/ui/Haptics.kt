package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class HapticKind {
    TICK,
    SUCCESS,
    WARNING,
    ERROR
}

class VaultHaptic(
    private val performer: (HapticKind) -> Unit = {},
    private val enabled: Boolean = true
) {
    fun perform(kind: HapticKind) {
        if (!enabled) return
        performer(kind)
    }
}

val LocalVaultHaptic = staticCompositionLocalOf { VaultHaptic() }

val currentHaptic: VaultHaptic
    @Composable get() = LocalVaultHaptic.current

@Composable
internal expect fun rememberPlatformHaptic(enabled: Boolean): VaultHaptic
