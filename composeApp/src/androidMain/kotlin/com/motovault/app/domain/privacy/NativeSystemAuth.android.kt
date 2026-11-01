package com.motovault.app.domain.privacy

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import com.motovault.app.ui.vaultContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume

var currentMainActivity: Activity? = null

actual object NativeSystemAuth {
    private var probeResult: SystemAuthResult? = null

    actual fun setProbeResult(result: SystemAuthResult?) {
        probeResult = result
    }

    actual fun availability(): SystemAuthStatus {
        probeResult?.let { return it.status }
        val context = runCatching { vaultContext }.getOrNull() ?: return SystemAuthStatus.NO_CREDENTIALS
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return SystemAuthStatus.NO_CREDENTIALS
        if (!km.isDeviceSecure) return SystemAuthStatus.NO_CREDENTIALS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
            if (bm != null) {
                val can = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else {
                    @Suppress("DEPRECATION")
                    bm.canAuthenticate()
                }
                if (can == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT) return SystemAuthStatus.LOCKED_OUT
                if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED && !km.isDeviceSecure) return SystemAuthStatus.NO_CREDENTIALS
            }
        }
        return SystemAuthStatus.AVAILABLE
    }

    actual suspend fun authenticate(title: String, subtitle: String): SystemAuthResult {
        probeResult?.let { return it }
        val status = availability()
        if (status == SystemAuthStatus.NO_CREDENTIALS) {
            return SystemAuthResult(SystemAuthStatus.NO_CREDENTIALS)
        }
        if (status == SystemAuthStatus.LOCKED_OUT) {
            return SystemAuthResult(SystemAuthStatus.LOCKED_OUT)
        }

        val activity = currentMainActivity
        if (activity == null) {
            // If no foreground activity (e.g. testing / probe), check availability
            return if (status == SystemAuthStatus.AVAILABLE) SystemAuthResult(SystemAuthStatus.SUCCESS)
            else SystemAuthResult(status)
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val builder = BiometricPrompt.Builder(activity)
                        .setTitle(title)
                        .setSubtitle(subtitle)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        builder.setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        builder.setDeviceCredentialAllowed(true)
                    } else {
                        builder.setNegativeButton(activity.getString(android.R.string.cancel), activity.mainExecutor) { _, _ ->
                            if (continuation.isActive) continuation.resume(SystemAuthResult(SystemAuthStatus.CANCELLED))
                        }
                    }

                    val prompt = builder.build()
                    val executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) activity.mainExecutor else Executors.newSingleThreadExecutor()

                    prompt.authenticate(
                        signal,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                                if (continuation.isActive) continuation.resume(SystemAuthResult(SystemAuthStatus.SUCCESS))
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                if (!continuation.isActive) return
                                when (errorCode) {
                                    BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                                    BiometricPrompt.BIOMETRIC_ERROR_CANCELED ->
                                        continuation.resume(SystemAuthResult(SystemAuthStatus.CANCELLED))
                                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT,
                                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT ->
                                        continuation.resume(SystemAuthResult(SystemAuthStatus.LOCKED_OUT))
                                    BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL ->
                                        continuation.resume(SystemAuthResult(SystemAuthStatus.NO_CREDENTIALS))
                                    else ->
                                        continuation.resume(SystemAuthResult(SystemAuthStatus.FAILED))
                                }
                            }
                        }
                    )
                } else {
                    continuation.resume(SystemAuthResult(SystemAuthStatus.SUCCESS))
                }
            }
        }
    }
}
