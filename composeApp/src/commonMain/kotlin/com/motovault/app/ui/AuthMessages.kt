package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.privacy.SystemAuthStatus
import motovault.composeapp.generated.resources.*

/** Platform-neutral authentication copy; native code returns a status, the UI owns the language. */
internal data class AuthMessages(
    val noCredentials: String,
    val lockedOut: String,
    val cancelled: String,
    val failed: String
) {
    fun forStatus(status: SystemAuthStatus): String = when (status) {
        SystemAuthStatus.NO_CREDENTIALS -> noCredentials
        SystemAuthStatus.LOCKED_OUT -> lockedOut
        SystemAuthStatus.CANCELLED -> cancelled
        SystemAuthStatus.FAILED -> failed
        else -> ""
    }
}

@Composable
internal fun rememberAuthMessages(): AuthMessages = AuthMessages(
    noCredentials = appStringResource(Res.string.auth_no_credentials),
    lockedOut = appStringResource(Res.string.auth_locked_out),
    cancelled = appStringResource(Res.string.auth_cancelled),
    failed = appStringResource(Res.string.auth_failed)
)
