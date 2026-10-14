package com.motovault.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.runtime.ExperimentalComposeApi
import com.motovault.app.ui.IosHostAppearance
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.UIKit.UIViewController

/** The host owns UIKit chrome; Compose keeps the device trait for System mode. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeApi::class, ExperimentalNativeApi::class)
fun MainViewController(onDarkAppearance: (Boolean) -> Unit): UIViewController {
    IosHostAppearance.onChange = onDarkAppearance
    return ComposeUIViewController(configure = {
        if (Platform.isDebugBinary) {
            accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
        }
    }) { App() }
}
