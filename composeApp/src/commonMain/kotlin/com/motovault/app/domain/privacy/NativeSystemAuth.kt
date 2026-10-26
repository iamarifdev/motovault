package com.motovault.app.domain.privacy

enum class SystemAuthStatus {
    AVAILABLE,
    SUCCESS,
    CANCELLED,
    NO_CREDENTIALS,
    LOCKED_OUT,
    FAILED
}

data class SystemAuthResult(
    val status: SystemAuthStatus,
    val message: String? = null
) {
    val isSuccess: Boolean get() = status == SystemAuthStatus.SUCCESS
}

expect object NativeSystemAuth {
    fun availability(): SystemAuthStatus
    suspend fun authenticate(title: String, subtitle: String): SystemAuthResult
    fun setProbeResult(result: SystemAuthResult?)
}
