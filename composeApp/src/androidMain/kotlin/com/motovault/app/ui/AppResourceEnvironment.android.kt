package com.motovault.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Suppress("DEPRECATION")
@Composable
internal actual fun PlatformResourceEnvironment(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val configuration = remember(systemConfiguration, language) {
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)
        Configuration(systemConfiguration).apply { setLocale(locale) }
    }
    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    CompositionLocalProvider(LocalConfiguration provides configuration) {
        content()
    }
}
