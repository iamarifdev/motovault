package com.motovault.app

import androidx.compose.ui.window.ComposeUIViewController
import com.motovault.app.ui.redesign.IosHostAppearance
import platform.UIKit.UIViewController

/** The host owns UIKit chrome; Compose keeps the device trait for System mode. */
fun MainViewController(onDarkAppearance: (Boolean) -> Unit): UIViewController {
    IosHostAppearance.onChange = onDarkAppearance
    return ComposeUIViewController { App() }
}
