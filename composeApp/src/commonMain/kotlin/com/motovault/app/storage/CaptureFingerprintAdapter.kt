package com.motovault.app.storage

import com.motovault.app.application.capture.CaptureFingerprint
import com.motovault.app.storage.security.documentSha256

internal object CaptureFingerprintAdapter : CaptureFingerprint {
    override fun digest(canonicalText: String): String = documentSha256(canonicalText.encodeToByteArray())
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
