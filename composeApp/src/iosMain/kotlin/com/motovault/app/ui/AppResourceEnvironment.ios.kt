package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

@Composable
internal actual fun PlatformResourceEnvironment(language: AppLanguage, content: @Composable () -> Unit) {
    remember(language) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(language.code), forKey = APPLE_LANGUAGES_KEY)
    }
    content()
}
