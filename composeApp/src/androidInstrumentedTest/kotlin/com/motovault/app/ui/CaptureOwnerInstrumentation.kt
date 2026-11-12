package com.motovault.app.ui

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import com.motovault.app.CaptureFixtureActivity
import java.io.File

internal fun Instrumentation.checkCaptureOwner(): String {
    val activity = startActivitySync(Intent(targetContext, CaptureFixtureActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("captureOwner", true)) as CaptureFixtureActivity
    try {
        awaitCapture { activity.captureOwner != null && activity.consumerStarts == 1 }
        val capture = requireNotNull(activity.captureOwner)
        runOnMainSync { capture.start() }
        awaitCapture { activity.consumerStops >= 1 && uiAutomation.rootInActiveWindow?.packageName != targetContext.packageName }
        check(capture.open)
        repeat(3) {
            if (capture.open) {
                uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemClock.sleep(500)
            }
        }
        awaitCapture { !capture.open && activity.consumerStarts >= 2 }
        check(activity.captureOwner === capture)
        check(capture.photo == null && capture.error == null)
        check(File(targetContext.cacheDir, "odometer-capture").listFiles().orEmpty().isEmpty())
        return "PASS actual external camera pause, consumer disposal/remount, retained root callback, native cancellation and empty scratch cache\n"
    } finally { runOnMainSync { activity.finish() } }
}

private fun awaitCapture(condition: () -> Boolean) {
    val deadline = SystemClock.elapsedRealtime() + 20_000
    while (!condition()) {
        check(SystemClock.elapsedRealtime() < deadline) { "Capture owner lifecycle checkpoint timed out" }
        SystemClock.sleep(100)
    }
}
