package com.motovault.app.storage.security

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.motovault.app.ui.CaptureOcrCorpus
import com.motovault.app.ui.checkCaptureOwner
import kotlinx.coroutines.runBlocking

internal fun Instrumentation.runCaptureProbe(captureOnly: Boolean, lifetimeOnly: Boolean, ownerOnly: Boolean): Boolean {
    if (!captureOnly && !lifetimeOnly && !ownerOnly) return false
    val report = Bundle()
    try {
        check(targetContext.packageName.endsWith(".privacyprobe"))
        val output = if (ownerOnly) checkCaptureOwner() else runBlocking {
            if (lifetimeOnly) CaptureOcrCorpus.runLifetime() else CaptureOcrCorpus.run()
        }
        report.putString("stream", output)
        finish(Activity.RESULT_OK, report)
    } catch (failure: Throwable) {
        report.putString("stream", "FAIL capture corpus: ${failure.stackTraceToString()}")
        finish(Activity.RESULT_CANCELED, report)
    }
    return true
}
