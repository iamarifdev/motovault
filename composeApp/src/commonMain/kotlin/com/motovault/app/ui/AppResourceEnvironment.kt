package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Applies the persisted application language to shared Compose resources. */
@Composable
internal fun AppResourceEnvironment(language: AppLanguage, content: @Composable () -> Unit) {
    PlatformResourceEnvironment(language) {
        CompositionLocalProvider(LocalResourceLanguage provides language, content = content)
    }
}

private val LocalResourceLanguage = staticCompositionLocalOf { AppLanguage.BN }

/** Refresh only the resource lookup when language changes; retain screen and sheet state. */
@Composable
internal fun appStringResource(resource: StringResource, vararg args: Any): String {
    val language = LocalResourceLanguage.current
    return key(language, args.toList()) { stringResource(resource, *args) }
}

@Composable
internal fun appPluralStringResource(resource: PluralStringResource, quantity: Int, vararg args: Any): String {
    val language = LocalResourceLanguage.current
    return key(language, quantity, args.toList()) { pluralStringResource(resource, quantity, *args) }
}

@Composable
internal expect fun PlatformResourceEnvironment(language: AppLanguage, content: @Composable () -> Unit)
