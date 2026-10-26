package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect

internal object IosHostAppearance {
    var onChange: (Boolean) -> Unit = {}
    var appDark = false
    private val documents = linkedMapOf<Any, Boolean>()
    fun publish() = onChange(documents.values.lastOrNull() ?: appDark)
    fun document(token: Any, dark: Boolean) { documents[token] = dark; publish() }
    fun remove(token: Any) { documents.remove(token); publish() }
}

@Composable
actual fun PlatformAppearance(dark: Boolean) {
    SideEffect { IosHostAppearance.appDark = dark; IosHostAppearance.publish() }
}

@Composable
actual fun DocumentWindowAppearance(dark: Boolean) {
    DisposableEffect(dark) {
        val token = Any()
        IosHostAppearance.document(token, dark)
        onDispose { IosHostAppearance.remove(token) }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
actual fun documentDialogProperties() = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, usePlatformInsets = false)
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal actual fun sheetDialogProperties() = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, usePlatformInsets = false)

@Composable
internal actual fun SheetWindowPolicy() = Unit
