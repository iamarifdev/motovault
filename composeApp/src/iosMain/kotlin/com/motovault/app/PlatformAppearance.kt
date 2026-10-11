package com.motovault.app.ui.redesign

import androidx.compose.runtime.Composable

// UIKit status-bar integration belongs to the iOS host, which is not in this checkout.
@Composable
actual fun PlatformAppearance(dark: Boolean) = Unit

@Composable
actual fun DocumentWindowAppearance(dark: Boolean) = Unit

actual fun documentDialogProperties() = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
