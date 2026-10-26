package com.motovault.app.domain.privacy

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorAppCancel
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual object NativeSystemAuth {
    private var probeResult: SystemAuthResult? = null

    actual fun setProbeResult(result: SystemAuthResult?) {
        probeResult = result
    }

    actual fun availability(): SystemAuthStatus {
        probeResult?.let { return it.status }
        val context = LAContext()
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val can = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = error.ptr)
            if (can) return SystemAuthStatus.AVAILABLE
            val nsError = error.value
            return when (nsError?.code) {
                LAErrorPasscodeNotSet -> SystemAuthStatus.NO_CREDENTIALS
                LAErrorBiometryLockout -> SystemAuthStatus.LOCKED_OUT
                else -> SystemAuthStatus.FAILED
            }
        }
    }

    actual suspend fun authenticate(title: String, subtitle: String): SystemAuthResult {
        probeResult?.let { return it }
        val status = availability()
        if (status == SystemAuthStatus.NO_CREDENTIALS) {
            return SystemAuthResult(SystemAuthStatus.NO_CREDENTIALS, "ফোনে স্ক্রিন লক নেই। ফোনের সেটিংসে PIN বা পাসকোড দিন। আগের তথ্য রাখা আছে।")
        }
        if (status == SystemAuthStatus.LOCKED_OUT) {
            return SystemAuthResult(SystemAuthStatus.LOCKED_OUT, "বায়োমেট্রিক আপাতত বন্ধ। ফোনের পাসকোড ব্যবহার করুন।")
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val context = LAContext()
                context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = title) { success, nsError ->
                    if (!continuation.isActive) return@evaluatePolicy
                    if (success) {
                        continuation.resume(SystemAuthResult(SystemAuthStatus.SUCCESS))
                    } else {
                        when (nsError?.code) {
                            LAErrorUserCancel, LAErrorSystemCancel, LAErrorAppCancel ->
                                continuation.resume(SystemAuthResult(SystemAuthStatus.CANCELLED, "যাচাই বাতিল হয়েছে।"))
                            LAErrorPasscodeNotSet ->
                                continuation.resume(SystemAuthResult(SystemAuthStatus.NO_CREDENTIALS, "ফোনে স্ক্রিন লক নেই। ফোনের সেটিংসে PIN বা পাসকোড দিন। আগের তথ্য রাখা আছে।"))
                            LAErrorBiometryLockout ->
                                continuation.resume(SystemAuthResult(SystemAuthStatus.LOCKED_OUT, "বায়োমেট্রিক আপাতত বন্ধ। ফোনের পাসকোড ব্যবহার করুন।"))
                            else ->
                                continuation.resume(SystemAuthResult(SystemAuthStatus.FAILED, nsError?.localizedDescription ?: "যাচাই ব্যর্থ হয়েছে।"))
                        }
                    }
                }
            }
        }
    }
}
